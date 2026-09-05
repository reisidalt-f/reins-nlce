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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunScriptReasoningFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void runCycleContinuesAfterRunScriptErrorResult() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);

        when(inferenceService.infer(any(), any())).thenReturn(
            response("INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\noperation: run_script\nscript: fail.bash\n"),
                response("INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nCompleted after analyzing script failure")
        );

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult errorResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT, "script:fail.bash", "Script exited with code 7.");
        errorResult.setStarted(true);
        errorResult.setExitCode(7);
        errorResult.setStdout("before-fail\n");
        errorResult.setStderr("failure details\n");
        when(toolService.execute(any(), any(), any(), any())).thenReturn(errorResult);

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                toolService,
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getGemini().setApiKey("test-key");
        Files.createDirectories(tempDir.resolve("scripts"));
        config.getReasoning().setScriptsPath("scripts");
        config.getReasoning().setEnabled(true);
        config.getReasoning().setMaxTurns(3);

        ReasoningResult result = service.runCycle(request(), config);

        assertEquals("finish_success", result.getFinalIntent());
        verify(inferenceService, times(2)).infer(any(), any());
    }

    @Test
    void malformedCustomToolResultFallsBackWhenFailOnErrorIsFalse() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);

        when(inferenceService.infer(any(), any())).thenReturn(
            response("INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\noperation: read_file\nbase: main\npath: src/main/nl/source.md\n")
        );
        when(toolService.execute(any(), any(), any(), any())).thenReturn(
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE, "main:src/main/nl/source.md", "ok")
        );

        Path customScripts = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(customScripts.resolve("tool-result.ftl"), "${1?api");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(customScripts.toFile(), null), customScripts.toFile());
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

        DefaultReasoningService service = new DefaultReasoningService(
            inferenceService,
            new ResponseDirectiveParser(),
            new ReasoningPromptBuilder(),
            toolService,
            new ToolResultFormatter(),
            new FileReasoningLogService(),
            new CompilationTrackingStore()
        ).withScriptEvaluator(evaluator);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getGemini().setApiKey("test-key");
        config.setFailOnError(false);
        config.getReasoning().setScriptsPath("scripts");
        config.getReasoning().setEnabled(true);
        config.getReasoning().setMaxTurns(2);

        assertDoesNotThrow(() -> service.runCycle(request(), config));
    }

        @Test
        void addInferenceNoteRemainsOptionalWhenMutationRequestsProceed() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);

        when(inferenceService.infer(any(), any())).thenReturn(
            response("INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                + "operation: run_script\nscript: run-build.sh\nargs: [\"com/example/Broken.java\"]\n"),
            response("INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                + "- operation: add_reasoning_note\n"
                + "  compiled: target:main/java/com/example/OtherFile.java\n"
                + "  note: \"Fix missing import for compilation\"\n"
                + "- operation: patch_file\n"
                + "  base: target\n"
                + "  path: main/java/com/example/Broken.java\n"
                + "  content: \"@@ -1 +1 @@\\n-old\\n+package com.example;\\n\"\n"),
            response("INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone"));

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult compileError = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT,
            "script:run-build.sh",
            "compile failed");
        compileError.setStdout("target/main/java/com/example/Broken.java:[12,8] cannot find symbol\n");

        when(toolService.execute(any(), any(), any(), any())).thenReturn(
            compileError,
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.ADD_REASONING_NOTE,
                "target:main/java/com/example/OtherFile.java", "note-added"),
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE,
                "target:main/java/com/example/Broken.java", "patched"));

        DefaultReasoningService service = new DefaultReasoningService(
            inferenceService,
            new ResponseDirectiveParser(),
            new ReasoningPromptBuilder(),
            toolService,
            new ToolResultFormatter(),
            new FileReasoningLogService(),
            new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getGemini().setApiKey("test-key");
        Files.createDirectories(tempDir.resolve("scripts"));
        config.getReasoning().setScriptsPath("scripts");
        config.getReasoning().setEnabled(true);
        config.getReasoning().setMaxTurns(4);

        ReasoningResult result = service.runCycle(request(), config);

        assertEquals("finish_success", result.getFinalIntent());
        assertTrue(result.getWrittenPaths().stream().anyMatch(p -> p.endsWith("Broken.java")));
        verify(inferenceService, times(3)).infer(any(), any());
        verify(toolService, times(3)).execute(any(), any(), any(), any());
        }

    private ReasoningRequest request() throws Exception {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(baseMappings());
        request.setSourceScope("main");
        request.setSourcePath("");
        return request;
    }

    private BasePathMappingSet baseMappings() throws Exception {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(Files.createDirectories(tempDir.resolve("src/main/nl")));
        mappings.setTestRoot(Files.createDirectories(tempDir.resolve("src/test/nl")));
        mappings.setTargetRoot(Files.createDirectories(tempDir.resolve("src")));
        mappings.setScriptRoot(Files.createDirectories(tempDir.resolve("scripts")));
        return mappings;
    }

    private MarkdownInferenceResponse response(String text) {
        MarkdownInferenceResponse response = new MarkdownInferenceResponse();
        response.setRawResponseText(text);
        return response;
    }
}
