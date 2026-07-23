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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

 
/**
 * ReasoningCycleLog is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing inference cycle log.
 */
public class ReasoningCycleLog {
    private String cycleId;
    private Path logDirectory;
    private String fileNameTimestamp;
    private Path logFilePath;
    private Instant createdAt;
    private Instant closedAt;
    private int entryCount;
    private List<LogEntryRecord> entries;

    /**
     * Constructs a new instance of {@link ReasoningCycleLog}.
     */
    public ReasoningCycleLog() {
        this.entries = new ArrayList<>();
        this.entryCount = 0;
    }

    
    /**
     * Gets the cycle id.
     *
     * @return the string result
     */
    public String getCycleId() {
        return cycleId;
    }

    /**
     * Sets the cycle id.
     *
     * @param cycleId the cycle id
     */
    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    /**
     * Gets the log directory.
     *
     * @return the resolved or constructed object
     */
    public Path getLogDirectory() {
        return logDirectory;
    }

    /**
     * Sets the log directory.
     *
     * @param logDirectory the log directory
     */
    public void setLogDirectory(Path logDirectory) {
        this.logDirectory = logDirectory;
    }

    /**
     * Gets the file name timestamp.
     *
     * @return the string result
     */
    public String getFileNameTimestamp() {
        return fileNameTimestamp;
    }

    /**
     * Sets the file name timestamp.
     *
     * @param fileNameTimestamp the file name timestamp
     */
    public void setFileNameTimestamp(String fileNameTimestamp) {
        this.fileNameTimestamp = fileNameTimestamp;
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
     * Gets the created at.
     *
     * @return the resolved or constructed object
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the created at
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the closed at.
     *
     * @return the resolved or constructed object
     */
    public Instant getClosedAt() {
        return closedAt;
    }

    /**
     * Sets the closed at.
     *
     * @param closedAt the closed at
     */
    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    /**
     * Gets the entry count.
     *
     * @return the numeric value
     */
    public int getEntryCount() {
        return entryCount;
    }

    /**
     * Sets the entry count.
     *
     * @param entryCount the entry count
     */
    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    /**
     * Gets the entries.
     *
     * @return the collection of elements
     */
    public List<LogEntryRecord> getEntries() {
        return entries;
    }

    /**
     * Sets the entries.
     *
     * @param entries the entries
     */
    public void setEntries(List<LogEntryRecord> entries) {
        this.entries = entries;
    }

    /**
     * Add Entry.
     *
     * @param entry the entry
     */
    public void addEntry(LogEntryRecord entry) {
        this.entries.add(entry);
        this.entryCount++;
    }

     
    /**
     * LogEntryRecord is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public static class LogEntryRecord {
        private String timestamp;
        private String direction;
        private String role;
        private int sequence;

        /**
         * Constructs a new instance of {@link LogEntryRecord}.
         *
         * @param timestamp the timestamp
         * @param direction the direction
         * @param role the role
         * @param sequence the sequence
         */
        public LogEntryRecord(String timestamp, String direction, String role, int sequence) {
            this.timestamp = timestamp;
            this.direction = direction;
            this.role = role;
            this.sequence = sequence;
        }

        /**
         * Gets the timestamp.
         *
         * @return the string result
         */
        public String getTimestamp() {
            return timestamp;
        }

        /**
         * Gets the direction.
         *
         * @return the string result
         */
        public String getDirection() {
            return direction;
        }

        /**
         * Gets the role.
         *
         * @return the string result
         */
        public String getRole() {
            return role;
        }

        /**
         * Gets the sequence.
         *
         * @return the numeric value
         */
        public int getSequence() {
            return sequence;
        }
    }
}
