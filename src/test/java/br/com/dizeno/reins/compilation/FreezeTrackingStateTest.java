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

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

 
class FreezeTrackingStateTest {

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
     
    private static final String STALE_SOURCE_PATH = "src/main/nl/deleted_feature.md";

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

        
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(SOURCE_PATH, new MarkdownSourceNode(
                SOURCE_PATH,
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(SOURCE_PATH));
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of(SOURCE_PATH));
        
        when(trackingStore.canonicalizePath(anyString())).thenAnswer(inv -> inv.getArgument(0, String.class));
        when(trackingStore.canonicalizePath(SOURCE_PATH)).thenReturn(CANONICAL_PATH);
        when(trackingStore.load(any(), any())).thenReturn(Optional.empty());
        when(trackingStore.listAllTrackedSourcePaths(any())).thenReturn(List.of());
    }

    
    
    

    @Test
    void freezeEnabled_successfulCompilation_trackingNotPersisted() throws Exception {
        ReasoningResult result = successResult(List.of("main:com/example/Feature.java"));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        verify(trackingStore, never()).save(any(), any(), any());
    }

    @Test
    void freezeEnabled_compilationStillCompletes_summaryHasCompiled() throws Exception {
        ReasoningResult result = successResult(List.of("main:com/example/Feature.java"));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        CompilationSummary summary = service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        assertEquals(1, summary.getCompiled());
        assertEquals(0, summary.getFailed());
    }

    @Test
    void freezeEnabled_skipRecord_notPersisted() throws Exception {
        
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        result.setReadMarkdownPaths(List.of());
        result.setInspectedPaths(List.of());
        result.setToolInfoPhrases(List.of());
        result.setUserFacingMessages(List.of());
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        verify(trackingStore, never()).save(any(), any(), any());
    }

    @Test
    void freezeEnabled_trackingLoadedForDecisionSupport() throws Exception {
        ReasoningResult result = successResult(List.of());
        result.setFinalIntent("finish_success");
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        
        verify(trackingStore).load(eq(tempDir), eq(CANONICAL_PATH));
    }

    
    
    

    @Test
    void freezeEnabled_staleCleanup_deleteNotCalled() throws Exception {
        
        when(trackingStore.listAllTrackedSourcePaths(tempDir)).thenReturn(List.of(STALE_SOURCE_PATH));
        SourceTrackingRecord staleRecord = new SourceTrackingRecord();
        staleRecord.setResolvedTargetRoot(null);
        when(trackingStore.load(eq(tempDir), eq(STALE_SOURCE_PATH))).thenReturn(Optional.of(staleRecord));

        
        ReinsConfig config = buildConfig(true);
        MarkdownDependencyGraph emptyGraph = new MarkdownDependencyGraph(Map.of(), Map.of(), Map.of(), List.of());
        when(graphBuilder.build(any(), any(), any())).thenReturn(emptyGraph);
        when(processingOrderResolver.resolve(emptyGraph)).thenReturn(List.of());

        service.processFiles(List.of(), config, tempDir, log);

        verify(trackingStore, never()).delete(any(), any());
    }

    @Test
    void freezeEnabled_staleCleanup_reportedInLog() throws Exception {
        when(trackingStore.listAllTrackedSourcePaths(tempDir)).thenReturn(List.of(STALE_SOURCE_PATH));
        SourceTrackingRecord staleRecord = new SourceTrackingRecord();
        staleRecord.setResolvedTargetRoot(null);
        when(trackingStore.load(eq(tempDir), eq(STALE_SOURCE_PATH))).thenReturn(Optional.of(staleRecord));

        ReinsConfig config = buildConfig(true);
        MarkdownDependencyGraph emptyGraph = new MarkdownDependencyGraph(Map.of(), Map.of(), Map.of(), List.of());
        when(graphBuilder.build(any(), any(), any())).thenReturn(emptyGraph);
        when(processingOrderResolver.resolve(emptyGraph)).thenReturn(List.of());

        service.processFiles(List.of(), config, tempDir, log);

        
        verify(log).info(contains(STALE_SOURCE_PATH));
    }

    
    
    

    @Test
    void freezeDisabled_default_trackingPersistedOnSuccess() throws Exception {
        ReasoningResult result = successResult(List.of("main:com/example/Feature.java"));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(false);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        verify(trackingStore).save(eq(tempDir), eq(CANONICAL_PATH), any(SourceTrackingRecord.class));
    }

    @Test
    void freezeDisabled_explicit_trackingPersistedOnSuccess() throws Exception {
        ReasoningResult result = successResult(List.of("main:com/example/Feature.java"));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfigWithExplicitFreezeState(false);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        verify(trackingStore).save(eq(tempDir), eq(CANONICAL_PATH), any(SourceTrackingRecord.class));
    }

    @Test
    void freezeDisabled_skipRecord_preservesExistingTrackings() throws Exception {
        SourceTrackingRecord priorRecord = new SourceTrackingRecord();
        priorRecord.setSourcePath(CANONICAL_PATH);
        priorRecord.setSourceCategory("main");
        priorRecord.setSourceHash("old-hash");
        priorRecord.setBlockFingerprints(List.of("old-fingerprint"));
        priorRecord.setModel("gemini-2.0-flash");
        priorRecord.setOutputPolicy("src/main/java");
        priorRecord.setResolvedTargetRoot("main");
        priorRecord.setCompiledFiles(Map.of(
                "main:com/example/Feature.java",
                new FileTrackingDetails(CANONICAL_PATH, "main", 10L)));
        priorRecord.setInspectedFiles(Map.of(
                "main:docs/context.md",
                new FileTrackingDetails(CANONICAL_PATH, "main", 20L)));
        priorRecord.setMarkdownReferences(Map.of(
                "main:shared/ref.md",
                new FileTrackingDetails(CANONICAL_PATH, "main", 30L)));
        when(trackingStore.load(eq(tempDir), eq(CANONICAL_PATH))).thenReturn(Optional.of(priorRecord));

        ReasoningResult result = successResult(List.of());
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(false);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(eq(tempDir), eq(CANONICAL_PATH), recordCaptor.capture());

        SourceTrackingRecord savedRecord = recordCaptor.getValue();
        assertEquals("skipped", savedRecord.getLastStatus());
        assertNotNull(savedRecord.getLastCompiledAt());
        
        assertEquals(1, savedRecord.getBlockFingerprints().size());
        assertEquals(savedRecord.getSourceHash(), savedRecord.getBlockFingerprints().get(0));
        
        assertEquals(Set.of("main:com/example/Feature.java"), savedRecord.getCompiledFiles().keySet());
        assertEquals(Set.of("main:docs/context.md"), savedRecord.getInspectedFiles().keySet());
        assertEquals(Set.of("main:shared/ref.md"), savedRecord.getMarkdownReferences().keySet());
    }

    @Test
    void freezeDisabled_staleCleanup_deleteIsCalled() throws Exception {
        when(trackingStore.listAllTrackedSourcePaths(tempDir)).thenReturn(List.of(STALE_SOURCE_PATH));
        SourceTrackingRecord staleRecord = new SourceTrackingRecord();
        staleRecord.setResolvedTargetRoot(null);
        when(trackingStore.load(eq(tempDir), eq(STALE_SOURCE_PATH))).thenReturn(Optional.of(staleRecord));

        ReinsConfig config = buildConfig(false);
        MarkdownDependencyGraph emptyGraph = new MarkdownDependencyGraph(Map.of(), Map.of(), Map.of(), List.of());
        when(graphBuilder.build(any(), any(), any())).thenReturn(emptyGraph);
        when(processingOrderResolver.resolve(emptyGraph)).thenReturn(List.of());

        service.processFiles(List.of(), config, tempDir, log);

        verify(trackingStore).delete(eq(tempDir), eq(STALE_SOURCE_PATH));
    }

    
    
    

    @Test
    void freezeEnabled_skipRecord_freezeLogMessageEmitted() throws Exception {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        result.setReadMarkdownPaths(List.of());
        result.setInspectedPaths(List.of());
        result.setToolInfoPhrases(List.of());
        result.setUserFacingMessages(List.of());
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, org.mockito.Mockito.atLeastOnce()).info(logCaptor.capture());
        boolean hasFreezeLog = logCaptor.getAllValues().stream()
                .anyMatch(msg -> msg != null && msg.contains("[tracking]") && msg.contains("Freeze mode"));
        org.junit.jupiter.api.Assertions.assertTrue(hasFreezeLog,
                "Expected a [tracking] Freeze mode log message but none was found");
    }

    @Test
    void freezeEnabled_successRecord_freezeLogMessageEmitted() throws Exception {
        ReasoningResult result = successResult(List.of("main:com/example/Feature.java"));
        when(reasoningService.runCycle(any(), any())).thenReturn(result);

        ReinsConfig config = buildConfig(true);
        service.processFiles(List.of(tempDir.resolve(SOURCE_PATH).toFile()), config, tempDir, log);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, org.mockito.Mockito.atLeastOnce()).info(logCaptor.capture());
        boolean hasFreezeLog = logCaptor.getAllValues().stream()
                .anyMatch(msg -> msg != null && msg.contains("[tracking]") && msg.contains("Freeze mode"));
        org.junit.jupiter.api.Assertions.assertTrue(hasFreezeLog,
                "Expected a [tracking] Freeze mode log message but none was found");
    }

    
    
    

    private ReinsConfig buildConfig(boolean freeze) {
        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        config.setGemini(gemini);
        config.setFailOnError(false);
        config.setScanRoots(List.of(tempDir.resolve("src/main/nl").toFile()));

        TargetSettings target = new TargetSettings();
        target.setMain("src/main/java");
        target.setTest("src/test/java");
        config.setTarget(target);
        TrackingSettings tracking = new TrackingSettings();
        tracking.setFreezeState(freeze);
        config.setTracking(tracking);
        return config;
    }

    private ReinsConfig buildConfigWithExplicitFreezeState(boolean freeze) {
        return buildConfig(freeze);
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
