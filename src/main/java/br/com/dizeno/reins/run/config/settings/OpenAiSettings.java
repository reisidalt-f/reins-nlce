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

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAiSettings is part of the general application functions in the reins
 * architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class OpenAiSettings implements ModelProviderSetting {
    private String model;
    private String endpoint = "https://api.openai.com";
    private String apiKey;
    private int timeoutSeconds = 60;
    private int retryAttempts = 3;
    private Map<String, Object> options = new HashMap<>();

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
     * Gets the options.
     *
     * @return the options map
     */
    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * Sets the options.
     *
     * @param options the options mapping
     */
    public void setOptions(Map<String, Object> options) {
        this.options = options != null ? options : new HashMap<>();
    }
}
