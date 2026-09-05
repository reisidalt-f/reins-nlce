package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

/**
 * OpenAiRetryAttemptEvent is part of the general application functions in the reins architecture.
 * Acts as a component managing openai retry attempt event.
 */
record OpenAiRetryAttemptEvent(int attempt,
                               int maxRetryAttempts,
                               Exception cause,
                               String responseClass,
                               boolean retryScheduled,
                               int delayMs) {
}
