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

package br.com.dizeno.reins.run.config.settings;

/**
 * GeminiSettings is part of the general application functions in the reins
 * architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class GeminiSettings implements ModelProviderSetting {
    private String apiKey;
    private String model;
    private String endpoint;
    private int timeoutSeconds = 30;
    private int retryAttempts = 3;
    private int emptyResponseRetryDelayMs = 1000;
    private Integer maximumTurns;
    private GenerationSettings generation;

    /**
     * Gets the api key.
     *
     * @return the string result
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the api key.
     *
     * @param apiKey the API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
     * Sets the model.
     *
     * @param model the model name string
     */
    public void setModel(String model) {
        this.model = model;
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
     * Sets the endpoint.
     *
     * @param endpoint the API endpoint URL
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
     * Sets the timeout seconds.
     *
     * @param timeoutSeconds the timeout seconds
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
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
     * Sets the retry attempts.
     *
     * @param retryAttempts the retry attempts
     */
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    /**
     * Gets the empty response retry delay ms.
     *
     * @return the numeric value
     */
    public int getEmptyResponseRetryDelayMs() {
        return emptyResponseRetryDelayMs;
    }

    /**
     * Sets the empty response retry delay ms.
     *
     * @param emptyResponseRetryDelayMs the empty response retry delay ms
     */
    public void setEmptyResponseRetryDelayMs(int emptyResponseRetryDelayMs) {
        this.emptyResponseRetryDelayMs = emptyResponseRetryDelayMs;
    }

    /**
     * Gets the maximum turns.
     *
     * @return the numeric value
     */
    public Integer getMaximumTurns() {
        return maximumTurns;
    }

    /**
     * Sets the maximum turns.
     *
     * @param maximumTurns the maximum turns
     */
    public void setMaximumTurns(Integer maximumTurns) {
        this.maximumTurns = maximumTurns;
    }

    /**
     * Resolves the configured value or path maximum turns.
     *
     * @return the numeric value
     */
    public int resolveMaximumTurns() {
        return maximumTurns == null ? 1 : maximumTurns;
    }

    /**
     * Gets the temperature.
     *
     * @return the float result or null
     */
    @Override
    public Float getTemperature() {
        return generation != null ? generation.getTemperature() : null;
    }

    /**
     * Gets the generation.
     *
     * @return the collection of elements
     */
    public GenerationSettings getGeneration() {
        return generation;
    }

    /**
     * Sets the generation.
     *
     * @param generation the generation
     */
    public void setGeneration(GenerationSettings generation) {
        this.generation = generation;
    }
}
