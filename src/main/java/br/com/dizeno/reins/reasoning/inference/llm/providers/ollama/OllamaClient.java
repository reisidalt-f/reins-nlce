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

package br.com.dizeno.reins.reasoning.inference.llm.providers.ollama;

import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.util.LogSanitizer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

 
/**
 * OllamaClient is part of the general application functions in the reins architecture.
 * Acts as a component managing ollama client.
 */
public class OllamaClient {
    private static final MediaType JSON = MediaType.get("application/json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OkHttpClient baseClient;
    private final OllamaRetryPolicy retryPolicy;
    private Log log;

    /**
     * Constructs a new instance of {@link OllamaClient}.
     */
    public OllamaClient() {
        this(new OkHttpClient(), new OllamaRetryPolicy());
    }

    /**
     * Constructs a new instance of {@link OllamaClient}.
     *
     * @param baseClient the base client
     */
    public OllamaClient(OkHttpClient baseClient) {
        this(baseClient, new OllamaRetryPolicy());
    }

    /**
     * Constructs a new instance of {@link OllamaClient}.
     *
     * @param baseClient the base client
     * @param retryPolicy the retry policy
     */
    public OllamaClient(OkHttpClient baseClient, OllamaRetryPolicy retryPolicy) {
        this.baseClient = baseClient;
        this.retryPolicy = retryPolicy;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        this.log = log;
    }

     
    /**
     * Compile Chat.
     *
     * @param params the parameters mapping
     * @param conversationHistory the conversation history
     * @return the string result
     */
    public String compileChat(OllamaRequestParams params, List<ConversationMessage> conversationHistory) throws Exception {
        String endpoint = params.getEndpoint().replaceAll("/$", ""); 
        String url = endpoint + "/api/chat";

        Map<String, Object> request = new HashMap<>();
        request.put("model", params.getModel());
        request.put("stream", false);

        SystemAndMessages normalized = splitSystemAndMessages(conversationHistory);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.addAll(normalized.systemMessages());
        messages.addAll(normalized.messages());
        request.put("messages", messages);

        Map<String, Object> options = buildOptions(params);
        if (!options.isEmpty()) {
            request.put("options", options);
        }

        return executeWithRetry(url, request, params, true);
    }

     
    /**
     * Compile.
     *
     * @param params the parameters mapping
     * @param prompt the prompt text
     * @return the string result
     */
    public String compile(OllamaRequestParams params, String prompt) throws Exception {
        String endpoint = params.getEndpoint().replaceAll("/$", ""); 
        String url = endpoint + "/api/compile";

        Map<String, Object> request = new HashMap<>();
        request.put("model", params.getModel());
        request.put("prompt", prompt);
        request.put("stream", false);

        Map<String, Object> options = buildOptions(params);
        if (!options.isEmpty()) {
            request.put("options", options);
        }

        return executeWithRetry(url, request, params, false);
    }

    private String executeWithRetry(String url,
                                    Map<String, Object> requestBody,
                                    OllamaRequestParams params,
                                    boolean isChat) throws Exception {
        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(params.getTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(params.getTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(params.getTimeoutSeconds()))
                .callTimeout(Duration.ofSeconds(params.getTimeoutSeconds()))
                .build();

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (params.getApiKey() != null && !params.getApiKey().isBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer " + params.getApiKey());
        }
        RequestBody body = RequestBody.create(MAPPER.writeValueAsBytes(requestBody), JSON);
        Request request = requestBuilder.post(body).build();

        Consumer<OllamaRetryAttemptEvent> listener = null;
        if (log != null) {
            int retryAttempts = params.getRetryAttempts();
            listener = event -> {
                if ("empty-or-blank".equals(event.responseClass())) {
                    String action = event.retryScheduled() ? "retrying" : "terminal";
                    log.info("[ollama-empty-response-retry] attempt=" + event.attempt() + "/" + retryAttempts
                            + " class=" + event.responseClass()
                            + " action=" + action
                            + " delayMs=" + event.delayMs()
                            + " reason=" + LogSanitizer.sanitizeForLog(event.cause().getMessage()));
                    return;
                }
                if (event.retryScheduled()) {
                    log.warn("Ollama request failed (attempt " + event.attempt() + "/" + retryAttempts + "): "
                            + LogSanitizer.sanitizeForLog(event.cause().getMessage()) + " - retrying...");
                }
            };
        }

        return retryPolicy.execute(
                () -> executeRequest(client, request, isChat),
                params.getRetryAttempts(),
                0,
                listener);
    }

    private String executeRequest(OkHttpClient client, Request request, boolean isChat) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                String sanitizedErrorBody = LogSanitizer.sanitizeForLog(errorBody);
                if (log != null) {
                    log.debug("[ollama] HTTP " + response.code() + " response: " + sanitizedErrorBody);
                }
                throw new IOException("Ollama API returned HTTP " + response.code() + ": " + sanitizedErrorBody);
            }

            String payload = response.body() == null ? "" : response.body().string();
            if (payload.isBlank()) {
                throw new OllamaEmptyResponseException("Empty response from Ollama API");
            }

            JsonNode root;
            int extraBodies = 0;
            try (JsonParser parser = MAPPER.createParser(payload)) {
                root = MAPPER.readTree(parser);
                JsonToken next;
                while ((next = parser.nextToken()) != null) {
                    if (next == JsonToken.START_OBJECT || next == JsonToken.START_ARRAY) {
                        extraBodies++;
                        parser.skipChildren();
                    }
                }
            }

            if (extraBodies > 0 && log != null) {
                log.info("Ollama returned " + extraBodies + " extra response "
                        + (extraBodies == 1 ? "body" : "bodies") + "; using first only.");
            }

            if (root.has("done") && !root.get("done").asBoolean()) {
                throw new IOException("Ollama returned done=false (incomplete response)");
            }

            String text;
            if (isChat) {
                JsonNode textNode = root.path("message").path("content");
                text = textNode.isMissingNode() ? "" : textNode.asText();
            } else {
                JsonNode textNode = root.path("response");
                text = textNode.isMissingNode() ? "" : textNode.asText();
            }

            if (text == null || text.isBlank()) {
                if (extraBodies > 0) {
                    throw new IOException("Ollama API returned empty first body in multi-body response");
                }
                throw new OllamaEmptyResponseException("Ollama API returned empty response content");
            }

            if (log != null) {
                log.debug("[ollama] Compiled response");
            }
            return text;
        }
    }

    private Map<String, Object> buildOptions(OllamaRequestParams params) {
        if (params.getOptions() == null || params.getOptions().isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>(params.getOptions());
        mapOption(normalized, "topP", "top_p");
        mapOption(normalized, "topK", "top_k");
        mapOption(normalized, "presencePenalty", "presence_penalty");
        mapOption(normalized, "frequencyPenalty", "frequency_penalty");
        mapOption(normalized, "temperature", "temperature");
        return normalized;
    }

    private void mapOption(Map<String, Object> options, String sourceKey, String targetKey) {
        if (!options.containsKey(targetKey) && options.containsKey(sourceKey)) {
            options.put(targetKey, options.get(sourceKey));
        }
    }

    private SystemAndMessages splitSystemAndMessages(List<ConversationMessage> conversationHistory) {
        List<Map<String, String>> systemMessages = new ArrayList<>();
        List<Map<String, String>> messages = new ArrayList<>();
        if (conversationHistory == null) {
            return new SystemAndMessages(systemMessages, messages);
        }

        for (ConversationMessage msg : conversationHistory) {
            Map<String, String> mapped = toOllamaMessage(msg);
            if (msg.getRole() == ConversationMessage.Role.SYSTEM) {
                systemMessages.add(mapped);
            } else {
                messages.add(mapped);
            }
        }
        return new SystemAndMessages(systemMessages, messages);
    }

    private Map<String, String> toOllamaMessage(ConversationMessage msg) {
        Map<String, String> mapped = new HashMap<>();
        String role = msg.getRoleString();
        if ("model".equals(role)) {
            role = "assistant";
        }
        mapped.put("role", role);
        mapped.put("content", buildMessageContent(msg));
        return mapped;
    }

    private String buildMessageContent(ConversationMessage msg) {
        StringBuilder content = new StringBuilder();
        if (msg.getText() != null && !msg.getText().isBlank()) {
            content.append(msg.getText());
        }

        if (msg.getAttachments() != null) {
            for (AttachedFilePayload attachment : msg.getAttachments()) {
                String attachmentText = attachment.getContent() == null ? "" : attachment.getContent();
                if (attachmentText.isBlank()) {
                    continue;
                }
                if (content.length() > 0) {
                    content.append("\n\n");
                }
                if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                    content.append("Content of ")
                            .append(attachment.getBase())
                            .append(":")
                            .append(attachment.getRelativePath())
                            .append(":\n");
                }
                content.append(attachmentText);
            }
        }
        return content.toString();
    }

    /**
     * SystemAndMessages is part of the general application functions in the reins architecture.
     * Acts as a component managing system and messages.
     */
    private record SystemAndMessages(List<Map<String, String>> systemMessages,
                                     List<Map<String, String>> messages) {
    }
}
