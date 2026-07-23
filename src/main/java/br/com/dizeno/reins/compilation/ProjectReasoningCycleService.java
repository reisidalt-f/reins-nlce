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
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * ProjectReasoningCycleService is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface ProjectReasoningCycleService {

     
    /**
     * Runs the execution cycle.
     *
     * @param compilationBackgroundPayload the compilation background payload
     * @param config the Reins configuration settings
     */
    void run(CompilationBackgroundPayload compilationBackgroundPayload,
             ReinsConfig config) throws MojoExecutionException;
}
