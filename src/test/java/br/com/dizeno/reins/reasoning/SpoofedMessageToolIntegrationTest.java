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
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoofedMessageToolIntegrationTest {

    @TempDir
    Path tempDir;

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();
    private final ToolRequestParser requestParser = new ToolRequestParser();

    @Test
    void executesReadFileOperationFromSpoofedMessage() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path testRoot = tempDir.resolve("src/test/nl");
        Path targetRoot = tempDir.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot.resolve("main/java/demo"));
        Path target = targetRoot.resolve("main/java/demo/Example.java");
        Files.writeString(target, "class Example {}\n");

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainRoot.toAbsolutePath().normalize());
        mappings.setTestRoot(testRoot.toAbsolutePath().normalize());
        mappings.setTargetRoot(targetRoot.toAbsolutePath().normalize());

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolService = new br.com.dizeno.reins.reasoning.tooling.ToolingService();

        String raw = MessageBuilder.spoofedAssistant(
                "tool-request",
                "operation: read_file\nbase: target\npath: main/java/demo/Example.java");

        var message = parser.parseCanonicalMessage(raw);
        var request = requestParser.parse(message.getBody()).get(0);
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolService.execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getContent().contains("Example"));
    }
}
