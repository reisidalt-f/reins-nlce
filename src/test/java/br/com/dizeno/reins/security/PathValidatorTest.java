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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PathValidatorTest {

    @TempDir
    Path projectRoot;

    @Test
    void validateRelativeInput_rejectsEmptyAbsoluteAndTraversal() {
        PathValidator validator = new PathValidator(projectRoot);

        assertThrows(IllegalArgumentException.class, () -> validator.validateRelativeInput(" "));
        assertThrows(IllegalArgumentException.class, () -> validator.validateRelativeInput("../escape.md"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateRelativeInput(projectRoot.resolve("src/main/nl/A.md").toString()));
    }

    @Test
    void validateInProject_rejectsOutOfProjectPath() {
        PathValidator validator = new PathValidator(projectRoot);

        assertThrows(SecurityException.class,
                () -> validator.validateInProject(projectRoot.getParent().resolve("outside.md")));
    }

    @Test
    void validateInProject_rejectsSymlinkEscapingProjectRoot() throws Exception {
        Assumptions.assumeTrue(!System.getProperty("os.name", "").toLowerCase().contains("win"));

        Path outsideDir = Files.createDirectories(projectRoot.getParent().resolve("reins-path-validator-outside"));
        Path symlink = projectRoot.resolve("escape-link");
        Files.createSymbolicLink(symlink, outsideDir);

        PathValidator validator = new PathValidator(projectRoot);
        assertThrows(SecurityException.class, () -> validator.validateInProject(symlink.resolve("file.md")));
    }
}
