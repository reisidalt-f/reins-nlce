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

import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiEmptyResponseException;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * LlmErrorMapper is part of the general application functions in the reins
 * architecture.
 * Acts as a helper utility for mapping its prefix data formats.
 */
public class LlmErrorMapper {
    /**
     * Map.
     *
     * @param throwable  the throwable
     * @param providerId the provider id
     * @return the resolved or constructed object
     */
    public LlmError map(Throwable throwable, String providerId) {
        LlmError error = new LlmError();
        error.setProviderId(providerId);
        String message = throwable == null ? "Unknown provider error" : throwable.getMessage();
        error.setMessage(message == null ? "Unknown provider error" : message);

        if (throwable instanceof LlmServiceException serviceException && serviceException.getError() != null) {
            return serviceException.getError();
        }
        if (throwable instanceof IllegalArgumentException) {
            error.setCategory(LlmError.Category.UNSUPPORTED_CAPABILITY);
            error.setRetryable(false);
            return error;
        }
        if (throwable instanceof SocketTimeoutException) {
            error.setCategory(LlmError.Category.TIMEOUT);
            error.setRetryable(true);
            return error;
        }
        if (throwable instanceof GeminiEmptyResponseException) {
            error.setCategory(LlmError.Category.UNAVAILABLE);
            error.setRetryable(true);
            return error;
        }
        if (throwable instanceof IOException ioException) {
            String lower = ioException.getMessage() == null ? "" : ioException.getMessage().toLowerCase();
            if (lower.contains("401") || lower.contains("403") || lower.contains("auth")) {
                error.setCategory(LlmError.Category.AUTHENTICATION);
                error.setRetryable(false);
                return error;
            }
            if (lower.contains("429") || lower.contains("rate")) {
                error.setCategory(LlmError.Category.RATE_LIMIT);
                error.setRetryable(true);
                return error;
            }
            if (lower.contains("timeout")) {
                error.setCategory(LlmError.Category.TIMEOUT);
                error.setRetryable(true);
                return error;
            }
            error.setCategory(LlmError.Category.UNAVAILABLE);
            error.setRetryable(true);
            return error;
        }
        error.setCategory(LlmError.Category.UNKNOWN);
        error.setRetryable(false);
        return error;
    }
}
