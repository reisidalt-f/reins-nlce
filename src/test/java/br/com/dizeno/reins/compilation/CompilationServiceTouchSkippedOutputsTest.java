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

package br.com.dizeno.reins.compilation;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.testutil.RecordingLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompilationServiceTouchSkippedOutputsTest {

    @TempDir
    Path projectRoot;

    @Test
    void testTouchSkippedOutputs_PreFilteredSkip() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig();
        RecordingLog log = new RecordingLog();

        String relativeSourcePath = "Sample.md";
        Path sourceFile = createSourceFile(relativeSourcePath, "# Sample source\n");
        String sourceHash = new SourceFingerprintService().sha256(Files.readString(sourceFile));

        
        Path outputFile = projectRoot.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "class Compiled {}\n");
        long pastTimeMillis = System.currentTimeMillis() - 100000;
        Files.setLastModifiedTime(outputFile, FileTime.fromMillis(pastTimeMillis));

        
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:Sample.md");
        record.setSourceCategory("main");
        record.setSourceHash(sourceHash);
        record.setBlockFingerprints(List.of(sourceHash));
        record.setResolvedTargetRoot("src/main/java");

        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put(
                "target:com/example/Compiled.java",
                new FileTrackingDetails("main:Sample.md", "main", pastTimeMillis)
        );
        record.setCompiledFiles(compiledFiles);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectRoot, record.getSourcePath(), record);

        
        CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile.toFile(), "src/main/nl/Sample.md", SourceProcessingStatus.SKIP);
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(sourceFile.toFile()),
                List.of(entry),
                false,
                List.of()
        );

        
        service.processFiles(preFilterResult, config, projectRoot, log);

        
        long currentMtime = Files.getLastModifiedTime(outputFile).toMillis();
        assertTrue(currentMtime > pastTimeMillis, "Output file should have its modification time updated");

        
        SourceTrackingRecord updatedRecord = trackingStore.load(projectRoot, record.getSourcePath()).orElse(null);
        assertNotNull(updatedRecord);
        FileTrackingDetails updatedDetails = updatedRecord.getCompiledFiles().get("target:com/example/Compiled.java");
        assertNotNull(updatedDetails);
        assertEquals(currentMtime, updatedDetails.getModificationTime());
    }

    @Test
    void testTouchSkippedOutputs_PostReasoningSkip() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig();
        RecordingLog log = new RecordingLog();

        String relativeSourcePath = "Sample.md";
        Path sourceFile = createSourceFile(relativeSourcePath, "# Sample source\n");
        String sourceHash = new SourceFingerprintService().sha256(Files.readString(sourceFile));

        
        Path outputFile = projectRoot.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "class Compiled {}\n");
        long pastTimeMillis = System.currentTimeMillis() - 100000;
        Files.setLastModifiedTime(outputFile, FileTime.fromMillis(pastTimeMillis));

        
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:Sample.md");
        record.setSourceCategory("main");
        record.setSourceHash("oldhash"); 
        record.setBlockFingerprints(List.of("oldhash"));
        record.setResolvedTargetRoot("src/main/java");

        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put(
                "target:com/example/Compiled.java",
                new FileTrackingDetails("main:Sample.md", "main", pastTimeMillis)
        );
        record.setCompiledFiles(compiledFiles);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectRoot, record.getSourcePath(), record);

        
        service.processFiles(List.of(sourceFile.toFile()), false, config, projectRoot, log);

        
        long currentMtime = Files.getLastModifiedTime(outputFile).toMillis();
        assertTrue(currentMtime > pastTimeMillis, "Output file should have its modification time updated");

        
        SourceTrackingRecord updatedRecord = trackingStore.load(projectRoot, record.getSourcePath()).orElse(null);
        assertNotNull(updatedRecord);
        FileTrackingDetails updatedDetails = updatedRecord.getCompiledFiles().get("target:com/example/Compiled.java");
        assertNotNull(updatedDetails);
        assertEquals(currentMtime, updatedDetails.getModificationTime());
        assertEquals("skipped", updatedRecord.getLastStatus());
    }

    @Test
    void testTouchSkippedOutputs_DryRun() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig();
        config.setDryRun(true); 
        RecordingLog log = new RecordingLog();

        String relativeSourcePath = "Sample.md";
        Path sourceFile = createSourceFile(relativeSourcePath, "# Sample source\n");
        String sourceHash = new SourceFingerprintService().sha256(Files.readString(sourceFile));

        Path outputFile = projectRoot.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "class Compiled {}\n");
        long pastTimeMillis = System.currentTimeMillis() - 100000;
        Files.setLastModifiedTime(outputFile, FileTime.fromMillis(pastTimeMillis));

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:Sample.md");
        record.setSourceCategory("main");
        record.setSourceHash(sourceHash);
        record.setBlockFingerprints(List.of(sourceHash));
        record.setResolvedTargetRoot("src/main/java");

        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put(
                "target:com/example/Compiled.java",
                new FileTrackingDetails("main:Sample.md", "main", pastTimeMillis)
        );
        record.setCompiledFiles(compiledFiles);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectRoot, record.getSourcePath(), record);

        
        CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile.toFile(), "src/main/nl/Sample.md", SourceProcessingStatus.SKIP);
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(sourceFile.toFile()),
                List.of(entry),
                false,
                List.of()
        );

        
        service.processFiles(preFilterResult, config, projectRoot, log);

        
        long currentMtime = Files.getLastModifiedTime(outputFile).toMillis();
        assertEquals(pastTimeMillis, currentMtime, "Dry run must not touch physical files");

        
        SourceTrackingRecord updatedRecord = trackingStore.load(projectRoot, record.getSourcePath()).orElse(null);
        assertNotNull(updatedRecord);
        FileTrackingDetails updatedDetails = updatedRecord.getCompiledFiles().get("target:com/example/Compiled.java");
        assertNotNull(updatedDetails);
        assertEquals(pastTimeMillis, updatedDetails.getModificationTime());
    }

    @Test
    void testTouchSkippedOutputs_FreezeState() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig();
        config.getTracking().setFreezeState(true); 
        RecordingLog log = new RecordingLog();

        String relativeSourcePath = "Sample.md";
        Path sourceFile = createSourceFile(relativeSourcePath, "# Sample source\n");
        String sourceHash = new SourceFingerprintService().sha256(Files.readString(sourceFile));

        Path outputFile = projectRoot.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "class Compiled {}\n");
        long pastTimeMillis = System.currentTimeMillis() - 100000;
        Files.setLastModifiedTime(outputFile, FileTime.fromMillis(pastTimeMillis));

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:Sample.md");
        record.setSourceCategory("main");
        record.setSourceHash(sourceHash);
        record.setBlockFingerprints(List.of(sourceHash));
        record.setResolvedTargetRoot("src/main/java");

        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put(
                "target:com/example/Compiled.java",
                new FileTrackingDetails("main:Sample.md", "main", pastTimeMillis)
        );
        record.setCompiledFiles(compiledFiles);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectRoot, record.getSourcePath(), record);

        
        CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile.toFile(), "src/main/nl/Sample.md", SourceProcessingStatus.SKIP);
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(sourceFile.toFile()),
                List.of(entry),
                false,
                List.of()
        );

        
        service.processFiles(preFilterResult, config, projectRoot, log);

        
        long currentMtime = Files.getLastModifiedTime(outputFile).toMillis();
        assertTrue(currentMtime > pastTimeMillis, "Freeze state should still touch physical files");

        
        SourceTrackingRecord updatedRecord = trackingStore.load(projectRoot, record.getSourcePath()).orElse(null);
        assertNotNull(updatedRecord);
        FileTrackingDetails updatedDetails = updatedRecord.getCompiledFiles().get("target:com/example/Compiled.java");
        assertNotNull(updatedDetails);
        assertEquals(pastTimeMillis, updatedDetails.getModificationTime());
    }

    

    private CompilationService createService() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(successResult());

        return new CompilationService(
                inferenceService,
                new OutputWriter(),
                new ResultPrinter(),
                new CompilationTrackingStore(),
                new SourceFingerprintService(),
                new RecompilationDecider(),
                new MarkdownDependencyGraphBuilder(),
                new ProcessingOrderResolver(),
                reasoningService,
                new ProjectContextService(),
                null);
    }

    private ReasoningResult successResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        result.setInspectedPaths(List.of());
        result.setReadMarkdownPaths(List.of());
        result.setToolInfoPhrases(List.of());
        return result;
    }

    private Path createSourceFile(String name, String content) throws Exception {
        Path source = projectRoot.resolve("src/main/nl").resolve(name);
        Files.createDirectories(source.getParent());
        Files.writeString(source, content);
        return source;
    }

    private ReinsConfig baseConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setScanRoots(List.of(projectRoot.resolve("src/main/nl").toFile()));
        config.setIncludePattern("**/*.md");
        config.setFailOnError(false);

        LogSettings logSettings = new LogSettings();
        logSettings.setSkipped(true);
        config.setLog(logSettings);

        TargetSettings target = new TargetSettings();
        target.setRoot(projectRoot.toFile());
        target.setMain("src/main/java");
        target.setTest("src/test/java");
        config.setTarget(target);

        return config;
    }
}
