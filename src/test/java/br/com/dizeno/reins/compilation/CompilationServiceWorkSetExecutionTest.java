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
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import org.apache.maven.plugin.logging.Log;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompilationServiceWorkSetExecutionTest {

    @TempDir
    Path tempDir;

    @Test
    void revalidatesSkippedParentWhenChangedChildInvalidatesIt() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        OutputWriter outputWriter = mock(OutputWriter.class);
        ResultPrinter resultPrinter = mock(ResultPrinter.class);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);
        SourceFingerprintService fingerprintService = new SourceFingerprintService();
        RecompilationDecider recompilationDecider = new RecompilationDecider();
        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver processingOrderResolver = mock(ProcessingOrderResolver.class);
        ReasoningService reasoningService = mock(ReasoningService.class);

        CompilationService service = new CompilationService(
                inferenceService,
                outputWriter,
                resultPrinter,
                trackingStore,
                fingerprintService,
                recompilationDecider,
                graphBuilder,
                processingOrderResolver,
                reasoningService);

        Path childSource = writeMarkdown("src/main/nl/domain/node.md", "# node");
        Path parentSource = writeMarkdown("src/main/nl/domain/app.md", "# app\n\nUses [node.md]");

        MarkdownDependencyGraph graph = graphFor(childSource, parentSource);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph))
                .thenReturn(List.of("src/main/nl/domain/node.md", "src/main/nl/domain/app.md"));
        when(trackingStore.canonicalizePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.load(any(), any())).thenReturn(Optional.empty());

        ReasoningResult childResult = new ReasoningResult();
        childResult.setFinalIntent("finish_success");
        childResult.setWrittenPaths(List.of(tempDir.resolve("src/main/java/compiled/Node.java").toString()));
        childResult.setUserFacingMessages(List.of("compiled node"));

        ReasoningResult parentResult = new ReasoningResult();
        parentResult.setFinalIntent("finish_success");
        parentResult.setWrittenPaths(List.of());
        parentResult.setUserFacingMessages(List.of("validated parent"));

        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenReturn(childResult, parentResult);

        ReinsConfig config = config();
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(childSource.toFile(), parentSource.toFile()),
                List.of(
                        new CycleWorkSetEntry(childSource.toFile(), "src/main/nl/domain/node.md", SourceProcessingStatus.COMPILE),
                        new CycleWorkSetEntry(parentSource.toFile(), "src/main/nl/domain/app.md", SourceProcessingStatus.SKIP)),
                true,
                List.of());

        CompilationSummary summary = service.processFiles(preFilterResult, config, tempDir, mock(Log.class));

        assertEquals(2, summary.getProcessed());
        assertEquals(0, summary.getSkipped());
        assertEquals(1, summary.getCompiled());
        assertEquals(1, summary.getReprocessedDueToChildChange());

        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        verify(reasoningService, times(2)).runCycle(requestCaptor.capture(), any(ReinsConfig.class));
        List<ReasoningRequest> requests = requestCaptor.getAllValues();
        assertTrue(requests.get(0).getMessage().startsWith("Build the files necessary"));
        assertTrue(requests.get(1).getMessage().startsWith("Revalidate the files previously compiled"));
        assertEquals("src/main/nl/domain/app.md", requests.get(1).getSourcePath());
    }

    @Test
    void validateOnlyEntryRunsThroughValidatePathAndReportsPromotionCount() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        OutputWriter outputWriter = mock(OutputWriter.class);
        ResultPrinter resultPrinter = mock(ResultPrinter.class);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);
        SourceFingerprintService fingerprintService = new SourceFingerprintService();
        RecompilationDecider recompilationDecider = new RecompilationDecider();
        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver processingOrderResolver = mock(ProcessingOrderResolver.class);
        ReasoningService reasoningService = mock(ReasoningService.class);

        CompilationService service = new CompilationService(
                inferenceService,
                outputWriter,
                resultPrinter,
                trackingStore,
                fingerprintService,
                recompilationDecider,
                graphBuilder,
                processingOrderResolver,
                reasoningService);

        Path source = writeMarkdown("src/main/nl/domain/validate.md", "# validate");
        MarkdownDependencyGraph graph = graphForSingle(source);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph))
                .thenReturn(List.of("src/main/nl/domain/validate.md"));
        when(trackingStore.canonicalizePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.load(any(), any())).thenReturn(Optional.empty());

        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        result.setUserFacingMessages(List.of("validated"));
        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenReturn(result);

        ReinsConfig config = config();
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(source.toFile()),
                List.of(new CycleWorkSetEntry(source.toFile(), "src/main/nl/domain/validate.md", SourceProcessingStatus.VALIDATE)),
                false,
                List.of(),
                1);

        CompilationSummary summary = service.processFiles(preFilterResult, config, tempDir, mock(Log.class));

        assertEquals(1, summary.getProcessed());
        assertEquals(1, summary.getValidateAllPromoted());

        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        verify(reasoningService).runCycle(requestCaptor.capture(), any(ReinsConfig.class));
        assertEquals(SourceProcessingStatus.VALIDATE, requestCaptor.getValue().getProcessingStatus());
    }

    @Test
    void keepsCompileEntriesInCompileModeWhenChildChanges() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        OutputWriter outputWriter = mock(OutputWriter.class);
        ResultPrinter resultPrinter = mock(ResultPrinter.class);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);
        SourceFingerprintService fingerprintService = new SourceFingerprintService();
        RecompilationDecider recompilationDecider = new RecompilationDecider();
        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver processingOrderResolver = mock(ProcessingOrderResolver.class);
        ReasoningService reasoningService = mock(ReasoningService.class);

        CompilationService service = new CompilationService(
                inferenceService,
                outputWriter,
                resultPrinter,
                trackingStore,
                fingerprintService,
                recompilationDecider,
                graphBuilder,
                processingOrderResolver,
                reasoningService);

        Path childSource = writeMarkdown("src/main/nl/domain/node.md", "# node");
        Path parentSource = writeMarkdown("src/main/nl/domain/app.md", "# app\n\nUses [node.md]");

        MarkdownDependencyGraph graph = graphFor(childSource, parentSource);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph))
                .thenReturn(List.of("src/main/nl/domain/node.md", "src/main/nl/domain/app.md"));
        when(trackingStore.canonicalizePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.load(any(), any())).thenReturn(Optional.empty());

        ReasoningResult childResult = new ReasoningResult();
        childResult.setFinalIntent("finish_success");
        childResult.setWrittenPaths(List.of(tempDir.resolve("src/main/java/compiled/Node.java").toString()));

        ReasoningResult parentResult = new ReasoningResult();
        parentResult.setFinalIntent("finish_success");
        parentResult.setWrittenPaths(List.of(tempDir.resolve("src/main/java/compiled/App.java").toString()));

        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenReturn(childResult, parentResult);

        ReinsConfig config = config();
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(childSource.toFile(), parentSource.toFile()),
                List.of(
                        new CycleWorkSetEntry(childSource.toFile(), "src/main/nl/domain/node.md", SourceProcessingStatus.COMPILE),
                        new CycleWorkSetEntry(parentSource.toFile(), "src/main/nl/domain/app.md", SourceProcessingStatus.COMPILE)),
                false,
                List.of());

        CompilationSummary summary = service.processFiles(preFilterResult, config, tempDir, mock(Log.class));

        assertEquals(2, summary.getProcessed());
        assertEquals(0, summary.getReprocessedDueToChildChange());

        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        verify(reasoningService, times(2)).runCycle(requestCaptor.capture(), any(ReinsConfig.class));
        List<ReasoningRequest> requests = requestCaptor.getAllValues();
        assertTrue(requests.get(0).getMessage().startsWith("Build the files necessary"));
        assertTrue(requests.get(1).getMessage().startsWith("Build the files necessary"));
    }

    @Test
    void executesQueueInResolvedDependencyOrderWhenWorkSetOrderDiffers() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        OutputWriter outputWriter = mock(OutputWriter.class);
        ResultPrinter resultPrinter = mock(ResultPrinter.class);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);
        SourceFingerprintService fingerprintService = new SourceFingerprintService();
        RecompilationDecider recompilationDecider = new RecompilationDecider();
        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver processingOrderResolver = mock(ProcessingOrderResolver.class);
        ReasoningService reasoningService = mock(ReasoningService.class);

        CompilationService service = new CompilationService(
                inferenceService,
                outputWriter,
                resultPrinter,
                trackingStore,
                fingerprintService,
                recompilationDecider,
                graphBuilder,
                processingOrderResolver,
                reasoningService);

        Path childSource = writeMarkdown("src/main/nl/domain/node.md", "# node");
        Path parentSource = writeMarkdown("src/main/nl/domain/app.md", "# app\n\nUses [node.md]");

        MarkdownDependencyGraph graph = graphFor(childSource, parentSource);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph))
                .thenReturn(List.of("src/main/nl/domain/node.md", "src/main/nl/domain/app.md"));
        when(trackingStore.canonicalizePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.load(any(), any())).thenReturn(Optional.empty());

        ReasoningResult childResult = new ReasoningResult();
        childResult.setFinalIntent("finish_success");
        childResult.setWrittenPaths(List.of(tempDir.resolve("src/main/java/compiled/Node.java").toString()));

        ReasoningResult parentResult = new ReasoningResult();
        parentResult.setFinalIntent("finish_success");
        parentResult.setWrittenPaths(List.of(tempDir.resolve("src/main/java/compiled/App.java").toString()));

        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenReturn(childResult, parentResult);

        ReinsConfig config = config();
        PreFilterResult preFilterResult = new PreFilterResult(
                List.of(childSource.toFile(), parentSource.toFile()),
                List.of(
                        new CycleWorkSetEntry(parentSource.toFile(), "src/main/nl/domain/app.md", SourceProcessingStatus.COMPILE),
                        new CycleWorkSetEntry(childSource.toFile(), "src/main/nl/domain/node.md", SourceProcessingStatus.COMPILE)),
                true,
                List.of());

        service.processFiles(preFilterResult, config, tempDir, mock(Log.class));

        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        verify(reasoningService, times(2)).runCycle(requestCaptor.capture(), any(ReinsConfig.class));
        List<ReasoningRequest> requests = requestCaptor.getAllValues();
        assertEquals("src/main/nl/domain/node.md", requests.get(0).getSourcePath());
        assertEquals("src/main/nl/domain/app.md", requests.get(1).getSourcePath());
    }

    private ReinsConfig config() {
        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        config.setGemini(gemini);

        TargetSettings target = new TargetSettings();
        target.setProject(tempDir.toFile());
        target.setRoot(tempDir.toFile());
        target.setMain("src/main/java");
        target.setTest("src/test/java");
        config.setTarget(target);

        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        config.setReasoning(reasoning);
        return config;
    }

    private Path writeMarkdown(String relativePath, String content) throws Exception {
        Path path = tempDir.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    private MarkdownDependencyGraph graphFor(Path childSource, Path parentSource) throws Exception {
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/domain/node.md", new MarkdownSourceNode(
                "src/main/nl/domain/node.md",
                childSource,
                Files.getLastModifiedTime(childSource).toMillis(),
                List.of()));
        nodes.put("src/main/nl/domain/app.md", new MarkdownSourceNode(
                "src/main/nl/domain/app.md",
                parentSource,
                Files.getLastModifiedTime(parentSource).toMillis(),
                List.of("node.md")));

        Map<String, List<String>> edges = new LinkedHashMap<>();
        edges.put("src/main/nl/domain/node.md", List.of());
        edges.put("src/main/nl/domain/app.md", List.of("src/main/nl/domain/node.md"));

        Map<String, List<String>> reverseEdges = new LinkedHashMap<>();
        reverseEdges.put("src/main/nl/domain/node.md", List.of("src/main/nl/domain/app.md"));
        reverseEdges.put("src/main/nl/domain/app.md", List.of());

        return new MarkdownDependencyGraph(nodes, edges, reverseEdges, List.of("src/main/nl/domain/app.md"));
    }

        private MarkdownDependencyGraph graphForSingle(Path source) throws Exception {
                Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
                nodes.put("src/main/nl/domain/validate.md", new MarkdownSourceNode(
                                "src/main/nl/domain/validate.md",
                                source,
                                Files.getLastModifiedTime(source).toMillis(),
                                List.of()));

                Map<String, List<String>> edges = new LinkedHashMap<>();
                edges.put("src/main/nl/domain/validate.md", List.of());

                Map<String, List<String>> reverseEdges = new LinkedHashMap<>();
                reverseEdges.put("src/main/nl/domain/validate.md", List.of());

                return new MarkdownDependencyGraph(nodes, edges, reverseEdges, List.of("src/main/nl/domain/validate.md"));
        }
}
