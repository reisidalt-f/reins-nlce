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

import br.com.dizeno.reins.security.PathValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * OutputWriter is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing output writer.
 */
public class OutputWriter {
    /**
     * Write.
     *
     * @param outputFile the output file
     * @param content the content
     * @param validator the path validator for security boundary checks
     * @param dryRun the dry run
     */
    public void write(Path outputFile, String content, PathValidator validator, boolean dryRun) throws IOException {
        validator.validateInProject(outputFile);
        if (dryRun) {
            return;
        }
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        Files.writeString(outputFile, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}
