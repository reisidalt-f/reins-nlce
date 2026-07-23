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

import java.util.HashMap;
import java.util.Map;

/**
 * LlmResponse is part of the general application functions in the reins architecture.
 * Acts as a component managing llm response.
 */
public class LlmResponse {
    /**
     * Status is part of the general application functions in the reins architecture.
     * Acts as a component managing status.
     */
    public enum Status {
        SUCCESS,
        ERROR
    }

    private String requestId;
    private String content;
    private Status status = Status.SUCCESS;
    private Map<String, String> metadata = new HashMap<>();
    private LlmResponseExtensions extensions;

    /**
     * Gets the request id.
     *
     * @return the string result
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request id.
     *
     * @param requestId the request id
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Gets the content.
     *
     * @return the string result
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the metadata.
     *
     * @return the string result
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata.
     *
     * @param metadata the metadata
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null ? new HashMap<>() : metadata;
    }

    /**
     * Gets the extensions.
     *
     * @return the resolved or constructed object
     */
    public LlmResponseExtensions getExtensions() {
        return extensions;
    }

    /**
     * Sets the extensions.
     *
     * @param extensions the extensions
     */
    public void setExtensions(LlmResponseExtensions extensions) {
        this.extensions = extensions;
    }
}
