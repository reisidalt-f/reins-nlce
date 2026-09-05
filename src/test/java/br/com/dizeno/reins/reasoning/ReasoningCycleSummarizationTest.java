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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.run.config.ConfigValidationException;
import br.com.dizeno.reins.run.config.ConfigValidator;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.ReinsConfigLoader;
import br.com.dizeno.reins.run.config.settings.ReasoningSettings;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReasoningCycleSummarizationTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultSummarizeCycleTurnsIsZero() {
        ReasoningSettings settings = new ReasoningSettings();
        assertEquals(0, settings.getSummarizeCycleTurns());
    }

    @Test
    void configLoaderBindsSummarizeCycleTurnsAndAlias() {
        Properties props = new Properties();
        props.setProperty("reins.reasoning.summarizeCycleTurns", "3");
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, tempDir.toFile());
        assertEquals(3, config.getReasoning().getSummarizeCycleTurns());

        Properties aliasProps = new Properties();
        aliasProps.setProperty("reins.reasoning.summarizeTurnInterval", "5");
        ReinsConfig aliasConfig = ReinsConfigLoader.load(null, aliasProps, null, tempDir.toFile());
        assertEquals(5, aliasConfig.getReasoning().getSummarizeCycleTurns());
    }

    @Test
    void configValidatorRejectsNegativeSummarizeCycleTurns() throws Exception {
        Path mainSource = tempDir.resolve("src/main/nl");
        Files.createDirectories(mainSource);

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.setSourceBase("main", mainSource.toFile());
        config.setTarget(new br.com.dizeno.reins.run.config.settings.TargetSettings());
        config.getTarget().setTargetBase("main", "src/main/java");
        config.getReasoning().setSummarizeCycleTurns(-1);

        ConfigValidator validator = new ConfigValidator();
        assertThrows(ConfigValidationException.class, () -> validator.validate(config, tempDir.toFile()));
    }

    @Test
    void messageFormattingServiceAppendsSummarizationInstruction() {
        MessageFormattingService service = new MessageFormattingService();
        String result = service.withSummarizationInstruction("Turn message");

        assertTrue(result.contains("Turn message"));
        assertTrue(result.contains("Please summarize the conversation in your response along with the answer/result of the main requested action, keeping enough information of:"));
        assertTrue(result.contains("- the goal of the reasoning"));
        assertTrue(result.contains("- what is available that is relevant"));
        assertTrue(result.contains("- how to achieve the goal with what is available"));
        assertTrue(result.contains("- what has been done so far"));
        assertTrue(result.contains("- what needs to be done or to be gathered"));
        assertTrue(result.contains("- gathered information: a cumulative, detailed list of every piece of information gathered so far"));
    }

    @Test
    void reasoningCycleAppendsInstructionAndSummarizesHistoryOnCycleTurn() throws Exception {
        Path source = tempDir.resolve("src/main/nl/domain.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse turn1Response = mock(MarkdownInferenceResponse.class);
        when(turn1Response.getRawResponseText()).thenReturn(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\n\nTurn 1 Summary: Goal is domain compilation, done so far: analyzed source.\n");
        MarkdownInferenceResponse turn2Response = mock(MarkdownInferenceResponse.class);
        when(turn2Response.getRawResponseText()).thenReturn(
                "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nTurn 2 Done.\n");

        when(inferenceService.infer(any(), any())).thenReturn(turn1Response, turn2Response);

        ResponseDirectiveParser parser = new ResponseDirectiveParser();
        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(ToolingService.class),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().setCachedContent(false);
        config.getReasoning().setSummarizeCycleTurns(1);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile domain source");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        service.runCycle(request, config);

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        verify(inferenceService, times(2)).infer(captor.capture(), any());

        List<MarkdownInferenceRequest> requests = captor.getAllValues();
        assertEquals(2, requests.size());

        // First turn request should contain summarization instruction
        ConversationMessage firstTurnUserMsg = requests.get(0).getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .findFirst()
                .orElseThrow();
        assertTrue(firstTurnUserMsg.getText().contains("Please summarize the conversation in your response"));
        assertTrue(firstTurnUserMsg.getText().contains("- the goal of the reasoning"));

        // Second turn request history should have been compressed, containing Turn 1 summary as model response
        List<ConversationMessage> secondTurnHistory = requests.get(1).getConversationHistory();
        ConversationMessage modelSummaryMsg = secondTurnHistory.stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.MODEL)
                .findFirst()
                .orElseThrow();
        assertTrue(modelSummaryMsg.getText().contains("Turn 1 Summary: Goal is domain compilation"));
    }

    @Test
    void reasoningCycleProcessesMultiBlockConversationSummaryAndToolRequest() throws Exception {
        Path source = tempDir.resolve("src/main/nl/domain.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse turn1MultiBlockResponse = mock(MarkdownInferenceResponse.class);
        when(turn1MultiBlockResponse.getRawResponseText()).thenReturn(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: conversation-summary\n\nTurn 1 Summary: Goal is domain compilation.\n\n"
              + "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\noperation: read_file\nbase: main\npath: domain.md\n");
        MarkdownInferenceResponse turn2Response = mock(MarkdownInferenceResponse.class);
        when(turn2Response.getRawResponseText()).thenReturn(
                "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nTask complete.\n");

        when(inferenceService.infer(any(), any())).thenReturn(turn1MultiBlockResponse, turn2Response);

        ToolingService toolingService = mock(ToolingService.class);
        when(toolingService.execute(any(), any())).thenReturn(ToolExecutionResult.success(ToolExecutionRequest.Operation.READ_FILE, "domain.md", "file content of domain.md"));

        ResponseDirectiveParser parser = new ResponseDirectiveParser();
        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                toolingService,
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().setCachedContent(false);
        config.getReasoning().setSummarizeCycleTurns(1);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile domain source");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        ReasoningResult result = service.runCycle(request, config);

        assertNotNull(result);
        assertFalse(result.getUserFacingMessages().contains("Turn 1 Summary: Goal is domain compilation."));
        assertTrue(result.getUserFacingMessages().contains("Task complete."));

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        verify(inferenceService, times(2)).infer(captor.capture(), any());

        List<MarkdownInferenceRequest> requests = captor.getAllValues();
        assertEquals(2, requests.size());

        List<ConversationMessage> secondTurnHistory = requests.get(1).getConversationHistory();
        ConversationMessage modelSummaryMsg = secondTurnHistory.stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.MODEL)
                .findFirst()
                .orElseThrow();
        assertEquals("Turn 1 Summary: Goal is domain compilation.", modelSummaryMsg.getText());
    }

    @Test
    void whenSummarizeCycleTurnsIsZeroHistoryIsNotSummarized() throws Exception {
        Path source = tempDir.resolve("src/main/nl/domain.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse turn1Response = mock(MarkdownInferenceResponse.class);
        when(turn1Response.getRawResponseText()).thenReturn(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\n\nTurn 1 Message\n");
        MarkdownInferenceResponse turn2Response = mock(MarkdownInferenceResponse.class);
        when(turn2Response.getRawResponseText()).thenReturn(
                "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nTurn 2 Done.\n");

        when(inferenceService.infer(any(), any())).thenReturn(turn1Response, turn2Response);

        ResponseDirectiveParser parser = new ResponseDirectiveParser();
        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                new ReasoningPromptBuilder(),
                mock(ToolingService.class),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getContext().setCachedContent(false);
        assertEquals(0, config.getReasoning().getSummarizeCycleTurns());

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile domain source");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        service.runCycle(request, config);

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        verify(inferenceService, times(2)).infer(captor.capture(), any());

        List<MarkdownInferenceRequest> requests = captor.getAllValues();
        assertEquals(2, requests.size());

        // First turn user prompt should NOT contain summarization instruction
        ConversationMessage firstTurnUserMsg = requests.get(0).getConversationHistory().stream()
                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                .findFirst()
                .orElseThrow();
        assertFalse(firstTurnUserMsg.getText().contains("Please summarize the conversation in your response"));
    }
}
