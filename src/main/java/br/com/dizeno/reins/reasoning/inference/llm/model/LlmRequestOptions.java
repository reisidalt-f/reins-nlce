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

package br.com.dizeno.reins.reasoning.inference.llm.model;

/**
 * LlmRequestOptions is part of the general application functions in the reins architecture.
 * Acts as a component managing llm request options.
 */
public class LlmRequestOptions {
    private Integer timeoutSeconds;
    private Integer retryAttempts;
    private Float temperature;

    /**
     * Gets the timeout seconds.
     *
     * @return the numeric value
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets the timeout seconds.
     *
     * @param timeoutSeconds the timeout seconds
     */
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Gets the retry attempts.
     *
     * @return the numeric value
     */
    public Integer getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Sets the retry attempts.
     *
     * @param retryAttempts the retry attempts
     */
    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    /**
     * Gets the temperature.
     *
     * @return the numeric value
     */
    public Float getTemperature() {
        return temperature;
    }

    /**
     * Sets the temperature.
     *
     * @param temperature the temperature
     */
    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }
}
