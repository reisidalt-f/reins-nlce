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
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.GeminiSettings;
import br.com.dizeno.reins.run.config.settings.TargetSettings;
import br.com.dizeno.reins.run.config.settings.TrackingSettings;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExplicitModeCleanupTest {

    @TempDir
    Path tempDir;

    private CompilationTrackingStore trackingStore;
    private ReasoningService reasoningService;
    private MarkdownDependencyGraphBuilder graphBuilder;
    private ProcessingOrderResolver processingOrderResolver;
    private CompilationService service;
    private Log log;

    private static final String SOURCE_PATH = "src/main/nl/feature.md";
    private static final String CANONICAL_PATH = "main:feature.md";
    private static final String PREVIOUS_OUTPUT_PATH = "target:com/example/OldFeature.java";
    private static final String NEW_OUTPUT_PATH = "target:com/example/NewFeature.java";

    private Path oldOutputFile;
    private Path newOutputFile;

    @BeforeEach
    void setUp() throws Exception {
        trackingStore = mock(CompilationTrackingStore.class);
        reasoningService = mock(ReasoningService.class);
        graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        processingOrderResolver = mock(ProcessingOrderResolver.class);
        log = mock(Log.class);

        service = new CompilationService(
                mock(br.com.dizeno.reins.reasoning.inference.InferenceService.class),
                mock(OutputWriter.class),
                mock(ResultPrinter.class),
                trackingStore,
                new SourceFingerprintService(),
                new RecompilationDecider(),
                graphBuilder,
                processingOrderResolver,
                reasoningService
        );

        Path source = tempDir.resolve(SOURCE_PATH);
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# feature\n\nDescribes feature.", StandardCharsets.UTF_8);

        oldOutputFile = tempDir.resolve("src/main/java/com/example/OldFeature.java");
        Files.createDirectories(oldOutputFile.getParent());
        Files.writeString(oldOutputFile, "package com.example; public class OldFeature {}", StandardCharsets.UTF_8);

        newOutputFile = tempDir.resolve("src/main/java/com/example/NewFeature.java");
        Files.writeString(newOutputFile, "package com.example; public class NewFeature {}", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(SOURCE_PATH, new MarkdownSourceNode(
                SOURCE_PATH,
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(SOURCE_PATH));
        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of(SOURCE_PATH));

        when(trackingStore.canonicalizePath(anyString())).thenAnswer(inv -> inv.getArgument(0, String.class));
        when(trackingStore.canonicalizePath(SOURCE_PATH)).thenReturn(CANONICAL_PATH);

        SourceTrackingRecord priorRecord = new SourceTrackingRecord();
        priorRecord.setSourcePath(CANONICAL_PATH);
        priorRecord.setSourceCategory("main");
        priorRecord.setSourceHash("old-hash");
        priorRecord.setResolvedTargetRoot("src/main/java");
        priorRecord.setCompiledFiles(Map.of(
                PREVIOUS_OUTPUT_PATH,
                new FileTrackingDetails(CANONICAL_PATH, "main", Files.getLastModifiedTime(oldOutputFile).toMillis())
        ));
        when(trackingStore.load(eq(tempDir), eq(CANONICAL_PATH))).thenReturn(Optional.of(priorRecord));
    }

    @Test
    void explicitMode_cleanupDisabled_staleFileNotDeletedAndPreservedInTracking() throws Exception {
        ReasoningResult result = successResult(List.of(NEW_OUTPUT_PATH));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(false, SOURCE_PATH);

        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        // Verify the old output file was NOT deleted from disk
        assertTrue(Files.exists(oldOutputFile), "Old output file should not be deleted when cleanupStaleCompiledFiles is false");

        // Verify old output file was preserved in tracking record
        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(eq(tempDir), eq(CANONICAL_PATH), recordCaptor.capture());

        SourceTrackingRecord savedRecord = recordCaptor.getValue();
        assertTrue(savedRecord.getCompiledFiles().containsKey(PREVIOUS_OUTPUT_PATH),
                "Previous output path should be preserved in tracking when cleanupStaleCompiledFiles is false");
        assertTrue(savedRecord.getCompiledFiles().containsKey(NEW_OUTPUT_PATH),
                "New output path should be included in tracking");
    }

    @Test
    void explicitMode_cleanupEnabled_staleFileIsDeleted() throws Exception {
        ReasoningResult result = successResult(List.of(NEW_OUTPUT_PATH));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true, SOURCE_PATH);

        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        // Verify the old output file WAS deleted from disk
        assertFalse(Files.exists(oldOutputFile), "Old output file should be deleted when cleanupStaleCompiledFiles is true");

        // Verify old output file is NOT preserved in tracking record
        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(eq(tempDir), eq(CANONICAL_PATH), recordCaptor.capture());

        SourceTrackingRecord savedRecord = recordCaptor.getValue();
        assertFalse(savedRecord.getCompiledFiles().containsKey(PREVIOUS_OUTPUT_PATH),
                "Previous output path should not be in tracking when cleanupStaleCompiledFiles is true");
        assertTrue(savedRecord.getCompiledFiles().containsKey(NEW_OUTPUT_PATH),
                "New output path should be included in tracking");
    }

    private ReinsConfig buildConfig(boolean cleanupStale, String explicitSource) {
        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        config.setGemini(gemini);
        config.setFailOnError(false);
        config.setSourceBase("main", tempDir.resolve("src/main/nl").toFile());
        config.setSource(explicitSource);

        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);

        TrackingSettings tracking = new TrackingSettings();
        tracking.setCleanupStaleCompiledFiles(cleanupStale);
        config.setTracking(tracking);
        return config;
    }

    private ReasoningResult successResult(List<String> writtenPaths) {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(writtenPaths);
        result.setReadMarkdownPaths(List.of());
        result.setInspectedPaths(List.of());
        result.setToolInfoPhrases(List.of());
        result.setUserFacingMessages(List.of("done"));
        return result;
    }
}
