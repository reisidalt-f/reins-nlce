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

/**
 * RecompileOnSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class RecompileOnSettings {
    private boolean markdownReferences = true;
    private boolean inspectedFiles = false;
    private boolean compiledFiles = false;

    /**
     * Checks if the component is markdown references.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isMarkdownReferences() {
        return markdownReferences;
    }

    /**
     * Sets the markdown references.
     *
     * @param markdownReferences the markdown references
     */
    public void setMarkdownReferences(boolean markdownReferences) {
        this.markdownReferences = markdownReferences;
    }

    /**
     * Checks if the component is inspected files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isInspectedFiles() {
        return inspectedFiles;
    }

    /**
     * Sets the inspected files.
     *
     * @param inspectedFiles the inspected files
     */
    public void setInspectedFiles(boolean inspectedFiles) {
        this.inspectedFiles = inspectedFiles;
    }

    /**
     * Checks if the component is compiled files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isCompiledFiles() {
        return compiledFiles;
    }

    /**
     * Sets the compiled files.
     *
     * @param compiledFiles the compiled files
     */
    public void setCompiledFiles(boolean compiledFiles) {
        this.compiledFiles = compiledFiles;
    }
}
