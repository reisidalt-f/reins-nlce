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
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.TargetSettings;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConcurrentCompilationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    public void testConcurrentCompilationExecution() throws Exception {
        Path mainSourceDir = tempDir.resolve("src/main/nl");
        Path targetDir = tempDir.resolve("src/main/java");
        Files.createDirectories(mainSourceDir);
        Files.createDirectories(targetDir);

        File fileA = mainSourceDir.resolve("FileA.md").toFile();
        File fileB = mainSourceDir.resolve("FileB.md").toFile();
        File fileC = mainSourceDir.resolve("FileC.md").toFile();

        Files.writeString(fileA.toPath(), "# Component A\nThis is component A.");
        Files.writeString(fileB.toPath(), "# Component B\nThis is component B.");
        Files.writeString(fileC.toPath(), "# Component C\nThis is component C.");

        ReasoningService reasoningService = mock(ReasoningService.class);
        ReasoningResult stubResult = new ReasoningResult();
        stubResult.setFinalIntent("finish_success");
        stubResult.setWrittenPaths(List.of("src/main/java/Output.java"));
        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenReturn(stubResult);

        ReinsConfig configSequential = new ReinsConfig();
        configSequential.setProvider("stub");
        configSequential.setTarget(new TargetSettings());
        configSequential.setSourceBase("main", mainSourceDir.toFile());
        configSequential.getTarget().setTargetBase("main", "src/main/java");
        configSequential.getBuild().setCompilationThreads(1);

        CompilationService serviceSequential = new CompilationService(
                new InferenceService(),
                new OutputWriter(),
                new ResultPrinter(),
                new CompilationTrackingStore(),
                new SourceFingerprintService(),
                new RecompilationDecider(),
                new MarkdownDependencyGraphBuilder(),
                new ProcessingOrderResolver(),
                reasoningService
        );

        CompilationSummary summarySequential = serviceSequential.processFiles(
                List.of(fileA, fileB, fileC),
                configSequential,
                tempDir,
                new SystemStreamLog()
        );

        assertNotNull(summarySequential);
        assertEquals(3, summarySequential.getDiscovered());
        assertEquals(3, summarySequential.getProcessed());
        assertEquals(0, summarySequential.getFailed());

        ReinsConfig configConcurrent = new ReinsConfig();
        configConcurrent.setProvider("stub");
        configConcurrent.setTarget(new TargetSettings());
        configConcurrent.setSourceBase("main", mainSourceDir.toFile());
        configConcurrent.getTarget().setTargetBase("main", "src/main/java");
        configConcurrent.getBuild().setCompilationThreads(4);

        CompilationService serviceConcurrent = new CompilationService(
                new InferenceService(),
                new OutputWriter(),
                new ResultPrinter(),
                new CompilationTrackingStore(),
                new SourceFingerprintService(),
                new RecompilationDecider(),
                new MarkdownDependencyGraphBuilder(),
                new ProcessingOrderResolver(),
                reasoningService
        );

        CompilationSummary summaryConcurrent = serviceConcurrent.processFiles(
                List.of(fileA, fileB, fileC),
                configConcurrent,
                tempDir,
                new SystemStreamLog()
        );

        assertNotNull(summaryConcurrent);
        assertEquals(summarySequential.getDiscovered(), summaryConcurrent.getDiscovered());
        assertEquals(summarySequential.getProcessed(), summaryConcurrent.getProcessed());
        assertEquals(summarySequential.getSkipped(), summaryConcurrent.getSkipped());
        assertEquals(summarySequential.getFailed(), summaryConcurrent.getFailed());
    }
}
