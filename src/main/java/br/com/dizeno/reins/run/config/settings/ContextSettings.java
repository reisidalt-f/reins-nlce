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

package br.com.dizeno.reins.run.config.settings;

import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ContextSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ContextSettings {
    private boolean includeProjectFiles = false;
    private boolean attachReferencedFiles = true;
    private boolean cachedContent = true;
    private boolean allowScriptedMessageData = true;
    private boolean allowScriptedAttachments = true;
    private List<File> sources = new ArrayList<>();
    private String referencesTreeDepth;

    /**
     * Checks if the component is include project files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isIncludeProjectFiles() {
        return includeProjectFiles;
    }

    /**
     * Sets the include project files.
     *
     * @param includeProjectFiles the include project files
     */
    public void setIncludeProjectFiles(boolean includeProjectFiles) {
        this.includeProjectFiles = includeProjectFiles;
    }

    /**
     * Checks if the component is attach referenced files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isAttachReferencedFiles() {
        return attachReferencedFiles;
    }

    /**
     * Sets the attach referenced files.
     *
     * @param attachReferencedFiles the attach referenced files
     */
    public void setAttachReferencedFiles(boolean attachReferencedFiles) {
        this.attachReferencedFiles = attachReferencedFiles;
    }

    /**
     * Checks if the component is cached content.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isCachedContent() {
        return cachedContent;
    }

    /**
     * Sets the cached content.
     *
     * @param cachedContent the cached content
     */
    public void setCachedContent(boolean cachedContent) {
        this.cachedContent = cachedContent;
    }

    /**
     * Checks if the component is allow scripted message data.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isAllowScriptedMessageData() {
        return allowScriptedMessageData;
    }

    /**
     * Sets the allow scripted message data.
     *
     * @param allowScriptedMessageData the allow scripted message data
     */
    public void setAllowScriptedMessageData(boolean allowScriptedMessageData) {
        this.allowScriptedMessageData = allowScriptedMessageData;
    }

    /**
     * Checks if the component is allow scripted attachments.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isAllowScriptedAttachments() {
        return allowScriptedAttachments;
    }

    /**
     * Sets the allow scripted attachments.
     *
     * @param allowScriptedAttachments the allow scripted attachments
     */
    public void setAllowScriptedAttachments(boolean allowScriptedAttachments) {
        this.allowScriptedAttachments = allowScriptedAttachments;
    }

    /**
     * Gets the sources.
     *
     * @return the collection of elements
     */
    public List<File> getSources() {
        return sources;
    }

    /**
     * Sets the sources.
     *
     * @param sources the sources
     */
    public void setSources(List<File> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    /**
     * Gets the references tree depth.
     *
     * @return the string result
     */
    public String getReferencesTreeDepth() {
        return referencesTreeDepth;
    }

    /**
     * Sets the references tree depth.
     *
     * @param referencesTreeDepth the references tree depth
     */
    public void setReferencesTreeDepth(String referencesTreeDepth) {
        this.referencesTreeDepth = referencesTreeDepth;
    }

    /**
     * Resolves the configured value or path reference depth policy.
     *
     * @return the resolved or constructed object
     */
    public ReferenceDepthPolicy resolveReferenceDepthPolicy() {
        return ReferenceDepthPolicy.parse(referencesTreeDepth);
    }
}
