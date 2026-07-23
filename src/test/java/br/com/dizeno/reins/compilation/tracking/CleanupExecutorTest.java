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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupExecutorTest {

    @TempDir
    Path projectDir;

    @Test
    void deletesExistingFile() throws Exception {
        Path file = projectDir.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Compiled {}\n");

        CleanupTarget target = new CleanupTarget(
                "target:src/main/java/com/example/Compiled.java",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                Files.size(file),
                Instant.now()
        );

        CleanupExecutor executor = new CleanupExecutor(
                new CleanupTargetValidator(new PathValidator(projectDir)),
                projectDir
        );

        CleanupOutcome outcome = executor.deleteTarget(target);

        assertEquals(CleanupOutcome.Status.DELETED, outcome.getStatus());
        assertFalse(Files.exists(file));
    }

    @Test
    void returnsAlreadyMissingForNonExistingFile() {
        CleanupTarget target = new CleanupTarget(
                "target:src/main/java/com/example/Missing.java",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                0L,
                Instant.now()
        );

        CleanupExecutor executor = new CleanupExecutor(
                new CleanupTargetValidator(new PathValidator(projectDir)),
                projectDir
        );

        CleanupOutcome outcome = executor.deleteTarget(target);

        assertEquals(CleanupOutcome.Status.ALREADY_MISSING, outcome.getStatus());
    }

    @Test
    void rejectsPathOutsideProjectRoot() {
        CleanupTarget target = new CleanupTarget(
                "target:../../etc/passwd",
                CleanupTarget.Type.FILE,
                "main:src/main/nl/sample.md",
                0L,
                Instant.now()
        );

        CleanupExecutor executor = new CleanupExecutor(
                new CleanupTargetValidator(new PathValidator(projectDir)),
                projectDir
        );

        CleanupOutcome outcome = executor.deleteTarget(target);

        assertEquals(CleanupOutcome.Status.REJECTED, outcome.getStatus());
        assertTrue(outcome.getReason().contains("project root") || outcome.getReason().contains(".."));
    }
}
