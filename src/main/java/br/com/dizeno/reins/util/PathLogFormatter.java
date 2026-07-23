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

package br.com.dizeno.reins.util;

import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.source.domain.FileReference;
import java.io.File;
import java.nio.file.Path;

/**
 * Utility for formatting paths relative to the project root in log messages.
 */
public final class PathLogFormatter {

    private PathLogFormatter() {
    }

    /**
     * Formats a path relative to the project root.
     *
     * @param path        the path to format
     * @param projectRoot the project root path
     * @return the relative path as a string with normalized forward slashes
     */
    public static String formatPath(Path path, Path projectRoot) {
        if (path == null) {
            return "";
        }
        if (projectRoot == null) {
            return PathNormalizer.toForwardSlashes(path.toString());
        }
        try {
            Path absolutePath = path.toAbsolutePath().normalize();
            Path absoluteProjectRoot = projectRoot.toAbsolutePath().normalize();
            Path relative = absoluteProjectRoot.relativize(absolutePath);
            return PathNormalizer.toForwardSlashes(relative.toString());
        } catch (Exception e) {
            return PathNormalizer.toForwardSlashes(path.toString());
        }
    }

    /**
     * Formats a file path relative to the project root.
     *
     * @param file        the file to format
     * @param projectRoot the project root path
     * @return the relative path as a string with normalized forward slashes
     */
    public static String formatPath(File file, Path projectRoot) {
        if (file == null) {
            return "";
        }
        return formatPath(file.toPath(), projectRoot);
    }

    /**
     * Formats a path string relative to the project root.
     *
     * @param pathStr     the path string to format (can be absolute, relative, or canonical)
     * @param projectRoot the project root path
     * @return the relative path as a string with normalized forward slashes
     */
    public static String formatPath(String pathStr, Path projectRoot) {
        if (pathStr == null || pathStr.isBlank()) {
            return "";
        }
        if (projectRoot == null) {
            return PathNormalizer.toForwardSlashes(pathStr);
        }

        // Handle comma-separated lists of paths
        if (pathStr.contains(",")) {
            String[] parts = pathStr.split(",");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(formatPath(parts[i].trim(), projectRoot));
            }
            return sb.toString();
        }

        // Check if it is a canonical path representation (main:, test:, target:)
        if (TrackedPathResolver.looksCanonical(pathStr)) {
            try {
                FileReference ref = FileReference.fromCanonical(pathStr);
                Path resolved;
                if (ref.getBase() == br.com.dizeno.reins.source.domain.FileReferenceBase.MAIN) {
                    resolved = projectRoot.resolve("src/main/nl").resolve(ref.getPath());
                } else if (ref.getBase() == br.com.dizeno.reins.source.domain.FileReferenceBase.TEST) {
                    resolved = projectRoot.resolve("src/test/nl").resolve(ref.getPath());
                } else {
                    resolved = TrackedPathResolver.resolveTrackedPath(projectRoot, pathStr, null);
                }
                return formatPath(resolved, projectRoot);
            } catch (Exception e) {
                // Fallback: strip canonical prefix and normalize
                int idx = pathStr.indexOf(':');
                if (idx >= 0) {
                    return PathNormalizer.toForwardSlashes(pathStr.substring(idx + 1));
                }
                return PathNormalizer.toForwardSlashes(pathStr);
            }
        }

        try {
            Path p = Path.of(pathStr);
            if (!p.isAbsolute()) {
                p = projectRoot.resolve(p);
            }
            return formatPath(p, projectRoot);
        } catch (Exception e) {
            return PathNormalizer.toForwardSlashes(pathStr);
        }
    }
}
