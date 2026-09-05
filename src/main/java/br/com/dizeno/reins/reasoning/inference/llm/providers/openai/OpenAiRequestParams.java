package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OpenAiRequestParams is part of the general application functions in the reins architecture.
 * Acts as a component managing openai request params.
 */
public final class OpenAiRequestParams {
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final int timeoutSeconds;
    private final int retryAttempts;
    private final Map<String, Object> options;

    /**
     * Constructs a new instance of {@link OpenAiRequestParams}.
     *
     * @param endpoint the API endpoint URL
     * @param model the model name string
     * @param apiKey the API key
     * @param timeoutSeconds the timeout seconds
     * @param retryAttempts the retry attempts
     * @param options the options mapping
     */
    public OpenAiRequestParams(
            String endpoint,
            String model,
            String apiKey,
            int timeoutSeconds,
            int retryAttempts,
            Map<String, Object> options) {
        this.endpoint = normalizeEndpoint(endpoint);
        this.model = Objects.requireNonNull(model, "model cannot be null").trim();
        this.apiKey = apiKey == null ? null : apiKey.trim();
        this.timeoutSeconds = timeoutSeconds;
        this.retryAttempts = retryAttempts;
        this.options = options != null ? Collections.unmodifiableMap(new HashMap<>(options)) : Collections.emptyMap();
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "https://api.openai.com";
        }
        return endpoint.trim();
    }

    /**
     * Gets the endpoint.
     *
     * @return the string result
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Gets the model.
     *
     * @return the string result
     */
    public String getModel() {
        return model;
    }

    /**
     * Gets the api key.
     *
     * @return the string result
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Gets the timeout seconds.
     *
     * @return the numeric value
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Gets the retry attempts.
     *
     * @return the numeric value
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Gets the options.
     *
     * @return the options map
     */
    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * Equals.
     *
     * @param o the o
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenAiRequestParams that = (OpenAiRequestParams) o;
        return timeoutSeconds == that.timeoutSeconds &&
                retryAttempts == that.retryAttempts &&
                endpoint.equals(that.endpoint) &&
                model.equals(that.model) &&
                Objects.equals(apiKey, that.apiKey) &&
                options.equals(that.options);
    }

    /**
     * Hash code.
     *
     * @return the numeric value
     */
    @Override
    public int hashCode() {
        return Objects.hash(endpoint, model, apiKey, timeoutSeconds, retryAttempts, options);
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "OpenAiRequestParams{" +
                "endpoint='" + endpoint + '\'' +
                ", model='" + model + '\'' +
                ", apiKey=***" +
                ", timeoutSeconds=" + timeoutSeconds +
                ", retryAttempts=" + retryAttempts +
                ", options=" + options +
                '}';
    }
}
