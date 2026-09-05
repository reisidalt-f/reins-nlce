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

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.run.config.settings.ContextSettings;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GeminiClient is part of the general application functions in the reins architecture.
 * Acts as a component managing gemini client.
 */
public class GeminiClient {
    private static final MediaType JSON = MediaType.get("application/json");

    private final ObjectMapper mapper;
    private final RetryPolicy retryPolicy;
     
    private final OkHttpClient baseClient;
    private Log log;

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * Constructs a new instance of {@link GeminiClient}.
     */
    public GeminiClient() {
        this(new ObjectMapper(), new RetryPolicy(), new OkHttpClient(), null);
    }

    /**
     * Constructs a new instance of {@link GeminiClient}.
     *
     * @param mapper the mapper
     * @param retryPolicy the retry policy
     * @param baseClient the base client
     */
    public GeminiClient(ObjectMapper mapper, RetryPolicy retryPolicy, OkHttpClient baseClient) {
        this(mapper, retryPolicy, baseClient, null);
    }

    /**
     * Constructs a new instance of {@link GeminiClient}.
     *
     * @param mapper the mapper
     * @param retryPolicy the retry policy
     * @param baseClient the base client
     * @param log the logger instance
     */
    public GeminiClient(ObjectMapper mapper, RetryPolicy retryPolicy, OkHttpClient baseClient, Log log) {
        this.mapper = mapper;
        this.retryPolicy = retryPolicy;
        this.baseClient = baseClient;
        this.log = log;
    }

    /**
     * Compile.
     *
     * @param params the parameters mapping
     * @param prompt the prompt text
     * @return the string result
     */
    public String compile(GeminiRequestParams params, String prompt) throws Exception {
        List<ConversationMessage> contents = new ArrayList<>();
        contents.add(new ConversationMessage(ConversationMessage.Role.USER, prompt));
        return compileWithHistory(params, contents);
    }

    /**
     * Compile With History.
     *
     * @param params the parameters mapping
     * @param conversationHistory the conversation history
     * @return the string result
     */
    public String compileWithHistory(GeminiRequestParams params,
                                      List<ConversationMessage> conversationHistory) throws Exception {
        return compileWithHistory(params, conversationHistory, null, false);
    }

    /**
     * Compile With History.
     *
     * @param params the parameters mapping
     * @param conversationHistory the conversation history
     * @param cachedContentId the cached content id
     * @param useCachedContent the use cached content
     * @return the string result
     */
    public String compileWithHistory(GeminiRequestParams params,
                                      List<ConversationMessage> conversationHistory,
                                      String cachedContentId,
                                      boolean useCachedContent) throws Exception {
        OkHttpClient client = baseClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(params.timeoutSeconds()))
            .readTimeout(Duration.ofSeconds(params.timeoutSeconds()))
            .writeTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .callTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .build();

        String url = params.endpoint() + "/v1beta/models/" + params.model() + ":compileContent?key=" + params.apiKey();
        SystemAndContents normalized = splitSystemAndContents(conversationHistory, params == null ? null : params.contextSettings());
        Map<String, Object> bodyPayload = new HashMap<>();
        bodyPayload.put("contents", normalized.contents());
        bodyPayload.put("generation", buildGeneration(params));
        if (useCachedContent && cachedContentId != null && !cachedContentId.isBlank()) {
            bodyPayload.put("cachedContent", normalizeCachedContentName(cachedContentId));
        } else if (!normalized.systemParts().isEmpty()) {
            bodyPayload.put("systemInstruction", Map.of("parts", normalized.systemParts()));
        }

        RequestBody body = RequestBody.create(mapper.writeValueAsBytes(bodyPayload), JSON);
        Request request = new Request.Builder().url(url).post(body).build();

        Consumer<RetryAttemptEvent> listener = null;
        if (log != null) {
            int retryAttempts = params.retryAttempts();
            listener = event -> {
                if ("empty-or-blank".equals(event.responseClass())) {
                    String action = event.retryScheduled() ? "retrying" : "terminal";
                    log.info("[empty-response-retry] attempt=" + event.attempt() + "/" + retryAttempts
                            + " class=" + event.responseClass()
                            + " action=" + action
                            + " delayMs=" + event.delayMs()
                            + " reason=" + event.cause().getMessage());
                    return;
                }
                if (event.retryScheduled()) {
                    log.warn("Gemini request failed (attempt " + event.attempt() + "/" + retryAttempts + "): "
                            + event.cause().getMessage() + " — retrying...");
                }
            };
        }
        return retryPolicy.execute(
                () -> executeRequest(client, request, params),
                params.retryAttempts(),
                params.emptyResponseRetryDelayMs(),
                listener);
    }

    /**
     * Creates a new resource cached content.
     *
     * @param params the parameters mapping
     * @param systemMessages the system messages
     * @return the string result
     */
    public String createCachedContent(GeminiRequestParams params,
                                      List<ConversationMessage> systemMessages) throws Exception {
        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .readTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .callTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .build();

        String url = params.endpoint() + "/v1beta/cachedContents?key=" + params.apiKey();
        SystemAndContents normalized = splitSystemAndContents(systemMessages, params == null ? null : params.contextSettings());

        Map<String, Object> bodyPayload = new HashMap<>();
        bodyPayload.put("model", "models/" + params.model());
        if (!normalized.systemParts().isEmpty()) {
            bodyPayload.put("systemInstruction", Map.of("parts", normalized.systemParts()));
        }

        RequestBody body = RequestBody.create(mapper.writeValueAsBytes(bodyPayload), JSON);
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini cached-content create failed: HTTP " + response.code());
            }
            String payload = response.body() == null ? "" : response.body().string();
            JsonNode root = mapper.readTree(payload);
            String id = root.path("name").asText();
            if (id == null || id.isBlank()) {
                throw new IOException("Gemini cached-content create returned empty cache id");
            }
            return id;
        }
    }

    /**
     * Deletes the target cached content.
     *
     * @param params the parameters mapping
     * @param cachedContentId the cached content id
     */
    public void deleteCachedContent(GeminiRequestParams params,
                                    String cachedContentId) throws Exception {
        if (cachedContentId == null || cachedContentId.isBlank()) {
            return;
        }

        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .readTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .callTimeout(Duration.ofSeconds(params.timeoutSeconds()))
                .build();

        String url = params.endpoint() + "/v1beta/" + normalizeCachedContentName(cachedContentId) + "?key=" + params.apiKey();
        Request request = new Request.Builder().url(url).delete().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini cached-content delete failed: HTTP " + response.code());
            }
        }
    }

    private Map<String, Object> buildGeneration(GeminiRequestParams params) {
        var gc = params.generation();
        if (gc == null) {
            return Map.of();
        }
        Map<String, Object> config = new LinkedHashMap<>();
        if (gc.getTemperature() != null) {
            config.put("temperature", gc.getTemperature().doubleValue());
        }
        if (gc.getTopP() != null) {
            config.put("topP", gc.getTopP().doubleValue());
        }
        if (gc.getTopK() != null) {
            config.put("topK", gc.getTopK().intValue());
        }
        if (gc.getPresencePenalty() != null) {
            config.put("presencePenalty", gc.getPresencePenalty().doubleValue());
        }
        if (gc.getFrequencyPenalty() != null) {
            config.put("frequencyPenalty", gc.getFrequencyPenalty().doubleValue());
        }
        return config;
    }

    private List<Map<String, Object>> buildContents(List<ConversationMessage> conversationHistory, ContextSettings contextSettings) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ConversationMessage msg : conversationHistory) {
            if (msg.getRole() == ConversationMessage.Role.SYSTEM) {
                continue;
            }
            List<Map<String, Object>> parts = new ArrayList<>();
            if (msg.getText() != null && !msg.getText().isBlank()) {
                parts.add(Map.of("text", msg.getText()));
            }

            if (msg.getAttachments() != null) {
                for (AttachedFilePayload attachment : msg.getAttachments()) {
                    String content = attachment.getContent() == null ? "" : attachment.getContent();
                    boolean isPlain = contextSettings != null
                            ? contextSettings.isPlainAttachment(attachment.getRelativePath())
                            : new ContextSettings().isPlainAttachment(attachment.getRelativePath());

                    if (isPlain) {
                        if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                            parts.add(Map.of("text", "Content of " + attachment.getBase() + ":" + attachment.getRelativePath() + ":\n" + content));
                        } else {
                            parts.add(Map.of("text", content));
                        }
                    } else {
                        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
                        if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                            parts.add(Map.of("text", "Content of " + attachment.getBase() + ":" + attachment.getRelativePath() + ":"));
                        }
                        Map<String, Object> inlineData = new LinkedHashMap<>();
                        inlineData.put("mimeType", "text/markdown");
                        inlineData.put("data", encoded);
                        parts.add(Map.of("inlineData", inlineData));
                    }
                }
            }

            if (parts.isEmpty()) {
                parts.add(Map.of("text", ""));
            }

            contents.add(Map.of(
                    "role", msg.getRoleString(),
                    "parts", parts
            ));
        }
        return contents;
    }

    private List<Map<String, Object>> buildSystemParts(List<ConversationMessage> conversationHistory, ContextSettings contextSettings) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (conversationHistory == null) {
            return parts;
        }
        for (ConversationMessage msg : conversationHistory) {
            if (msg.getRole() != ConversationMessage.Role.SYSTEM) {
                continue;
            }
            if (msg.getText() != null && !msg.getText().isBlank()) {
                parts.add(Map.of("text", msg.getText()));
            }
            if (msg.getAttachments() != null) {
                for (AttachedFilePayload attachment : msg.getAttachments()) {
                    String content = attachment.getContent() == null ? "" : attachment.getContent();
                    boolean isPlain = contextSettings != null
                            ? contextSettings.isPlainAttachment(attachment.getRelativePath())
                            : new ContextSettings().isPlainAttachment(attachment.getRelativePath());

                    if (isPlain) {
                        if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                            parts.add(Map.of("text", "Content of " + attachment.getBase() + ":" + attachment.getRelativePath() + ":\n" + content));
                        } else {
                            parts.add(Map.of("text", content));
                        }
                    } else {
                        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
                        if (attachment.getBase() != null && attachment.getRelativePath() != null) {
                            parts.add(Map.of("text", "Content of " + attachment.getBase() + ":" + attachment.getRelativePath() + ":"));
                        }
                        Map<String, Object> inlineData = new LinkedHashMap<>();
                        inlineData.put("mimeType", "text/markdown");
                        inlineData.put("data", encoded);
                        parts.add(Map.of("inlineData", inlineData));
                    }
                }
            }
        }
        return parts;
    }

    private SystemAndContents splitSystemAndContents(List<ConversationMessage> conversationHistory, ContextSettings contextSettings) {
        List<Map<String, Object>> systemParts = buildSystemParts(conversationHistory, contextSettings);
        List<Map<String, Object>> contents = buildContents(conversationHistory, contextSettings);
        return new SystemAndContents(systemParts, contents);
    }


    private String normalizeCachedContentName(String cachedContentId) {
        if (cachedContentId.startsWith("cachedContents/")) {
            return cachedContentId;
        }
        if (cachedContentId.startsWith("v1beta/cachedContents/")) {
            return cachedContentId.substring("v1beta/".length());
        }
        return "cachedContents/" + cachedContentId;
    }

    /**
     * SystemAndContents is part of the general application functions in the reins architecture.
     * Acts as a component managing system and contents.
     */
    private record SystemAndContents(List<Map<String, Object>> systemParts,
                                     List<Map<String, Object>> contents) {
    }

    private String executeRequest(OkHttpClient client, Request request, GeminiRequestParams params) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gemini API request failed: HTTP " + response.code());
            }
            String payload = response.body() == null ? "" : response.body().string();

            
            JsonNode root;
            int extraBodies = 0;
            try (JsonParser parser = mapper.createParser(payload)) {
                root = mapper.readTree(parser);
                JsonToken next;
                while ((next = parser.nextToken()) != null) {
                    if (next == JsonToken.START_OBJECT || next == JsonToken.START_ARRAY) {
                        extraBodies++;
                        parser.skipChildren();
                    }
                }
            }

            
            if (extraBodies > 0 && params.verbose() && log != null) {
                log.info("Gemini returned " + extraBodies + " extra response "
                        + (extraBodies == 1 ? "body" : "bodies") + "; using first only.");
            }

            
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String text = textNode.isMissingNode() ? "" : textNode.asText();
            if (text == null || text.isBlank()) {
                if (extraBodies > 0) {
                    throw new IOException("Gemini API returned empty first body in multi-body response");
                }
                throw new GeminiEmptyResponseException("Gemini API returned empty candidates (safety filter or empty prompt)");
            }
            return text;
        }
    }
}
