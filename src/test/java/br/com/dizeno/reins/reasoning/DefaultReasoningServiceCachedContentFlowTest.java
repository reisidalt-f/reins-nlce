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
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultReasoningServiceCachedContentFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void cacheEnabled_createsUsesAndDeletesAcrossTurns() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.createCachedContent(any(), any())).thenReturn("cachedContents/cycle-a");

        MarkdownInferenceResponse planning = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse finish = mock(MarkdownInferenceResponse.class);
        when(planning.getRawResponseText()).thenReturn("planning");
        when(finish.getRawResponseText()).thenReturn("finish");
        when(inferenceService.infer(any(), any())).thenReturn(planning, finish);

        ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);
        ResponseDirective waiting = new ResponseDirective();
        waiting.setValid(true);
        waiting.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waiting.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirective done = new ResponseDirective();
        done.setValid(true);
        done.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        done.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        when(parser.parse("planning")).thenReturn(new ResponseDirectiveParser.ParseResult(waiting, "Planning"));
        when(parser.parse("finish")).thenReturn(new ResponseDirectiveParser.ParseResult(done, "Done"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = service.runCycle(
                baseRequest(tempDir.relativize(source).toString().replace('\\', '/')),
                config);

        assertEquals("finish_success", result.getFinalIntent());
        verify(inferenceService).createCachedContent(any(), any());
        verify(inferenceService).deleteCachedContent(any(), any());

        List<MarkdownInferenceRequest> calls = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .collect(Collectors.toList());

        assertEquals(2, calls.size());
        for (MarkdownInferenceRequest call : calls) {
            assertTrue(call.isUseCachedContent());
            assertEquals("cachedContents/cycle-a", call.getCachedContentId());
        }

        MarkdownInferenceRequest firstCall = calls.get(0);
        ConversationMessage firstUser = firstCall.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .findFirst()
                .orElseThrow();
        assertTrue(firstUser.getAttachments() == null || firstUser.getAttachments().isEmpty(),
                "With cache active, first user message should not resend context attachments");

        @SuppressWarnings("unchecked")
        List<ConversationMessage> cacheMessages = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("createCachedContent"))
                .map(i -> (List<ConversationMessage>) i.getArgument(1))
                .findFirst()
                .orElseThrow();
        assertEquals(5, cacheMessages.size());
        assertTrue(cacheMessages.stream().allMatch(m -> m.getRole() == ConversationMessage.Role.SYSTEM));
        assertTrue(cacheMessages.stream().noneMatch(m -> m.getText().contains("Compile this source file:")),
                "Cached system payloads should not include first user compile instruction");
        ConversationMessage backgroundFiles = cacheMessages.stream()
                .filter(m -> m.getText().contains("Background attachments passed to this cycle:"))
                .findFirst()
                .orElseThrow();
        assertTrue(backgroundFiles.getAttachments().isEmpty(),
                "Background-files system payload should contain only files added through background sources");
    }

    @Test
    void cacheEnabled_cleanupRunsOnFinishError() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.createCachedContent(any(), any())).thenReturn("cachedContents/cycle-b");

        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("finish-error");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);
        ResponseDirective finishError = new ResponseDirective();
        finishError.setValid(true);
        finishError.setIntent(ResponseDirective.Intent.FINISH_ERROR);
        finishError.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(parser.parse("finish-error")).thenReturn(new ResponseDirectiveParser.ParseResult(finishError, "fail"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = service.runCycle(baseRequest("src/main/nl/domain.md"), config);

        assertEquals("finish_error", result.getFinalIntent());
        verify(inferenceService).deleteCachedContent(any(), any());
    }

    @Test
    void cacheCreateFailure_fallsBackToDirectSystemMode() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.createCachedContent(any(), any())).thenThrow(new RuntimeException("cache down"));

        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("finish");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(parser.parse("finish")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "Done"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = service.runCycle(baseRequest("src/main/nl/domain.md"), config);

        assertEquals("finish_success", result.getFinalIntent());
        verify(inferenceService, never()).deleteCachedContent(any(), any());

        MarkdownInferenceRequest firstCall = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertFalse(firstCall.isUseCachedContent());
        assertNull(firstCall.getCachedContentId());
        assertTrue(firstCall.getConversationHistory().stream().anyMatch(m -> m.getRole() == ConversationMessage.Role.SYSTEM));
    }

    @Test
    void disabledMode_usesSystemRoleAndCompileOnlyFirstUserMessage() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(parser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().setCachedContent(false);

        ReasoningRequest request = baseRequest(tempDir.relativize(source).toString().replace('\\', '/'));
        service.runCycle(request, config);

        verify(inferenceService, never()).createCachedContent(any(), any());
        verify(inferenceService, never()).deleteCachedContent(any(), any());

        MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .findFirst()
                .orElseThrow();

        List<ConversationMessage> systemMessages = sent.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
                .toList();
        ConversationMessage firstUser = sent.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .findFirst()
                .orElseThrow();

        assertFalse(systemMessages.isEmpty());
        assertTrue(systemMessages.stream().noneMatch(m -> m.getText().contains("Compile this source file:")));
        assertTrue(firstUser.getText().contains("domain.md"));
        assertTrue(firstUser.getText().endsWith("Turn 1/10"));
        assertTrue(firstUser.getAttachments() == null || firstUser.getAttachments().isEmpty());
        assertFalse(sent.isUseCachedContent());
    }

    private ReasoningRequest baseRequest(String sourcePath) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Build the files necessary to implement the design from source.");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(sourcePath);

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        return request;
    }

    private Path write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
