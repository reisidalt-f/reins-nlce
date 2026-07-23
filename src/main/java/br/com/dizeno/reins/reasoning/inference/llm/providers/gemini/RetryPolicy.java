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

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * RetryPolicy is part of the general application functions in the reins architecture.
 * Acts as a component managing retry policy.
 */
public class RetryPolicy {

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
     * @param listener the listener
     * @return the resolved or constructed object
     */
    public <T> T execute(Callable<T> callable, int retryAttempts,
                         Consumer<RetryAttemptEvent> listener) throws Exception {
        return execute(callable, retryAttempts, 0, listener);
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
                         Consumer<RetryAttemptEvent> listener) throws Exception {
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
                    listener.accept(new RetryAttemptEvent(
                            attempts,
                            retryAttempts,
                            ex,
                            classify(ex),
                            retryScheduled,
                            appliedDelayMs));
                }
                if (!retryScheduled) {
                    if (ex instanceof GeminiEmptyResponseException gee) {
                        throw new GeminiEmptyResponseException(gee.getMessage(), attempts, attempts);
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
        return true;
    }

    private String classify(Exception ex) {
        if (ex instanceof GeminiEmptyResponseException) {
            return "empty-or-blank";
        }
        return "other-failure";
    }
}
