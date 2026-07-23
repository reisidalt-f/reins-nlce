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

import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LlmRequest is part of the general application functions in the reins architecture.
 * Acts as a component managing llm request.
 */
public class LlmRequest {
    private String requestId;
    private String sourcePath;
    private String sourceScope;
    private String markdownContent;
    private List<ConversationMessage> conversationHistory;
    private String cachedContentId;
    private boolean useCachedContent;
    private LlmRequestOptions options;
    private Map<String, String> context = new HashMap<>();

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
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Sets the source path.
     *
     * @param sourcePath the path of the source file
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * Gets the source scope.
     *
     * @return the string result
     */
    public String getSourceScope() {
        return sourceScope;
    }

    /**
     * Sets the source scope.
     *
     * @param sourceScope the source scope
     */
    public void setSourceScope(String sourceScope) {
        this.sourceScope = sourceScope;
    }

    /**
     * Gets the markdown content.
     *
     * @return the string result
     */
    public String getMarkdownContent() {
        return markdownContent;
    }

    /**
     * Sets the markdown content.
     *
     * @param markdownContent the markdown content
     */
    public void setMarkdownContent(String markdownContent) {
        this.markdownContent = markdownContent;
    }

    /**
     * Gets the conversation history.
     *
     * @return the collection of elements
     */
    public List<ConversationMessage> getConversationHistory() {
        return conversationHistory;
    }

    /**
     * Sets the conversation history.
     *
     * @param conversationHistory the conversation history
     */
    public void setConversationHistory(List<ConversationMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    /**
     * Gets the cached content id.
     *
     * @return the string result
     */
    public String getCachedContentId() {
        return cachedContentId;
    }

    /**
     * Sets the cached content id.
     *
     * @param cachedContentId the cached content id
     */
    public void setCachedContentId(String cachedContentId) {
        this.cachedContentId = cachedContentId;
    }

    /**
     * Checks if the component is use cached content.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isUseCachedContent() {
        return useCachedContent;
    }

    /**
     * Sets the use cached content.
     *
     * @param useCachedContent the use cached content
     */
    public void setUseCachedContent(boolean useCachedContent) {
        this.useCachedContent = useCachedContent;
    }

    /**
     * Gets the options.
     *
     * @return the resolved or constructed object
     */
    public LlmRequestOptions getOptions() {
        return options;
    }

    /**
     * Sets the options.
     *
     * @param options the options mapping
     */
    public void setOptions(LlmRequestOptions options) {
        this.options = options;
    }

    /**
     * Gets the context.
     *
     * @return the string result
     */
    public Map<String, String> getContext() {
        return context;
    }

    /**
     * Sets the context.
     *
     * @param context the context
     */
    public void setContext(Map<String, String> context) {
        this.context = context == null ? new HashMap<>() : context;
    }
}
