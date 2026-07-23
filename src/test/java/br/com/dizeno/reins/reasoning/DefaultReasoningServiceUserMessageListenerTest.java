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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

class DefaultReasoningServiceUserMessageListenerTest {

    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private DefaultReasoningService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);

        service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
            new CompilationTrackingStore()
        );
    }

    
    @Test
    void singlePlanningMessage_listenerCalledOnceWithTrimmedBody() throws Exception {
        MarkdownInferenceResponse planningResp = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse finishResp = mock(MarkdownInferenceResponse.class);
        when(planningResp.getRawResponseText()).thenReturn("planning-raw");
        when(finishResp.getRawResponseText()).thenReturn("finish-raw");
        when(inferenceService.infer(any(), any())).thenReturn(planningResp, finishResp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective planning = buildDirective(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE, ResponseDirective.ContentType.MESSAGE_TO_USER);
        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("planning-raw")).thenReturn(new ResponseDirectiveParser.ParseResult(planning, "  Hello from planning  "));
        when(directiveParser.parse("finish-raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "Done."));

        List<String> captured = new ArrayList<>();
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(captured::add);

        service.runCycle(request, configWithMaxTurns(5));

        
        assertEquals(2, captured.size());
        assertEquals("Hello from planning", captured.get(0));
        assertEquals("Done.", captured.get(1));
    }

    
    @Test
    void multiplePlanningMessages_listenerCalledInOrder() throws Exception {
        MarkdownInferenceResponse r1 = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse r2 = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse r3 = mock(MarkdownInferenceResponse.class);
        when(r1.getRawResponseText()).thenReturn("raw1");
        when(r2.getRawResponseText()).thenReturn("raw2");
        when(r3.getRawResponseText()).thenReturn("raw3");
        when(inferenceService.infer(any(), any())).thenReturn(r1, r2, r3);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective waiting = buildDirective(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE, ResponseDirective.ContentType.MESSAGE_TO_USER);
        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("raw1")).thenReturn(new ResponseDirectiveParser.ParseResult(waiting, "Step 1"));
        when(directiveParser.parse("raw2")).thenReturn(new ResponseDirectiveParser.ParseResult(waiting, "Step 2"));
        when(directiveParser.parse("raw3")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "Done."));

        List<String> captured = new ArrayList<>();
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(captured::add);

        service.runCycle(request, configWithMaxTurns(5));

        assertEquals(3, captured.size());
        assertEquals("Step 1", captured.get(0));
        assertEquals("Step 2", captured.get(1));
        assertEquals("Done.", captured.get(2));
    }

    
    @Test
    void finishSuccessMessage_listenerCalledOnce() throws Exception {
        MarkdownInferenceResponse finishResp = mock(MarkdownInferenceResponse.class);
        when(finishResp.getRawResponseText()).thenReturn("finish-raw");
        when(inferenceService.infer(any(), any())).thenReturn(finishResp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("finish-raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "All done!"));

        List<String> captured = new ArrayList<>();
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(captured::add);

        service.runCycle(request, configWithMaxTurns(3));

        assertEquals(1, captured.size());
        assertEquals("All done!", captured.get(0));
    }

    
    @Test
    void emptyBodyMessage_listenerNotCalled() throws Exception {
        MarkdownInferenceResponse resp = mock(MarkdownInferenceResponse.class);
        when(resp.getRawResponseText()).thenReturn("raw");
        when(inferenceService.infer(any(), any())).thenReturn(resp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "   "));

        List<String> captured = new ArrayList<>();
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(captured::add);

        service.runCycle(request, configWithMaxTurns(3));

        assertTrue(captured.isEmpty());
    }

    
    @Test
    void nullListener_noNpeAndCycleCompletes() throws Exception {
        MarkdownInferenceResponse resp = mock(MarkdownInferenceResponse.class);
        when(resp.getRawResponseText()).thenReturn("raw");
        when(inferenceService.infer(any(), any())).thenReturn(resp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "Done."));

        ReasoningRequest request = buildRequest();
        

        ReasoningResult result = assertDoesNotThrow(() -> service.runCycle(request, configWithMaxTurns(3)));

        assertEquals("finish_success", result.getFinalIntent());
        assertEquals(1, result.getUserFacingMessages().size());
        assertEquals("Done.", result.getUserFacingMessages().get(0));
    }

    
    @Test
    void throwingListener_exceptionSwallowedAndCycleCompletesCorrectly() throws Exception {
        MarkdownInferenceResponse resp = mock(MarkdownInferenceResponse.class);
        when(resp.getRawResponseText()).thenReturn("raw");
        when(inferenceService.infer(any(), any())).thenReturn(resp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finish = buildDirective(ResponseDirective.Intent.FINISH_SUCCESS, ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("raw")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "All good."));

        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(body -> {
            listenerCalled.set(true);
            throw new RuntimeException("listener blew up");
        });

        ReasoningResult result = assertDoesNotThrow(() -> service.runCycle(request, configWithMaxTurns(3)));

        assertTrue(listenerCalled.get(), "Listener should have been called");
        assertEquals("finish_success", result.getFinalIntent());
        assertEquals(1, result.getUserFacingMessages().size());
        assertEquals("All good.", result.getUserFacingMessages().get(0));
    }

    
    @Test
    void graceTurnUserMessage_listenerCalled() throws Exception {
        
        MarkdownInferenceResponse toolResp = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse graceResp = mock(MarkdownInferenceResponse.class);
        when(toolResp.getRawResponseText()).thenReturn("tool-raw");
        when(graceResp.getRawResponseText()).thenReturn("grace-raw");
        
        when(inferenceService.infer(any(), any())).thenReturn(toolResp, graceResp);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective toolDirective = buildDirective(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE, ResponseDirective.ContentType.TOOL_REQUEST);
        ResponseDirective graceDirective = buildDirective(ResponseDirective.Intent.FINISH_ERROR, ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(directiveParser.parse("tool-raw")).thenReturn(
                new ResponseDirectiveParser.ParseResult(toolDirective, "operation: list_files\nbase: main\npath: \"\"\nrecursive: false")
        );
        when(directiveParser.parse("grace-raw")).thenReturn(
                new ResponseDirectiveParser.ParseResult(graceDirective, "Grace turn message.")
        );

        br.com.dizeno.reins.reasoning.tooling.ToolingService toolService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        when(toolService.execute(any(), any(), any(), any())).thenReturn(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES, "main:/", "[]")
        );

        service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                toolService,
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
            new CompilationTrackingStore()
        );

        List<String> captured = new ArrayList<>();
        ReasoningRequest request = buildRequest();
        request.setUserMessageListener(captured::add);

        service.runCycle(request, configWithMaxTurns(1));

        assertEquals(1, captured.size());
        assertEquals("Grace turn message.", captured.get(0));
    }

    

    private ResponseDirective buildDirective(ResponseDirective.Intent intent, ResponseDirective.ContentType contentType) {
        ResponseDirective d = new ResponseDirective();
        d.setValid(true);
        d.setIntent(intent);
        d.setContentType(contentType);
        return d;
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
        ReasoningSettings settings = new ReasoningSettings();
        settings.setEnabled(true);
        settings.setMaxTurns(maxTurns);
        config.setReasoning(settings);
        return config;
    }
}
