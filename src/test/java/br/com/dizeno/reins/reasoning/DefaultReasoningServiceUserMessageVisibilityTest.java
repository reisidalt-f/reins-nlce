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
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultReasoningServiceUserMessageVisibilityTest {

    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService toolService;
    private ToolResultFormatter toolResultFormatter;
    private DefaultReasoningService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        toolService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        toolResultFormatter = mock(ToolResultFormatter.class);

        service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                toolService,
                toolResultFormatter,
                new FileReasoningLogService(),
            new CompilationTrackingStore()
        );
    }

    @Test
    void capturesOnlyUserFacingMessagesInOrder() throws Exception {
        MarkdownInferenceResponse first = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse second = mock(MarkdownInferenceResponse.class);
        when(first.getRawResponseText()).thenReturn("first");
        when(second.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(first, second);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt-1", "prompt-2");

        ResponseDirective waitingUserMessage = new ResponseDirective();
        waitingUserMessage.setValid(true);
        waitingUserMessage.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waitingUserMessage.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirective finishError = new ResponseDirective();
        finishError.setValid(true);
        finishError.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishError.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("first")).thenReturn(
                new ResponseDirectiveParser.ParseResult(waitingUserMessage, "First user-facing update")
        );
        when(directiveParser.parse("second")).thenReturn(
                new ResponseDirectiveParser.ParseResult(finishError, "Final user-facing explanation")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        assertEquals(2, result.getUserFacingMessages().size());
        assertEquals("First user-facing update", result.getUserFacingMessages().get(0));
        assertEquals("Final user-facing explanation", result.getUserFacingMessages().get(1));
    }

    @Test
    void doesNotTreatToolPayloadAsUserMessage() throws Exception {
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("raw");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finishError = new ResponseDirective();
        finishError.setValid(true);
        finishError.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishError.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        when(directiveParser.parse("raw")).thenReturn(
                new ResponseDirectiveParser.ParseResult(finishError, "operation: list_files\nbase: main\npath: \"\"\nrecursive: true")
        );
        when(toolService.execute(any(), any(), any(), any())).thenReturn(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES, "main:/", "ok")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(1));

        assertTrue(result.getUserFacingMessages().isEmpty());
        assertFalse(result.isGraceTurnUsed());
    }

    @Test
    void failedReadFileToolPayloadDoesNotBecomeUserFacingMessage() throws Exception {
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("raw");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
        when(toolResultFormatter.format(any())).thenReturn("tool-summary");

        ResponseDirective finishError = new ResponseDirective();
        finishError.setValid(true);
        finishError.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishError.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        when(directiveParser.parse("raw")).thenReturn(
                new ResponseDirectiveParser.ParseResult(finishError, "operation: read_file\nbase: main\npath: missing.md")
        );
        when(toolService.execute(any(), any(), any(), any())).thenReturn(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE, "main:/missing.md", "File does not exist.")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(1));

        assertTrue(result.getUserFacingMessages().isEmpty());
        assertFalse(result.isGraceTurnUsed());
    }

        
        @Test
        void nonFinishUserMessagePlanningContentCapturedInUserFacingMessages() throws Exception {
        MarkdownInferenceResponse planningResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse finishResponse = mock(MarkdownInferenceResponse.class);
        when(planningResponse.getRawResponseText()).thenReturn("planning-raw");
        when(finishResponse.getRawResponseText()).thenReturn("finish-raw");
        when(inferenceService.infer(any(), any())).thenReturn(planningResponse, finishResponse);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective planningDirective = new ResponseDirective();
        planningDirective.setValid(true);
        planningDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        planningDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        ResponseDirective.PlanningContent pc = new ResponseDirective.PlanningContent();
        pc.setGoalSummary("Implement service layer");
        pc.setStrategySummary("Inspect entities first");
        pc.setProgressSummary("Entities reviewed");
        planningDirective.setPlanningContent(pc);

        ResponseDirective finishDirective = new ResponseDirective();
        finishDirective.setValid(true);
        finishDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finishDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        String planningBody = "GOAL: Implement service layer\nSTRATEGY: Inspect entities first\nPROGRESS: Entities reviewed\nNo new decisions this turn.";
        when(directiveParser.parse("planning-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(planningDirective, planningBody)
        );
        when(directiveParser.parse("finish-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(finishDirective, "All done.")
        );
        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(5));

        assertEquals(2, result.getUserFacingMessages().size());
        assertEquals(planningBody, result.getUserFacingMessages().get(0));
        assertEquals("All done.", result.getUserFacingMessages().get(1));
        }

        
        @Test
        void nonFinishUserMessageProducesUnderstoodReply() throws Exception {
        
        
        MarkdownInferenceResponse planningResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse finishResponse = mock(MarkdownInferenceResponse.class);
        when(planningResponse.getRawResponseText()).thenReturn("planning-raw");
        when(finishResponse.getRawResponseText()).thenReturn("finish-raw");
        when(inferenceService.infer(any(), any())).thenReturn(planningResponse, finishResponse);

        
        final String[] capturedNextMessage = new String[1];
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenAnswer(inv -> {
            String msg = inv.getArgument(1);
            capturedNextMessage[0] = msg;
            return "prompt";
        });

        ResponseDirective planningDirective = new ResponseDirective();
        planningDirective.setValid(true);
        planningDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        planningDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirective finishDirective = new ResponseDirective();
        finishDirective.setValid(true);
        finishDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finishDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("planning-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(planningDirective, "Planning body")
        );
        when(directiveParser.parse("finish-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(finishDirective, "Done.")
        );
        service.runCycle(buildRequest(), configWithMaxTurns(5));

        
        assertEquals("Understood.", capturedNextMessage[0]);
        }

        
        @Test
        void finishSuccessIsNotTreatedAsUnderstoodReply() throws Exception {
        MarkdownInferenceResponse finishResponse = mock(MarkdownInferenceResponse.class);
        when(finishResponse.getRawResponseText()).thenReturn("finish-raw");
        when(inferenceService.infer(any(), any())).thenReturn(finishResponse);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finishDirective = new ResponseDirective();
        finishDirective.setValid(true);
        finishDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finishDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("finish-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(finishDirective, "Task completed.")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        
        assertEquals("finish_success", result.getFinalIntent());
        assertEquals(1, result.getUserFacingMessages().size());
        assertEquals("Task completed.", result.getUserFacingMessages().get(0));
        }

        
        @Test
        void finishErrorIsNotTreatedAsUnderstoodReply() throws Exception {
        MarkdownInferenceResponse finishResponse = mock(MarkdownInferenceResponse.class);
        when(finishResponse.getRawResponseText()).thenReturn("error-raw");
        when(inferenceService.infer(any(), any())).thenReturn(finishResponse);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finishErrorDirective = new ResponseDirective();
        finishErrorDirective.setValid(true);
        finishErrorDirective.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishErrorDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("error-raw")).thenReturn(
            new ResponseDirectiveParser.ParseResult(finishErrorDirective, "Could not complete.")
        );

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        assertEquals("finish_error", result.getFinalIntent());
        assertEquals(1, result.getUserFacingMessages().size());
        assertEquals("Could not complete.", result.getUserFacingMessages().get(0));
        }

    
    @Test
    void settingListenerDoesNotAlterUserFacingMessages() throws Exception {
        MarkdownInferenceResponse planningResp = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse finishResp = mock(MarkdownInferenceResponse.class);
        when(planningResp.getRawResponseText()).thenReturn("p-raw");
        when(finishResp.getRawResponseText()).thenReturn("f-raw");
        when(inferenceService.infer(any(), any())).thenReturn(planningResp, finishResp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective waiting = new ResponseDirective();
        waiting.setValid(true);
        waiting.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waiting.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("p-raw")).thenReturn(new ResponseDirectiveParser.ParseResult(waiting, "Planning message"));
        when(directiveParser.parse("f-raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "Finish message"));

        
        ReasoningResult baseline = service.runCycle(buildRequest(), configWithMaxTurns(5));

        
        ReasoningRequest requestWithListener = buildRequest();
        java.util.List<String> listenerCapture = new java.util.ArrayList<>();
        requestWithListener.setUserMessageListener(listenerCapture::add);

        
        when(inferenceService.infer(any(), any())).thenReturn(planningResp, finishResp);
        ReasoningResult withListener = service.runCycle(requestWithListener, configWithMaxTurns(5));

        assertEquals(baseline.getUserFacingMessages(), withListener.getUserFacingMessages(),
                "Setting a userMessageListener must not change getUserFacingMessages() result");
        assertTrue(listenerCapture.contains("Planning message"),
            "Listener should capture the planning user-facing message");
        assertTrue(listenerCapture.contains("Finish message"),
            "Listener should capture the finish user-facing message");
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
        config.setProvider("gemini");
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        settings.setMaxTurns(maxTurns);
        config.setReasoning(settings);
        return config;
    }
}
