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

package br.com.dizeno.reins.reasoning.inference;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.util.List;

/**
 * MarkdownInferenceRequest is part of the API interactions with LLM endpoints,
 * configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing markdown inference request.
 */
public class MarkdownInferenceRequest {
    private String sourcePath;
    private String sourceScope;
    private String sourceBase;
    private String sourceRelativePath;
    private String sourceHash;
    private String markdownContent;
    private List<ConversationMessage> conversationHistory;
    private String promptTemplateVersion;
    private GeminiSettings modelConfigSnapshot;
    private String cachedContentId;
    private boolean useCachedContent;

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
     * Gets the source base.
     *
     * @return the string result
     */
    public String getSourceBase() {
        return sourceBase;
    }

    /**
     * Sets the source base.
     *
     * @param sourceBase the source base
     */
    public void setSourceBase(String sourceBase) {
        this.sourceBase = sourceBase;
    }

    /**
     * Gets the source relative path.
     *
     * @return the string result
     */
    public String getSourceRelativePath() {
        return sourceRelativePath;
    }

    /**
     * Sets the source relative path.
     *
     * @param sourceRelativePath the source relative path
     */
    public void setSourceRelativePath(String sourceRelativePath) {
        this.sourceRelativePath = sourceRelativePath;
    }

    /**
     * Gets the source hash.
     *
     * @return the string result
     */
    public String getSourceHash() {
        return sourceHash;
    }

    /**
     * Sets the source hash.
     *
     * @param sourceHash the source hash
     */
    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
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
     * Gets the prompt template version.
     *
     * @return the string result
     */
    public String getPromptTemplateVersion() {
        return promptTemplateVersion;
    }

    /**
     * Sets the prompt template version.
     *
     * @param promptTemplateVersion the prompt template version
     */
    public void setPromptTemplateVersion(String promptTemplateVersion) {
        this.promptTemplateVersion = promptTemplateVersion;
    }

    /**
     * Gets the model config snapshot.
     *
     * @return the collection of elements
     */
    public GeminiSettings getModelConfigSnapshot() {
        return modelConfigSnapshot;
    }

    /**
     * Sets the model config snapshot.
     *
     * @param modelConfigSnapshot the model config snapshot
     */
    public void setModelConfigSnapshot(GeminiSettings modelConfigSnapshot) {
        this.modelConfigSnapshot = modelConfigSnapshot;
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

    private String projectContext;

    /**
     * Gets the project context.
     *
     * @return the string result
     */
    public String getProjectContext() {
        return projectContext;
    }

    /**
     * Sets the project context.
     *
     * @param projectContext the project context
     */
    public void setProjectContext(String projectContext) {
        this.projectContext = projectContext;
    }
}
