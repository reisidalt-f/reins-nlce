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

import java.util.HashMap;
import java.util.Map;

/**
 * LlmResponseExtensions is part of the general application functions in the reins architecture.
 * Acts as a component managing llm response extensions.
 */
public class LlmResponseExtensions {
    private String providerId;
    private Map<String, Object> payload = new HashMap<>();

    /**
     * Gets the provider id.
     *
     * @return the string result
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the provider id.
     *
     * @param providerId the provider id
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * Gets the payload.
     *
     * @return the string result
     */
    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the message payload text
     */
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new HashMap<>() : payload;
    }
}
