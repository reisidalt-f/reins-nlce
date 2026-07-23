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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompilationServiceLogToggleTest {

    @TempDir
    Path projectRoot;

    @Test
    void processingOrderDisabledSuppressesProcessingOrderMessage() throws Exception {
        CompilationService service = createServiceWithSkipResult();
        ReinsConfig config = baseConfig(false, false, false);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-a.md")), false, config, projectRoot, log);

        assertFalse(log.hasInfoStartingWith("Processing order: "));
    }

    @Test
    void processingOrderEnabledEmitsProcessingOrderMessage() throws Exception {
        CompilationService service = createServiceWithSkipResult();
        ReinsConfig config = baseConfig(false, true, false);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-b.md")), false, config, projectRoot, log);

        assertTrue(log.hasInfoStartingWith("Processing order: "));
    }

    @Test
    void trackingFileLoggingDisabledSuppressesSkippedTrackingMessage() throws Exception {
        CompilationService service = createServiceWithSkipResult();
        ReinsConfig config = baseConfig(false, true, false, false);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-c.md")), false, config, projectRoot, log);

        assertFalse(log.hasInfoContaining("Tracking file written (skipped):"));
    }

    @Test
    void trackingFileLoggingEnabledEmitsSkippedTrackingMessage() throws Exception {
        CompilationService service = createServiceWithSkipResult();
        ReinsConfig config = baseConfig(true, false, false, true);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-d.md")), false, config, projectRoot, log);

        assertTrue(log.hasInfoContaining("Tracking file written (skipped):"));
    }

    @Test
    void noEmptyRetryFlowDoesNotEmitEmptyResponseRetryMessages() throws Exception {
        CompilationService service = createServiceWithSkipResult();
        ReinsConfig config = baseConfig(false, false, false);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-e.md")), false, config, projectRoot, log);

        assertFalse(log.hasInfoContaining("[empty-response-retry]"));
    }

    private CompilationService createServiceWithSkipResult() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(skipResult());

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

    private ReasoningResult skipResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        result.setInspectedPaths(List.of());
        result.setReadMarkdownPaths(List.of());
        result.setToolInfoPhrases(List.of());
        return result;
    }

    private File createSourceFile(String name) throws Exception {
        Path source = projectRoot.resolve("src/main/nl").resolve(name);
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# Test source\n\nSimple markdown content.");
        return source.toFile();
    }

    private ReinsConfig baseConfig(boolean skipped, boolean processingOrder, boolean eagerlyProvided) {
        return baseConfig(skipped, processingOrder, eagerlyProvided, false);
    }

    private ReinsConfig baseConfig(boolean skipped, boolean processingOrder, boolean eagerlyProvided, boolean trackingFile) {
        ReinsConfig config = new ReinsConfig();
        config.setScanRoots(List.of(projectRoot.resolve("src/main/nl").toFile()));
        config.setIncludePattern("**/*.md");
        config.setFailOnError(false);

        LogSettings logSettings = new LogSettings();
        logSettings.setSkipped(skipped);
        logSettings.setProcessingOrder(processingOrder);
        logSettings.setEagerlyProvided(eagerlyProvided);
        config.setLog(logSettings);

        config.getLogging().setTrackingFile(trackingFile);

        return config;
    }
}
