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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolInfoPhraseFormatterTest {

    @TempDir
    Path tempDir;

    @Test
    void formatsReadFilePhraseWithQualifiedPathAndIntention() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
        request.setBase("main");
        request.setPath("domain/entities.md");
        request.setIntent("understand entity model");

        ToolInfoPhraseFormatter formatter = new ToolInfoPhraseFormatter();
        String phrase = formatter.format(request, resolver);

        assertEquals("Read of file main:domain/entities.md to understand entity model", phrase);
    }

    @Test
    void appliesFallbackTokensForMissingPathAndIntention() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        request.setBase("");
        request.setPath(" ");
        request.setIntent(null);

        ToolInfoPhraseFormatter formatter = new ToolInfoPhraseFormatter();
        String phrase = formatter.format(request, resolver);

        assertEquals("Write of file <unspecified-path> to unspecified intention", phrase);
    }

    @Test
    void sanitizesAndRedactsIntentionValues() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
        request.setBase("main");
        request.setPath("domain");
        request.setIntent("inspect\napiKey=abc123");

        ToolInfoPhraseFormatter formatter = new ToolInfoPhraseFormatter();
        String phrase = formatter.format(request, resolver);

        assertEquals("List of files in main:domain to inspect apiKey=<redacted>", phrase);
    }

    @Test
    void formatsRunScriptPhraseWithScriptPathAndIntention() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT);
        request.setScript("compile.sh");
        request.setIntent("Compile FilterCriteria.java to check for errors");

        ToolInfoPhraseFormatter formatter = new ToolInfoPhraseFormatter();
        String phrase = formatter.format(request, resolver);

        assertEquals("Run of script compile.sh to Compile FilterCriteria.java to check for errors", phrase);
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver createResolver() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path testRoot = tempDir.resolve("src/test/nl");
        Path targetRoot = tempDir.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot);

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainRoot);
        mappings.setTestRoot(testRoot);
        mappings.setTargetRoot(targetRoot);

        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
    }
}
