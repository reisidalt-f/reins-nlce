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

package br.com.dizeno.reins.reasoning.inference;

/**
 * MarkdownInferenceResponse is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing markdown inference response.
 */
public class MarkdownInferenceResponse {
    private String rawResponseText;
    private long durationMs;

    /**
     * Gets the raw response text.
     *
     * @return the string result
     */
    public String getRawResponseText() {
        return rawResponseText;
    }

    /**
     * Sets the raw response text.
     *
     * @param rawResponseText the raw response text
     */
    public void setRawResponseText(String rawResponseText) {
        this.rawResponseText = rawResponseText;
    }

    /**
     * Gets the duration ms.
     *
     * @return the numeric value
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the duration ms.
     *
     * @param durationMs the duration ms
     */
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
}