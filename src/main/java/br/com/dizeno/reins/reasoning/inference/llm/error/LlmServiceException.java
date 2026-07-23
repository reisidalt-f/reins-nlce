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

import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;

/**
 * LlmServiceException is part of the general application functions in the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class LlmServiceException extends Exception {
    private final LlmError error;

    /**
     * Constructs a new instance of {@link LlmServiceException}.
     *
     * @param error the error
     */
    public LlmServiceException(LlmError error) {
        super(error == null ? "LLM service error" : error.getMessage());
        this.error = error;
    }

    /**
     * Gets the error.
     *
     * @return the resolved or constructed object
     */
    public LlmError getError() {
        return error;
    }
}
