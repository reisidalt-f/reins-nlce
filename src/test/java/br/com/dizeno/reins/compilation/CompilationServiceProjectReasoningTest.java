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
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import org.apache.maven.plugin.MojoExecutionException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompilationServiceProjectReasoningTest {

    @TempDir
    Path tempDir;

    
    
    

    

    private CompilationService buildService(ReasoningService reasoningService,
                                           ProjectContextService contextService,
                                           ProjectReasoningCycleService cycleService,
                                           Path source) throws Exception {
        MarkdownDependencyGraph graph = singleNodeGraph(source);
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
                cycleService
        );
    }

    private ReasoningResult finishSuccessResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setTerminalReasonMessage("done");
        result.setToolInfoPhrases(List.of());
        return result;
    }

    private ReinsConfig reasoningConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setFailOnError(false);
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        settings.setMaxTurns(1);
        config.setReasoning(settings);
        return config;
    }

    private Path createSource(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private MarkdownDependencyGraph singleNodeGraph(Path source) throws Exception {
        String relativePath = "src/main/nl/domain.md";
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        nodes.put(relativePath, new MarkdownSourceNode(
                relativePath,
                source,
                Files.getLastModifiedTime(source).toMillis(),
                List.of()
        ));
        return new MarkdownDependencyGraph(nodes, Map.of(), Map.of(), List.of(relativePath));
    }

    private CompilationTrackingStore mockedTrackingStore() throws Exception {
        return mock(CompilationTrackingStore.class);
    }
}
