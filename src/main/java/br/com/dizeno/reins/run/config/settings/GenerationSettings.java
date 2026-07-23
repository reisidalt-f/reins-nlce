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
 * GenerationSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class GenerationSettings {
    private Float temperature;
    private Float topP;
    private Integer topK;
    private Float presencePenalty;
    private Float frequencyPenalty;

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

    /**
     * Gets the top p.
     *
     * @return the numeric value
     */
    public Float getTopP() {
        return topP;
    }

    /**
     * Sets the top p.
     *
     * @param topP the top p
     */
    public void setTopP(Float topP) {
        this.topP = topP;
    }

    /**
     * Gets the top k.
     *
     * @return the numeric value
     */
    public Integer getTopK() {
        return topK;
    }

    /**
     * Sets the top k.
     *
     * @param topK the top k
     */
    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    /**
     * Gets the presence penalty.
     *
     * @return the numeric value
     */
    public Float getPresencePenalty() {
        return presencePenalty;
    }

    /**
     * Sets the presence penalty.
     *
     * @param presencePenalty the presence penalty
     */
    public void setPresencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    /**
     * Gets the frequency penalty.
     *
     * @return the numeric value
     */
    public Float getFrequencyPenalty() {
        return frequencyPenalty;
    }

    /**
     * Sets the frequency penalty.
     *
     * @param frequencyPenalty the frequency penalty
     */
    public void setFrequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
}
