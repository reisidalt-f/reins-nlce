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

package br.com.dizeno.reins.compilation.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

 
/**
 * ReasoningNote is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReasoningNote {

     
    /**
     * Origin is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing origin.
     */
    public enum Origin {
        MAVEN_GOAL,
        TOOL,
        CLI,
        LIBRARY;

        /**
         * From String.
         *
         * @param value the value
         * @return the resolved or constructed object
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public static Origin fromString(String value) {
            if (value == null) return null;
            String upper = value.toUpperCase(java.util.Locale.ROOT);
            if ("MCP".equals(upper)) {
                return TOOL;
            }
            try {
                return Origin.valueOf(upper);
            } catch (IllegalArgumentException e) {
                return TOOL; 
            }
        }
    }

    private String text;
    private String createdAt;
    private Origin origin;

    /**
     * Constructs a new instance of {@link ReasoningNote}.
     */
    public ReasoningNote() {
    }

    /**
     * Constructs a new instance of {@link ReasoningNote}.
     *
     * @param text the text
     * @param createdAt the created at
     * @param origin the origin
     */
    public ReasoningNote(String text, String createdAt, Origin origin) {
        this.text = text;
        this.createdAt = createdAt;
        this.origin = origin;
    }

    /**
     * Gets the text.
     *
     * @return the string result
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text.
     *
     * @param text the text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the created at.
     *
     * @return the string result
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the created at
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the origin.
     *
     * @return the resolved or constructed object
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Sets the origin.
     *
     * @param origin the origin
     */
    public void setOrigin(Origin origin) {
        this.origin = origin;
    }
}
