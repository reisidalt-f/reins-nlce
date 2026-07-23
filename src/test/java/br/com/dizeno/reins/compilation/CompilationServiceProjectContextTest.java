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
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompilationServiceProjectContextTest {

    @TempDir
    Path tempDir;

    

    @Test
    void includeProjectFilesTrue_nonEmptyContextFile_loadsPayloadAndLogsFileName() throws Exception {
        Path contextFile = write("src/project.md", "# Project guidance");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(infoCaptor.capture());
        List<String> infos = infoCaptor.getAllValues();
        boolean hasContextLog = infos.stream().anyMatch(m -> m.contains("Compilation background loaded"));
        assertTrue(hasContextLog, "Expected INFO 'Compilation background loaded' to be emitted");
        boolean usesFileName = infos.stream()
                .filter(m -> m.contains("Compilation background loaded"))
                .anyMatch(m -> m.contains("project.md") && !m.contains(tempDir.toAbsolutePath().toString()));
        assertTrue(usesFileName, "INFO log should contain 'project.md' (not absolute path)");
    }

    @Test
    void includeProjectFilesTrue_absentContextFile_returnsEmptyNoError() throws Exception {
        File absentContextFile = tempDir.resolve("project.md").toFile();
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(absentContextFile);
        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        try {
            ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
            verify(log, atLeastOnce()).info(infoCaptor.capture());
            boolean hasContextLog = infoCaptor.getAllValues().stream()
                    .anyMatch(m -> m.contains("Compilation background loaded"));
            assertFalse(hasContextLog, "Should not log 'Compilation background loaded' when file is absent");
        } catch (org.mockito.exceptions.verification.WantedButNotInvoked ignored) {
            
        }
    }

    @Test
    void includeProjectFilesTrue_multipleSourceFiles_infoLoggedOnceNotPerSource() throws Exception {
        Path contextFile = write("src/project.md", "# Project guidance");
        Path source1 = write("src/main/nl/domain.md", "# domain");
        Path source2 = write("src/main/nl/service.md", "# service");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildServiceWithTwoSources(reasoningService, realContext, source1, source2);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        Log log = mock(Log.class);

        service.processFiles(List.of(source1.toFile(), source2.toFile()), config, tempDir, log);

        ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(infoCaptor.capture());
        long contextLogCount = infoCaptor.getAllValues().stream()
                .filter(m -> m.contains("Compilation background loaded"))
                .count();
        assertTrue(contextLogCount == 1,
                "'Compilation background loaded' should appear exactly once per build, but appeared " + contextLogCount + " times");
    }

        @Test
        void defaultReferenceDepthLogsAndKeepsOnlyDirectProjectContextDescendants() throws Exception {
        Path contextFile = write("src/project.md", "[child.md]");
        write("src/child.md", "[grandchild.md]");
        write("src/grandchild.md", "# Grandchild");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
            ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        List<br.com.dizeno.reins.compilation.context.CompilationBackgroundFile> files =
            requestCaptor.getValue().getCompilationBackgroundPayload().getFiles();
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(file -> file.getDisplayPath().equals("src/project.md")));
        assertTrue(files.stream().anyMatch(file -> file.getDisplayPath().equals("src/child.md")));
        assertFalse(files.stream().anyMatch(file -> file.getDisplayPath().equals("src/grandchild.md")));

        ArgumentCaptor<String> infoCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(infoCaptor.capture());
        assertTrue(infoCaptor.getAllValues().stream().anyMatch(message -> message.contains("Reference tree depth: default (1)")));
        }

        @Test
        void referenceDepthZeroKeepsOnlyProjectContextRoot() throws Exception {
        Path contextFile = write("src/project.md", "[child.md]");
        write("src/child.md", "# Child");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
            ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        config.getContext().setReferencesTreeDepth("0");

        service.processFiles(List.of(source.toFile()), config, tempDir, mock(Log.class));

        List<br.com.dizeno.reins.compilation.context.CompilationBackgroundFile> files =
            requestCaptor.getValue().getCompilationBackgroundPayload().getFiles();
        assertEquals(1, files.size());
        assertEquals("src/project.md", files.get(0).getDisplayPath());
        }

        @Test
        void referenceDepthTwoIncludesSecondLevelProjectContextDescendants() throws Exception {
        Path contextFile = write("src/project.md", "[child.md]");
        write("src/child.md", "[grandchild.md]");
        write("src/grandchild.md", "# Grandchild");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
            ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        config.getContext().setReferencesTreeDepth("2");

        service.processFiles(List.of(source.toFile()), config, tempDir, mock(Log.class));

        List<br.com.dizeno.reins.compilation.context.CompilationBackgroundFile> files =
            requestCaptor.getValue().getCompilationBackgroundPayload().getFiles();
        assertEquals(3, files.size());
        assertTrue(files.stream().anyMatch(file -> file.getDisplayPath().equals("src/grandchild.md")));
        }

    @Test
    void attachReferencedFilesFalse_keepsProjectContextRootOnly() throws Exception {
        Path contextFile = write("src/project.md", "[child.md]");
        write("src/child.md", "[grandchild.md]");
        write("src/grandchild.md", "# Grandchild");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService realContext = new ProjectContextService();
        ReasoningService reasoningService = mock(ReasoningService.class);
        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> requestCaptor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        when(reasoningService.runCycle(requestCaptor.capture(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, realContext, source);
        ReinsConfig config = contextEnabledConfig(contextFile.toFile());
        config.getContext().setReferencesTreeDepth("*");
        config.getContext().setAttachReferencedFiles(false);

        service.processFiles(List.of(source.toFile()), config, tempDir, mock(Log.class));

        List<br.com.dizeno.reins.compilation.context.CompilationBackgroundFile> files =
                requestCaptor.getValue().getCompilationBackgroundPayload().getFiles();
        assertEquals(1, files.size());
        assertEquals("src/project.md", files.get(0).getDisplayPath());
    }

    

    @Test
    void includeProjectFilesFalse_contextFilePresent_emitsWarnWithFileNameNotAbsolutePath() throws Exception {
        Path contextFile = write("project.md", "# Project guidance");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService mockContext = mock(ProjectContextService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, mockContext, source);
        ReinsConfig config = contextDisabledConfig(contextFile.toFile());
        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        ArgumentCaptor<String> warnCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).warn(warnCaptor.capture());
        List<String> warns = warnCaptor.getAllValues();
        long warnCount = warns.stream().filter(m -> m.contains("includeProjectFiles")).count();
        assertTrue(warnCount == 1, "Expected exactly one WARN about includeProjectFiles, got " + warnCount);
        boolean usesFileName = warns.stream()
                .filter(m -> m.contains("includeProjectFiles"))
                .anyMatch(m -> m.contains("project.md") && !m.contains(tempDir.toAbsolutePath().toString()));
        assertTrue(usesFileName, "WARN should contain 'project.md' (configured name), not absolute path");
        verify(mockContext, never()).load(any(), any(), org.mockito.ArgumentMatchers.<java.util.Set<java.nio.file.Path>>any());
    }

    @Test
    void includeProjectFilesFalse_contextFileAbsent_noWarnEmitted() throws Exception {
        File absentContextFile = tempDir.resolve("project.md").toFile();
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService mockContext = mock(ProjectContextService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, mockContext, source);
        ReinsConfig config = contextDisabledConfig(absentContextFile);
        Log log = mock(Log.class);

        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        try {
            ArgumentCaptor<String> warnCaptor = ArgumentCaptor.forClass(String.class);
            verify(log, atLeastOnce()).warn(warnCaptor.capture());
            boolean hasContextWarn = warnCaptor.getAllValues().stream()
                    .anyMatch(m -> m.contains("includeProjectFiles"));
            assertFalse(hasContextWarn, "Should not emit WARN about includeProjectFiles when file is absent");
        } catch (org.mockito.exceptions.verification.WantedButNotInvoked ignored) {
            
        }
        verify(mockContext, never()).load(any(), any(), org.mockito.ArgumentMatchers.<java.util.Set<java.nio.file.Path>>any());
    }

    @Test
    void includeProjectFilesFalse_contextServiceNeverCalled() throws Exception {
        
        Path contextFile = write("project.md", "[will-fail-if-read.md]");
        Path source = write("src/main/nl/domain.md", "# domain");

        ProjectContextService mockContext = mock(ProjectContextService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(finishSuccessResult());

        CompilationService service = buildService(reasoningService, mockContext, source);
        ReinsConfig config = contextDisabledConfig(contextFile.toFile());
        Log log = mock(Log.class);

        
        service.processFiles(List.of(source.toFile()), config, tempDir, log);

        verify(mockContext, never()).load(any(), any(), org.mockito.ArgumentMatchers.<java.util.Set<java.nio.file.Path>>any());
    }

    

    private Path write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private ReinsConfig contextEnabledConfig(File contextFile) {
        ReinsConfig config = new ReinsConfig();
        config.setFailOnError(false);
        config.setEnableProjectInference(true);
        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        reasoning.setMaxTurns(1);
        config.setReasoning(reasoning);
        LoggingSettings logging = new LoggingSettings();
        logging.setFileListingAndReading(true);
        config.setLogging(logging);
        ContextSettings ctx = new ContextSettings();
        ctx.setIncludeProjectFiles(true);
        config.setContext(ctx);
        config.setProjectContextFile(contextFile);
        config.setMainNlRoot(tempDir.resolve("src/main/nl").toFile());
        config.setTestNlRoot(tempDir.resolve("src/test/nl").toFile());
        return config;
    }

    private ReinsConfig contextDisabledConfig(File contextFile) {
        ReinsConfig config = new ReinsConfig();
        config.setFailOnError(false);
        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        reasoning.setMaxTurns(1);
        config.setReasoning(reasoning);
        config.setContext(new ContextSettings()); 
        config.setProjectContextFile(contextFile);
        config.setMainNlRoot(tempDir.resolve("src/main/nl").toFile());
        config.setTestNlRoot(tempDir.resolve("src/test/nl").toFile());
        return config;
    }

    private CompilationService buildService(ReasoningService reasoningService,
                                           ProjectContextService contextService,
                                           Path source) throws Exception {
        MarkdownDependencyGraph graph = singleNodeGraph(source, "src/main/nl/domain.md");
        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver orderResolver = mock(ProcessingOrderResolver.class);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(orderResolver.resolve(graph)).thenReturn(List.of("src/main/nl/domain.md"));
        return new CompilationService(
                mock(InferenceService.class),
                mock(OutputWriter.class),
                mock(ResultPrinter.class),
                mockedTrackingStore(),
                new SourceFingerprintService(),
                new RecompilationDecider(),
                graphBuilder,
                orderResolver,
                reasoningService,
                contextService,
                null
        );
    }

    private CompilationService buildServiceWithTwoSources(ReasoningService reasoningService,
                                                         ProjectContextService contextService,
                                                         Path source1, Path source2) throws Exception {
        String rel1 = "src/main/nl/domain.md";
        String rel2 = "src/main/nl/service.md";
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(rel1, new MarkdownSourceNode(rel1, source1,
                Files.getLastModifiedTime(source1).toMillis(), List.of()));
        nodes.put(rel2, new MarkdownSourceNode(rel2, source2,
                Files.getLastModifiedTime(source2).toMillis(), List.of()));
        MarkdownDependencyGraph graph = new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(rel1, rel2));

        MarkdownDependencyGraphBuilder graphBuilder = mock(MarkdownDependencyGraphBuilder.class);
        ProcessingOrderResolver orderResolver = mock(ProcessingOrderResolver.class);
        when(graphBuilder.build(any(), any(), any())).thenReturn(graph);
        when(orderResolver.resolve(graph)).thenReturn(List.of(rel1, rel2));
        return new CompilationService(
                mock(InferenceService.class),
                mock(OutputWriter.class),
                mock(ResultPrinter.class),
                mockedTrackingStore(),
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

    private MarkdownDependencyGraph singleNodeGraph(Path source, String relativePath) throws Exception {
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(relativePath, new MarkdownSourceNode(
                relativePath, source, Files.getLastModifiedTime(source).toMillis(), List.of()));
        return new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(relativePath));
    }

    private CompilationTrackingStore mockedTrackingStore() throws Exception {
        return mock(CompilationTrackingStore.class);
    }
}
