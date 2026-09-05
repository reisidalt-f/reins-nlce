package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * OpenAiRetryPolicy is part of the general application functions in the reins architecture.
 * Acts as a component managing openai retry policy.
 */
public class OpenAiRetryPolicy {

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
                         Consumer<OpenAiRetryAttemptEvent> listener) throws Exception {
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
                    listener.accept(new OpenAiRetryAttemptEvent(
                            attempts,
                            retryAttempts,
                            ex,
                            classify(ex),
                            retryScheduled,
                            appliedDelayMs));
                }
                if (!retryScheduled) {
                    if (ex instanceof OpenAiEmptyResponseException oee) {
                        throw new OpenAiEmptyResponseException(oee.getMessage(), attempts, attempts);
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
        if (ex instanceof OpenAiEmptyResponseException) {
            return "empty-or-blank";
        }
        return "other-failure";
    }
}
