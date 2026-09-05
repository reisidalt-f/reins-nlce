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

    @Test
    void cleanupDeletesOnlySpecifiedSourceWhenTargetSourceFilesProvided() throws Exception {
        Path sourceFile1 = projectDir.resolve("src/main/nl/Sample1.md");
        Path sourceFile2 = projectDir.resolve("src/main/nl/Sample2.md");
        Files.createDirectories(sourceFile1.getParent());
        Files.writeString(sourceFile1, "Sample 1 content");
        Files.writeString(sourceFile2, "Sample 2 content");

        Path compiledFile1 = projectDir.resolve("src/main/java/com/example/Compiled1.java");
        Path compiledFile2 = projectDir.resolve("src/main/java/com/example/Compiled2.java");
        Files.createDirectories(compiledFile1.getParent());
        Files.writeString(compiledFile1, "class Compiled1 {}\n");
        Files.writeString(compiledFile2, "class Compiled2 {}\n");

        SourceTrackingRecord record1 = new SourceTrackingRecord();
        record1.setSourcePath("main:Sample1.md");
        record1.setSourceCategory("main");
        record1.setResolvedTargetRoot("src");
        LinkedHashMap<String, FileTrackingDetails> compiled1Map = new LinkedHashMap<>();
        compiled1Map.put("target:main/java/com/example/Compiled1.java",
                new FileTrackingDetails("main:Sample1.md", "main", Files.getLastModifiedTime(compiledFile1).toMillis()));
        record1.setCompiledFiles(compiled1Map);

        SourceTrackingRecord record2 = new SourceTrackingRecord();
        record2.setSourcePath("main:Sample2.md");
        record2.setSourceCategory("main");
        record2.setResolvedTargetRoot("src");
        LinkedHashMap<String, FileTrackingDetails> compiled2Map = new LinkedHashMap<>();
        compiled2Map.put("target:main/java/com/example/Compiled2.java",
                new FileTrackingDetails("main:Sample2.md", "main", Files.getLastModifiedTime(compiledFile2).toMillis()));
        record2.setCompiledFiles(compiled2Map);

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectDir, record1.getSourcePath(), record1);
        trackingStore.save(projectDir, record2.getSourcePath(), record2);

        Path trackingFile1 = trackingStore.trackingFilePath(projectDir, record1.getSourcePath());
        Path trackingFile2 = trackingStore.trackingFilePath(projectDir, record2.getSourcePath());

        CleanupService cleanupService = new CleanupService();
        CleanupOutcomeSummary summary = cleanupService.executeCleanup(
                projectDir,
                new PathValidator(projectDir),
                java.util.List.of(sourceFile1.toFile())
        );

        assertTrue(summary.isSuccessful());

        // Source 1 artifacts deleted
        assertFalse(Files.exists(compiledFile1));
        assertFalse(Files.exists(trackingFile1));

        // Source 2 artifacts retained
        assertTrue(Files.exists(compiledFile2));
        assertTrue(Files.exists(trackingFile2));
    }
}
