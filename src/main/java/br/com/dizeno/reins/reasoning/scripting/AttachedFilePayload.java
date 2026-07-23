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

package br.com.dizeno.reins.reasoning.scripting;

/**
 * AttachedFilePayload is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class AttachedFilePayload {
    private String base;
    private String relativePath;
    private String qualifiedPath;
    private String content;
    private String sourceCanonicalPath;
    private String sourceSimpleName;

    /**
     * Gets the base.
     *
     * @return the string result
     */
    public String getBase() {
        return base;
    }

    /**
     * Sets the base.
     *
     * @param base the base
     */
    public void setBase(String base) {
        this.base = base;
    }

    /**
     * Gets the relative path.
     *
     * @return the string result
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Sets the relative path.
     *
     * @param relativePath the relative path
     */
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    /**
     * Gets the qualified path.
     *
     * @return the string result
     */
    public String getQualifiedPath() {
        return qualifiedPath;
    }

    /**
     * Sets the qualified path.
     *
     * @param qualifiedPath the qualified path
     */
    public void setQualifiedPath(String qualifiedPath) {
        this.qualifiedPath = qualifiedPath;
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
     * Gets the source canonical path.
     *
     * @return the string result
     */
    public String getSourceCanonicalPath() {
        return sourceCanonicalPath;
    }

    /**
     * Sets the source canonical path.
     *
     * @param sourceCanonicalPath the source canonical path
     */
    public void setSourceCanonicalPath(String sourceCanonicalPath) {
        this.sourceCanonicalPath = sourceCanonicalPath;
    }

    /**
     * Gets the source simple name.
     *
     * @return the string result
     */
    public String getSourceSimpleName() {
        return sourceSimpleName;
    }

    /**
     * Sets the source simple name.
     *
     * @param sourceSimpleName the source simple name
     */
    public void setSourceSimpleName(String sourceSimpleName) {
        this.sourceSimpleName = sourceSimpleName;
    }
}
