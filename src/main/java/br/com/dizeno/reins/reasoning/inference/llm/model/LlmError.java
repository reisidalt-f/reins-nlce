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

package br.com.dizeno.reins.reasoning.inference.llm.model;

/**
 * LlmError is part of the general application functions in the reins architecture.
 * Acts as a component managing llm error.
 */
public class LlmError {
    /**
     * Category is part of the general application functions in the reins architecture.
     * Acts as a component managing category.
     */
    public enum Category {
        TIMEOUT,
        AUTHENTICATION,
        RATE_LIMIT,
        UNAVAILABLE,
        UNSUPPORTED_CAPABILITY,
        COMPATIBILITY,
        UNKNOWN
    }

    private Category category = Category.UNKNOWN;
    private String message;
    private boolean retryable;
    private String providerId;

    /**
     * Gets the category.
     *
     * @return the resolved or constructed object
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Sets the category.
     *
     * @param category the category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Gets the message.
     *
     * @return the string result
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Checks if the component is retryable.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Sets the retryable.
     *
     * @param retryable the retryable
     */
    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    /**
     * Gets the provider id.
     *
     * @return the string result
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the provider id.
     *
     * @param providerId the provider id
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
