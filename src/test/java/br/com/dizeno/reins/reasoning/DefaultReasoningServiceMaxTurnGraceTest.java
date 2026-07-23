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
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultReasoningServiceMaxTurnGraceTest {

    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService;
    private ToolResultFormatter mcpResultFormatter;
    private DefaultReasoningService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        mcpService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        mcpResultFormatter = mock(ToolResultFormatter.class);

        service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                mcpService,
                mcpResultFormatter,
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );
    }

    @Test
    void returnsMaxTurnExhaustedWhenGraceTurnHasNoFinishIntent() throws Exception {
        MarkdownInferenceResponse first = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse second = mock(MarkdownInferenceResponse.class);
        when(first.getRawResponseText()).thenReturn("first");
        when(second.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(first, second);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
        when(mcpResultFormatter.format(any())).thenReturn("mcp-result");

        ResponseDirective wait = new ResponseDirective();
        wait.setValid(true);
        wait.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        wait.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective stillWaiting = new ResponseDirective();
        stillWaiting.setValid(true);
        stillWaiting.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        stillWaiting.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("first")).thenReturn(
                new ResponseDirectiveParser.ParseResult(wait, "operation: list_files\nbase: main\npath: \"\"\nrecursive: true")
        );
        when(directiveParser.parse("second")).thenReturn(
                new ResponseDirectiveParser.ParseResult(stillWaiting, "Still investigating")
        );

        when(mcpService.execute(any(), any(), any(), any())).thenReturn(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES, "main:/", "ok")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(1));

        assertEquals("finish_error", result.getFinalIntent());
        assertEquals("max_turn_exhausted", result.getTerminalReasonCode());
        assertTrue(result.isGraceTurnUsed());
                assertEquals(1, result.getTurnCount(), "Only fulfilled turns should count toward turn budget");
                assertEquals(1, result.getClosedTurnCount(), "Closed-turn count should match the fulfilled budget");
    }

    @Test
    void acceptsFinishErrorFromGraceTurnAndCapturesUserMessage() throws Exception {
        MarkdownInferenceResponse first = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse second = mock(MarkdownInferenceResponse.class);
        when(first.getRawResponseText()).thenReturn("first");
        when(second.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(first, second);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
        when(mcpResultFormatter.format(any())).thenReturn("mcp-result");

        ResponseDirective wait = new ResponseDirective();
        wait.setValid(true);
        wait.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        wait.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective finishError = new ResponseDirective();
        finishError.setValid(true);
        finishError.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishError.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("first")).thenReturn(
                new ResponseDirectiveParser.ParseResult(wait, "operation: list_files\nbase: main\npath: \"\"\nrecursive: true")
        );
        when(directiveParser.parse("second")).thenReturn(
                new ResponseDirectiveParser.ParseResult(finishError, "Could not conclude after max turns")
        );

        when(mcpService.execute(any(), any(), any(), any())).thenReturn(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES, "main:/", "ok")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(1));

        assertEquals("finish_error", result.getFinalIntent());
        assertEquals("finish_error", result.getTerminalReasonCode());
        assertTrue(result.isGraceTurnUsed());
                assertEquals(1, result.getClosedTurnCount(), "Grace turn should not increment closed-turn count");
        assertTrue(result.getUserFacingMessages().stream().anyMatch(m -> m.contains("max turns")));

        ArgumentCaptor<br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest> inferCaptor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest.class);
        verify(inferenceService, times(2)).infer(inferCaptor.capture(), any());

        br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest secondInferRequest = inferCaptor.getAllValues().get(1);
        ConversationMessage graceTurnUserMessage = secondInferRequest.getConversationHistory().stream()
                .filter(message -> message.getRole() == ConversationMessage.Role.USER)
                .reduce((firstMessage, nextMessage) -> nextMessage)
                .orElseThrow();

        assertTrue(graceTurnUserMessage.getText().contains("Turn 2/1"));
        assertTrue(graceTurnUserMessage.getText().contains("Respond with a finish-success message requesting the MCP operations needed to complete the task or a finish-error with a message to the user indicating the failure. You should not wait for the results of the MCP operations, the task will be considered complete if all MCP operations succeed."));
    }

    @Test
    void compileLikeScriptFailure_doesNotBlockNextMutationRequest() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);

        MarkdownInferenceResponse turn1 = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse turn2 = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse turn3 = mock(MarkdownInferenceResponse.class);
        when(turn1.getRawResponseText()).thenReturn(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                        + "operation: run_script\n"
                        + "script: compile-one-java.sh\n"
                        + "args: [\"com/example/Broken.java\"]\n");
        when(turn2.getRawResponseText()).thenReturn(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                        + "operation: patch_file\n"
                        + "base: target\n"
                        + "path: main/java/com/example/Broken.java\n"
                        + "atLine: 1\n"
                        + "replacing: 0\n"
                        + "content: \"package com.example;\\n\"\n");
        when(turn3.getRawResponseText()).thenReturn(
                "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone\n");
        when(inferenceService.infer(any(), any())).thenReturn(turn1, turn2, turn3);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult compileError = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT,
                "script:compile-one-java.sh",
                "compile failed");
        compileError.setStdout("target/main/java/com/example/Broken.java:[12,8] cannot find symbol\n");
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult patchSuccess = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE,
                "target:main/java/com/example/Broken.java",
                "patched");
        when(mcpService.execute(any(), any(), any(), any())).thenReturn(compileError, patchSuccess);

        DefaultReasoningService localService = new DefaultReasoningService(
                inferenceService,
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                mcpService,
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.getGemini().setMaximumTurns(2);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(Files.createDirectories(tempDir.resolve("src/main/nl")));
        mappings.setTestRoot(Files.createDirectories(tempDir.resolve("src/test/nl")));
        mappings.setTargetRoot(Files.createDirectories(tempDir.resolve("src")));
        mappings.setScriptRoot(Files.createDirectories(tempDir.resolve("scripts")));
        request.setBaseMappings(mappings);

        ReasoningResult result = localService.runCycle(request, config);

        assertEquals("finish_success", result.getFinalIntent());
        verify(inferenceService, times(3)).infer(any(), any());
        verify(mcpService, times(2)).execute(any(), any(), any(), any());
    }

    private ReasoningRequest buildRequest() {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        return request;
    }

    private ReinsConfig configWithMaxTurns(int maxTurns) {
        ReinsConfig config = new ReinsConfig();
                config.getGemini().setMaximumTurns(maxTurns);
        return config;
    }
}
