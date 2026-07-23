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
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * OllamaRetryPolicy is part of the general application functions in the reins architecture.
 * Acts as a component managing ollama retry policy.
 */
public class OllamaRetryPolicy {

    /**
     * Executes the operation.
     *
     * @param callable the callable
     * @param retryAttempts the retry attempts
     * @return the resolved or constructed object
     */
    public <T> T execute(Callable<T> callable, int retryAttempts) throws Exception {
        return execute(callable, retryAttempts, 0, null);
    }

    /**
     * Executes the operation.
     *
     * @param callable the callable
     * @param retryAttempts the retry attempts
     * @param retryDelayMs the retry delay ms
     * @param listener the listener
     * @return the resolved or constructed object
     */
    public <T> T execute(Callable<T> callable,
                         int retryAttempts,
                         int retryDelayMs,
                         Consumer<OllamaRetryAttemptEvent> listener) throws Exception {
        int attempts = 0;
        while (true) {
            try {
                return callable.call();
            } catch (Exception ex) {
                attempts++;
                boolean retryable = isTransient(ex);
                boolean retryScheduled = attempts <= retryAttempts && retryable;
                int appliedDelayMs = retryScheduled ? Math.max(0, retryDelayMs) : 0;
                if (listener != null) {
                    listener.accept(new OllamaRetryAttemptEvent(
                            attempts,
                            retryAttempts,
                            ex,
                            classify(ex),
                            retryScheduled,
                            appliedDelayMs));
                }
                if (!retryScheduled) {
                    if (ex instanceof OllamaEmptyResponseException oee) {
                        throw new OllamaEmptyResponseException(oee.getMessage(), attempts, attempts);
                    }
                    throw ex;
                }
                if (appliedDelayMs > 0) {
                    try {
                        Thread.sleep(appliedDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                }
            }
        }
    }

    boolean isTransient(Exception ex) {
        if (ex instanceof SocketTimeoutException) {
            return true;
        }
        if (ex instanceof IOException io) {
            String msg = io.getMessage();
            if (msg != null) {
                return !msg.contains("HTTP 400")
                        && !msg.contains("HTTP 401")
                        && !msg.contains("HTTP 403");
            }
            return true;
        }
        return true;
    }

    private String classify(Exception ex) {
        if (ex instanceof OllamaEmptyResponseException) {
            return "empty-or-blank";
        }
        return "other-failure";
    }
}
