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
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompilationServiceContextSourcesTest {

    @TempDir
    Path tempDir;

    

    @Test
    void singleNonEmptySource_logsInfoWithConfiguredExpression() throws Exception {
        Path sourceDoc = write("docs/context.md", "# Shared context guidance");
        Path nlSource = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, nlSource);
        ReinsConfig config = sourcesConfig(List.of(sourceDoc.toFile()));
        Log log = mock(Log.class);

        service.processFiles(List.of(nlSource.toFile()), config, tempDir, log);

        ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(infoCaptor.capture());
        boolean hasSourcesLog = infoCaptor.getAllValues().stream()
                .anyMatch(m -> m.contains("[context.sources] Loaded:"));
        assertTrue(hasSourcesLog, "Expected [INFO] '[context.sources] Loaded:' to be emitted");

        
        boolean usesFilename = infoCaptor.getAllValues().stream()
                .filter(m -> m.contains("[context.sources] Loaded:"))
                .anyMatch(m -> m.contains("context.md"));
        assertTrue(usesFilename, "INFO log should contain 'context.md'");
    }

    @Test
    void blankSourceFile_logsWarnAndNoInfoForThatEntry() throws Exception {
        Path blankDoc = write("docs/blank.md", "   ");
        Path nlSource = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, nlSource);
        ReinsConfig config = sourcesConfig(List.of(blankDoc.toFile()));
        Log log = mock(Log.class);

        service.processFiles(List.of(nlSource.toFile()), config, tempDir, log);

        ArgumentCaptor<String> warnCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).warn(warnCaptor.capture());
        boolean hasBlankWarn = warnCaptor.getAllValues().stream()
                .anyMatch(m -> m.contains("[context.sources]") && m.contains("blank"));
        assertTrue(hasBlankWarn, "Expected [WARN] about blank context source; got: " + warnCaptor.getAllValues());
        assertTrue(warnCaptor.getAllValues().stream()
                .filter(m -> m.contains("[context.sources]") && m.contains("blank"))
                .anyMatch(m -> m.contains("blank.md")),
                "WARN should identify the blank file");
    }

    @Test
    void emptySources_noSourcesLoadedNoExtraLogging() throws Exception {
        Path nlSource = write("src/main/nl/domain.md", "# domain");

        
        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, nlSource);
        ReinsConfig config = sourcesConfig(List.of()); 
        Log log = mock(Log.class);

        
        service.processFiles(List.of(nlSource.toFile()), config, tempDir, log);

        ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
        try {
            verify(log, atLeastOnce()).info(infoCaptor.capture());
            boolean hasSourcesLog = infoCaptor.getAllValues().stream()
                    .anyMatch(m -> m.contains("[context.sources]"));
            assertFalse(hasSourcesLog, "Should not emit any [context.sources] INFO when empty");
        } catch (org.mockito.exceptions.verification.WantedButNotInvoked ignored) {
            
        }
    }

    

    @Test
    void projectFilesAndSources_projectContentBeforeSourcesInPayload() throws Exception {
        Path projectCtx = write("src/project.md", "# Compilation background file");
        Path sourceDoc = write("docs/api.md", "# API context");
        Path nlSource = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);

        
        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, nlSource);
        ReinsConfig config = projectAndSourcesConfig(projectCtx.toFile(), List.of(sourceDoc.toFile()));
        Log log = mock(Log.class);

        service.processFiles(List.of(nlSource.toFile()), config, tempDir, log);

        ReasoningRequest captured = requestCaptor.getValue();
        List<CompilationBackgroundFile> files = captured.getCompilationBackgroundPayload().getFiles();
        assertFalse(files.isEmpty(), "Expected context payload to contain files");
        
        assertEquals("src/project.md", files.get(0).getDisplayPath(),
                "Compilation background file must appear first in payload");
        
        boolean hasApiDoc = files.stream().anyMatch(f -> f.getDisplayPath().contains("api.md"));
        assertTrue(hasApiDoc, "Source doc api.md must appear in payload");
        int projectIdx = indexOf(files, "src/project.md");
        int apiIdx = indexOfPartial(files, "api.md");
        assertTrue(projectIdx < apiIdx, "project.md must appear before api.md in payload");
    }

    @Test
    void sameFileInProjectAndSources_appearsOnce() throws Exception {
        
        Path shared = write("docs/shared.md", "# Shared content");
        Path projectCtx = write("src/project.md", "[../docs/shared.md]");
        Path nlSource = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);

        ArgumentCaptor<ReasoningRequest> requestCaptor = ArgumentCaptor.forClass(ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, nlSource);
        ReinsConfig config = projectAndSourcesConfig(projectCtx.toFile(), List.of(shared.toFile()));
        Log log = mock(Log.class);

        service.processFiles(List.of(nlSource.toFile()), config, tempDir, log);

        ReasoningRequest captured = requestCaptor.getValue();
        List<CompilationBackgroundFile> files = captured.getCompilationBackgroundPayload().getFiles();
        long sharedCount = files.stream()
                .filter(f -> f.getDisplayPath().endsWith("shared.md"))
                .count();
        assertEquals(1, sharedCount,
                "shared.md referenced by both project tree and context.sources must appear exactly once; got " + sharedCount);
    }

    

    private Path write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private ReinsConfig sourcesConfig(List<File> sources) {
        ReinsConfig config = new ReinsConfig();
        config.setFailOnError(false);
        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        reasoning.setMaxTurns(1);
        config.setReasoning(reasoning);
        ContextSettings ctx = new ContextSettings();
        ctx.setIncludeProjectFiles(false);
        ctx.setSources(sources);
        config.setContext(ctx);
        LoggingSettings logging = new LoggingSettings();
        logging.setFileListingAndReading(true);
        config.setLogging(logging);
        config.setProjectContextFile(tempDir.resolve("project.md").toFile()); 
        config.setMainNlRoot(tempDir.resolve("src/main/nl").toFile());
        config.setTestNlRoot(tempDir.resolve("src/test/nl").toFile());
        return config;
    }

    private ReinsConfig projectAndSourcesConfig(File projectFile, List<File> sources) {
        ReinsConfig config = new ReinsConfig();
        config.setFailOnError(false);
        config.setEnableProjectInference(true);
        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        reasoning.setMaxTurns(1);
        config.setReasoning(reasoning);
        ContextSettings ctx = new ContextSettings();
        ctx.setIncludeProjectFiles(true);
        ctx.setSources(sources);
        config.setContext(ctx);
        config.setProjectContextFile(projectFile);
        config.setMainNlRoot(tempDir.resolve("src/main/nl").toFile());
        config.setTestNlRoot(tempDir.resolve("src/test/nl").toFile());
        return config;
    }

    private CompilationService buildService(ReasoningService reasoningService,
                                           ProjectContextService contextService,
                                           Path nlSource) throws Exception {
        String relPath = "src/main/nl/domain.md";
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(relPath, new MarkdownSourceNode(
                relPath, nlSource, Files.getLastModifiedTime(nlSource).toMillis(), List.of()));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(relPath));

        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver orderResolver = mock(ProcessingOrderResolver.class);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(orderResolver.resolve(graph)).thenReturn(List.of(relPath));

        CompilationTrackingStore store = mock(CompilationTrackingStore.class);

        return new CompilationService(
                mock(InferenceService.class),
                mock(OutputWriter.class),
                mock(ResultPrinter.class),
                store,
                new SourceFingerprintService(),
                new RecompilationDecider(),
                graphBuilder,
                orderResolver,
                reasoningService,
                contextService,
                null
        );
    }

    private ReasoningResult finishSuccessResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setTerminalReasonMessage("done");
        result.setWrittenPaths(List.of());
        result.setToolInfoPhrases(List.of());
        return result;
    }

    private int indexOf(List<CompilationBackgroundFile> files, String displayPath) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getDisplayPath().equals(displayPath)) return i;
        }
        return -1;
    }

    private int indexOfPartial(List<CompilationBackgroundFile> files, String partial) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).getDisplayPath().contains(partial)) return i;
        }
        return -1;
    }
}
