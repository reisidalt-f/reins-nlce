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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class DefaultReasoningServiceCompiledFilesTest {
    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService;
    private ToolResultFormatter mcpResultFormatter;
    private DefaultReasoningService reasoningService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        mcpService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        mcpResultFormatter = mock(ToolResultFormatter.class);

        reasoningService = new DefaultReasoningService(
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
    void attachesReadableCompiledFilesAndReportsNonTextMetadata() throws Exception {
        Path javaFile = tempDir.resolve("src/main/java/demo/App.java");
        Path binaryFile = tempDir.resolve("src/main/resources/demo/logo.bin");
        Files.createDirectories(javaFile.getParent());
        Files.createDirectories(binaryFile.getParent());
        Files.writeString(javaFile, "class App {}\n");
        Files.write(binaryFile, new byte[]{0, 1, 2, 3});

        MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
        when(firstResponse.getRawResponseText()).thenReturn("first");
        when(secondResponse.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

                List<List<AttachedFilePayload>> capturedAttachments = new ArrayList<>();
                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
                        List<AttachedFilePayload> attachments = invocation.getArgument(2);
                        capturedAttachments.add(new ArrayList<>(attachments));
                        return capturedAttachments.size() == 1 ? "prompt-1" : "prompt-2";
                });
        when(mcpResultFormatter.format(any())).thenReturn("mcp-result");

        ResponseDirective waitDirective = new ResponseDirective();
        waitDirective.setValid(true);
        waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective doneDirective = new ResponseDirective();
        doneDirective.setValid(true);
        doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(
                waitDirective,
                "operation: list_compiled_files\nbase: main\npath: feature.md\n"
        );
        ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
        when(directiveParser.parse("first")).thenReturn(firstParse);
        when(directiveParser.parse("second")).thenReturn(secondParse);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult mcpResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES,
                "main:/feature.md",
                "listed"
        );
        mcpResult.setListedPaths(List.of(
                "target:/main/java/demo/App.java",
                "target:/main/resources/demo/logo.bin"
        ));
        when(mcpService.execute(any(), any(), any(), any())).thenReturn(mcpResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        reasoningService.runCycle(request, new ReinsConfig());

        assertEquals(2, capturedAttachments.size());
        List<AttachedFilePayload> secondAttachments = capturedAttachments.get(1);
        assertEquals(1, secondAttachments.size());
        assertEquals("target:main/java/demo/App.java", secondAttachments.get(0).getQualifiedPath());
        assertTrue(secondAttachments.get(0).getContent().contains("class App"));

        assertFalse(mcpResult.getCompiledFileStatuses().isEmpty());
        assertTrue(mcpResult.getCompiledFileStatuses().stream().anyMatch(s -> "attached".equals(s.getAttachStatus())));
        assertTrue(mcpResult.getCompiledFileStatuses().stream().anyMatch(s -> "not_attachable".equals(s.getAttachStatus())));
    }

        @Test
        void attachesSuccessfulReadFileResultAndTracksReadFileStatus() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

                List<List<AttachedFilePayload>> capturedAttachments = new ArrayList<>();
                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
                        List<AttachedFilePayload> attachments = invocation.getArgument(2);
                        capturedAttachments.add(new ArrayList<>(attachments));
                        return capturedAttachments.size() == 1 ? "prompt-1" : "prompt-2";
                });
                when(mcpResultFormatter.format(any())).thenReturn("mcp-result");

                ResponseDirective waitDirective = new ResponseDirective();
                waitDirective.setValid(true);
                waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
                waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

                ResponseDirective doneDirective = new ResponseDirective();
                doneDirective.setValid(true);
                doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
                doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

                ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(
                                waitDirective,
                                "operation: read_file\nbase: main\npath: domain/entities.md\n"
                );
                ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
                when(directiveParser.parse("first")).thenReturn(firstParse);
                when(directiveParser.parse("second")).thenReturn(secondParse);

                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult readResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                                "main:/domain/entities.md",
                                "read"
                );
                readResult.setResolvedBase("main");
                readResult.setContent("entity-line-1\nentity-line-2\n");
                when(mcpService.execute(any(), any(), any(), any())).thenReturn(readResult);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                reasoningService.runCycle(request, new ReinsConfig());

                assertEquals(2, capturedAttachments.size());
                List<AttachedFilePayload> secondAttachments = capturedAttachments.get(1);
                assertEquals(1, secondAttachments.size());
                assertEquals("main:domain/entities.md", secondAttachments.get(0).getQualifiedPath());
                assertEquals("entity-line-1\nentity-line-2\n", secondAttachments.get(0).getContent());

                assertFalse(readResult.getReadFileStatuses().isEmpty());
                assertEquals("attached", readResult.getReadFileStatuses().get(0).getAttachStatus());
        }

        @Test
        void mixedReadFileBatchAttachesOnlySuccessfulReads() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

                List<List<AttachedFilePayload>> capturedAttachments = new ArrayList<>();
                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
                        List<AttachedFilePayload> attachments = invocation.getArgument(2);
                        capturedAttachments.add(new ArrayList<>(attachments));
                        return capturedAttachments.size() == 1 ? "prompt-1" : "prompt-2";
                });
                when(mcpResultFormatter.formatBatch(any())).thenReturn("batch-result");

                ResponseDirective waitDirective = new ResponseDirective();
                waitDirective.setValid(true);
                waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
                waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

                ResponseDirective doneDirective = new ResponseDirective();
                doneDirective.setValid(true);
                doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
                doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

                ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(
                                waitDirective,
                                "- operation: read_file\n"
                                                + "  base: main\n"
                                                + "  path: domain/entities.md\n"
                                                + "- operation: read_file\n"
                                                + "  base: main\n"
                                                + "  path: missing.md\n"
                );
                ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
                when(directiveParser.parse("first")).thenReturn(firstParse);
                when(directiveParser.parse("second")).thenReturn(secondParse);

                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult success = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                                "main:/domain/entities.md",
                                "read"
                );
                success.setResolvedBase("main");
                success.setContent("ok\n");

                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult failure = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                                "main:/missing.md",
                                "File does not exist."
                );
                failure.setResolvedBase("main");

                when(mcpService.execute(any(), any(), any(), any())).thenReturn(success, failure);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                reasoningService.runCycle(request, new ReinsConfig());

                assertEquals(2, capturedAttachments.size());
                List<AttachedFilePayload> secondAttachments = capturedAttachments.get(1);
                assertEquals(1, secondAttachments.size());
                assertEquals("main:domain/entities.md", secondAttachments.get(0).getQualifiedPath());

                assertFalse(success.getReadFileStatuses().isEmpty());
                assertEquals("attached", success.getReadFileStatuses().get(0).getAttachStatus());
                assertFalse(failure.getReadFileStatuses().isEmpty());
                assertEquals("read_failed", failure.getReadFileStatuses().get(0).getAttachStatus());
        }

    private BasePathMappingSet createBaseMappings() {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        return mappings;
    }

        @Test
        void acceptsYamlListForBatchMcpRequests() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
                when(mcpResultFormatter.formatBatch(any())).thenReturn("batch-result");

                ResponseDirective waitDirective = new ResponseDirective();
                waitDirective.setValid(true);
                waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
                waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

                ResponseDirective doneDirective = new ResponseDirective();
                doneDirective.setValid(true);
                doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
                doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

                ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(
                                waitDirective,
                                "- operation: list_files\n"
                                                + "  base: main\n"
                                                + "  path: domain\n"
                                                + "- operation: list_compiled_files\n"
                                                + "  base: main\n"
                                                + "  path: domain/entities.md\n"
                );
                ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
                when(directiveParser.parse("first")).thenReturn(firstParse);
                when(directiveParser.parse("second")).thenReturn(secondParse);

                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES,
                                "main:/domain",
                                "ok"
                );
                when(mcpService.execute(any(), any(), any(), any())).thenReturn(result);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                reasoningService.runCycle(request, new ReinsConfig());

                verify(mcpService, times(2)).execute(any(), any(), any(), any());
        }

    @Test
    void completesWithFinishSuccessEvenWhenNoFencedBlocksExist() throws Exception {
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("raw-response");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");

        ResponseDirective finishDirective = new ResponseDirective();
        finishDirective.setValid(true);
        finishDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finishDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("raw-response")).thenReturn(
                new ResponseDirectiveParser.ParseResult(finishDirective, "done")
        );

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReasoningResult result = reasoningService.runCycle(request, new ReinsConfig());

        assertEquals("finish_success", result.getFinalIntent());
        assertEquals("finish_success", result.getTerminalReasonCode());
    }

    @Test
    void acceptsSequentialYamlMapsForBatchMcpRequests() throws Exception {
        MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
        when(firstResponse.getRawResponseText()).thenReturn("first");
        when(secondResponse.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
        when(mcpResultFormatter.formatBatch(any())).thenReturn("batch-result");

        ResponseDirective waitDirective = new ResponseDirective();
        waitDirective.setValid(true);
        waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective doneDirective = new ResponseDirective();
        doneDirective.setValid(true);
        doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        String sequentialBody = "operation: write_file\n"
                + "base: target\n"
                + "path: main/java/demo/A.java\n"
                + "content: |\n"
                + "  class A {}\n"
                + "intent: first write\n\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: main/java/demo/B.java\n"
                + "content: |\n"
                + "  class B {}\n"
                + "intent: second write\n";

        ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(waitDirective, sequentialBody);
        ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
        when(directiveParser.parse("first")).thenReturn(firstParse);
        when(directiveParser.parse("second")).thenReturn(secondParse);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE,
                "target:/main/java/demo/A.java",
                "ok"
        );
        when(mcpService.execute(any(), any(), any(), any())).thenReturn(result);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        reasoningService.runCycle(request, new ReinsConfig());

        verify(mcpService, times(2)).execute(any(), any(), any(), any());
    }

        @Test
        void continuesAfterMalformedMcpRequestBody() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse thirdResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(thirdResponse.getRawResponseText()).thenReturn("third");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse, thirdResponse);

                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
                when(mcpResultFormatter.format(any())).thenReturn("mcp-error");
                when(mcpResultFormatter.formatBatch(any())).thenReturn("batch-result");

                ResponseDirective waitDirective = new ResponseDirective();
                waitDirective.setValid(true);
                waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
                waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

                ResponseDirective doneDirective = new ResponseDirective();
                doneDirective.setValid(true);
                doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
                doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

                String malformedBody = "operation: patch_file\n"
                                + "base: target\n"
                                + "path: main/java/demo/A.java\n"
                                + "atLine: 1\n"
                                + "content: this text contains a colon: and is not quoted\n"
                                + "intent: malformed patch content\n";

                String validBody = "operation: write_file\n"
                                + "base: target\n"
                                + "path: main/java/demo/B.java\n"
                                + "content: \"class B {}\\n\"\n"
                                + "intent: valid write after malformed request\n";

                when(directiveParser.parse("first")).thenReturn(new ResponseDirectiveParser.ParseResult(waitDirective, malformedBody));
                when(directiveParser.parse("second")).thenReturn(new ResponseDirectiveParser.ParseResult(waitDirective, validBody));
                when(directiveParser.parse("third")).thenReturn(new ResponseDirectiveParser.ParseResult(doneDirective, "done"));

                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult okResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE,
                                "target:/main/java/demo/B.java",
                                "ok"
                );
                when(mcpService.execute(any(), any(), any(), any())).thenReturn(okResult);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                ReasoningResult result = reasoningService.runCycle(request, new ReinsConfig());

                assertEquals("finish_success", result.getFinalIntent());
                verify(mcpService, times(1)).execute(any(), any(), any(), any());

                ArgumentCaptor<String> nextMessageCaptor = ArgumentCaptor.forClass(String.class);
                verify(promptBuilder, atLeast(1)).buildPrompt(any(), nextMessageCaptor.capture(), any(), anyBoolean());
                boolean routedToRetryMessage = nextMessageCaptor.getAllValues().stream()
                        .anyMatch(message -> message != null
                                && message.contains("The previous response was empty or could not be parsed.")
                                && message.contains("Invalid tool YAML payload.")
                                && message.contains("Avoid repeating this exact formatting mistake."));
                assertTrue(routedToRetryMessage,
                        "Malformed MCP YAML should surface the concrete parser error and corrective guidance");
        }
}
