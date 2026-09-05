package br.com.dizeno.reins.reasoning.inference.llm.logging;

import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.ModelSettings;
import br.com.dizeno.reins.run.config.settings.TargetSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModelRequestResponseLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDefaultModelSettingsIsFalse() {
        ModelSettings settings = new ModelSettings();
        assertFalse(settings.isRequestResponseLog());
    }

    @Test
    public void testExtractFileNameBeingProcessed() {
        assertEquals("User.md", ModelRequestResponseLogger.extractFileNameBeingProcessed("main:domain/User.md"));
        assertEquals("User.md", ModelRequestResponseLogger.extractFileNameBeingProcessed("src/main/domain/User.md"));
        assertEquals("User.md", ModelRequestResponseLogger.extractFileNameBeingProcessed("User.md"));
        assertEquals("Order.java", ModelRequestResponseLogger.extractFileNameBeingProcessed("test:Order.java"));
        assertEquals("request.md", ModelRequestResponseLogger.extractFileNameBeingProcessed(null));
        assertEquals("request.md", ModelRequestResponseLogger.extractFileNameBeingProcessed("   "));
    }

    @Test
    public void testLogDisabledByDefault() {
        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        config.setTarget(target);

        LlmRequest request = new LlmRequest();
        request.setSourcePath("main:domain/User.md");
        request.setMarkdownContent("Test prompt");

        ModelRequestResponseLogger.log(request, "Test response", config, tempDir);

        Path modelLogDir = tempDir.resolve("model-log");
        assertFalse(Files.exists(modelLogDir));
    }

    @Test
    public void testLogFileCreatedWhenEnabled() throws IOException {
        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        config.setTarget(target);
        config.getModel().setRequestResponseLog(true);
        config.setProvider("gemini");
        config.getGemini().setModel("gemini-1.5-pro");

        LlmRequest request = new LlmRequest();
        request.setSourcePath("main:domain/User.md");
        request.setConversationHistory(List.of(
                new ConversationMessage(ConversationMessage.Role.USER, "Generate Java class for User")
        ));

        ModelRequestResponseLogger.log(request, "public class User {}", config, tempDir);

        Path modelLogDir = tempDir.resolve("model-log");
        assertTrue(Files.exists(modelLogDir));

        try (var stream = Files.list(modelLogDir)) {
            List<Path> logFiles = stream.toList();
            assertEquals(1, logFiles.size());
            Path logFile = logFiles.get(0);
            assertTrue(logFile.getFileName().toString().endsWith("-User.md.log"));

            String content = Files.readString(logFile);
            assertTrue(content.contains("gemini-1.5-pro"));
            assertTrue(content.contains("gemini"));
            assertTrue(content.contains("-- REQUEST --"));
            assertTrue(content.contains("[user]\nGenerate Java class for User"));
            assertTrue(content.contains("-- RESPONSE --"));
            assertTrue(content.contains("public class User {}"));
        }
    }
}
