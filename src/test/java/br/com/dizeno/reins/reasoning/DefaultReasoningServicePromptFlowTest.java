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
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultReasoningServicePromptFlowTest {

        @TempDir
        Path tempDir;

        @Test
        void runScriptReferenceUsesScriptArgsContractWithoutLegacyBasePath() {
                String reference = ToolOperationsReference.build(FilePolicy.allPermissive(), true, true, true, true);

                assertTrue(reference.contains("- **run_script**: Execute an enabled script"));
                assertTrue(reference.contains("required: script"));
                assertTrue(reference.contains("base and path fields are non-operative for run_script and are ignored"));
                assertTrue(reference.contains("canonical path under configured script root"));
                assertFalse(reference.contains("required: base=script, path"));
                assertFalse(reference.contains("path format: script file path only"));
        }

        @Test
        void firstUserCompileInstructionShape_isStableAcrossCachedContentToggleStates() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\n");

                MarkdownInferenceRequest disabledReq = runAndCaptureFirstInferRequest(
                                tempDir.relativize(source).toString().replace('\\', '/'),
                                false);

                MarkdownInferenceRequest enabledReq = runAndCaptureFirstInferRequest(
                                tempDir.relativize(source).toString().replace('\\', '/'),
                                true);

                ConversationMessage disabledFirstUser = disabledReq.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .findFirst()
                                .orElseThrow();
                ConversationMessage enabledFirstUser = enabledReq.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .findFirst()
                                .orElseThrow();

                assertEquals(disabledFirstUser.getText(), enabledFirstUser.getText());
                assertTrue(enabledFirstUser.getText().contains("main:domain.md"));
                assertTrue(enabledFirstUser.getText().contains("Turn 1/10"));

                assertTrue(disabledReq.getConversationHistory().stream()
                                .anyMatch(m -> m.getRole() == ConversationMessage.Role.SYSTEM));
                assertFalse(enabledReq.getConversationHistory().stream()
                                .anyMatch(m -> m.getRole() == ConversationMessage.Role.SYSTEM),
                                "Cache-enabled request should not resend system context in history when cache creation succeeds");
                assertTrue(disabledFirstUser.getAttachments() == null || disabledFirstUser.getAttachments().isEmpty(),
                                "Cache-disabled first user message should also remain compile-only without context attachments");
                assertTrue(enabledFirstUser.getAttachments() == null || enabledFirstUser.getAttachments().isEmpty(),
                                "Cache-enabled first user message should not resend context attachments");
                assertFalse(disabledReq.isUseCachedContent());
                assertTrue(enabledReq.isUseCachedContent());
        }

        @Test
        void runScriptCompileError_doesNotRequireAddInferenceNoteBeforePatch() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\n");

                InferenceService inferenceService = mock(InferenceService.class);
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

                br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService = mock(
                                br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult compileError = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult
                                .error(
                                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT,
                                                "script:compile-one-java.sh",
                                                "compile failed");
                compileError.setStarted(true);
                compileError.setExitCode(1);
                compileError.setStdout("target/main/java/com/example/Broken.java:[12,8] cannot find symbol\n");
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult patchSuccess = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult
                                .success(
                                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE,
                                                "target:main/java/com/example/Broken.java",
                                                "patched");
                when(mcpService.execute(any(), any(), any(), any())).thenReturn(compileError, patchSuccess);

                DefaultReasoningService service = new DefaultReasoningService(
                                inferenceService,
                                new ResponseDirectiveParser(),
                                new ReasoningPromptBuilder(),
                                mcpService,
                                new ToolResultFormatter(),
                                new FileReasoningLogService(),
                                new CompilationTrackingStore());

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                mappings.setScriptRoot(tempDir.resolve("scripts"));
                request.setBaseMappings(mappings);

                service.runCycle(request, config);

                verify(inferenceService, times(3)).infer(any(), any());
                ArgumentCaptor<br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest> operationCaptor = ArgumentCaptor
                                .forClass(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.class);
                verify(mcpService, times(2)).execute(operationCaptor.capture(), any(), any(), any());

                List<br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest> captured = operationCaptor
                                .getAllValues();
                assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT,
                                captured.get(0).getOperation());
                assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE,
                                captured.get(1).getOperation());

                List<MarkdownInferenceRequest> inferRequests = org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                                .toList();
                String secondTurnUserPayload = inferRequests.get(1).getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .reduce((first, next) -> next)
                                .map(ConversationMessage::getText)
                                .orElse("");
                assertFalse(secondTurnUserPayload.contains("you MUST emit add_reasoning_note"));
                assertFalse(secondTurnUserPayload.contains("Cross-source compile failures detected"));
        }

        @Test
        void blankCustomSystemContextFallsBackToBundledAndCycleSucceeds() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\n");
                Path customDir = tempDir.resolve("custom-scripts");
                Files.createDirectories(customDir);
                Files.writeString(customDir.resolve("system-context.ftl"), "   \n\t ");

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

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(customDir.toFile(), null),
                                customDir.toFile());
                registry.validateAll();

                DefaultReasoningService service = new DefaultReasoningService(
                                inferenceService,
                                parser,
                                new ReasoningPromptBuilder(),
                                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                                mock(ToolResultFormatter.class),
                                new FileReasoningLogService(),
                                new CompilationTrackingStore())
                                .withScriptEvaluator(new ScriptEvaluator(registry, null));

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                request.setBaseMappings(mappings);

                service.runCycle(request, config);

                MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                                .findFirst()
                                .orElseThrow();

                ConversationMessage system = sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
                                .findFirst()
                                .orElseThrow();
                assertTrue(system.getText() != null && !system.getText().isBlank());
        }

        @Test
        void fatalCustomSystemContextStopsCycleWithoutFallbackAndIncludesDiagnostics() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\n");
                Path customDir = tempDir.resolve("custom-fatal");
                Files.createDirectories(customDir);
                Files.writeString(customDir.resolve("system-context.ftl"), "${\"1+1\"?eval}");

                InferenceService inferenceService = mock(InferenceService.class);
                ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(customDir.toFile(), null),
                                customDir.toFile());
                DefaultReasoningService service = new DefaultReasoningService(
                                inferenceService,
                                parser,
                                new ReasoningPromptBuilder(),
                                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                                mock(ToolResultFormatter.class),
                                new FileReasoningLogService(),
                                new CompilationTrackingStore())
                                .withScriptEvaluator(new ScriptEvaluator(registry, null));

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                request.setBaseMappings(mappings);

                org.junit.jupiter.api.Assertions.assertThrows(
                                IllegalStateException.class,
                                () -> service.runCycle(request, config));
        }

        @Test
        void turnCountNoteDisabled_omitsTurnSuffixFromOutboundMessages() throws Exception {
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
                                new CompilationTrackingStore());

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);
                config.getReasoning().setTurnCountNote(false);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                request.setBaseMappings(mappings);

                service.runCycle(request, config);

                MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                                .findFirst()
                                .orElseThrow();

                ConversationMessage system = sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.SYSTEM)
                                .findFirst()
                                .orElseThrow();
                ConversationMessage firstUser = sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .findFirst()
                                .orElseThrow();

                assertFalse(system.getText().contains("Turn 1/10"));
                assertFalse(firstUser.getText().contains("Turn 1/10"));
                assertTrue(firstUser.getText().contains("main:domain.md"));
        }

        @Test
        void spoofedAssistantPipelineMcpRequest_executesWithoutUserHistoryOrTurnSuffix() throws Exception {
                Path source = write("src/main/nl/domain.md", "# domain\nsource body\n");
                write("src/main/nl/notes.md", "# notes\nextra context\n");
                Path customScripts = tempDir.resolve("custom-scripts");
                Files.createDirectories(customScripts);
                Files.writeString(customScripts.resolve("reasoning-pipeline.ftl"), """
                                <#ftl strip_whitespace=true>
                                <#assign p = (phase!\"\")?trim>
                                <#if p == \"list-phases\">
                                default-cycle
                                <#elseif p == \"default-cycle\">
                                <#if project.pipeline?? && project.pipeline.currentToolResultAvailable>
                                INTENT: waiting-for-next-message
                                CONTENT_TYPE: gemini-message

                                The MCP result is now available. Finish successfully.
                                <#else>
                                ROLE: assistant
                                INTENT: waiting-for-next-message
                                CONTENT_TYPE: tool-request

                                operation: read_file
                                base: main
                                path: notes.md
                                </#if>
                                <#else>
                                INTENT: finish-error
                                CONTENT_TYPE: message-to-user

                                unexpected phase
                                </#if>
                                """);

                InferenceService inferenceService = mock(InferenceService.class);
                MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
                when(response.getRawResponseText())
                                .thenReturn("INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone\n");
                when(inferenceService.infer(any(), any())).thenReturn(response);

                br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService = mock(
                                br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
                when(mcpService.execute(any(), any(), any(), any())).thenReturn(
                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                                                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                                                "main:notes.md",
                                                "# notes\nextra context\n"));

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(customScripts.toFile(), null),
                                customScripts.toFile());

                DefaultReasoningService service = new DefaultReasoningService(
                                inferenceService,
                                new ResponseDirectiveParser(),
                                new ReasoningPromptBuilder(),
                                mcpService,
                                new ToolResultFormatter(),
                                new FileReasoningLogService(),
                                new CompilationTrackingStore())
                                .withScriptEvaluator(new ScriptEvaluator(registry, null));

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                request.setBaseMappings(mappings);

                service.runCycle(request, config);

                verify(inferenceService, times(1)).infer(any(), any());
                verify(mcpService, times(1)).execute(any(), any(), any(), any());

                MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                                .findFirst()
                                .orElseThrow();

                assertFalse(sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .anyMatch(m -> m.getText().contains("ROLE: assistant")));

                ConversationMessage spoofedAssistant = sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.MODEL)
                                .filter(m -> m.getText().contains("ROLE: assistant"))
                                .findFirst()
                                .orElseThrow();

                assertTrue(spoofedAssistant.getText().contains("CONTENT_TYPE: tool-request"));
                assertFalse(spoofedAssistant.getText().contains("Turn 1/10"));
        }

        @Test
        void firstTurnAtMaxTurn_includesTerminalFinishInstructionNote() throws Exception {
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
                                new CompilationTrackingStore());

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(false);
                config.getGemini().setMaximumTurns(1);

                ReasoningRequest request = new ReasoningRequest();
                request.setMessage("Build the files necessary to implement the design from source.");
                request.setProjectRoot(tempDir);
                request.setSourceScope("main");
                request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

                BasePathMappingSet mappings = new BasePathMappingSet();
                mappings.setMainRoot(tempDir.resolve("src/main/nl"));
                mappings.setTestRoot(tempDir.resolve("src/test/nl"));
                mappings.setTargetRoot(tempDir.resolve("src"));
                request.setBaseMappings(mappings);

                service.runCycle(request, config);

                MarkdownInferenceRequest sent = org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
                                .findFirst()
                                .orElseThrow();

                ConversationMessage firstUser = sent.getConversationHistory().stream()
                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                                .findFirst()
                                .orElseThrow();

                assertTrue(firstUser.getText().contains("Turn 1/1"));
                assertTrue(firstUser.getText().contains(
                                "Respond with a finish-success message requesting the MCP operations needed to complete the task or a finish-error with a message to the user indicating the failure. You should not wait for the results of the MCP operations, the task will be considered complete if all MCP operations succeed."));
        }

        private MarkdownInferenceRequest runAndCaptureFirstInferRequest(String sourcePath,
                        boolean cachedContentEnabled) throws Exception {
                InferenceService inferenceService = mock(InferenceService.class);
                if (cachedContentEnabled) {
                        when(inferenceService.createCachedContent(any(), any()))
                                        .thenReturn("cachedContents/prompt-flow");
                }

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
                                new CompilationTrackingStore());

                ReinsConfig config = new ReinsConfig();
                config.getContext().setCachedContent(cachedContentEnabled);

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

                service.runCycle(request, config);

                return org.mockito.Mockito.mockingDetails(inferenceService)
                                .getInvocations()
                                .stream()
                                .filter(i -> i.getMethod().getName().equals("infer"))
                                .map(i -> (MarkdownInferenceRequest) i.getArgument(0))
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
