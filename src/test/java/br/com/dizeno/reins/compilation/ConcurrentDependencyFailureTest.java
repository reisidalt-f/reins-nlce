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

public class ConcurrentDependencyFailureTest {

    @TempDir
    Path tempDir;

    @Test
    public void testConcurrentDependencyFailureSkipsDependentFile() throws Exception {
        Path mainSourceDir = tempDir.resolve("src/main/nl");
        Path targetDir = tempDir.resolve("src/main/java");
        Files.createDirectories(mainSourceDir);
        Files.createDirectories(targetDir);

        File fileB = mainSourceDir.resolve("FileB.md").toFile();
        File fileA = mainSourceDir.resolve("FileA.md").toFile(); // FileA references FileB

        Files.writeString(fileB.toPath(), "# Component B\nThis is B.");
        Files.writeString(fileA.toPath(), "# Component A\nSee [FileB](FileB.md) for details.");

        ReasoningService reasoningService = mock(ReasoningService.class);
        // Fail FileB compilation
        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenAnswer(invocation -> {
                    ReasoningRequest req = invocation.getArgument(0);
                    if (req.getSourcePath() != null && req.getSourcePath().contains("FileB.md")) {
                        throw new RuntimeException("Simulated failure compiling FileB");
                    }
                    ReasoningResult ok = new ReasoningResult();
                    ok.setFinalIntent("finish_success");
                    return ok;
                });

        ReinsConfig configConcurrent = new ReinsConfig();
        configConcurrent.setProvider("stub");
        configConcurrent.setFailOnError(false);
        configConcurrent.setTarget(new TargetSettings());
        configConcurrent.setSourceBase("main", mainSourceDir.toFile());
        configConcurrent.getTarget().setTargetBase("main", "src/main/java");
        configConcurrent.getBuild().setCompilationThreads(2);

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

        CompilationSummary summary = serviceConcurrent.processFiles(
                List.of(fileB, fileA),
                configConcurrent,
                tempDir,
                new SystemStreamLog()
        );

        assertNotNull(summary);
        // Both FileB (which failed directly) and FileA (which was skipped due to FileB failing) increment failed count
        assertTrue(summary.getFailed() >= 2, "Both dependency and dependent file should be recorded as failed");
    }
}
