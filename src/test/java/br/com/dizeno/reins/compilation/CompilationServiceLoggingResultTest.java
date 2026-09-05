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
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.*;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompilationServiceLoggingResultTest {

    @TempDir
    Path projectRoot;

    @Test
    void defaultResultLoggingIsFalseAndSuppressesSuccessLogs() throws Exception {
        CompilationService service = createServiceWithSuccessResult();
        ReinsConfig config = baseConfig(); // default logging.result is false
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("test1.md")), config, projectRoot, log);

        // Verify no compiled log messages are present
        assertFalse(log.hasInfoContaining("[compiled]"));
    }

    @Test
    void resultLoggingEnabledEmitsSuccessLogs() throws Exception {
        CompilationService service = createServiceWithSuccessResult();
        ReinsConfig config = baseConfig();
        config.getLogging().setResult(true);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("test2.md")), config, projectRoot, log);

        // Verify compiled log message is present
        assertTrue(log.hasInfoContaining("[compiled]"));
    }

    @Test
    void resultLoggingDisabledStillEmitsFailureLogs() throws Exception {
        CompilationService service = createServiceWithFailureResult();
        ReinsConfig config = baseConfig(); // default logging.result is false
        RecordingLog log = new RecordingLog();

        try {
            service.processFiles(List.of(createSourceFile("test3.md")), config, projectRoot, log);
        } catch (Exception ignored) {
        }

        // Verify failure log message is printed as error log
        assertTrue(log.getErrorMessages().stream().anyMatch(msg -> msg != null && msg.contains("[failed]")));
    }

    @Test
    void resultLoggingDisabledSuppressesSkippedLogs() throws Exception {
        CompilationService service = createServiceWithSkippedResult();
        ReinsConfig config = baseConfig(); // default logging.result is false
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("testSkipped1.md")), config, projectRoot, log);

        // Verify no skipped log messages are present
        assertFalse(log.hasInfoContaining("[skipped]"));
    }

    @Test
    void resultLoggingEnabledEmitsSkippedLogs() throws Exception {
        CompilationService service = createServiceWithSkippedResult();
        ReinsConfig config = baseConfig();
        config.getLogging().setResult(true);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("testSkipped2.md")), config, projectRoot, log);

        // Verify skipped / no-change log message is present
        assertTrue(log.hasInfoContaining("[skipped]") || log.hasInfoContaining("[no-change]"));
    }

    private CompilationService createServiceWithSuccessResult() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(successResult());

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
                new ProjectContextService());
    }

    private CompilationService createServiceWithFailureResult() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenThrow(new RuntimeException("Inference failure"));

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
                new ProjectContextService());
    }

    private ReasoningResult successResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of("src/main/java/br/com/dizeno/reins/run/App.java"));
        result.setWrittenMtimes(Map.of("src/main/java/br/com/dizeno/reins/run/App.java", 123456L));
        result.setInspectedPaths(List.of());
        result.setReadMarkdownPaths(List.of());
        result.setToolInfoPhrases(List.of());
        return result;
    }

    private CompilationService createServiceWithSkippedResult() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(), any())).thenReturn(skippedResult());

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
                new ProjectContextService());
    }

    private ReasoningResult skippedResult() {
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

    private ReinsConfig baseConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", projectRoot.resolve("src/main/nl").toFile());
        config.setIncludePattern("**/*.md");
        config.setFailOnError(false);
        config.setLogging(new LoggingSettings()); // defaults to result = false
        
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        target.setTargetBase("test", "src/test/java");
        config.setTarget(target);
        
        return config;
    }
}
