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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunScriptOperationTest {

    @TempDir
    Path tempDir;

    @Test
    void runScriptReturnsStdoutStderrAndExitCodeOnSuccess() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("hello.bash"), "#!/usr/bin/env bash\necho hello\necho stderr-line 1>&2\nexit 0\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request("hello.bash"),
                resolver(scriptDir),
                null,
                scriptConfig("scripts")
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.isStarted());
        assertEquals(0, result.getExitCode());
        assertEquals("scripts/hello.bash", result.getResolvedPath());
        assertTrue(result.getStdout().contains("hello"));
        assertTrue(result.getStderr().contains("stderr-line"));
    }

    @Test
    void runScriptAcceptsRedundantScriptPrefixInPath() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("run-build.sh"), "#!/usr/bin/env bash\necho compiled\nexit 0\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request("script:run-build.sh"),
                resolver(scriptDir),
                null,
                scriptConfig("scripts")
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("script:run-build.sh", result.getQualifiedPath());
        assertEquals("scripts/run-build.sh", result.getResolvedPath());
        assertTrue(result.getStdout().contains("compiled"));
    }

    @Test
    void runScriptNonZeroExitReturnsErrorResult() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("fail.bash"), "#!/usr/bin/env bash\necho before-fail\necho fail-stderr 1>&2\nexit 7\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request("fail.bash"),
                resolver(scriptDir),
                null,
                scriptConfig("scripts")
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.isStarted());
        assertEquals(7, result.getExitCode());
        assertTrue(result.getStdout().contains("before-fail"));
        assertTrue(result.getStderr().contains("fail-stderr"));
    }

    @Test
    void runScriptRequestParsingAllowsScriptBasePathsForRuntimeValidation() {
        assertDoesNotThrow(() -> br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\nRUN_SCRIPT ../outside.bash\n--reins-boundary--\n"
        ));
    }

    @Test
    void runScriptRequestParsingAcceptsSeparateArgsList() {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest parsed = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\nRUN_SCRIPT run-build.sh path/to/file.ext --verbose\n--reins-boundary--\n"
        );

        assertEquals("run-build.sh", parsed.getScript());
        assertEquals(2, parsed.getArgs().size());
        assertEquals("path/to/file.ext", parsed.getArgs().get(0));
        assertEquals("--verbose", parsed.getArgs().get(1));
    }

    @Test
    void runScriptRequestParsingAcceptsTypedArgsAndNormalizesToString() {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest parsed = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\nRUN_SCRIPT run-build.sh true 3 hello\n--reins-boundary--\n"
        );

        assertEquals(3, parsed.getArgs().size());
        assertEquals("true", parsed.getArgs().get(0));
        assertEquals("3", parsed.getArgs().get(1));
        assertEquals("hello", parsed.getArgs().get(2));
    }

    @Test
    void runScriptRequestParsingSeparatesArgsFromIntentFlag() {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest parsed = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\nRUN_SCRIPT run-build.sh path/to/file.ext --intent build target component\n--reins-boundary--\n"
        );

        assertEquals("run-build.sh", parsed.getScript());
        assertEquals(1, parsed.getArgs().size());
        assertEquals("path/to/file.ext", parsed.getArgs().get(0));
        assertEquals("build target component", parsed.getIntent());
    }

    @Test
    void runScriptExecutesWithSeparateArgsParameter() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("echo-args.bash"), "#!/usr/bin/env bash\necho ARG1:$1\necho ARG2:$2\necho ARG3:$3\nexit 0\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = request("echo-args.bash");
        request.setArgs(java.util.List.of("com/dizeno/mdwriter/editor/layout/graphics/ImageGlyph.java", "true", "3"));

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request,
                resolver(scriptDir),
                null,
                scriptConfig("scripts")
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getStdout().contains("ARG1:com/dizeno/mdwriter/editor/layout/graphics/ImageGlyph.java"));
        assertTrue(result.getStdout().contains("ARG2:true"));
        assertTrue(result.getStdout().contains("ARG3:3"));
    }

    @Test
    void runScriptIgnoresLegacyBaseAndPathWhenScriptIsPresent() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("hello.bash"), "#!/usr/bin/env bash\necho hello\nexit 0\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = request("hello.bash");
        request.setBase("main");
        request.setPath("wrong/legacy.md");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request,
                resolver(scriptDir),
                null,
                scriptConfig("scripts")
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("script:hello.bash", result.getQualifiedPath());
    }

    @Test
    void runScriptUnavailableReturnsOperationResult() throws Exception {
        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        writeExecutableScript(scriptDir.resolve("hello.bash"), "#!/usr/bin/env bash\necho hello\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(
                request("hello.bash"),
                resolver(scriptDir),
                null,
                null
        );

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("not available"));
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

    private br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig scriptConfig(String scriptPath) {
        ReinsConfig config = new ReinsConfig();
        config.getReasoning().setScriptsPath(scriptPath);
        return br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig.fromSettings(config, tempDir);
    }

    private void writeExecutableScript(Path path, String content) throws Exception {
        Files.writeString(path, content);
        assertTrue(path.toFile().setExecutable(true));
    }
}
