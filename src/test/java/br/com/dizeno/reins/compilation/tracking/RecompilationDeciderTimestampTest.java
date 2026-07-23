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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecompilationDeciderTimestampTest {
    @TempDir
    Path projectDir;

    @Test
    void returnsSkipWhenSourceAndOutputsAreCurrent() throws Exception {
        Path source = write(projectDir.resolve("src/main/nl/example.md"), "# source\n");
        Path output = write(projectDir.resolve("src/main/java/example/Example.java"), "class Example {}\n");
        long now = System.currentTimeMillis();
        Files.setLastModifiedTime(source, FileTime.fromMillis(now));
        Files.setLastModifiedTime(output, FileTime.fromMillis(now + 500));

        SourceTrackingRecord record = record("src/main/nl/example.md", "hash", List.of("fingerprint"), List.of("src/main/java/example/Example.java"), List.of(), Map.of());
        record.setSourceModificationTime(Files.getLastModifiedTime(source).toMillis());
        ReprocessingDecision decision = new RecompilationDecider().evaluate(
                record.getSourcePath(),
                record,
                "hash",
                List.of("fingerprint"),
                List.of("src/main/java/example/Example.java"),
                projectDir,
                Files.getLastModifiedTime(source).toMillis(),
                Set.of(),
                null,
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, decision.reason());
    }

    @Test
    void reprocessesWhenSourceIsNewerThanOutputs() throws Exception {
        Path source = write(projectDir.resolve("src/main/nl/example.md"), "# source\n");
        Path output = write(projectDir.resolve("src/main/java/example/Example.java"), "class Example {}\n");
        long now = System.currentTimeMillis();
        Files.setLastModifiedTime(output, FileTime.fromMillis(now));
        Files.setLastModifiedTime(source, FileTime.fromMillis(now + 500));

        SourceTrackingRecord record = record("src/main/nl/example.md", "hash", List.of("fingerprint"), List.of("src/main/java/example/Example.java"), List.of(), Map.of());
        record.setSourceModificationTime(now);
        ReprocessingDecision decision = new RecompilationDecider().evaluate(
                record.getSourcePath(),
                record,
                "hash",
                List.of("fingerprint"),
                List.of("src/main/java/example/Example.java"),
                projectDir,
                Files.getLastModifiedTime(source).toMillis(),
                Set.of(),
                null,
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_NEWER_THAN_OUTPUTS, decision.reason());
    }

    @Test
    void reprocessesWhenReferencedChildChangedInCurrentRun() throws Exception {
        SourceTrackingRecord record = record(
                "src/main/nl/parent.md",
                "hash",
                List.of("fingerprint"),
                List.of(),
                List.of("src/main/nl/child.md"),
                Map.of()
        );

        ReprocessingDecision decision = new RecompilationDecider().evaluate(
                record.getSourcePath(),
                record,
                "hash",
                List.of("fingerprint"),
                List.of(),
                projectDir,
                System.currentTimeMillis(),
                Set.of("src/main/nl/child.md"),
                null,
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.CHILD_OUTPUT_CHANGED, decision.reason());
    }

    private SourceTrackingRecord record(String sourcePath,
                                           String sourceHash,
                                           List<String> blockFingerprints,
                                           List<String> compiledPaths,
                                           List<String> markdownReferences,
                                           Map<String, Long> compiledFileMtimes) {
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(sourcePath);
        record.setSourceHash(sourceHash);
        record.setBlockFingerprints(blockFingerprints);
        Map<String, FileTrackingDetails> markdownReferenceFiles = new LinkedHashMap<>();
        for (String path : markdownReferences) {
            markdownReferenceFiles.put(path, new FileTrackingDetails(path, null, null));
        }
        record.setMarkdownReferences(markdownReferenceFiles);
        
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        for (String path : compiledPaths) {
            Long mtime = compiledFileMtimes != null ? compiledFileMtimes.get(path) : null;
            files.put(path, new FileTrackingDetails(null, null, mtime));
        }
        record.setCompiledFiles(files);
        return record;
    }

    private Path write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}