/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.run.mojo;

import br.com.dizeno.reins.source.scanner.CoderMdScanner;
import br.com.dizeno.reins.source.scanner.SourceDiscoveryMode;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.validation.ExplicitSourceValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * ExplicitSourceResolver is part of the general application functions in the reins architecture.
 * Acts as a helper utility for resolving explicit source compilation arguments.
 */
public class ExplicitSourceResolver {

    private final ExplicitSourceValidator validator;

    public ExplicitSourceResolver() {
        this(new ExplicitSourceValidator());
    }

    ExplicitSourceResolver(ExplicitSourceValidator validator) {
        this.validator = validator;
    }

    public ResolutionResult resolve(String source,
                                    Path projectRoot,
                                    Map<String, File> sourceBases,
                                    String includePattern,
                                    PathValidator pathValidator) {
        List<String> sources = validator.validateSourceInputs(source);
        return resolve(sources, projectRoot, sourceBases, includePattern, pathValidator);
    }

    public ResolutionResult resolve(List<String> sources,
                                    Path projectRoot,
                                    Map<String, File> sourceBases,
                                    String includePattern,
                                    PathValidator pathValidator) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("System property 'source' must not be empty.");
        }

        java.util.Set<File> aggregatedFiles = new java.util.LinkedHashSet<>();
        java.util.Set<Path> aggregatedPaths = new java.util.LinkedHashSet<>();
        boolean anyDirectory = false;

        for (String sourceInput : sources) {
            SingleResolutionResult single = resolveSingle(sourceInput, projectRoot, sourceBases, includePattern, pathValidator);
            aggregatedFiles.addAll(single.files());
            aggregatedPaths.add(single.resolvedPath());
            if (single.directory()) {
                anyDirectory = true;
            }
        }

        List<File> filesList = new ArrayList<>(aggregatedFiles);
        List<Path> pathsList = new ArrayList<>(aggregatedPaths);
        Path primaryPath = pathsList.isEmpty() ? null : pathsList.get(0);

        return new ResolutionResult(
                SourceDiscoveryMode.EXPLICIT_SOURCE,
                primaryPath,
                anyDirectory,
                filesList,
                pathsList
        );
    }

    private SingleResolutionResult resolveSingle(String sourceInput,
                                                 Path projectRoot,
                                                 Map<String, File> sourceBases,
                                                 String includePattern,
                                                 PathValidator pathValidator) {
        String validated = validator.validateSourceInput(sourceInput);
        String baseScheme = null;
        String subPath = validated;

        int colonIdx = validated.indexOf(':');
        if (colonIdx > 0 && !validated.contains("://")) {
            baseScheme = validated.substring(0, colonIdx).toLowerCase(java.util.Locale.ROOT);
            subPath = validated.substring(colonIdx + 1);
        }
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }

        List<Path> searchBases = new ArrayList<>();
        if (baseScheme != null) {
            File baseFile = sourceBases != null ? sourceBases.get(baseScheme) : null;
            if (baseFile == null) {
                throw new IllegalArgumentException("Explicit source base '" + baseScheme + "' is not configured in source." + baseScheme);
            }
            if (baseFile.exists()) {
                searchBases.add(pathValidator.validateInProject(baseFile.toPath()));
            }
        } else {
            if (sourceBases != null) {
                File mainFile = sourceBases.get("main");
                if (mainFile != null && mainFile.exists()) {
                    searchBases.add(pathValidator.validateInProject(mainFile.toPath()));
                }
                File testFile = sourceBases.get("test");
                if (testFile != null && testFile.exists()) {
                    searchBases.add(pathValidator.validateInProject(testFile.toPath()));
                }
            }
        }

        if (searchBases.isEmpty()) {
            throw new IllegalArgumentException("No eligible source bases found to resolve explicit source '" + sourceInput + "'.");
        }

        if (containsGlobWildcards(subPath)) {
            List<File> globMatchedFiles = resolveGlobPattern(subPath, projectRoot, searchBases, includePattern, pathValidator);
            if (globMatchedFiles.isEmpty()) {
                throw new IllegalArgumentException(
                        "Explicit source '" + validated + "' does not resolve to an eligible directory or file under configured source bases.");
            }
            Path primaryPath = globMatchedFiles.get(0).toPath();
            return new SingleResolutionResult(primaryPath, true, globMatchedFiles);
        }

        for (Path root : searchBases) {
            Path effectiveSubPath = Path.of(subPath);
            try {
                Path relRoot = projectRoot.toAbsolutePath().normalize().relativize(root.toAbsolutePath().normalize());
                if (effectiveSubPath.startsWith(relRoot)) {
                    effectiveSubPath = relRoot.relativize(effectiveSubPath);
                }
            } catch (Exception ignored) {
            }
            Path targetDir = pathValidator.validateInProject(root.resolve(effectiveSubPath));
            if (Files.isDirectory(targetDir)) {
                List<File> allBaseFiles = new CoderMdScanner().scan(List.of(root.toFile()), includePattern, pathValidator);
                Path targetDirAbs = targetDir.toAbsolutePath().normalize();
                List<File> files = allBaseFiles.stream()
                        .filter(f -> f.toPath().toAbsolutePath().normalize().startsWith(targetDirAbs))
                        .toList();
                return new SingleResolutionResult(targetDir, true, files);
            }
        }

        List<Path> fileMatches = new ArrayList<>();
        for (Path root : searchBases) {
            Path effectiveSubPath = Path.of(subPath);
            try {
                Path relRoot = projectRoot.toAbsolutePath().normalize().relativize(root.toAbsolutePath().normalize());
                if (effectiveSubPath.startsWith(relRoot)) {
                    effectiveSubPath = relRoot.relativize(effectiveSubPath);
                }
            } catch (Exception ignored) {
            }
            Path targetFile = pathValidator.validateInProject(root.resolve(effectiveSubPath));
            if (Files.isRegularFile(targetFile)) {
                fileMatches.add(targetFile);
            }
        }

        if (fileMatches.isEmpty()) {
            String fileName = new File(subPath).getName();
            for (Path root : searchBases) {
                List<Path> matches = findFilesByName(root, fileName);
                fileMatches.addAll(matches);
                if (!fileMatches.isEmpty()) {
                    break;
                }
            }
        }

        validator.validateSingleMatch(fileMatches, validated);

        if (fileMatches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Explicit source '" + validated + "' does not resolve to an eligible directory or file under configured source bases.");
        }

        return new SingleResolutionResult(fileMatches.get(0), false, List.of(fileMatches.get(0).toFile()));
    }

    private boolean containsGlobWildcards(String str) {
        if (str == null) return false;
        return str.contains("*") || str.contains("?") || str.contains("[") || str.contains("{");
    }

    private List<File> resolveGlobPattern(String subPathPattern,
                                          Path projectRoot,
                                          List<Path> searchBases,
                                          String includePattern,
                                          PathValidator pathValidator) {
        List<File> matchedFiles = new ArrayList<>();
        java.util.Set<Path> visited = new java.util.LinkedHashSet<>();

        for (Path root : searchBases) {
            if (!Files.exists(root) || !Files.isDirectory(root)) {
                continue;
            }

            String effectivePattern = subPathPattern.replace('\\', '/');
            try {
                Path relRoot = projectRoot.toAbsolutePath().normalize().relativize(root.toAbsolutePath().normalize());
                String relRootStr = br.com.dizeno.reins.util.PathNormalizer.toForwardSlashes(relRoot.toString());
                if (!relRootStr.isEmpty() && (effectivePattern.startsWith(relRootStr + "/") || effectivePattern.equals(relRootStr))) {
                    effectivePattern = effectivePattern.substring(relRootStr.length());
                    if (effectivePattern.startsWith("/")) {
                        effectivePattern = effectivePattern.substring(1);
                    }
                }
            } catch (Exception ignored) {
            }

            final String patternToMatch = effectivePattern;
            final boolean hasSlashOrDoubleStar = patternToMatch.contains("/") || patternToMatch.contains("**");

            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .forEach(p -> {
                            Path validPath = pathValidator.validateInProject(p);
                            Path normalizedValid = validPath.toAbsolutePath().normalize();
                            if (visited.contains(normalizedValid)) {
                                return;
                            }
                            Path relToRoot = root.toAbsolutePath().normalize().relativize(normalizedValid);
                            String relPathStr = br.com.dizeno.reins.util.PathNormalizer.toForwardSlashes(relToRoot.toString());
                            String fileName = validPath.getFileName().toString();

                            boolean matchesGlob = br.com.dizeno.reins.compilation.context.GlobPatternMatcher.matchPath(patternToMatch, relPathStr);
                            if (!matchesGlob && !hasSlashOrDoubleStar) {
                                matchesGlob = br.com.dizeno.reins.compilation.context.GlobPatternMatcher.matchPath(patternToMatch, fileName);
                            }

                            if (matchesGlob) {
                                if (includePattern != null && !includePattern.isBlank()) {
                                    boolean matchesInclude = br.com.dizeno.reins.compilation.context.GlobPatternMatcher.matchPath(includePattern, relPathStr)
                                            || br.com.dizeno.reins.compilation.context.GlobPatternMatcher.matchPath(includePattern, fileName);
                                    if (matchesInclude) {
                                        visited.add(normalizedValid);
                                        matchedFiles.add(validPath.toFile());
                                    }
                                } else {
                                    visited.add(normalizedValid);
                                    matchedFiles.add(validPath.toFile());
                                }
                            }
                        });
            } catch (IOException ignored) {
            }
        }

        matchedFiles.sort((a, b) -> a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath()));
        return matchedFiles;
    }

    private List<Path> findFilesByName(Path root, String fileName) {
        List<Path> matches = new ArrayList<>();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return matches;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .forEach(matches::add);
        } catch (IOException e) {
            // ignore
        }
        return matches;
    }

    private record SingleResolutionResult(
            Path resolvedPath,
            boolean directory,
            List<File> files
    ) {
    }

    public record ResolutionResult(
            SourceDiscoveryMode mode,
            Path resolvedPath,
            boolean directory,
            List<File> files,
            List<Path> resolvedPaths
    ) {
        public ResolutionResult(SourceDiscoveryMode mode, Path resolvedPath, boolean directory, List<File> files) {
            this(mode, resolvedPath, directory, files, resolvedPath != null ? List.of(resolvedPath) : List.of());
        }
    }
}
