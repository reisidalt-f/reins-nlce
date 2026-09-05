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

package br.com.dizeno.reins.reasoning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PipelineExchangeMessage is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing pipeline exchange message.
 */
public class PipelineExchangeMessage {
    private final String intent;
    private final String contentType;
    private final Map<String, String> headers;
    private final String body;
    private final String messageToModel;
    private final String messageToUser;
    private final boolean valid;
    private final String failureReason;

    /**
     * Constructs a new instance of {@link PipelineExchangeMessage}.
     *
     * @param intent the reasoning intent
     * @param contentType the content type
     * @param headers the headers
     * @param body the body
     * @param messageToModel the message to model
     * @param messageToUser the message to user
     * @param valid the valid
     * @param failureReason the failure reason
     */
    public PipelineExchangeMessage(String intent,
                                   String contentType,
                                   Map<String, String> headers,
                                   String body,
                                   String messageToModel,
                                   String messageToUser,
                                   boolean valid,
                                   String failureReason) {
        this.intent = intent;
        this.contentType = contentType;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body;
        this.messageToModel = messageToModel;
        this.messageToUser = messageToUser;
        this.valid = valid;
        this.failureReason = failureReason;
    }

    /**
     * Invalid.
     *
     * @param body the body
     * @param failureReason the failure reason
     * @return the resolved or constructed object
     */
    public static PipelineExchangeMessage invalid(String body, String failureReason) {
        return new PipelineExchangeMessage(null, null, Collections.emptyMap(), body, null, null, false, failureReason);
    }

    /**
     * Gets the intent.
     *
     * @return the string result
     */
    public String getIntent() {
        return intent;
    }

    /**
     * Gets the content type.
     *
     * @return the string result
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the headers.
     *
     * @return the string result
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Gets the body.
     *
     * @return the string result
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the message to model.
     *
     * @return the string result
     */
    public String getMessageToModel() {
        return messageToModel;
    }

    /**
     * Gets the message to user.
     *
     * @return the string result
     */
    public String getMessageToUser() {
        return messageToUser;
    }

    /**
     * Checks if the component is valid.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the failure reason.
     *
     * @return the string result
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Checks if the component is finish success.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFinishSuccess() {
        return "finish-success".equalsIgnoreCase(intent);
    }

    /**
     * Checks if the component is finish error.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFinishError() {
        return "finish-error".equalsIgnoreCase(intent);
    }

    /**
     * Gets the role.
     *
     * @return the string result
     */
    public String getRole() {
        String role = headers.get("role");
        return role == null ? null : role.trim();
    }

    /**
     * Checks if the component has role header.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean hasRoleHeader() {
        return headers.containsKey("role");
    }

    /**
     * Checks if the component is spoofed assistant role.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSpoofedAssistantRole() {
        return hasRoleHeader() && "assistant".equalsIgnoreCase(getRole());
    }

    /**
     * Sets the role.
     *
     * @param role the role
     * @return the resolved or constructed object
     */
    public PipelineExchangeMessage setRole(String role) {
        Map<String, String> updatedHeaders = new LinkedHashMap<>(this.headers);
        if (role == null || role.isBlank()) {
            updatedHeaders.remove("role");
        } else {
            updatedHeaders.put("role", role.trim());
        }
        return new PipelineExchangeMessage(
                intent,
                contentType,
                updatedHeaders,
                body,
                messageToModel,
                messageToUser,
                valid,
                failureReason);
    }
}