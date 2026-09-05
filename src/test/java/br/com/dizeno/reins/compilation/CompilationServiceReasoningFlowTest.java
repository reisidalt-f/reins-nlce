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
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.EagerlyProvideResult;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompilationServiceReasoningFlowTest {

    @TempDir
    Path tempDir;

    private ReinsConfig createValidConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", tempDir.resolve("src/main/nl").toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        return config;
    }

    @Test
    void usesReasoningTerminalReasonWhenNoBlocksAreCompiled() throws Exception {
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
                reasoningService
        );

        Path source = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# entities", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/domain/entities.md", new MarkdownSourceNode(
                "src/main/nl/domain/entities.md",
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of("src/main/nl/domain/entities.md"));
        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/domain/entities.md"));

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_error");
        reasoningResult.setTerminalReasonMessage("Reasoning could not conclude because max turns were reached without a valid finish intent.");

        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", tempDir.resolve("src/main/nl").toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        config.setFailOnError(true);
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        settings.setMaxTurns(1);
        config.setReasoning(settings);

        Log log = mock(Log.class);

        assertThrows(Exception.class,
                () -> service.processFiles(List.of(source.toFile()), config, tempDir, log));
        
        
    }

    @Test
    void canonicalSourcePath_isNotDoubleQualifiedInMainSourcePath() throws Exception {
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
                reasoningService
        );

        Path source = tempDir.resolve("src/main/nl/md/application.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# application", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/md/application.md", new MarkdownSourceNode(
                "src/main/nl/md/application.md",
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of("src/main/nl/md/application.md"));
        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/md/application.md"));
        when(trackingStore.canonicalizePath("src/main/nl/md/application.md")).thenReturn("main:md/application.md");
        when(trackingStore.load(any(), any())).thenReturn(java.util.Optional.empty());

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_success");
        reasoningResult.setWrittenPaths(List.of());
        reasoningResult.setToolInfoPhrases(List.of());
        reasoningResult.setUserFacingMessages(List.of("done"));
        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        ReinsConfig config = createValidConfig();
        config.setFailOnError(false);
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        settings.setMaxTurns(1);
        config.setReasoning(settings);

        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(requestCaptor.capture(), any());

        assertEquals("main:md/application.md", requestCaptor.getValue().getMainSourceQualifiedPath());
    }

    @Test
    void processesOnlyRequestedSources_whenGraphAddsReferencedNodes() throws Exception {
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
                reasoningService
        );

        Path nodeSource = tempDir.resolve("src/main/nl/domain/node.md");
        Files.createDirectories(nodeSource.getParent());
        Files.writeString(nodeSource, "# node", StandardCharsets.UTF_8);

        Path appSource = tempDir.resolve("src/main/nl/domain/app.md");
        Files.createDirectories(appSource.getParent());
        Files.writeString(appSource, "# app\n\nUses [node.md]", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/domain/node.md", new MarkdownSourceNode(
                "src/main/nl/domain/node.md",
                nodeSource,
                Files.getLastModifiedTime(nodeSource).toMillis(),
                List.of()
        ));
        nodes.put("src/main/nl/domain/app.md", new MarkdownSourceNode(
                "src/main/nl/domain/app.md",
                appSource,
                Files.getLastModifiedTime(appSource).toMillis(),
                List.of("node.md")
        ));
        Map<String, List<String>> edges = new LinkedHashMap<>();
        edges.put("src/main/nl/domain/app.md", List.of("src/main/nl/domain/node.md"));
        edges.put("src/main/nl/domain/node.md", List.of());
        Map<String, List<String>> reverseEdges = new LinkedHashMap<>();
        reverseEdges.put("src/main/nl/domain/node.md", List.of("src/main/nl/domain/app.md"));
        reverseEdges.put("src/main/nl/domain/app.md", List.of());
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, edges, reverseEdges, List.of("src/main/nl/domain/app.md"));

        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph))
                .thenReturn(List.of("src/main/nl/domain/node.md", "src/main/nl/domain/app.md"));
        when(trackingStore.canonicalizePath(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.load(any(), any())).thenReturn(java.util.Optional.empty());

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_success");
        reasoningResult.setWrittenPaths(List.of());
        reasoningResult.setToolInfoPhrases(List.of());
        reasoningResult.setUserFacingMessages(List.of("done"));
        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        ReinsConfig config = createValidConfig();
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        config.setReasoning(settings);

        service.processFiles(List.of(appSource.toFile()), config, tempDir, mock(Log.class));

        verify(reasoningService).runCycle(argThat(req -> "src/main/nl/domain/app.md".equals(req.getSourcePath())), any());
        verify(reasoningService, never()).runCycle(argThat(req -> "src/main/nl/domain/node.md".equals(req.getSourcePath())), any());
    }

    @Test
    void singleTurnIncludesEagerlyProvidedAttachmentsFromReferencedTrackedSources() throws Exception {
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
                reasoningService
        );

        Path appSource = tempDir.resolve("src/main/nl/md/application.md");
        Path editorSource = tempDir.resolve("src/main/nl/md/editor/editor.md");
        Path compiledEditor = tempDir.resolve("target/compiled-sources/md/editor/Editor.java");
        Files.createDirectories(appSource.getParent());
        Files.createDirectories(editorSource.getParent());
        Files.createDirectories(compiledEditor.getParent());
        Files.writeString(appSource, "# application\n\nUses [editor/editor.md]", StandardCharsets.UTF_8);
        Files.writeString(editorSource, "# editor", StandardCharsets.UTF_8);
        Files.writeString(compiledEditor, "package md.editor;\npublic class Editor {}\n", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/md/application.md", new MarkdownSourceNode(
                "src/main/nl/md/application.md",
                appSource,
                Files.getLastModifiedTime(appSource).toMillis(),
                List.of("editor/editor.md")
        ));
        nodes.put("src/main/nl/md/editor/editor.md", new MarkdownSourceNode(
                "src/main/nl/md/editor/editor.md",
                editorSource,
                Files.getLastModifiedTime(editorSource).toMillis(),
                List.of()
        ));

        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(
                nodes,
                Map.of(
                        "src/main/nl/md/application.md", List.of("src/main/nl/md/editor/editor.md"),
                        "src/main/nl/md/editor/editor.md", List.of()
                ),
                Map.of(
                        "src/main/nl/md/application.md", List.of(),
                        "src/main/nl/md/editor/editor.md", List.of("src/main/nl/md/application.md")
                ),
                List.of("src/main/nl/md/application.md")
        );

        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/md/application.md"));
        when(trackingStore.canonicalizePath("src/main/nl/md/application.md")).thenReturn("main:md/application.md");
        when(trackingStore.canonicalizePath("src/main/nl/md/editor/editor.md")).thenReturn("main:md/editor/editor.md");

        SourceTrackingRecord rootRecord = new SourceTrackingRecord();
        rootRecord.setSourcePath("main:md/application.md");
        rootRecord.setResolvedTargetRoot("target/compiled-sources");

        SourceTrackingRecord referencedRecord = new SourceTrackingRecord();
        referencedRecord.setSourcePath("main:md/editor/editor.md");
        referencedRecord.setResolvedTargetRoot("target/compiled-sources");
        referencedRecord.setCompiledFiles(Map.of(
                "target:md/editor/Editor.java",
                new FileTrackingDetails("main:md/editor/editor.md", "main", Files.getLastModifiedTime(compiledEditor).toMillis())
        ));

        when(trackingStore.load(any(), any())).thenAnswer(invocation -> {
            String sourcePath = invocation.getArgument(1, String.class);
            if ("main:md/application.md".equals(sourcePath)) {
                return Optional.of(rootRecord);
            }
            if ("main:md/editor/editor.md".equals(sourcePath)) {
                return Optional.of(referencedRecord);
            }
            return Optional.empty();
        });

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_success");
        reasoningResult.setWrittenPaths(List.of());
        reasoningResult.setToolInfoPhrases(List.of());
        reasoningResult.setUserFacingMessages(List.of("done"));
        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        ReinsConfig config = createValidConfig();
        config.setFailOnError(false);
        ContextSettings context = new ContextSettings();
        context.getReferencesTree().setAttachFiles(true);
        context.getReferencesTree().setDepth("*");
        context.setCompiledFiles(true);
        context.setInspectedFiles(false);
        config.setContext(context);

        service.processFiles(List.of(appSource.toFile()), config, tempDir, mock(Log.class));

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(requestCaptor.capture(), any());

        EagerlyProvideResult captured = requestCaptor.getValue().getEagerlyProvide();
        assertTrue(captured.getCompiledAttachments().stream()
                        .anyMatch(a -> "target:md/editor/Editor.java".equals(a.getQualifiedPath())),
                "Expected eagerly provided attachments to include compiled files from referenced tracked sources");
        assertTrue(captured.getCompiledSourceGroups().stream()
                        .anyMatch(g -> "main:md/editor/editor.md".equals(g.getSourceCanonicalPath())
                                && g.getCompiledAttachments().stream()
                                .anyMatch(a -> "target:md/editor/Editor.java".equals(a.getQualifiedPath()))),
                "Expected grouped source entries to include referenced source compiled outputs");
        assertTrue(captured.getCompiledSourceGroups().stream()
                        .anyMatch(g -> "main:md/application.md".equals(g.getSourceCanonicalPath())),
                "Expected grouped source entries to include main source even when it has no compiled outputs");
        assertTrue(captured.getCompiledSourceGroups().stream()
                        .filter(g -> "main:md/application.md".equals(g.getSourceCanonicalPath()))
                        .allMatch(g -> g.getCompiledAttachments().isEmpty()),
                "Expected main source group to be marked with no compiled outputs when none are tracked");
    }

    @Test
    void successfulCompilation_preservesExistingCompiledFilesThatStillExist() throws Exception {
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
                reasoningService
        );

        Path source = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# entities", StandardCharsets.UTF_8);

        Path keptTarget = tempDir.resolve("src/main/java/domain/KeptType.java");
        Files.createDirectories(keptTarget.getParent());
        Files.writeString(keptTarget, "class KeptType {}\n", StandardCharsets.UTF_8);

        Path newTarget = tempDir.resolve("src/main/java/domain/NewType.java");
        Files.createDirectories(newTarget.getParent());
        Files.writeString(newTarget, "class NewType {}\n", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/domain/entities.md", new MarkdownSourceNode(
                "src/main/nl/domain/entities.md",
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of("src/main/nl/domain/entities.md"));
        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/domain/entities.md"));
        when(trackingStore.canonicalizePath(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingStore.canonicalizePath("src/main/nl/domain/entities.md"))
                .thenReturn("main:domain/entities.md");

        SourceTrackingRecord previous = new SourceTrackingRecord();
        previous.setSourcePath("main:domain/entities.md");
        previous.setSourceCategory("main");
        previous.setCompiledFiles(Map.of(
                "target:domain/KeptType.java", new FileTrackingDetails("main:domain/entities.md", "main", 10L),
                "target:domain/RemovedType.java", new FileTrackingDetails("main:domain/entities.md", "main", 20L)
        ));
        when(trackingStore.load(any(), any())).thenReturn(Optional.of(previous));

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_success");
        reasoningResult.setWrittenPaths(List.of("src/main/java/domain/NewType.java"));
        reasoningResult.setInspectedPaths(List.of());
        reasoningResult.setReadMarkdownPaths(List.of());
        reasoningResult.setToolInfoPhrases(List.of());
        reasoningResult.setUserFacingMessages(List.of("done"));
        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        ReinsConfig config = createValidConfig();
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        config.setReasoning(settings);

        service.processFiles(List.of(source.toFile()), config, tempDir, mock(Log.class));

        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(any(), any(), recordCaptor.capture());

        SourceTrackingRecord saved = recordCaptor.getValue();
        assertEquals(Set.of(
                        "target:domain/KeptType.java",
                        "target:domain/NewType.java"),
                saved.getCompiledFiles().keySet());
    }

    @Test
    void singleTurnDropsNonCanonicalStoredCompiledPathsInsteadOfAcceptingThem() throws Exception {
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
                reasoningService
        );

        Path source = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# entities", StandardCharsets.UTF_8);

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/domain/entities.md", new MarkdownSourceNode(
                "src/main/nl/domain/entities.md",
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of("src/main/nl/domain/entities.md"));
        when(graphBuilder.build(any(), any(), any(), any())).thenReturn(graph);
        when(processingOrderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/domain/entities.md"));
        when(trackingStore.canonicalizePath("src/main/nl/domain/entities.md"))
                .thenReturn("main:domain/entities.md");

        Path validTarget = tempDir.resolve("src/main/java/domain/ValidType.java");
        Files.createDirectories(validTarget.getParent());
        Files.writeString(validTarget, "class ValidType {}\n", StandardCharsets.UTF_8);

        SourceTrackingRecord previous = new SourceTrackingRecord();
        previous.setSourcePath("main:domain/entities.md");
        previous.setSourceCategory("main");
        previous.setCompiledFiles(Map.of(
                "src/main/java/domain/LegacyType.java",
                new FileTrackingDetails("main:domain/entities.md", "main", 10L)
        ));
        when(trackingStore.load(any(), any())).thenReturn(Optional.of(previous));

        ReasoningResult reasoningResult = new ReasoningResult();
        reasoningResult.setFinalIntent("finish_success");
        reasoningResult.setWrittenPaths(List.of("src/main/java/domain/ValidType.java"));
        reasoningResult.setInspectedPaths(List.of());
        reasoningResult.setReadMarkdownPaths(List.of());
        reasoningResult.setToolInfoPhrases(List.of());
        reasoningResult.setUserFacingMessages(List.of("done"));
        when(reasoningService.runCycle(any(), any())).thenReturn(reasoningResult);

        Log log = mock(Log.class);
        ReinsConfig config = createValidConfig();
        config.setFailOnError(false);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(any(), any(), recordCaptor.capture());

        SourceTrackingRecord saved = recordCaptor.getValue();
        assertEquals(Set.of("target:domain/ValidType.java"), saved.getCompiledFiles().keySet(), "Non-canonical compiled paths should be dropped, not preserved");
    }

            
            
}
