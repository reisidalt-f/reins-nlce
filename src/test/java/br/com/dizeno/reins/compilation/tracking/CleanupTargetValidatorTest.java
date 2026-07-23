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

import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupTargetValidatorTest {

    @TempDir
    Path projectDir;

    @Test
    void acceptsValidCanonicalPathWithinProjectRoot() {
        CleanupTargetValidator validator = new CleanupTargetValidator(new PathValidator(projectDir));
        CleanupTarget target = new CleanupTarget(
                "target:src/main/java/com/example/Compiled.java",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                0L,
                Instant.now()
        );

        CleanupTargetValidator.ValidationResult result = validator.validateTarget(target, projectDir);

        assertTrue(result.isValid());
    }

    @Test
    void rejectsPathTraversalOutsideProjectRoot() {
        CleanupTargetValidator validator = new CleanupTargetValidator(new PathValidator(projectDir));
        CleanupTarget target = new CleanupTarget(
                "target:../../etc/passwd",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                0L,
                Instant.now()
        );

        CleanupTargetValidator.ValidationResult result = validator.validateTarget(target, projectDir);

        assertFalse(result.isValid());
    }

    @Test
    void rejectsInvalidCanonicalFormat() {
        CleanupTargetValidator validator = new CleanupTargetValidator(new PathValidator(projectDir));
        CleanupTarget target = new CleanupTarget(
                "src/main/java/com/example/Compiled.java",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                0L,
                Instant.now()
        );

        CleanupTargetValidator.ValidationResult result = validator.validateTarget(target, projectDir);

        assertFalse(result.isValid());
    }
}
