package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.util.LogSanitizer;
import com.fasterxml.jackson.core.JsonParser;
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
 * OpenAiClient is part of the general application functions in the reins architecture.
 * Acts as a component managing openai client.
 */
public class OpenAiClient {
    private static final MediaType JSON = MediaType.get("application/json");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OkHttpClient baseClient;
    private final OpenAiRetryPolicy retryPolicy;
    private Log log;

    /**
     * Constructs a new instance of {@link OpenAiClient}.
     */
    public OpenAiClient() {
        this(new OkHttpClient(), new OpenAiRetryPolicy());
    }

    /**
     * Constructs a new instance of {@link OpenAiClient}.
     *
     * @param baseClient the base client
     */
    public OpenAiClient(OkHttpClient baseClient) {
        this(baseClient, new OpenAiRetryPolicy());
    }

    /**
     * Constructs a new instance of {@link OpenAiClient}.
     *
     * @param baseClient the base client
     * @param retryPolicy the retry policy
     */
    public OpenAiClient(OkHttpClient baseClient, OpenAiRetryPolicy retryPolicy) {
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
    public String compileChat(OpenAiRequestParams params, List<ConversationMessage> conversationHistory) throws Exception {
        String endpoint = params.getEndpoint().replaceAll("/$", "");
        String url;
        if (endpoint.endsWith("/v1")) {
            url = endpoint + "/chat/completions";
        } else {
            url = endpoint + "/v1/chat/completions";
        }

        Map<String, Object> request = new HashMap<>();
        request.put("model", params.getModel());

        List<Map<String, String>> messages = new ArrayList<>();
        if (conversationHistory != null) {
            for (ConversationMessage msg : conversationHistory) {
                Map<String, String> mapped = new HashMap<>();
                String role = msg.getRoleString();
                if ("model".equals(role)) {
                    role = "assistant";
                }
                mapped.put("role", role);
                mapped.put("content", buildMessageContent(msg));
                messages.add(mapped);
            }
        }
        request.put("messages", messages);

        Map<String, Object> options = buildOptions(params);
        if (!options.isEmpty()) {
            request.putAll(options);
        }

        return executeWithRetry(url, request, params);
    }

    /**
     * Compile.
     *
     * @param params the parameters mapping
     * @param prompt the prompt text
     * @return the string result
     */
    public String compile(OpenAiRequestParams params, String prompt) throws Exception {
        String endpoint = params.getEndpoint().replaceAll("/$", "");
        String url;
        if (endpoint.endsWith("/v1")) {
            url = endpoint + "/chat/completions";
        } else {
            url = endpoint + "/v1/chat/completions";
        }

        Map<String, Object> request = new HashMap<>();
        request.put("model", params.getModel());

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        request.put("messages", messages);

        Map<String, Object> options = buildOptions(params);
        if (!options.isEmpty()) {
            request.putAll(options);
        }

        return executeWithRetry(url, request, params);
    }

    private String executeWithRetry(String url, Map<String, Object> requestBody, OpenAiRequestParams params) throws Exception {
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

        Consumer<OpenAiRetryAttemptEvent> listener = null;
        if (log != null) {
            int retryAttempts = params.getRetryAttempts();
            listener = event -> {
                if ("empty-or-blank".equals(event.responseClass())) {
                    String action = event.retryScheduled() ? "retrying" : "terminal";
                    log.info("[openai-empty-response-retry] attempt=" + event.attempt() + "/" + retryAttempts
                            + " class=" + event.responseClass()
                            + " action=" + action
                            + " delayMs=" + event.delayMs()
                            + " reason=" + LogSanitizer.sanitizeForLog(event.cause().getMessage()));
                    return;
                }
                if (event.retryScheduled()) {
                    log.warn("OpenAI request failed (attempt " + event.attempt() + "/" + retryAttempts + "): "
                            + LogSanitizer.sanitizeForLog(event.cause().getMessage()) + " - retrying...");
                }
            };
        }

        return retryPolicy.execute(
                () -> executeRequest(client, request),
                params.getRetryAttempts(),
                0,
                listener);
    }

    private String executeRequest(OkHttpClient client, Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                String sanitizedErrorBody = LogSanitizer.sanitizeForLog(errorBody);
                if (log != null) {
                    log.debug("[openai] HTTP " + response.code() + " response: " + sanitizedErrorBody);
                }
                throw new IOException("OpenAI API returned HTTP " + response.code() + ": " + sanitizedErrorBody);
            }

            String payload = response.body() == null ? "" : response.body().string();
            if (payload.isBlank()) {
                throw new OpenAiEmptyResponseException("Empty response from OpenAI API");
            }

            JsonNode root;
            try (JsonParser parser = MAPPER.createParser(payload)) {
                root = MAPPER.readTree(parser);
            }

            JsonNode choices = root.path("choices");
            String text = null;
            if (choices.isArray() && choices.size() > 0) {
                JsonNode contentNode = choices.get(0).path("message").path("content");
                text = contentNode.isMissingNode() ? null : contentNode.asText();
            }

            if (text == null || text.isBlank()) {
                throw new OpenAiEmptyResponseException("OpenAI API returned empty response content");
            }

            if (log != null) {
                log.debug("[openai] Compiled response");
            }
            return text;
        }
    }

    private Map<String, Object> buildOptions(OpenAiRequestParams params) {
        if (params.getOptions() == null || params.getOptions().isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>(params.getOptions());
        mapOption(normalized, "topP", "top_p");
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
}
