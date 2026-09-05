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

package br.com.dizeno.reins.source.domain;

import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * FileReference is part of the general application functions in the reins architecture.
 * Acts as a model representation of a source file reference.
 */
public final class FileReference {
    private final FileReferenceBase base;
    private final String customBaseName;
    private final String path;

    /**
     * Constructs a new instance of {@link FileReference}.
     *
     * @param base the base
     * @param path the file or directory path
     */
    public FileReference(FileReferenceBase base, String path) {
        this(base, base != null ? base.value() : "main", path);
    }

    /**
     * Constructs a new instance of {@link FileReference} with a custom base name.
     *
     * @param base the base enum fallback
     * @param customBaseName the explicit base name
     * @param path the file or directory path
     */
    public FileReference(FileReferenceBase base, String customBaseName, String path) {
        this.base = base != null ? base : FileReferenceBase.MAIN;
        this.customBaseName = customBaseName != null && !customBaseName.isBlank()
                ? customBaseName.trim().toLowerCase(java.util.Locale.ROOT)
                : this.base.value();
        this.path = normalizePath(path);
    }

    /**
     * Of.
     *
     * @param base the base
     * @param path the file or directory path
     * @return the resolved or constructed object
     */
    public static FileReference of(String base, String path) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("Base is required.");
        }
        String normalized = base.trim().toLowerCase(java.util.Locale.ROOT);
        FileReferenceBase fileBase;
        try {
            fileBase = FileReferenceBase.from(normalized);
        } catch (Exception ex) {
            fileBase = FileReferenceBase.MAIN;
        }
        return new FileReference(fileBase, normalized, path);
    }

    /**
     * From Canonical.
     *
     * @param qualifiedPath the qualified path
     * @return the resolved or constructed object
     */
    public static FileReference fromCanonical(String qualifiedPath) {
        if (qualifiedPath == null || qualifiedPath.isBlank()) {
            throw new IllegalArgumentException("Qualified path cannot be null or blank.");
        }
        if (qualifiedPath.contains(":/")) {
            throw new IllegalArgumentException(
                    "Canonical path must use '<base>:<relative path>' format without ':/': " + qualifiedPath);
        }
        int separator = qualifiedPath.indexOf(':');
        if (separator <= 0) {
            throw new IllegalArgumentException(
                    "Canonical path must use '<base>:<relative path>' format: " + qualifiedPath);
        }
        return of(qualifiedPath.substring(0, separator), qualifiedPath.substring(separator + 1));
    }

    /**
     * Gets the base.
     *
     * @return the resolved or constructed object
     */
    public FileReferenceBase getBase() {
        return base;
    }

    /**
     * Gets the custom or explicit base name.
     *
     * @return base name string
     */
    public String getBaseName() {
        return customBaseName;
    }

    /**
     * Gets the path.
     *
     * @return the string result
     */
    public String getPath() {
        return path;
    }

    /**
     * To Canonical String.
     *
     * @return the string result
     */
    public String toCanonicalString() {
        return customBaseName + ":" + path;
    }

    /**
     * To Absolute Path.
     *
     * @param baseRootResolver the base root resolver
     * @return the resolved or constructed object
     */
    public Path toAbsolutePath(Function<FileReferenceBase, Path> baseRootResolver) {
        Objects.requireNonNull(baseRootResolver, "baseRootResolver must not be null");
        Path baseRoot = Objects.requireNonNull(baseRootResolver.apply(base), "base root must not be null")
                .toAbsolutePath().normalize();
        Path candidate = path.isBlank() ? baseRoot : baseRoot.resolve(path).normalize();
        if (!candidate.startsWith(baseRoot)) {
            throw new SecurityException("Path is outside base root: " + toCanonicalString());
        }
        return candidate;
    }

    /**
     * To Absolute Path using String base resolver.
     *
     * @param baseRootResolver the base root resolver by string base name
     * @return the resolved or constructed object
     */
    public Path toAbsolutePathByBaseName(Function<String, Path> baseRootResolver) {
        Objects.requireNonNull(baseRootResolver, "baseRootResolver must not be null");
        Path baseRoot = Objects.requireNonNull(baseRootResolver.apply(customBaseName), "base root must not be null for base " + customBaseName)
                .toAbsolutePath().normalize();
        Path candidate = path.isBlank() ? baseRoot : baseRoot.resolve(path).normalize();
        if (!candidate.startsWith(baseRoot)) {
            throw new SecurityException("Path is outside base root: " + toCanonicalString());
        }
        return candidate;
    }

    /**
     * To Project Relative Path.
     *
     * @param baseRootResolver the base root resolver
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public String toProjectRelativePath(Function<FileReferenceBase, Path> baseRootResolver, Path projectRoot) {
        Path absolutePath = toAbsolutePath(baseRootResolver);
        Path normalizedProjectRoot = Objects.requireNonNull(projectRoot, "projectRoot must not be null")
                .toAbsolutePath().normalize();
        if (!absolutePath.startsWith(normalizedProjectRoot)) {
            throw new SecurityException("Path is outside project root: " + toCanonicalString());
        }
        return PathNormalizer.toForwardSlashes(normalizedProjectRoot.relativize(absolutePath));
    }

    /**
     * To Project Relative Path using String base resolver.
     *
     * @param baseRootResolver the base root resolver by string base name
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public String toProjectRelativePathByBaseName(Function<String, Path> baseRootResolver, Path projectRoot) {
        Path absolutePath = toAbsolutePathByBaseName(baseRootResolver);
        Path normalizedProjectRoot = Objects.requireNonNull(projectRoot, "projectRoot must not be null")
                .toAbsolutePath().normalize();
        if (!absolutePath.startsWith(normalizedProjectRoot)) {
            throw new SecurityException("Path is outside project root: " + toCanonicalString());
        }
        return PathNormalizer.toForwardSlashes(normalizedProjectRoot.relativize(absolutePath));
    }

    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String normalized = PathNormalizer.toForwardSlashes(rawPath.trim());
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Path traversal not allowed: " + rawPath);
            }
        }
        normalized = PathNormalizer.toForwardSlashes(Path.of(normalized).normalize());
        return ".".equals(normalized) ? "" : normalized;
    }
}