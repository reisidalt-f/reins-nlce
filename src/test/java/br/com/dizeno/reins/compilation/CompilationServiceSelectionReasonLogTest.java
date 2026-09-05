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

 
class CompilationServiceSelectionReasonLogTest {

    @TempDir
    Path projectRoot;

    @Test
    void selectionReasonDisabled_noReasonLineEmitted() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig(false);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-no-reason.md")), config, projectRoot, log);

        assertFalse(log.hasInfoStartingWith("Selection reason: "),
                "No 'Selection reason:' line must appear when the flag is false");
    }

    @Test
    void selectionReasonEnabled_reasonLineEmittedAfterBanner() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig(true);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-with-reason.md")), config, projectRoot, log);

        assertTrue(log.hasInfoStartingWith("Selection reason: "),
                "'Selection reason: <REASON>' must appear after the ** banner when the flag is true");
    }

    @Test
    void selectionReasonEnabled_noPreFilterRecord_emitsSelectionReasonLine() throws Exception {
        CompilationService service = createService();
        ReinsConfig config = baseConfig(true);
        RecordingLog log = new RecordingLog();

        service.processFiles(List.of(createSourceFile("feature-unknown-reason.md")), config, projectRoot, log);

        assertTrue(log.hasInfoStartingWith("Selection reason: NO_PRIOR_RECORD"),
                "A source with no prior record must emit a 'Selection reason: NO_PRIOR_RECORD' line when the flag is true");
    }

    

    private CompilationService createService() throws Exception {
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

    private ReasoningResult successResult() {
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

    private ReinsConfig baseConfig(boolean selectionReason) {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", projectRoot.resolve("src/main/nl").toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        config.setIncludePattern("**/*.md");
        config.setFailOnError(false);

        LoggingSettings loggingSettings = new LoggingSettings();
        loggingSettings.setSelectionReason(selectionReason);
        config.setLogging(loggingSettings);

        return config;
    }
}
