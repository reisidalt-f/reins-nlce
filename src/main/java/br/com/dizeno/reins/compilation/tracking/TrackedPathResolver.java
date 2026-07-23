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

package br.com.dizeno.reins.compilation.tracking;

import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.FileReferenceBase;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;

/**
 * TrackedPathResolver is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public final class TrackedPathResolver {

    private TrackedPathResolver() {
    }

    /**
     * Canonicalize Source Path.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public static String canonicalizeSourcePath(String sourcePath) {
        String normalized = normalizeLoose(sourcePath);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("sourcePath must not be null or blank");
        }
        if (normalized.contains(":/")) {
            throw new IllegalArgumentException(
                    "Canonical path must use '<base>:<relative path>' format without ':/': " + sourcePath);
        }
        if (looksCanonical(normalized)) {
            return FileReference.fromCanonical(normalized).toCanonicalString();
        }
        if (normalized.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return new FileReference(
                    FileReferenceBase.MAIN,
                    normalized.substring((ProjectDirectoryPaths.MAIN_NL_ROOT + "/").length())
            ).toCanonicalString();
        }
        if (normalized.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return new FileReference(
                    FileReferenceBase.TEST,
                    normalized.substring((ProjectDirectoryPaths.TEST_NL_ROOT + "/").length())
            ).toCanonicalString();
        }
        return normalized;
    }

    /**
     * Resolves the configured value or path tracked path.
     *
     * @param projectRoot the root path of the project
     * @param trackedPath the tracked path
     * @param resolvedTargetRoot the resolved target root
     * @return the resolved or constructed object
     */
    public static Path resolveTrackedPath(Path projectRoot, String trackedPath, String resolvedTargetRoot) {
        String normalized = normalizeLoose(trackedPath);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("trackedPath must not be null or blank");
        }
        if (looksCanonical(normalized)) {
            FileReference reference = FileReference.fromCanonical(normalized);
            return reference.toAbsolutePath(base -> resolveBaseRoot(projectRoot, base, resolvedTargetRoot));
        }
        Path candidate = Path.of(normalized);
        return candidate.isAbsolute() ? candidate.normalize() : projectRoot.resolve(candidate).normalize();
    }

    /**
     * Tracking File Path.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @return the resolved or constructed object
     */
    public static Path trackingFilePath(Path projectRoot, String sourcePath) {
        String normalized = canonicalizeSourcePath(sourcePath);
        Path trackingDir = projectRoot.resolve(ProjectDirectoryPaths.COMPILATION_TRACKING_DIR).normalize();
        Path resolved;
        if (looksCanonical(normalized)) {
            FileReference reference = FileReference.fromCanonical(normalized);
            String fileStem = reference.getPath().isBlank() ? "_root" : reference.getPath();
            resolved = trackingDir.resolve(reference.getBase().value()).resolve(fileStem + ".json").normalize();
        } else {
            resolved = trackingDir.resolve(normalized + ".json").normalize();
        }
        if (!resolved.startsWith(trackingDir)) {
            throw new IllegalArgumentException("Tracking file path escapes tracking directory: " + sourcePath);
        }
        return resolved;
    }

    /**
     * Source Path From Tracking File.
     *
     * @param trackingDir the tracking dir
     * @param trackingFile the tracking file
     * @return the string result
     */
    public static String sourcePathFromTrackingFile(Path trackingDir, Path trackingFile) {
        Path relative = trackingDir.relativize(trackingFile);
        String relativePath = PathNormalizer.toForwardSlashes(relative.toString()).replaceAll("\\.json$", "");
        int slash = relativePath.indexOf('/');
        if (slash > 0) {
            String firstSegment = relativePath.substring(0, slash);
            String remainder = relativePath.substring(slash + 1);
            if (isKnownBase(firstSegment)) {
                return firstSegment + ":" + remainder;
            }
        }
        return relativePath;
    }

    /**
     * Looks Canonical.
     *
     * @param path the file or directory path
     * @return true if successful or matching, false otherwise
     */
    public static boolean looksCanonical(String path) {
        if (path == null || path.isBlank() || path.contains(":/")) {
            return false;
        }
        int separator = path.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        return isKnownBase(path.substring(0, separator));
    }

    private static Path resolveBaseRoot(Path projectRoot,
                                        FileReferenceBase base,
                                        String resolvedTargetRoot) {
        return switch (base) {
            case MAIN -> projectRoot.resolve(ProjectDirectoryPaths.MAIN_NL_ROOT).normalize();
            case TEST -> projectRoot.resolve(ProjectDirectoryPaths.TEST_NL_ROOT).normalize();
            case TARGET -> {
                if (resolvedTargetRoot == null || resolvedTargetRoot.isBlank()) {
                    yield projectRoot.toAbsolutePath().normalize();
                }
                yield projectRoot.resolve(resolvedTargetRoot).normalize();
            }
            case SCRIPT -> throw new IllegalArgumentException("Script base is not supported for compilation tracking paths.");
        };
    }

    private static boolean isKnownBase(String value) {
        return "main".equals(value) || "test".equals(value) || "target".equals(value);
    }

    private static String normalizeLoose(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String normalized = rawPath.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Path traversal not allowed: " + rawPath);
            }
        }
        return normalized;
    }
}