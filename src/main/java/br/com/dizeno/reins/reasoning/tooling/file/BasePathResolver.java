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

package br.com.dizeno.reins.reasoning.tooling.file;

import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.FileReferenceBase;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;

/**
 * BasePathResolver is part of the general application functions in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class BasePathResolver {
    private final BasePathMappingSet mappings;
    private final PathValidator pathValidator;

    /**
     * Constructs a new instance of {@link BasePathResolver}.
     *
     * @param mappings the mappings
     * @param pathValidator the path validator
     */
    public BasePathResolver(BasePathMappingSet mappings, PathValidator pathValidator) {
        this.mappings = mappings;
        this.pathValidator = pathValidator;
    }

     
    /**
     * Gets the mappings.
     *
     * @return the collection of elements
     */
    public BasePathMappingSet getMappings() {
        return mappings;
    }

    /**
     * Resolves the configured value or path.
     *
     * @param base the base
     * @param relativePath the relative path
     * @return the resolved or constructed object
     */
    public Path resolve(String base, String relativePath) {
        return resolve(toFileReference(base, relativePath));
    }

    /**
     * Resolves the configured value or path.
     *
     * @param reference the reference
     * @return the resolved or constructed object
     */
    public Path resolve(FileReference reference) {
        FileReference normalizedRef = sanitizeTargetReference(reference);
        Path candidate = normalizedRef.toAbsolutePathByBaseName(this::resolveBaseRoot);
        return pathValidator.validateInProject(candidate);
    }

    private FileReference sanitizeTargetReference(FileReference reference) {
        if (reference == null || reference.getBase() != br.com.dizeno.reins.source.domain.FileReferenceBase.TARGET) {
            return reference;
        }
        Path targetRoot = resolveBaseRoot(br.com.dizeno.reins.source.domain.FileReferenceBase.TARGET);
        if (targetRoot == null) {
            return reference;
        }
        try {
            Path projectRoot = getProjectRoot();
            if (targetRoot.startsWith(projectRoot) && !targetRoot.equals(projectRoot)) {
                String targetRel = PathNormalizer.toForwardSlashes(projectRoot.relativize(targetRoot).toString());
                while (targetRel.startsWith("/")) targetRel = targetRel.substring(1);
                while (targetRel.endsWith("/")) targetRel = targetRel.substring(0, targetRel.length() - 1);

                String path = reference.getPath();
                if (!targetRel.isBlank() && path != null && path.startsWith(targetRel + "/")) {
                    return new FileReference(br.com.dizeno.reins.source.domain.FileReferenceBase.TARGET, path.substring(targetRel.length() + 1));
                }
            }
        } catch (Exception ignored) {
        }
        return reference;
    }

    /**
     * Resolves the configured value or path active output base.
     *
     * @param preferredBase the preferred base
     * @return the string result
     */
    public String resolveActiveOutputBase(String preferredBase) {
        if (preferredBase == null || preferredBase.isBlank()) {
            return "target";
        }
        return normalizeBase(preferredBase);
    }

    /**
     * Resolves the configured value or path active output base root.
     *
     * @param preferredBase the preferred base
     * @return the resolved or constructed object
     */
    public Path resolveActiveOutputBaseRoot(String preferredBase) {
        return resolveBaseRoot(resolveActiveOutputBase(preferredBase));
    }

    /**
     * Checks if the component is within base root.
     *
     * @param candidate the candidate
     * @param base the base
     * @return true if successful or matching, false otherwise
     */
    public boolean isWithinBaseRoot(Path candidate, String base) {
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path baseRoot = resolveBaseRoot(base);
        return normalizedCandidate.startsWith(baseRoot);
    }

    /**
     * Checks if the component is within target base.
     *
     * @param candidate the candidate
     * @return true if successful or matching, false otherwise
     */
    public boolean isWithinTargetBase(Path candidate) {
        return isWithinBaseRoot(candidate, "target");
    }

    /**
     * Qualify.
     *
     * @param base the base
     * @param relativePath the relative path
     * @return the string result
     */
    public String qualify(String base, String relativePath) {
        return toFileReference(base, relativePath).toCanonicalString();
    }

    /**
     * Qualify Absolute.
     *
     * @param absolutePath the absolute path
     * @return the string result
     */
    public String qualifyAbsolute(Path absolutePath) {
        return toFileReference(absolutePath).toCanonicalString();
    }

    /**
     * To Project Relative Path.
     *
     * @param absolutePath the absolute path
     * @return the string result
     */
    public String toProjectRelativePath(Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        Path root = getProjectRoot();
        if (!normalized.startsWith(root)) {
            throw new SecurityException("Path is outside project root: " + normalized);
        }
        return PathNormalizer.toForwardSlashes(root.relativize(normalized));
    }

    /**
     * To Project Relative Path.
     *
     * @param reference the reference
     * @return the string result
     */
    public String toProjectRelativePath(FileReference reference) {
        return reference.toProjectRelativePath(this::resolveBaseRoot, getProjectRoot());
    }

    /**
     * Resolves the configured value or path project path.
     *
     * @param relativePath the relative path
     * @return the resolved or constructed object
     */
    public Path resolveProjectPath(String relativePath) {
        Path candidate = getProjectRoot().resolve(relativePath == null ? "" : relativePath).normalize();
        return pathValidator.validateInProject(candidate);
    }

    /**
     * Canonicalize Source Path.
     *
     * @param base the base
     * @param path the file or directory path
     * @return the string result
     */
    public String canonicalizeSourcePath(String base, String path) {
        return toFileReference(base, path).toCanonicalString();
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return pathValidator.getProjectRoot();
    }

    /**
     * Relativize.
     *
     * @param base the base
     * @param absolutePath the absolute path
     * @return the string result
     */
    public String relativize(String base, Path absolutePath) {
        Path root = resolveBaseRoot(FileReferenceBase.from(base));
        Path normalizedAbsolute = absolutePath.toAbsolutePath().normalize();
        if (!normalizedAbsolute.startsWith(root)) {
            throw new SecurityException("Path is outside base root: " + absolutePath);
        }
        return PathNormalizer.toForwardSlashes(root.relativize(normalizedAbsolute));
    }

    /**
     * Resolves the configured value or path base root.
     *
     * @param base the base
     * @return the resolved or constructed object
     */
    public Path resolveBaseRoot(String base) {
        String normalized = normalizeBase(base);
        if ("project".equals(normalized)) {
            return mappings.getMainRoot();
        }
        Path sourceRoot = mappings.getSourceRoot(normalized);
        if (sourceRoot != null) {
            return sourceRoot;
        }
        Path targetRoot = mappings.getTargetRoot(normalized);
        if (targetRoot != null) {
            return targetRoot;
        }
        return resolveBaseRoot(FileReferenceBase.from(normalized));
    }

    /**
     * Resolves the configured value or path base root.
     *
     * @param base the base
     * @return the resolved or constructed object
     */
    public Path resolveBaseRoot(FileReferenceBase base) {
        return switch (base) {
            case MAIN -> mappings.getMainRoot();
            case TEST -> {
                Path testRoot = mappings.getTestRoot();
                if (testRoot == null) {
                    throw new IllegalStateException("test base is not available during project file inference");
                }
                yield testRoot;
            }
            case TARGET -> mappings.getTargetRoot();
            case SCRIPT -> {
                Path scriptRoot = mappings.getScriptRoot();
                if (scriptRoot == null) {
                    throw new IllegalStateException("script base is not available in this cycle");
                }
                yield scriptRoot;
            }
        };
    }

    /**
     * Normalize Base.
     *
     * @param base the base
     * @return the string result
     */
    public String normalizeBase(String base) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("Base is required.");
        }
        String normalized = base.trim().toLowerCase();
        if (normalized.endsWith(":/")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        if (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if ("main-target".equals(normalized) || "test-target".equals(normalized)) {
            return "target";
        }
        return normalized;
    }

    /**
     * To File Reference.
     *
     * @param base the base
     * @param relativePath the relative path
     * @return the resolved or constructed object
     */
    public FileReference toFileReference(String base, String relativePath) {
        return FileReference.of(normalizeBase(base), normalizeRelative(relativePath));
    }

    /**
     * To File Reference.
     *
     * @param absolutePath the absolute path
     * @return the resolved or constructed object
     */
    public FileReference toFileReference(Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        if (mappings != null) {
            for (java.util.Map.Entry<String, Path> entry : mappings.getTargetRoots().entrySet()) {
                if (entry.getValue() != null && normalized.startsWith(entry.getValue())) {
                    return FileReference.of("target", relativize("target", normalized));
                }
            }
            if (mappings.getTargetRoot() != null && normalized.startsWith(mappings.getTargetRoot())) {
                return FileReference.of("target", relativize("target", normalized));
            }
            for (java.util.Map.Entry<String, Path> entry : mappings.getSourceRoots().entrySet()) {
                if (entry.getValue() != null && normalized.startsWith(entry.getValue())) {
                    return FileReference.of(entry.getKey(), relativize(entry.getKey(), normalized));
                }
            }
        }
        if (mappings.getMainRoot() != null && normalized.startsWith(mappings.getMainRoot())) {
            return FileReference.of("main", relativize("main", normalized));
        }
        if (mappings.getTestRoot() != null && normalized.startsWith(mappings.getTestRoot())) {
            return FileReference.of("test", relativize("test", normalized));
        }
        if (mappings.getScriptRoot() != null && normalized.startsWith(mappings.getScriptRoot())) {
            return FileReference.of("script", relativize("script", normalized));
        }
        if (mappings.getTargetRoot() != null && normalized.startsWith(mappings.getTargetRoot())) {
            return FileReference.of("target", relativize("target", normalized));
        }
        throw new SecurityException("Path is outside configured base roots: " + normalized);
    }

    private String normalizeRelative(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }
        String normalized = PathNormalizer.toForwardSlashes(relativePath);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

     
    public java.util.List<String> findComposedSourceCandidates(String relativePath, String sourceScope) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        Path[] bases = mappings.getComposedSourceBases(sourceScope);
        
        for (Path base : bases) {
            Path candidate = base.resolve(relativePath).normalize();
            if (java.nio.file.Files.isRegularFile(candidate)) {
                String baseLabel = getBaseLabelForPath(base);
                candidates.add(baseLabel + ":" + relativePath);
            }
        }
        
        return candidates;
    }

     
    public java.util.List<String> findComposedTargetCandidates(String relativePath, String sourceScope) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        Path[] bases = mappings.getComposedTargetBases(sourceScope);
        if (bases.length == 0) {
            Path fallback = mappings.getTargetRoot();
            if (fallback != null) {
                bases = new Path[]{fallback};
            }
        }
        
        for (Path base : bases) {
            if (base == null) {
                continue;
            }
            Path candidate = base.resolve(relativePath).normalize();
            if (java.nio.file.Files.isRegularFile(candidate)) {
                String baseLabel = getBaseLabelForPath(base);
                candidates.add(baseLabel + ":" + relativePath);
            }
        }
        
        return candidates;
    }

     
    private String getBaseLabelForPath(Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();

        if (mappings.getMainTargetRoot() != null) {
            Path mainTargetNormalized = mappings.getMainTargetRoot().toAbsolutePath().normalize();
            if (normalized.equals(mainTargetNormalized)) {
                return "target";
            }
        }

        if (mappings.getTestTargetRoot() != null) {
            Path testTargetNormalized = mappings.getTestTargetRoot().toAbsolutePath().normalize();
            if (normalized.equals(testTargetNormalized)) {
                return "target";
            }
        }
        
        
        if (mappings.getMainRoot() != null) {
            Path mainNormalized = mappings.getMainRoot().toAbsolutePath().normalize();
            if (normalized.equals(mainNormalized)) {
                return "main";
            }
        }
        
        
        if (mappings.getTestRoot() != null) {
            Path testNormalized = mappings.getTestRoot().toAbsolutePath().normalize();
            if (normalized.equals(testNormalized)) {
                return "test";
            }
        }
        
        
        return "unknown";
    }
}
