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

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultReasoningServiceReferenceTreeTest {
    @TempDir
    Path tempDir;

    @Test
    void includesReferenceTreeInFirstOutboundMessageAndReferencedSourceAttachments() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\nUse [grandchild.md]\n");
        write("src/main/nl/grandchild.md", "Grandchild\n");

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
            new CompilationTrackingStore()
        );

        ReasoningRequest request = baseRequest(root);
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setAttachFiles(true);
        config.getContext().getReferencesTree().setDepth("1");
        ReasoningResult result = service.runCycle(request, config);

        MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .findFirst()
                .orElseThrow();

        List<ConversationMessage> history = sent.getConversationHistory();
        assertTrue(history.size() >= 2);

        ConversationMessage systemContext = history.stream()
            .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
            .filter(m -> m.getText().contains("Reference tree:"))
            .findFirst()
            .orElseThrow();
        ConversationMessage backgroundFiles = history.stream()
            .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
            .filter(m -> m.getText().contains("Background attachments passed to this cycle:"))
            .findFirst()
            .orElseThrow();
        ConversationMessage firstUser = history.stream()
            .filter(m -> m.getRole() == ConversationMessage.Role.USER)
            .findFirst()
            .orElseThrow();

        assertTrue(systemContext.getText().contains("Reference tree:"));
        assertTrue(systemContext.getText().contains("root.md"));
        assertTrue(systemContext.getText().contains("child.md"));
        assertFalse(systemContext.getText().contains("grandchild.md"));
        assertTrue(firstUser.getText().contains("Compile this source file:"));
        assertTrue(firstUser.getText().contains("root.md"));

        assertTrue(firstUser.getAttachments() == null || firstUser.getAttachments().isEmpty());
        assertNotNull(backgroundFiles.getAttachments());
        assertEquals(0, backgroundFiles.getAttachments().size());

        assertNotNull(systemContext.getAttachments());
        assertEquals(2, systemContext.getAttachments().size());
        assertEquals("main:root.md", systemContext.getAttachments().get(0).getQualifiedPath());
        assertEquals("main:child.md", systemContext.getAttachments().get(1).getQualifiedPath());

        assertNotNull(result.getFirstTurnReferenceTree());
        assertTrue(result.getFirstTurnReferenceTree().contains("child.md"));
        assertFalse(result.getFirstTurnReferenceTree().contains("grandchild.md"));
        assertTrue(result.getFirstTurnReferenceTree().contains("└── child.md"));
    }

    @Test
    void keepsCompiledFilesOutsideReferenceTreeWhenTrackingRecordHasOutputs() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\n");

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("src/main/nl/root.md");
        record.setSourceCategory("main");
        record.setCompiledFiles(Map.of(
            "target:src/main/java/com/example/Root.java",
            new FileTrackingDetails("src/main/nl/root.md", "main", 1L)
        ));
        new CompilationTrackingStore().save(tempDir, "src/main/nl/root.md", record);

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = service.runCycle(baseRequest(root), config);

    MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
        .getInvocations()
        .stream()
        .filter(i -> i.getMethod().getName().equals("infer"))
        .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
        .findFirst()
        .orElseThrow();

    ConversationMessage systemContext = sent.getConversationHistory().stream()
        .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
        .findFirst()
        .orElseThrow();

    assertFalse(result.getFirstTurnReferenceTree().contains("[compiled files]"));
    assertFalse(result.getFirstTurnReferenceTree().contains("[no compiled file]"));
    assertFalse(result.getFirstTurnReferenceTree().contains("target:src/main/java/com/example/Root.java"));
    assertFalse(systemContext.getText().contains("Files previously compiled from the main source document:"));
    }

    @Test
    void attachReferencedFilesFalse_keepsOnlySourceAttachment() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\n");

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setAttachFiles(false);
        service.runCycle(baseRequest(root), config);

        MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                .getInvocations()
                .stream()
                .filter(i -> i.getMethod().getName().equals("infer"))
                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                .findFirst()
                .orElseThrow();

        List<AttachedFilePayload> attachments = sent.getConversationHistory().stream()
            .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
            .filter(m -> m.getText().contains("Reference tree:"))
            .findFirst()
            .orElseThrow()
            .getAttachments();
        assertEquals(1, attachments.size());
        assertEquals("main:root.md", attachments.get(0).getQualifiedPath());
    }

    @Test
    void depthZeroOmitsReferencedDescendantsFromFirstTurnTree() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\n");

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setDepth("0");
        ReasoningResult result = service.runCycle(baseRequest(root), config);

        assertNotNull(result.getFirstTurnReferenceTree());
        assertTrue(result.getFirstTurnReferenceTree().contains("root.md"));
        assertFalse(result.getFirstTurnReferenceTree().contains("child.md"));
    }

    @Test
    void boundedDepthTwoIncludesGrandchild() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\nUse [grandchild.md]\n");
        write("src/main/nl/grandchild.md", "Grandchild\n");

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setDepth("2");
        ReasoningResult result = service.runCycle(baseRequest(root), config);

        assertTrue(result.getFirstTurnReferenceTree().contains("grandchild.md"));
    }

    @Test
    void unlimitedDepthIncludesAllReachableDescendants() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [child.md]\n");
        write("src/main/nl/child.md", "Child\nUse [grandchild.md]\n");
        write("src/main/nl/grandchild.md", "Grandchild\nUse [great-grandchild.md]\n");
        write("src/main/nl/great-grandchild.md", "Great grandchild\n");

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

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setDepth("*");
        ReasoningResult result = service.runCycle(baseRequest(root), config);

        assertTrue(result.getFirstTurnReferenceTree().contains("great-grandchild.md"));
    }

    @Test
    void failsBeforeInferenceWhenReferenceCycleDetected() throws Exception {
        Path root = write("src/main/nl/root.md", "Use [a.md]\n");
        write("src/main/nl/a.md", "Use [root.md]\n");

        InferenceService inferenceService = mock(InferenceService.class);
        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReasoningRequest request = baseRequest(root);
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().getReferencesTree().setDepth("*");
        ReasoningResult result = service.runCycle(request, config);

        assertEquals("finish_error", result.getFinalIntent());
        assertTrue(result.getTerminalReasonMessage().toLowerCase().contains("circular")
                || result.getTerminalReasonMessage().toLowerCase().contains("cycle"));
        verify(inferenceService, never()).infer(any(), any());
    }

    private ReasoningRequest baseRequest(Path root) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Use attached source markdown as the primary context.");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(root).toString().replace('\\', '/'));

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
