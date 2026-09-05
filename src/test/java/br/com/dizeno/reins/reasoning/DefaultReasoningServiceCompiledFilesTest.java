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
import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.domain.FileReference;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService;
    private ToolResultFormatter toolResultFormatter;
    private DefaultReasoningService reasoningService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        toolingService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        toolResultFormatter = mock(ToolResultFormatter.class);

        reasoningService = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
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
        when(toolResultFormatter.format(any())).thenReturn("tool-result");

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

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult toolResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES,
                "main:/feature.md",
                "listed"
        );
        toolResult.setListedPaths(List.of(
                "target:/main/java/demo/App.java",
                "target:/main/resources/demo/logo.bin"
        ));
        when(toolingService.execute(any(), any(), any(), any())).thenReturn(toolResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        reasoningService.runCycle(request, config);

        assertEquals(2, capturedAttachments.size());
        List<AttachedFilePayload> secondAttachments = capturedAttachments.get(1);
        assertEquals(1, secondAttachments.size());
        assertEquals("target:main/java/demo/App.java", secondAttachments.get(0).getQualifiedPath());
        assertTrue(secondAttachments.get(0).getContent().contains("class App"));

        assertFalse(toolResult.getCompiledFileStatuses().isEmpty());
        assertTrue(toolResult.getCompiledFileStatuses().stream().anyMatch(s -> "attached".equals(s.getAttachStatus())));
        assertTrue(toolResult.getCompiledFileStatuses().stream().anyMatch(s -> "not_attachable".equals(s.getAttachStatus())));
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
                when(toolResultFormatter.format(any())).thenReturn("tool-result");

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
                when(toolingService.execute(any(), any(), any(), any())).thenReturn(readResult);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                ReinsConfig config = new ReinsConfig();
                config.setProvider("gemini");
                reasoningService.runCycle(request, config);

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
                when(toolResultFormatter.formatBatch(any())).thenReturn("batch-result");

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

                when(toolingService.execute(any(), any(), any(), any())).thenReturn(success, failure);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                ReinsConfig config = new ReinsConfig();
                config.setProvider("gemini");
                reasoningService.runCycle(request, config);

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
        void acceptsYamlListForBatchToolRequests() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
                when(toolResultFormatter.formatBatch(any())).thenReturn("batch-result");

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
                when(toolingService.execute(any(), any(), any(), any())).thenReturn(result);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                ReinsConfig config = new ReinsConfig();
                config.setProvider("gemini");
                reasoningService.runCycle(request, config);

                verify(toolingService, times(2)).execute(any(), any(), any(), any());
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

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = reasoningService.runCycle(request, config);

        assertEquals("finish_success", result.getFinalIntent());
        assertEquals("finish_success", result.getTerminalReasonCode());
    }

    @Test
    void acceptsSequentialYamlMapsForBatchToolRequests() throws Exception {
        MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
        when(firstResponse.getRawResponseText()).thenReturn("first");
        when(secondResponse.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
        when(toolResultFormatter.formatBatch(any())).thenReturn("batch-result");

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
        when(toolingService.execute(any(), any(), any(), any())).thenReturn(result);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        reasoningService.runCycle(request, config);

        verify(toolingService, times(2)).execute(any(), any(), any(), any());
    }

        @Test
        void continuesAfterMalformedToolRequestBody() throws Exception {
                MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
                MarkdownInferenceResponse thirdResponse = mock(MarkdownInferenceResponse.class);
                when(firstResponse.getRawResponseText()).thenReturn("first");
                when(secondResponse.getRawResponseText()).thenReturn("second");
                when(thirdResponse.getRawResponseText()).thenReturn("third");
                when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse, thirdResponse);

                when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt");
                when(toolResultFormatter.format(any())).thenReturn("tool-error");
                when(toolResultFormatter.formatBatch(any())).thenReturn("batch-result");

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
                when(toolingService.execute(any(), any(), any(), any())).thenReturn(okResult);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("start");
                request.setProjectRoot(tempDir);
                request.setBaseMappings(createBaseMappings());

                ReinsConfig config = new ReinsConfig();
                config.setProvider("gemini");
                ReasoningResult result = reasoningService.runCycle(request, config);

                assertEquals("finish_success", result.getFinalIntent());
                verify(toolingService, times(1)).execute(any(), any(), any(), any());

                ArgumentCaptor<String> nextMessageCaptor = ArgumentCaptor.forClass(String.class);
                verify(promptBuilder, atLeast(1)).buildPrompt(any(), nextMessageCaptor.capture(), any(), anyBoolean());
                boolean routedToRetryMessage = nextMessageCaptor.getAllValues().stream()
                        .anyMatch(message -> message != null
                                && message.contains("The previous response was empty or could not be parsed.")
                                && message.contains("Invalid tool YAML payload.")
                                && message.contains("Avoid repeating this exact formatting mistake."));
                assertTrue(routedToRetryMessage,
                        "Malformed tool YAML should surface the concrete parser error and corrective guidance");
        }

    @Test
    void compiledFileRegistryHandler_normalizesTargetPathsAndResolvesCleanly() throws Exception {
        Path projectRoot = tempDir.resolve("project");
        Path mainNlDir = projectRoot.resolve("src/main/nl/br/com/demo");
        Path targetClassDir = projectRoot.resolve("src/main/nl/br/com/demo");
        Files.createDirectories(mainNlDir);
        Files.createDirectories(targetClassDir);

        Path sourceFile = mainNlDir.resolve("service.md");
        Path compiledClass = targetClassDir.resolve("ServiceImpl.java");
        Files.writeString(sourceFile, "Markdown source");
        Files.writeString(compiledClass, "public class ServiceImpl {}");

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectRoot.resolve("src/main/nl"));
        mappings.setTargetRoot(projectRoot.resolve("src"));
        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(projectRoot));

        CompilationTrackingStore store = new CompilationTrackingStore();
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:br/com/demo/service.md");
        record.setResolvedTargetRoot("src");

        Map<String, FileTrackingDetails> compiledFilesMap = new LinkedHashMap<>();
        compiledFilesMap.put("target:src/main/nl/br/com/demo/ServiceImpl.java", new FileTrackingDetails("target:src/main/nl/br/com/demo/ServiceImpl.java", "main", System.currentTimeMillis()));
        record.setCompiledFiles(compiledFilesMap);
        store.save(projectRoot, "main:br/com/demo/service.md", record);

        br.com.dizeno.reins.reasoning.tooling.handler.CompiledFileRegistryHandler handler = new br.com.dizeno.reins.reasoning.tooling.handler.CompiledFileRegistryHandler(store);
        ToolExecutionRequest request = new ToolExecutionRequest();
        request.setOperation(ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("br/com/demo/service.md");

        ToolExecutionResult result = handler.execute(request, resolver, "main", mock(ScriptRunnerConfig.class));

        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getListedPaths().size());
        assertEquals("target:main/nl/br/com/demo/ServiceImpl.java", result.getListedPaths().get(0));

        Path resolved = resolver.resolve(FileReference.fromCanonical(result.getListedPaths().get(0)));
        assertTrue(Files.exists(resolved), "Resolved path should exist on disk");
        assertEquals(compiledClass.toAbsolutePath().normalize(), resolved.toAbsolutePath().normalize());
    }
}
