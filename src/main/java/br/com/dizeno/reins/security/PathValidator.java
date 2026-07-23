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

package br.com.dizeno.reins.security;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

/**
 * PathValidator is part of the security boundary validation and sandboxing rules for file operations in the reins architecture.
 * Provides security path validation to ensure file operations stay within the project root sandbox.
 */
public class PathValidator {
    private final Path projectRoot;
    private final Path projectRootReal;

    /**
     * Constructs a new instance of {@link PathValidator}.
     *
     * @param projectRoot the root path of the project
     */
    public PathValidator(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        try {
            this.projectRootReal = this.projectRoot.toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve project root", e);
        }
    }

    /**
     * Validates the inputs or files in project.
     *
     * @param candidate the candidate
     * @return the resolved or constructed object
     */
    public Path validateInProject(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!normalized.startsWith(projectRoot)) {
            throw new SecurityException("Path escapes project root: " + candidate);
        }
        try {
            ensureRealPathInsideProject(normalized);
        } catch (IOException e) {
            throw new SecurityException("Path validation failed for: " + candidate, e);
        }
        return normalized;
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Validates the inputs or files relative input.
     *
     * @param candidate the candidate
     * @return the string result
     */
    public String validateRelativeInput(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            throw new IllegalArgumentException("Path input must not be empty.");
        }
        Path path = Path.of(candidate.trim());
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Path input must be relative to project source bases.");
        }
        for (String segment : candidate.replace('\\', '/').split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("Path input must not contain '..' segments.");
            }
        }
        return candidate.trim();
    }

    /**
     * Resolves the configured value or path in project.
     *
     * @param baseDir the base dir
     * @param relative the relative
     * @return the resolved or constructed object
     */
    public Path resolveInProject(Path baseDir, String relative) throws IOException {
        Path base = validateInProject(baseDir.toRealPath());
        Path resolved = base.resolve(relative).normalize();
        return validateInProject(resolved);
    }

    private void ensureRealPathInsideProject(Path normalized) throws IOException {
        if (Files.exists(normalized)) {
            Path real = normalized.toRealPath();
            if (!real.startsWith(projectRootReal)) {
                throw new SecurityException("Resolved path escapes project root via symlink: " + normalized);
            }
            return;
        }

        Path current = normalized;
        while (current != null && !Files.exists(current)) {
            current = current.getParent();
        }
        if (current == null) {
            throw new SecurityException("Unable to resolve existing parent path: " + normalized);
        }
        Path realParent = current.toRealPath();
        if (!realParent.startsWith(projectRootReal)) {
            throw new SecurityException("Parent path escapes project root via symlink: " + normalized);
        }
    }
}
