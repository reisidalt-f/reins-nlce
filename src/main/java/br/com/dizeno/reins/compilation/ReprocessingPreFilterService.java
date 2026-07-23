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
 * ReprocessingPreFilterService is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface ReprocessingPreFilterService {

     
    /**
     * Filter.
     *
     * @param sourceFiles the list of source files to process
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param log the logger instance
     * @return the resulting result
     */
    PreFilterResult filter(List<File> sourceFiles, ReinsConfig config, Path projectRoot, Log log)
            throws Exception;
}
