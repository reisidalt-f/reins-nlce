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
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupServiceTest {

    @TempDir
    Path projectDir;

    @Test
    void cleanupDeletesCompiledOutputsFromPerSourceTrackingAndRemovesTrackingArtifacts() throws Exception {
        Path compiledFile = projectDir.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(compiledFile.getParent());
        Files.writeString(compiledFile, "class Compiled {}\n");

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:Sample.md");
        record.setSourceCategory("main");
        record.setResolvedTargetRoot("src");

        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put(
                "target:main/java/com/example/Compiled.java",
                new FileTrackingDetails("main:Sample.md", "main", Files.getLastModifiedTime(compiledFile).toMillis())
        );
        record.setCompiledFiles(compiledFiles);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectDir, record.getSourcePath(), record);
        Path trackingFile = trackingStore.trackingFilePath(projectDir, record.getSourcePath());

        CleanupService cleanupService = new CleanupService();
        CleanupOutcomeSummary summary = cleanupService.executeCleanup(
                projectDir,
                new PathValidator(projectDir)
        );

        assertTrue(summary.isSuccessful());
        assertTrue(summary.getStatistics().getTargetedForDeletion() >= 2);
        assertEquals(0, summary.getStatistics().getFailedDeletion());

        assertFalse(Files.exists(compiledFile));
        assertFalse(Files.exists(trackingFile));
    }
}
