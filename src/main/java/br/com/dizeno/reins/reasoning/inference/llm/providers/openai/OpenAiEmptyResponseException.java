package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

import java.io.IOException;

import br.com.dizeno.reins.reasoning.inference.llm.error.LlmEmptyResponseException;

/**
 * OpenAiEmptyResponseException is part of the general application functions in
 * the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class OpenAiEmptyResponseException extends LlmEmptyResponseException {
    private final int totalAttempts;
    private final int noUsableContentCount;

    /**
     * Constructs a new instance of {@link OpenAiEmptyResponseException}.
     *
     * @param message the message content
     */
    public OpenAiEmptyResponseException(String message) {
        this(message, -1, -1);
    }

    /**
     * Constructs a new instance of {@link OpenAiEmptyResponseException}.
     *
     * @param message              the message content
     * @param totalAttempts        the total attempts
     * @param noUsableContentCount the no usable content count
     */
    public OpenAiEmptyResponseException(String message, int totalAttempts, int noUsableContentCount) {
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
