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
 * ReasoningSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ReasoningSettings {
    private Boolean enabled;
    private int maxTurns = 10;
    private int maxReferenceDepth = 8;
    private String scriptsPath;
    private boolean thinkingOutLoud = false;
    private boolean logSystemContext = false;
    private boolean logParseErrorRecovery = false;
    private boolean enableReasoningLog = false;
    private boolean turnCountNote = true;

    /**
     * Gets the enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled.
     *
     * @param enabled the enabled
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the max turns.
     *
     * @return the numeric value
     */
    public int getMaxTurns() {
        return maxTurns;
    }

    /**
     * Sets the max turns.
     *
     * @param maxTurns the maximum turn limit for the reasoning cycle
     */
    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    /**
     * Gets the max reference depth.
     *
     * @return the numeric value
     */
    public int getMaxReferenceDepth() {
        return maxReferenceDepth;
    }

    /**
     * Sets the max reference depth.
     *
     * @param maxReferenceDepth the max reference depth
     */
    public void setMaxReferenceDepth(int maxReferenceDepth) {
        this.maxReferenceDepth = maxReferenceDepth;
    }

    /**
     * Gets the scripts path.
     *
     * @return the string result
     */
    public String getScriptsPath() {
        return scriptsPath;
    }

    /**
     * Sets the scripts path.
     *
     * @param scriptsPath the scripts path
     */
    public void setScriptsPath(String scriptsPath) {
        this.scriptsPath = scriptsPath;
    }

    /**
     * Checks if the component is thinking out loud.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isThinkingOutLoud() {
        return thinkingOutLoud;
    }

    /**
     * Sets the thinking out loud.
     *
     * @param thinkingOutLoud the thinking out loud
     */
    public void setThinkingOutLoud(boolean thinkingOutLoud) {
        this.thinkingOutLoud = thinkingOutLoud;
    }

    /**
     * Checks if the component is log system context.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isLogSystemContext() {
        return logSystemContext;
    }

    /**
     * Sets the log system context.
     *
     * @param logSystemContext the log system context
     */
    public void setLogSystemContext(boolean logSystemContext) {
        this.logSystemContext = logSystemContext;
    }

    /**
     * Checks if the component is log parse error recovery.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isLogParseErrorRecovery() {
        return logParseErrorRecovery;
    }

    /**
     * Sets the log parse error recovery.
     *
     * @param logParseErrorRecovery the log parse error recovery
     */
    public void setLogParseErrorRecovery(boolean logParseErrorRecovery) {
        this.logParseErrorRecovery = logParseErrorRecovery;
    }

    /**
     * Checks if the component is enable reasoning log.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEnableReasoningLog() {
        return enableReasoningLog;
    }

    /**
     * Sets the enable reasoning log.
     *
     * @param enableReasoningLog the enable reasoning log
     */
    public void setEnableReasoningLog(boolean enableReasoningLog) {
        this.enableReasoningLog = enableReasoningLog;
    }

    /**
     * Checks if the component is turn count note.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isTurnCountNote() {
        return turnCountNote;
    }

    /**
     * Sets the turn count note.
     *
     * @param turnCountNote the turn count note
     */
    public void setTurnCountNote(boolean turnCountNote) {
        this.turnCountNote = turnCountNote;
    }
}
