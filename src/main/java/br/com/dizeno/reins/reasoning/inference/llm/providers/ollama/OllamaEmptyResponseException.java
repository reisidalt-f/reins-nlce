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

package br.com.dizeno.reins.reasoning.inference.llm.providers.ollama;

import java.io.IOException;

import br.com.dizeno.reins.reasoning.inference.llm.error.LlmEmptyResponseException;

/**
 * OllamaEmptyResponseException is part of the general application functions in
 * the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class OllamaEmptyResponseException extends LlmEmptyResponseException {
    private final int totalAttempts;
    private final int noUsableContentCount;

    /**
     * Constructs a new instance of {@link OllamaEmptyResponseException}.
     *
     * @param message the message content
     */
    public OllamaEmptyResponseException(String message) {
        this(message, -1, -1);
    }

    /**
     * Constructs a new instance of {@link OllamaEmptyResponseException}.
     *
     * @param message              the message content
     * @param totalAttempts        the total attempts
     * @param noUsableContentCount the no usable content count
     */
    public OllamaEmptyResponseException(String message, int totalAttempts, int noUsableContentCount) {
        super(message);
        this.totalAttempts = totalAttempts;
        this.noUsableContentCount = noUsableContentCount;
    }

    /**
     * Gets the total attempts.
     *
     * @return the numeric value
     */
    public int getTotalAttempts() {
        return totalAttempts;
    }

    /**
     * Gets the no usable content count.
     *
     * @return the numeric value
     */
    public int getNoUsableContentCount() {
        return noUsableContentCount;
    }
}
