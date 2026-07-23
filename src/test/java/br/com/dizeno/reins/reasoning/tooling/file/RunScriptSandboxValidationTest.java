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

package br.com.dizeno.reins.reasoning.tooling.file;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunScriptSandboxValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsTraversalPathBeforeProcessStart() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request("../outside.bash"), resolver(scriptDir), null, scriptConfig());

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertFalse(result.isStarted());
        assertTrue(result.getFailureReason().contains("escapes configured script root"));
    }

    @Test
    void rejectsAbsolutePathBeforeProcessStart() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request("/etc/passwd"), resolver(scriptDir), null, scriptConfig());

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertFalse(result.isStarted());
        assertTrue(result.getFailureReason().contains("escapes configured script root"));
    }

    @Test
    void rejectsNonExecutableFile() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scriptDir.resolve("plain.bash"), "#!/usr/bin/env bash\necho hi\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request("plain.bash"), resolver(scriptDir), null, scriptConfig());

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertFalse(result.isStarted());
        assertTrue(result.getFailureReason().contains("not executable"));
        assertEquals("scripts/plain.bash", result.getResolvedPath());
    }

    private br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request(String path) {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT);
        request.setScript(path);
        return request;
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver(Path scriptDir) throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet mappings = new br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet();
        mappings.setMainRoot(Files.createDirectories(tempDir.resolve("src/main/nl")).toAbsolutePath().normalize());
        mappings.setTestRoot(Files.createDirectories(tempDir.resolve("src/test/nl")).toAbsolutePath().normalize());
        mappings.setTargetRoot(Files.createDirectories(tempDir.resolve("src")).toAbsolutePath().normalize());
        mappings.setScriptRoot(scriptDir.toAbsolutePath().normalize());
        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
    }

    private br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig scriptConfig() {
        ReinsConfig config = new ReinsConfig();
        config.getReasoning().setScriptsPath("scripts");
        return br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig.fromSettings(config, tempDir);
    }
}
