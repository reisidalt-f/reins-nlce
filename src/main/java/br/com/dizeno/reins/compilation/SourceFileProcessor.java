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

import br.com.dizeno.reins.run.config.*;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * SourceFileProcessor is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing source file processor.
 */
public class SourceFileProcessor {

    private final CompilationService compilationService;

    /**
     * Constructs a new instance of {@link SourceFileProcessor}.
     *
     * @param compilationService the compilation service
     */
    public SourceFileProcessor(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    /**
     * Processes the target elements.
     *
     * @param sourceFile the source file to process
     * @param runProjectInference the run project inference
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param log the logger instance
     * @return the resulting result
     */
    public ProcessingResult process(File sourceFile,
                                    boolean runProjectInference,
                                    ReinsConfig config,
                                    Path projectRoot,
                                    Log log) throws Exception {
        CompilationSummary summary = compilationService.processFiles(
                List.of(sourceFile),
                runProjectInference,
                config,
                projectRoot,
                log);
        if (summary == null) {
            summary = new CompilationSummary();
        }
        boolean success = summary.getFailed() == 0;
        String state = success ? "committed" : "failed";
        boolean printProvider = config.getLogging() != null && config.getLogging().isLlmProvider();
        String providerPrefix = printProvider
                ? "provider=" + (config.getProvider() == null ? "gemini" : config.getProvider()) + ", "
                : "";
        String message = providerPrefix
                + "processed=" + summary.getProcessed()
                + ", compiled=" + summary.getCompiled()
                + ", skipped=" + summary.getSkipped()
                + ", failed=" + summary.getFailed();
        return new ProcessingResult(sourceFile.getPath(), List.of(), state, success, message);
    }
}
