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
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultReasoningServiceProjectContextTest {

    @TempDir
    Path tempDir;

    @Test
    void contextFileAttachmentsAreAddedBeforeSourceAttachment() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        CompilationBackgroundFile ctxFile = new CompilationBackgroundFile(
                tempDir.resolve("project.md"),
                "project.md",
                "Project overview\n"
        );
        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(ctxFile));

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        request.setCompilationBackgroundPayload(payload);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        List<AttachedFilePayload> attachments = backgroundFilesMessage(sent).getAttachments();

        assertTrue(attachments.size() >= 1, "Expected at least 1 background attachment");
        assertEquals("main:project.md", attachments.get(0).getQualifiedPath());
    }

    @Test
    void multipleContextFilesAllPrependedBeforeSource() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        CompilationBackgroundFile ctx1 = new CompilationBackgroundFile(
                tempDir.resolve("project.md"), "project.md", "Overview\n");
        CompilationBackgroundFile ctx2 = new CompilationBackgroundFile(
                tempDir.resolve("arch.md"), "arch.md", "Architecture\n");
        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(ctx1, ctx2));

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        request.setCompilationBackgroundPayload(payload);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        List<AttachedFilePayload> attachments = backgroundFilesMessage(sent).getAttachments();

        assertEquals(2, attachments.size());
        assertEquals("main:project.md", attachments.get(0).getQualifiedPath());
        assertEquals("main:arch.md", attachments.get(1).getQualifiedPath());
    }

    @Test
    void firstTurnMessageBodyDoesNotContainProjectContextSection() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        CompilationBackgroundFile ctxFile = new CompilationBackgroundFile(
                tempDir.resolve("project.md"), "project.md", "High level overview\n");
        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(ctxFile));

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        request.setCompilationBackgroundPayload(payload);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        String firstTurnText = firstUserMessage(sent).getText();

        assertFalse(firstTurnText.contains("Compilation background:"), "Compilation background should not appear in first-turn message body (sent as attachment)");
        assertFalse(firstTurnText.contains("High level overview"), "Context file content should not appear in first-turn message body");
    }

    

    @Test
    void nullPayloadProducesOnlySourceAttachment() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        List<AttachedFilePayload> attachments = backgroundFilesMessage(sent).getAttachments();

        assertEquals(0, attachments.size(), "No background attachments expected when payload is null");
    }

    @Test
    void nullPayloadProducesNoProjectContextSection() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        String firstTurnText = firstUserMessage(sent).getText();

        assertFalse(firstTurnText.contains("Compilation background:"), "Should not contain 'Compilation background:' when payload is null");
    }

    @Test
    void emptyPayloadProducesOnlySourceAttachment() throws Exception {
        Path source = write("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mockFinishSuccessInference();
        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = baseRequest(source);
        request.setCompilationBackgroundPayload(CompilationBackgroundPayload.empty());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        service.runCycle(request, config);

        MarkdownInferenceRequest sent = captureFirstInferRequest(inferenceService);
        List<AttachedFilePayload> attachments = backgroundFilesMessage(sent).getAttachments();

        assertEquals(0, attachments.size(), "No background attachments expected when payload is empty");
    }

    

    private InferenceService mockFinishSuccessInference() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        return inferenceService;
    }

    private DefaultReasoningService buildService(InferenceService inferenceService) throws Exception {
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        return new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );
    }

    private ReasoningRequest baseRequest(Path source) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Use attached source markdown as the primary context.");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        return request;
    }

    private MarkdownInferenceRequest captureFirstInferRequest(InferenceService inferenceService) {
        return org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .findFirst()
                .orElseThrow();
    }

    private ConversationMessage firstSystemMessage(MarkdownInferenceRequest sent) {
        return sent.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
                .findFirst()
                .orElseThrow();
    }

    private ConversationMessage backgroundFilesMessage(MarkdownInferenceRequest sent) {
        return sent.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
                .filter(m -> m.getText() != null && m.getText().contains("Background attachments passed to this cycle:"))
                .findFirst()
                .orElseThrow();
    }

    private ConversationMessage firstUserMessage(MarkdownInferenceRequest sent) {
        return sent.getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .findFirst()
                .orElseThrow();
    }

    private Path write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
