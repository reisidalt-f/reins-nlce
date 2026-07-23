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

import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.LoggingSettings;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

class SourceFileProcessorTest {

    private CompilationService compilationService;
    private SourceFileProcessor processor;
    private Log log;

    @BeforeEach
    void setUp() {
        compilationService = Mockito.mock(CompilationService.class);
        processor = new SourceFileProcessor(compilationService);
        log = Mockito.mock(Log.class);
    }

    @Test
    void messageContainsProviderWhenLlmProviderLoggingIsEnabled() throws Exception {
        CompilationSummary summary = new CompilationSummary();
        summary.incrementProcessed();

        when(compilationService.processFiles(any(), anyBoolean(), any(), any(), any()))
                .thenReturn(summary);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        LoggingSettings logging = new LoggingSettings();
        logging.setLlmProvider(true);
        config.setLogging(logging);

        ProcessingResult result = processor.process(
                new File("src/main/nl/demo.md"),
                false,
                config,
                Path.of("."),
                log
        );

        assertTrue(result.success());
        assertTrue(result.message().contains("provider=gemini"));
        assertTrue(result.message().contains("processed=1"));
    }

    @Test
    void messageOmitProviderWhenLlmProviderLoggingIsDisabled() throws Exception {
        CompilationSummary summary = new CompilationSummary();
        summary.incrementProcessed();

        when(compilationService.processFiles(any(), anyBoolean(), any(), any(), any()))
                .thenReturn(summary);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        LoggingSettings logging = new LoggingSettings();
        logging.setLlmProvider(false);
        config.setLogging(logging);

        ProcessingResult result = processor.process(
                new File("src/main/nl/demo.md"),
                false,
                config,
                Path.of("."),
                log
        );

        assertTrue(result.success());
        assertFalse(result.message().contains("provider="));
        assertTrue(result.message().contains("processed=1"));
    }
}
