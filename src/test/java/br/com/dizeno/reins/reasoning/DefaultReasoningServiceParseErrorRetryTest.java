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
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class DefaultReasoningServiceParseErrorRetryTest {

    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService;
    private ToolResultFormatter toolResultFormatter;
    private DefaultReasoningService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        toolingService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        toolResultFormatter = mock(ToolResultFormatter.class);

        service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );
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
                settings.setEnableReasoningLog(true);
        config.setReasoning(settings);
        return config;
    }

    private ReinsConfig configWithMaxTurnsAndLogging(int maxTurns, boolean logParseErrorRecovery) {
        ReinsConfig config = configWithMaxTurns(maxTurns);
        config.getReasoning().setLogParseErrorRecovery(logParseErrorRecovery);
        return config;
    }

    private ResponseDirectiveParser.ParseResult waitingUserMessage() {
        ResponseDirective directive = new ResponseDirective();
        directive.setValid(true);
        directive.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        directive.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        return new ResponseDirectiveParser.ParseResult(directive, "Working on it.");
    }

    private ResponseDirectiveParser.ParseResult finishSuccess() {
        ResponseDirective directive = new ResponseDirective();
        directive.setValid(true);
        directive.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        directive.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        return new ResponseDirectiveParser.ParseResult(directive, "Done.");
    }

    private ResponseDirectiveParser.ParseResult invalidDirective(String failureReason) {
        ResponseDirective directive = new ResponseDirective();
        directive.setValid(false);
        directive.setFailureReason(failureReason);
        return new ResponseDirectiveParser.ParseResult(directive, "");
    }

    private MarkdownInferenceResponse responseWithText(String text) {
        MarkdownInferenceResponse r = new MarkdownInferenceResponse();
        r.setRawResponseText(text);
        return r;
    }

    private Path findLogFile() throws Exception {
        Path logsDir = tempDir.resolve("reasoning-logs");
        assertTrue(Files.exists(logsDir), "reasoning-logs directory should exist");
        return Files.list(logsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log file found in reasoning-logs"));
    }

    

    @Test
    void singleParseErrorMidCycle_cycleCompletesSuccessfully() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective("Unexpected format."));
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        assertEquals("finish_success", result.getFinalIntent());
        assertEquals(0, result.getRetryStreakCount(), "Successful completion should reset the active retry streak");
    }

    

    @Test
    void parseErrorAtTurnTwo_conversationHistoryExcludesMalformedResponse() throws Exception {
        String malformedText = "MALFORMED-UNIQUE-XZQ-TEXT-9182736";

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText(malformedText),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse(malformedText)).thenReturn(invalidDirective("Parse failed."));
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        
        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        verify(inferenceService, times(3)).infer(captor.capture(), any());

        List<ConversationMessage> historyOnRetry = captor.getAllValues().get(2).getConversationHistory();
        boolean malformedPresent = historyOnRetry.stream()
                .anyMatch(m -> m.getText() != null && m.getText().contains(malformedText));

        assertFalse(malformedPresent,
                "Malformed response text must not appear in conversation history on the corrective retry turn");
        assertEquals(0, result.getRetryStreakCount(), "Successful completion should reset the retry streak count");
    }

    

    @Test
    void correctiveMessage_containsFailureReasonFromParser() throws Exception {
        String failureReason = "Missing-Required-Blank-Line-Separator-9182736";

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective(failureReason));
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        service.runCycle(buildRequest(), configWithMaxTurns(3));

        
        ArgumentCaptor<String> nextMessageCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(promptBuilder, atLeast(1)).buildPrompt(any(), nextMessageCaptor.capture(), any(), anyBoolean());

        boolean foundFailureReason = nextMessageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg != null
                        && msg.contains("The previous response was empty or could not be parsed.")
                        && msg.contains(failureReason)
                        && msg.contains("Avoid repeating this exact formatting mistake."));

        assertTrue(foundFailureReason,
                "Corrective message must include the parser failure reason and explicit corrective guidance");
    }

    @Test
    void retryTurnMessages_includeTurnCountNoteByDefault() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        service.runCycle(buildRequest(), configWithMaxTurns(3));

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        verify(inferenceService, times(2)).infer(captor.capture(), any());

        List<ConversationMessage> secondCallHistory = captor.getAllValues().get(1).getConversationHistory();
        ConversationMessage retryTurnUser = secondCallHistory.stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .reduce((first, second) -> second)
                .orElseThrow();

        assertTrue(retryTurnUser.getText().endsWith("Turn 2/3"),
                "Second-turn outbound message should include the current turn count note");
    }

    

    @Test
    void logParseErrorRecoveryEnabled_writeLogCalledWithCorrectRole() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective("Bad headers."));
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        service.runCycle(buildRequest(), configWithMaxTurnsAndLogging(3, true));

        Path logFile = findLogFile();
        String logContent = Files.readString(logFile);
        assertTrue(logContent.contains("direction: INBOUND") && logContent.contains("role: parse-error-recovery"),
                "Expected parse-error-recovery INBOUND log entry");
        assertTrue(logContent.contains("retryStreakCount=1"),
                "Parse-error recovery log should include the active retry streak count");
    }

    

    @Test
    void logParseErrorRecoveryDisabled_noRecoveryLogEntry() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective("Bad headers."));
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        
        service.runCycle(buildRequest(), configWithMaxTurns(3));

        Path logFile = findLogFile();
        String logContent = Files.readString(logFile);
        boolean recoveryEntryPresent = logContent.contains("role: parse-error-recovery");

        assertFalse(recoveryEntryPresent,
                "No parse-error-recovery log entry should be written when logParseErrorRecovery is false");
    }

    

    @Test
        void nullFailureReason_correctiveMessageUsesRetryScript() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("success-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective(null)); 
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        service.runCycle(buildRequest(), configWithMaxTurns(3));

        ArgumentCaptor<String> nextMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(promptBuilder, atLeast(1)).buildPrompt(any(), nextMessageCaptor.capture(), any(), anyBoolean());

        boolean foundFallback = nextMessageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg != null && msg.contains("The previous response was empty or could not be parsed."));

        assertTrue(foundFallback,
                "Corrective message must come from retry-message.ftl when failureReason is null");
    }

    

    @Test
    void firstTurnParseFailure_retriesAndCanRecover() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("malformed-turn1-response"),
                responseWithText("success-response")
        );

        
        when(directiveParser.parse("malformed-turn1-response")).thenReturn(invalidDirective("No headers."));
        
        when(directiveParser.parse("success-response")).thenReturn(finishSuccess());

        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(3));

        
        assertEquals("finish_success", result.getFinalIntent());
        
        verify(inferenceService, times(2)).infer(any(), any());
    }

    

    @Test
    void allTurnsFailParsing_cycleExhaustsMaxTurnsBudget() throws Exception {
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        
        when(inferenceService.infer(any(), any())).thenReturn(
                responseWithText("waiting-response"),
                responseWithText("malformed-response"),
                responseWithText("grace-response")
        );

        when(directiveParser.parse("waiting-response")).thenReturn(waitingUserMessage());
        when(directiveParser.parse("malformed-response")).thenReturn(invalidDirective("Bad format."));
        
        when(directiveParser.parse("grace-response")).thenReturn(invalidDirective("Still bad."));

        
        
        ReasoningResult result = service.runCycle(buildRequest(), configWithMaxTurns(2));

        assertEquals("finish_error", result.getFinalIntent());
        assertFalse(result.isGraceTurnUsed(),
                "Grace turn should not be used when failure is due to non-fulfillment attempt exhaustion");
    }
}
