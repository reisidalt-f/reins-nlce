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

package br.com.dizeno.reins.compilation.pipeline;

import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

 
/**
 * PathHelper is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a helper utility assisting in its prefix tasks.
 */
public final class PathHelper {

    private PathHelper() {}

    

    /**
     * Resolves the configured value or path source category.
     *
     * @param sourcePath the path of the source file
     * @param projectRoot the root path of the project
     * @return the string result
     */
    /**
     * Resolves the configured value or path source category.
     *
     * @param sourcePath the path of the source file
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public static String resolveSourceCategory(Path sourcePath, Path projectRoot) {
        return resolveSourceCategory(sourcePath, null, projectRoot);
    }

    /**
     * Resolves the configured value or path source category using configuration settings.
     *
     * @param sourcePath the path of the source file
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public static String resolveSourceCategory(Path sourcePath, ReinsConfig config, Path projectRoot) {
        if (sourcePath == null) {
            return SourceScope.UNCLASSIFIED.value();
        }
        Path normalized = sourcePath.toAbsolutePath().normalize();
        Path normalizedProjectRoot = projectRoot == null ? Path.of(".") : projectRoot.toAbsolutePath().normalize();
        if (config != null && config.getSourceBases() != null) {
            for (java.util.Map.Entry<String, java.io.File> entry : config.getSourceBases().entrySet()) {
                if (entry.getValue() != null) {
                    Path baseDir = entry.getValue().isAbsolute()
                            ? entry.getValue().toPath().toAbsolutePath().normalize()
                            : normalizedProjectRoot.resolve(entry.getValue().toPath()).toAbsolutePath().normalize();
                    if (normalized.startsWith(baseDir)) {
                        return entry.getKey().toLowerCase(java.util.Locale.ROOT);
                    }
                }
            }
        }
        String relative = PathNormalizer.toForwardSlashes(normalizedProjectRoot.relativize(normalized).toString());
        return SourceScope.fromPath(relative).value();
    }

    /**
     * Resolves the configured value or path target root for source category.
     *
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static String resolveTargetRootForSourceCategory(ReinsConfig config,
                                                             Path projectRoot,
                                                             String sourceCategory) {
        return PathNormalizer.toForwardSlashes(
                projectRoot.relativize(BasePathMappingSet.forScope(config, projectRoot, sourceCategory).getTargetRoot()).toString());
    }

    public static boolean isMainOrTestScope(String sourceCategory) {
        return sourceCategory != null && !sourceCategory.isBlank() && !"unclassified".equalsIgnoreCase(sourceCategory);
    }

    /**
     * Resolves the configured value or path main source qualified path.
     *
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static String resolveMainSourceQualifiedPath(String canonicalSourcePath,
                                                         String sourceCategory) {
        return resolveMainSourceQualifiedPath(canonicalSourcePath, sourceCategory, null, null);
    }

    public static String resolveMainSourceQualifiedPath(String canonicalSourcePath,
                                                         String sourceCategory,
                                                         ReinsConfig config,
                                                         Path projectRoot) {
        if (TrackedPathResolver.looksCanonical(canonicalSourcePath)) {
            return canonicalSourcePath;
        }
        String base = (sourceCategory != null && !sourceCategory.isBlank()) ? sourceCategory.toLowerCase(java.util.Locale.ROOT) : "main";
        String relative = canonicalSourcePath == null ? "" : canonicalSourcePath;
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }

        if (config != null && config.getSourceBases() != null && config.getSourceBases().containsKey(base)) {
            java.io.File baseFile = config.getSourceBases().get(base);
            if (baseFile != null) {
                String baseRelPath = PathNormalizer.toForwardSlashes(baseFile.getPath());
                while (baseRelPath.startsWith("/")) baseRelPath = baseRelPath.substring(1);
                while (baseRelPath.endsWith("/")) baseRelPath = baseRelPath.substring(0, baseRelPath.length() - 1);
                if (!baseRelPath.isBlank() && relative.startsWith(baseRelPath + "/")) {
                    relative = relative.substring(baseRelPath.length() + 1);
                }
            }
        } else if ("main".equals(base) && relative.startsWith("src/main/nl/")) {
            relative = relative.substring("src/main/nl/".length());
        } else if ("test".equals(base) && relative.startsWith("src/test/nl/")) {
            relative = relative.substring("src/test/nl/".length());
        }

        return base + ":" + relative;
    }

    /**
     * Formats a source path as base:relativePathUnderBase (e.g. main:com/notesmanager/services/sorting-service.md).
     *
     * @param sourcePath the path of the source file
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public static String formatBaseRelativePath(Path sourcePath, ReinsConfig config, Path projectRoot) {
        if (sourcePath == null) {
            return "";
        }
        Path normalizedSource = sourcePath.toAbsolutePath().normalize();
        Path normalizedProjectRoot = projectRoot == null ? Path.of(".") : projectRoot.toAbsolutePath().normalize();
        if (config != null && config.getSourceBases() != null) {
            for (java.util.Map.Entry<String, java.io.File> entry : config.getSourceBases().entrySet()) {
                if (entry.getValue() != null) {
                    Path baseDir = entry.getValue().isAbsolute()
                            ? entry.getValue().toPath().toAbsolutePath().normalize()
                            : normalizedProjectRoot.resolve(entry.getValue().toPath()).toAbsolutePath().normalize();
                    if (normalizedSource.startsWith(baseDir)) {
                        Path rel = baseDir.relativize(normalizedSource);
                        String relStr = PathNormalizer.toForwardSlashes(rel.toString());
                        if (relStr.startsWith("/")) {
                            relStr = relStr.substring(1);
                        }
                        return entry.getKey() + ":" + relStr;
                    }
                }
            }
        }
        if (normalizedSource.startsWith(normalizedProjectRoot)) {
            return PathNormalizer.toForwardSlashes(normalizedProjectRoot.relativize(normalizedSource).toString());
        }
        return PathNormalizer.toForwardSlashes(normalizedSource.toString());
    }

    

    /**
     * Canonicalize Tracked Artifact Paths.
     *
     * @param paths the paths
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static List<String> canonicalizeTrackedArtifactPaths(List<String> paths,
                                                                ReinsConfig config,
                                                                Path projectRoot,
                                                                String sourceCategory) {
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }

        BasePathResolver resolver = new BasePathResolver(
                BasePathMappingSet.forScope(config, projectRoot, sourceCategory),
                new PathValidator(projectRoot));
        List<String> canonicalPaths = new ArrayList<>(paths.size());
        for (String path : paths) {
            canonicalPaths.add(canonicalizeTrackedArtifactPath(path, projectRoot, resolver));
        }
        return canonicalPaths;
    }

    

    /**
     * Canonicalize Tracked Artifact Path.
     *
     * @param path the file or directory path
     * @param projectRoot the root path of the project
     * @param resolver the resolver
     * @return the string result
     */
    public static String canonicalizeTrackedArtifactPath(String path,
                                                          Path projectRoot,
                                                          BasePathResolver resolver) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Tracked artifact path cannot be null or blank.");
        }
        if (TrackedPathResolver.looksCanonical(path)) {
            try {
                FileReference ref = FileReference.fromCanonical(path);
                Path resolved = resolver.resolve(ref);
                if (java.nio.file.Files.exists(resolved)) {
                    return resolver.qualifyAbsolute(resolved);
                }
            } catch (Exception ignored) {
            }
            return FileReference.fromCanonical(path).toCanonicalString();
        }

        Path absolutePath = Path.of(path);
        if (!absolutePath.isAbsolute()) {
            absolutePath = projectRoot.resolve(path);
        }
        return resolver.qualifyAbsolute(absolutePath.toAbsolutePath().normalize());
    }
}
