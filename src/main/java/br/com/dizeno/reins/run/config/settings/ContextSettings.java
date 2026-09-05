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
import java.util.Set;

/**
 * ContextSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ContextSettings {
    private boolean cachedContent = true;
    private boolean allowScriptedMessageData = true;
    private boolean allowScriptedAttachments = true;
    private boolean compiledFiles = true;
    private boolean inspectedFiles = true;
    private List<ContextSourceSpec> sources = new ArrayList<>();
    private List<String> plainAttachmentExtensions = new ArrayList<>();
    private ReferencesTreeSettings referencesTree = new ReferencesTreeSettings();

    private static final Set<String> WELL_KNOWN_PLAIN_TEXT_EXTENSIONS = Set.of(
            "md", "markdown", "txt", "java", "py", "js", "ts", "jsx", "tsx",
            "json", "yaml", "yml", "xml", "html", "htm", "css", "scss", "sass",
            "sql", "sh", "bash", "kt", "kts", "c", "cpp", "h", "hpp", "cs", "go",
            "rs", "toml", "env", "properties", "ini", "conf", "log", "csv",
            "gradle", "proto", "graphql", "groovy", "scala", "swift", "rb", "php",
            "dockerfile", "ftl", "mustache"
    );

    /**
     * Gets the references tree settings.
     *
     * @return the references tree settings object
     */
    public ReferencesTreeSettings getReferencesTree() {
        return referencesTree;
    }

    /**
     * Sets the references tree settings.
     *
     * @param referencesTree the references tree settings object
     */
    public void setReferencesTree(ReferencesTreeSettings referencesTree) {
        this.referencesTree = referencesTree != null ? referencesTree : new ReferencesTreeSettings();
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
     * Checks if compiled files are eagerly provided in context.
     *
     * @return true if compiled files are enabled, false otherwise
     */
    public boolean isCompiledFiles() {
        return compiledFiles;
    }

    /**
     * Sets whether compiled files are eagerly provided in context.
     *
     * @param compiledFiles whether compiled files are enabled
     */
    public void setCompiledFiles(boolean compiledFiles) {
        this.compiledFiles = compiledFiles;
    }

    /**
     * Checks if inspected files are eagerly provided in context.
     *
     * @return true if inspected files are enabled, false otherwise
     */
    public boolean isInspectedFiles() {
        return inspectedFiles;
    }

    /**
     * Sets whether inspected files are eagerly provided in context.
     *
     * @param inspectedFiles whether inspected files are enabled
     */
    public void setInspectedFiles(boolean inspectedFiles) {
        this.inspectedFiles = inspectedFiles;
    }

    /**
     * Gets the sources.
     *
     * @return the collection of elements
     */
    public List<ContextSourceSpec> getSources() {
        return sources;
    }

    /**
     * Sets the sources.
     *
     * @param sources the sources
     */
    public void setSources(List<ContextSourceSpec> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    /**
     * Gets the plain attachment extensions list.
     *
     * @return the collection of elements
     */
    public List<String> getPlainAttachmentExtensions() {
        return plainAttachmentExtensions;
    }

    /**
     * Sets the plain attachment extensions list.
     *
     * @param plainAttachmentExtensions the list of plain attachment extensions
     */
    public void setPlainAttachmentExtensions(List<String> plainAttachmentExtensions) {
        this.plainAttachmentExtensions = plainAttachmentExtensions != null ? plainAttachmentExtensions : new ArrayList<>();
    }

    /**
     * Checks if the given path or filename has a plain attachment extension (either well-known or user-configured).
     *
     * @param pathOrFilename the path or filename
     * @return true if it should be attached as plain text/markdown, false otherwise
     */
    public boolean isPlainAttachment(String pathOrFilename) {
        if (pathOrFilename == null || pathOrFilename.isBlank()) {
            return false;
        }
        String ext = extractExtension(pathOrFilename);
        if (ext.isEmpty()) {
            return false;
        }
        if (WELL_KNOWN_PLAIN_TEXT_EXTENSIONS.contains(ext)) {
            return true;
        }
        if (plainAttachmentExtensions != null) {
            for (String userExt : plainAttachmentExtensions) {
                if (userExt != null) {
                    String cleanUserExt = userExt.trim();
                    if (cleanUserExt.startsWith(".")) {
                        cleanUserExt = cleanUserExt.substring(1);
                    }
                    if (cleanUserExt.equalsIgnoreCase(ext)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String extractExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase().trim();
        }
        return "";
    }
}

