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
 * LoggingSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class LoggingSettings {
    private boolean scriptsEvents = false;
    private boolean eagerlyProvided = false;
    private boolean fileListingAndReading = false;
    private boolean fileMutating = false;
    private boolean scriptRun = false;
    private boolean selectionReason = false;
    private boolean result = false;
    private boolean trackingFile = false;
    private boolean llmProvider = false;

    /**
     * Checks if the component is scripts events.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isScriptsEvents() {
        return scriptsEvents;
    }

    /**
     * Sets the scripts events.
     *
     * @param scriptsEvents the scripts events
     */
    public void setScriptsEvents(boolean scriptsEvents) {
        this.scriptsEvents = scriptsEvents;
    }

    /**
     * Checks if the component is eagerly provided.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEagerlyProvided() {
        return eagerlyProvided;
    }

    /**
     * Sets the eagerly provided.
     *
     * @param eagerlyProvided the eagerly provided
     */
    public void setEagerlyProvided(boolean eagerlyProvided) {
        this.eagerlyProvided = eagerlyProvided;
    }

    /**
     * Checks if the component is file listing and reading.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFileListingAndReading() {
        return fileListingAndReading;
    }

    /**
     * Sets the file listing and reading.
     *
     * @param fileListingAndReading the file listing and reading
     */
    public void setFileListingAndReading(boolean fileListingAndReading) {
        this.fileListingAndReading = fileListingAndReading;
    }

    /**
     * Checks if the component is file mutating.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFileMutating() {
        return fileMutating;
    }

    /**
     * Sets the file mutating.
     *
     * @param fileMutating the file mutating
     */
    public void setFileMutating(boolean fileMutating) {
        this.fileMutating = fileMutating;
    }

    /**
     * Checks if the component is script run.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isScriptRun() {
        return scriptRun;
    }

    /**
     * Sets the script run.
     *
     * @param scriptRun the script run
     */
    public void setScriptRun(boolean scriptRun) {
        this.scriptRun = scriptRun;
    }

    /**
     * Checks if the component is selection reason.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSelectionReason() {
        return selectionReason;
    }

    /**
     * Sets the selection reason.
     *
     * @param selectionReason the selection reason
     */
    public void setSelectionReason(boolean selectionReason) {
        this.selectionReason = selectionReason;
    }

    /**
     * Checks if the component is result.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isResult() {
        return result;
    }

    /**
     * Sets the result.
     *
     * @param result the result
     */
    public void setResult(boolean result) {
        this.result = result;
    }

    /**
     * Checks if the component is tracking file.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isTrackingFile() {
        return trackingFile;
    }

    /**
     * Sets the tracking file.
     *
     * @param trackingFile the tracking file
     */
    public void setTrackingFile(boolean trackingFile) {
        this.trackingFile = trackingFile;
    }

    /**
     * Checks if the component is llm provider.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isLlmProvider() {
        return llmProvider;
    }

    /**
     * Sets the llm provider.
     *
     * @param llmProvider the llm provider
     */
    public void setLlmProvider(boolean llmProvider) {
        this.llmProvider = llmProvider;
    }
}
