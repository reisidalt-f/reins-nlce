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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.logging.ReasoningLogAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

 
public class DefaultReasoningServiceLoggingTest {
    private DefaultReasoningService reasoningService;
    private InferenceService inferenceService;
    private ResponseDirectiveParser directiveParser;
    private ReasoningPromptBuilder promptBuilder;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService;
    private ToolResultFormatter mcpResultFormatter;
    private ReasoningLogService logService;

    @TempDir
    Path tempDir;

    private BasePathMappingSet createBaseMappings() {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir);
        mappings.setTestRoot(tempDir);
        mappings.setTargetRoot(tempDir);
        return mappings;
    }

    @BeforeEach
    void setUp() {
        inferenceService = mock(InferenceService.class);
        directiveParser = mock(ResponseDirectiveParser.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        mcpService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        mcpResultFormatter = mock(ToolResultFormatter.class);
        logService = new FileReasoningLogService();

        reasoningService = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                mcpService,
                mcpResultFormatter,
                logService
        );
    }

    private ReinsConfig loggingEnabledConfig() {
        ReinsConfig config = new ReinsConfig();
        config.getReasoning().setEnableReasoningLog(true);
        return config;
    }

    @Test
    void testLoggingDisabledByDefaultCreatesNoLogArtifacts() throws Exception {
        MarkdownInferenceResponse mockResponse = mock(MarkdownInferenceResponse.class);
        when(mockResponse.getRawResponseText()).thenReturn("Processed successfully");

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("Process this file");
        when(inferenceService.infer(any(), any())).thenReturn(mockResponse);

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        reasoningService.runCycle(request, new ReinsConfig());

        assertFalse(Files.exists(tempDir.resolve("reasoning-logs")));
    }

    @Test
    void testLoggingCapturesOutboundAndInboundMessages() throws Exception {
        
        String outboundMessage = "Process this file";
        String inboundResponse = "Processed successfully";

        MarkdownInferenceResponse mockResponse = mock(MarkdownInferenceResponse.class);
        when(mockResponse.getRawResponseText()).thenReturn(inboundResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn(outboundMessage);
        when(inferenceService.infer(any(), any())).thenReturn(mockResponse);

        
        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(directiveParser.parse(any())).thenReturn(parseResult);

        
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = loggingEnabledConfig();

        
        reasoningService.runCycle(request, config);

        
        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        assertTrue(java.nio.file.Files.exists(inferenceLogsDir),
                "reasoning-logs directory should be created");

        
        var logFiles = java.nio.file.Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertTrue(logFiles.size() > 0, "At least one log file should exist");

        Path logFile = logFiles.get(0);
        ReasoningLogAssertions.assertFirstEntryIsOutbound(logFile);
        var entries = ReasoningLogAssertions.getLogEntries(logFile);
        assertTrue(entries.stream().anyMatch(e -> "OUTBOUND".equals(e.direction)));
        assertTrue(entries.stream().anyMatch(e -> "INBOUND".equals(e.direction)));
    }

    @Test
    void testLoggingIncludesInitialContextMessage() throws Exception {
        
        String initialContext = "This is the initial context message";
        String firstResponse = "Responding to context";

        MarkdownInferenceResponse mockResponse = mock(MarkdownInferenceResponse.class);
        when(mockResponse.getRawResponseText()).thenReturn(firstResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn(initialContext);
        when(inferenceService.infer(any(), any())).thenReturn(mockResponse);

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage(initialContext);
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = loggingEnabledConfig();

        
        reasoningService.runCycle(request, config);

        
        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = java.nio.file.Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        Path logFile = logFiles.get(0);
        var entries = ReasoningLogAssertions.getLogEntries(logFile);
        assertTrue(entries.stream().anyMatch(e -> e.body.toLowerCase().contains("initial")));
    }

    @Test
    void testLoggingHandlesLogServiceFailureGracefully() throws Exception {
        
        ReasoningLogService failingLogService = new ReasoningLogService() {
            @Override
            public ReasoningCycleLog initializeCycleLog(String cycleId, Path projectRoot) {
                throw new RuntimeException("Log initialization failed");
            }

            @Override
            public ReasoningLogWriteResult writeEntry(ReasoningCycleLog cycleLog, ReasoningLogEntry entry) {
                return null;
            }

            @Override
            public void closeCycleLog(ReasoningCycleLog cycleLog) {
            }
        };

        DefaultReasoningService serviceWithFailingLog = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                promptBuilder,
                mcpService,
                mcpResultFormatter,
                failingLogService
        );

        MarkdownInferenceResponse mockResponse = mock(MarkdownInferenceResponse.class);
        when(mockResponse.getRawResponseText()).thenReturn("Response");

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("Message");
        when(inferenceService.infer(any(), any())).thenReturn(mockResponse);

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = loggingEnabledConfig();

        
        
        assertDoesNotThrow(() -> serviceWithFailingLog.runCycle(request, config));
    }

    @Test
    void testLoggingSequenceNumbersAreMonotonic() throws Exception {
        
        String initialContext = "Analyze this";
        String firstResponse = "\n<!-- reasoning: continue -->\nNeed more info";
        String secondResponse = "\n<!-- reasoning: finish -->\nAnalysis complete";

        MarkdownInferenceResponse firstMockResponse = mock(MarkdownInferenceResponse.class);
        when(firstMockResponse.getRawResponseText()).thenReturn(firstResponse);

        MarkdownInferenceResponse secondMockResponse = mock(MarkdownInferenceResponse.class);
        when(secondMockResponse.getRawResponseText()).thenReturn(secondResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean()))
                .thenReturn(initialContext)
                .thenReturn("Follow-up");

        when(inferenceService.infer(any(), any()))
                .thenReturn(firstMockResponse)
                .thenReturn(secondMockResponse);

        ResponseDirective continueDirective = mock(ResponseDirective.class);
        when(continueDirective.isValid()).thenReturn(true);
        when(continueDirective.getIntent()).thenReturn(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult continueParseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(continueParseResult.getDirective()).thenReturn(continueDirective);
        when(continueParseResult.getBody()).thenReturn("Need more info");

        ResponseDirectiveParser.ParseResult finishParseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(finishParseResult.getDirective()).thenReturn(finishDirective);
        when(finishParseResult.getBody()).thenReturn("Complete");

        when(directiveParser.parse(firstResponse)).thenReturn(continueParseResult);
        when(directiveParser.parse(secondResponse)).thenReturn(finishParseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage(initialContext);
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = loggingEnabledConfig();

        
        reasoningService.runCycle(request, config);

        
        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = java.nio.file.Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        Path logFile = logFiles.get(0);
        var entries = ReasoningLogAssertions.getLogEntries(logFile);
        var nonLifecycle = entries.stream().filter(e -> e.sequence > 0).toList();
        for (int i = 0; i < nonLifecycle.size(); i++) {
            assertEquals(i + 1, nonLifecycle.get(i).sequence);
        }
    }

    @Test
    void testLoggingShowsSourceAttachmentAsBasePathWithoutContent() throws Exception {
        Path sourceFile = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(sourceFile.getParent());
        String markdownContent = "# Entities\n\nSecret domain details";
        Files.writeString(sourceFile, markdownContent, StandardCharsets.UTF_8);

        MarkdownInferenceResponse mockResponse = mock(MarkdownInferenceResponse.class);
        when(mockResponse.getRawResponseText()).thenReturn("Done");
        when(inferenceService.infer(any(), any())).thenReturn(mockResponse);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("ignored");

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(parseResult.getBody()).thenReturn("Done");
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        request.setSourceScope("main");
        request.setSourcePath("src/main/nl/domain/entities.md");

        ReinsConfig config = loggingEnabledConfig();
        config.getReasoning().setLogSystemContext(true);

        reasoningService.runCycle(request, config);

        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        String fileContent = Files.readString(logFiles.get(0), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("attachments:"));
        assertTrue(fileContent.contains("- main:domain/entities.md"));
        assertFalse(fileContent.contains("Secret domain details"),
                "Attachment content must not be logged");
    }

    @Test
    void testLoggingShowsReadFileAttachmentReferenceWithoutContent() throws Exception {
        MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
        when(firstResponse.getRawResponseText()).thenReturn("first");
        when(secondResponse.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt-1", "prompt-2");
        when(mcpResultFormatter.format(any())).thenReturn("mcp-read-summary");

        ResponseDirective waitDirective = mock(ResponseDirective.class);
        when(waitDirective.isValid()).thenReturn(true);
        when(waitDirective.getIntent()).thenReturn(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        when(waitDirective.getContentType()).thenReturn(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);
        when(finishDirective.getContentType()).thenReturn(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult firstParse = mock(ResponseDirectiveParser.ParseResult.class);
        when(firstParse.getDirective()).thenReturn(waitDirective);
        when(firstParse.getBody()).thenReturn("operation: read_file\nbase: main\npath: domain/entities.md\n");

        ResponseDirectiveParser.ParseResult secondParse = mock(ResponseDirectiveParser.ParseResult.class);
        when(secondParse.getDirective()).thenReturn(finishDirective);
        when(secondParse.getBody()).thenReturn("done");

        when(directiveParser.parse("first")).thenReturn(firstParse);
        when(directiveParser.parse("second")).thenReturn(secondParse);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult readResult = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                "main:/domain/entities.md",
                "read"
        );
        readResult.setResolvedBase("main");
        readResult.setContent("Top secret content");
        when(mcpService.execute(any(), any(), any(), any())).thenReturn(readResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        reasoningService.runCycle(request, loggingEnabledConfig());

        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        String fileContent = Files.readString(logFiles.get(0), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("attachments:"));
        assertTrue(fileContent.contains("- main:domain/entities.md"));
        assertFalse(fileContent.contains("Top secret content"), "Attachment content must not be logged");
    }

    @Test
    void testLoggingIncludesCachedContentLifecycleDiagnostics() throws Exception {
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);
        when(inferenceService.createCachedContent(any(), any())).thenReturn("cachedContents/cycle-1");
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("ignored");

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);
        when(finishDirective.getContentType()).thenReturn(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(parseResult.getBody()).thenReturn("ok");
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = loggingEnabledConfig();
        config.getReasoning().setLogSystemContext(true);
        reasoningService.runCycle(request, config);

        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        String fileContent = Files.readString(logFiles.get(0), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("lifecycle: cache-create-attempt"));
        assertTrue(fileContent.contains("lifecycle: cache-created: cachedContents/cycle-1"));
        assertTrue(fileContent.contains("lifecycle: cache-used: cachedContents/cycle-1 turn=1"));
        assertTrue(fileContent.contains("lifecycle: cache-delete-attempt: cachedContents/cycle-1"));
        assertTrue(fileContent.contains("lifecycle: cache-deleted: cachedContents/cycle-1"));
        assertTrue(fileContent.contains("lifecycle: cache-create-payload-first-user-preview:"));
        assertTrue(fileContent.contains("lifecycle: cache-create-payload-attachments:"));
        assertTrue(fileContent.contains("role: context-system-context"), "Cached path should log individual context steps");
    }

    @Test
    void testLoggingIncludesUncachedSystemPayloadWhenCacheDisabled() throws Exception {
        Path sourceFile = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# Entities\n", StandardCharsets.UTF_8);

        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("ignored");

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);
        when(finishDirective.getContentType()).thenReturn(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(parseResult.getBody()).thenReturn("ok");
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setSourceScope("main");
        request.setSourcePath("src/main/nl/domain/entities.md");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        ReinsConfig config = loggingEnabledConfig();
        config.getReasoning().setLogSystemContext(true);
        config.getContext().setCachedContent(false);

        reasoningService.runCycle(request, config);

        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        String fileContent = Files.readString(logFiles.get(0), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("role: context-system-context"), "Uncached path should log individual context steps");
        assertTrue(fileContent.contains("role: context-background-files"), "background-files context step should be logged");
        assertTrue(fileContent.contains("- main:domain/entities.md"), "Source attachment should appear in background-files context entry");
    }

    @Test
    void testSystemContextLoggingDisabledDoesNotEmitContextPayloads() throws Exception {
        Path sourceFile = tempDir.resolve("src/main/nl/domain/entities.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# Entities\n", StandardCharsets.UTF_8);

        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);
        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("ignored");

        ResponseDirective finishDirective = mock(ResponseDirective.class);
        when(finishDirective.isValid()).thenReturn(true);
        when(finishDirective.getIntent()).thenReturn(ResponseDirective.Intent.FINISH_SUCCESS);
        when(finishDirective.getContentType()).thenReturn(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult parseResult = mock(ResponseDirectiveParser.ParseResult.class);
        when(parseResult.getDirective()).thenReturn(finishDirective);
        when(parseResult.getBody()).thenReturn("ok");
        when(directiveParser.parse(any())).thenReturn(parseResult);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Initial context");
        request.setSourceScope("main");
        request.setSourcePath("src/main/nl/domain/entities.md");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);

        ReinsConfig config = loggingEnabledConfig();
        config.getReasoning().setLogSystemContext(false);
        config.getContext().setCachedContent(false);

        reasoningService.runCycle(request, config);

        Path inferenceLogsDir = tempDir.resolve("reasoning-logs");
        var logFiles = Files.list(inferenceLogsDir)
                .filter(p -> p.toString().endsWith(".log"))
                .toList();

        assertFalse(logFiles.isEmpty(), "Log file should exist");
        String fileContent = Files.readString(logFiles.get(0), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("lifecycle: uncached-prepend-context: prepend-messages: count="));
        assertFalse(fileContent.contains("role: system-context"));
        assertFalse(fileContent.contains("role: context-system-context"));
        assertFalse(fileContent.contains("=== MANDATORY RESPONSE FORMAT ==="));
    }
}
