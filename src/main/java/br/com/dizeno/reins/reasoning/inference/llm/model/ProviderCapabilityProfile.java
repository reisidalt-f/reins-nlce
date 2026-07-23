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

import java.util.ArrayList;
import java.util.List;

/**
 * ProviderCapabilityProfile is part of the general application functions in the reins architecture.
 * Acts as a component managing provider capability profile.
 */
public class ProviderCapabilityProfile {
    private boolean supportsTemperature = true;
    private boolean supportsStructuredExtensions = true;
    private List<String> supportedModels = new ArrayList<>();

    /**
     * Checks if the component is supports temperature.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSupportsTemperature() {
        return supportsTemperature;
    }

    /**
     * Sets the supports temperature.
     *
     * @param supportsTemperature the supports temperature
     */
    public void setSupportsTemperature(boolean supportsTemperature) {
        this.supportsTemperature = supportsTemperature;
    }

    /**
     * Checks if the component is supports structured extensions.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSupportsStructuredExtensions() {
        return supportsStructuredExtensions;
    }

    /**
     * Sets the supports structured extensions.
     *
     * @param supportsStructuredExtensions the supports structured extensions
     */
    public void setSupportsStructuredExtensions(boolean supportsStructuredExtensions) {
        this.supportsStructuredExtensions = supportsStructuredExtensions;
    }

    /**
     * Gets the supported models.
     *
     * @return the string result
     */
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    /**
     * Sets the supported models.
     *
     * @param supportedModels the supported models
     */
    public void setSupportedModels(List<String> supportedModels) {
        this.supportedModels = supportedModels == null ? new ArrayList<>() : supportedModels;
    }
}
