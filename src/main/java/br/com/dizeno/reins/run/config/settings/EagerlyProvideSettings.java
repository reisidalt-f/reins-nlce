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
 * EagerlyProvideSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class EagerlyProvideSettings {
    private boolean previouslyCompiledFiles = true;
    private boolean previouslyInspectedFiles = true;
    private long maxAttachmentSizeBytes = 0L;

    /**
     * Checks if the component is previously compiled files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isPreviouslyCompiledFiles() {
        return previouslyCompiledFiles;
    }

    /**
     * Sets the previously compiled files.
     *
     * @param previouslyCompiledFiles the previously compiled files
     */
    public void setPreviouslyCompiledFiles(boolean previouslyCompiledFiles) {
        this.previouslyCompiledFiles = previouslyCompiledFiles;
    }

    /**
     * Checks if the component is previously inspected files.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isPreviouslyInspectedFiles() {
        return previouslyInspectedFiles;
    }

    /**
     * Sets the previously inspected files.
     *
     * @param previouslyInspectedFiles the previously inspected files
     */
    public void setPreviouslyInspectedFiles(boolean previouslyInspectedFiles) {
        this.previouslyInspectedFiles = previouslyInspectedFiles;
    }

    /**
     * Gets the max attachment size bytes.
     *
     * @return the numeric value
     */
    public long getMaxAttachmentSizeBytes() {
        return maxAttachmentSizeBytes;
    }

    /**
     * Sets the max attachment size bytes.
     *
     * @param maxAttachmentSizeBytes the max attachment size bytes
     */
    public void setMaxAttachmentSizeBytes(long maxAttachmentSizeBytes) {
        this.maxAttachmentSizeBytes = maxAttachmentSizeBytes;
    }
}
