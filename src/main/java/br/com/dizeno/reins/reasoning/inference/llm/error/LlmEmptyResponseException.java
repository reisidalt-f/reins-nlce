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

package br.com.dizeno.reins.reasoning.inference.llm.error;

import java.io.IOException;

/**
 * LlmEmptyResponseException is part of the reasoning inference error management
 * in the reins architecture.
 * Acts as a base exception representing empty response errors across LLM providers.
 */
public class LlmEmptyResponseException extends IOException {
    private final int totalAttempts;
    private final int noUsableContentCount;

    /**
     * Constructs a new instance of {@link LlmEmptyResponseException}.
     *
     * @param message the message content
     */
    public LlmEmptyResponseException(String message) {
        this(message, -1, -1);
    }

    /**
     * Constructs a new instance of {@link LlmEmptyResponseException}.
     *
     * @param message              the message content
     * @param totalAttempts        the total attempts
     * @param noUsableContentCount the no usable content count
     */
    public LlmEmptyResponseException(String message, int totalAttempts, int noUsableContentCount) {
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
