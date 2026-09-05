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

package br.com.dizeno.reins.reasoning.inference.llm.logging;

import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.run.config.ReinsConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ModelRequestResponseLogger logs the effective request and response for language model invocations.
 */
public class ModelRequestResponseLogger {
    private static final String MODEL_LOG_DIR = "model-log";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Logs the LLM request and response if model.requestResponseLog is enabled.
     *
     * @param request the LLM request
     * @param responseContent the raw response text from the LLM
     * @param config the Reins configuration
     */
    public static void log(LlmRequest request, String responseContent, ReinsConfig config) {
        log(request, responseContent, config, null);
    }

    /**
     * Logs the LLM request and response if model.requestResponseLog is enabled.
     *
     * @param request the LLM request
     * @param responseContent the raw response text from the LLM
     * @param config the Reins configuration
     * @param projectRoot the project root directory
     */
    public static void log(LlmRequest request, String responseContent, ReinsConfig config, Path projectRoot) {
        if (config == null || config.getModel() == null || !config.getModel().isRequestResponseLog()) {
            return;
        }

        try {
            Path root = projectRoot != null ? projectRoot.toAbsolutePath().normalize() : Path.of(".").toAbsolutePath().normalize();
            Path logDir = root.resolve(MODEL_LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            String sourcePath = request != null ? request.getSourcePath() : null;
            String fileNameBeingProcessed = extractFileNameBeingProcessed(sourcePath);

            String timestamp = TIMESTAMP_FORMATTER.format(LocalDateTime.now(ZoneId.systemDefault()));
            String fileName = timestamp + "-" + fileNameBeingProcessed + ".log";
            Path logFile = logDir.resolve(fileName);

            int counter = 1;
            while (Files.exists(logFile)) {
                fileName = timestamp + "-" + counter + "-" + fileNameBeingProcessed + ".log";
                logFile = logDir.resolve(fileName);
                counter++;
            }

            String model = config.resolveModel();
            if (model == null || model.isBlank()) {
                model = "unknown";
            }
            String provider = config.getProvider();
            if (provider == null || provider.isBlank()) {
                provider = "unknown";
            }

            String requestBody = formatRequestBody(request);
            String responseBody = responseContent != null ? responseContent : "";

            StringBuilder sb = new StringBuilder();
            sb.append(model).append("\n");
            sb.append(provider).append("\n\n");
            sb.append("-- REQUEST --\n");
            sb.append(requestBody);
            if (!requestBody.endsWith("\n")) {
                sb.append("\n");
            }
            sb.append("\n-- RESPONSE --\n");
            sb.append(responseBody);
            if (!responseBody.endsWith("\n")) {
                sb.append("\n");
            }

            Files.writeString(logFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // Logging failure should not disrupt inference pipeline execution
        }
    }

    /**
     * Extracts the filename of the source file being processed.
     *
     * @param sourcePath the qualified or relative source path
     * @return the simple filename
     */
    public static String extractFileNameBeingProcessed(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return "request.md";
        }
        String clean = sourcePath.trim().replace('\\', '/');
        int lastColon = clean.lastIndexOf(':');
        if (lastColon >= 0) {
            clean = clean.substring(lastColon + 1);
        }
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        return clean.isEmpty() ? "request.md" : clean;
    }

    private static Path resolveProjectRoot(ReinsConfig config) {
        return Path.of(".").toAbsolutePath().normalize();
    }

    private static String formatRequestBody(LlmRequest request) {
        if (request == null) {
            return "";
        }
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            return formatConversationHistory(request.getConversationHistory());
        }
        if (request.getMarkdownContent() != null) {
            return request.getMarkdownContent();
        }
        return "";
    }

    private static String formatConversationHistory(List<ConversationMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            ConversationMessage msg = history.get(i);
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append("[").append(msg.getRoleString()).append("]\n");
            if (msg.getText() != null) {
                sb.append(msg.getText());
            }
            if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
                for (AttachedFilePayload attachment : msg.getAttachments()) {
                    sb.append("\n--- Attachment: ");
                    if (attachment.getQualifiedPath() != null) {
                        sb.append(attachment.getQualifiedPath());
                    } else if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                        sb.append(attachment.getBase()).append(":").append(attachment.getRelativePath());
                    } else {
                        sb.append(attachment.getRelativePath());
                    }
                    sb.append(" ---\n");
                    if (attachment.getContent() != null) {
                        sb.append(attachment.getContent());
                    }
                }
            }
        }
        return sb.toString();
    }
}
