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

package br.com.dizeno.reins.reasoning;

import java.nio.file.Path;

 
/**
 * ReasoningLogWriteResult is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ReasoningLogWriteResult {
    /**
     * LogWriteStatus is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing log write status.
     */
    public enum LogWriteStatus {
        SUCCESS,
        FAILED
    }

    private LogWriteStatus status;
    private Path logFilePath;
    private int entrySequence;
    private String errorMessage;

    /**
     * Constructs a new instance of {@link ReasoningLogWriteResult}.
     */
    public ReasoningLogWriteResult() {
    }

    /**
     * Constructs a new instance of {@link ReasoningLogWriteResult}.
     *
     * @param status the status
     * @param logFilePath the log file path
     * @param entrySequence the entry sequence
     */
    public ReasoningLogWriteResult(LogWriteStatus status, Path logFilePath, int entrySequence) {
        this.status = status;
        this.logFilePath = logFilePath;
        this.entrySequence = entrySequence;
    }

    /**
     * Constructs a new instance of {@link ReasoningLogWriteResult}.
     *
     * @param status the status
     * @param logFilePath the log file path
     * @param entrySequence the entry sequence
     * @param errorMessage the error message
     */
    public ReasoningLogWriteResult(LogWriteStatus status, Path logFilePath, int entrySequence, String errorMessage) {
        this.status = status;
        this.logFilePath = logFilePath;
        this.entrySequence = entrySequence;
        this.errorMessage = errorMessage;
    }

    
    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public LogWriteStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(LogWriteStatus status) {
        this.status = status;
    }

    /**
     * Gets the log file path.
     *
     * @return the resolved or constructed object
     */
    public Path getLogFilePath() {
        return logFilePath;
    }

    /**
     * Sets the log file path.
     *
     * @param logFilePath the log file path
     */
    public void setLogFilePath(Path logFilePath) {
        this.logFilePath = logFilePath;
    }

    /**
     * Gets the entry sequence.
     *
     * @return the numeric value
     */
    public int getEntrySequence() {
        return entrySequence;
    }

    /**
     * Sets the entry sequence.
     *
     * @param entrySequence the entry sequence
     */
    public void setEntrySequence(int entrySequence) {
        this.entrySequence = entrySequence;
    }

    /**
     * Gets the error message.
     *
     * @return the string result
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if the component is success.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSuccess() {
        return status == LogWriteStatus.SUCCESS;
    }
}
