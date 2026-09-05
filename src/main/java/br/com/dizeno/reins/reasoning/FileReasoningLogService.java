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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import br.com.dizeno.reins.reasoning.inference.llm.logging.ModelRequestResponseLogger;

 
/**
 * FileReasoningLogService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class FileReasoningLogService implements ReasoningLogService {
    private static final String REASONING_LOGS_DIR = "reasoning-logs";
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String LOG_EXTENSION = ".log";
    private static final String FIELD_SEPARATOR = ": ";
    private static final String ENTRY_SEPARATOR = "---";

    /**
     * Initializes the component cycle log.
     *
     * @param cycleId the cycle id
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    @Override
    public ReasoningCycleLog initializeCycleLog(String cycleId, Path projectRoot) throws Exception {
        return initializeCycleLog(cycleId, projectRoot, null);
    }

    /**
     * Initializes the component cycle log with source path.
     *
     * @param cycleId the cycle id
     * @param projectRoot the root path of the project
     * @param sourcePath the current compilation source path
     * @return the resolved or constructed object
     */
    @Override
    public ReasoningCycleLog initializeCycleLog(String cycleId, Path projectRoot, String sourcePath) throws Exception {
        Path logDirectory = projectRoot.resolve(REASONING_LOGS_DIR);
        
        
        if (!Files.exists(logDirectory)) {
            Files.createDirectories(logDirectory);
        }

        ReasoningCycleLog cycleLog = new ReasoningCycleLog();
        cycleLog.setCycleId(cycleId);
        cycleLog.setLogDirectory(logDirectory);
        cycleLog.setCreatedAt(Instant.now());

        
        String filenameTimestamp = allocateLogFilename(logDirectory, sourcePath);
        cycleLog.setFileNameTimestamp(filenameTimestamp);

        Path logFilePath = logDirectory.resolve(filenameTimestamp + LOG_EXTENSION);
        cycleLog.setLogFilePath(logFilePath);

        return cycleLog;
    }

    /**
     * Write Entry.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param entry the entry
     * @return the resulting result
     */
    @Override
    public ReasoningLogWriteResult writeEntry(ReasoningCycleLog cycleLog, ReasoningLogEntry entry) {
        try {
            Path logFilePath = cycleLog.getLogFilePath();
            
            
            String entryText = serializeEntry(entry);
            
            
            Files.writeString(
                    logFilePath,
                    entryText,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            
            ReasoningCycleLog.LogEntryRecord record = new ReasoningCycleLog.LogEntryRecord(
                    entry.getTimestamp().toString(),
                    entry.getDirection().name(),
                    entry.getRole(),
                    entry.getSequence()
            );
            cycleLog.addEntry(record);

            return new ReasoningLogWriteResult(
                    ReasoningLogWriteResult.LogWriteStatus.SUCCESS,
                    logFilePath,
                    entry.getSequence()
            );
        } catch (Exception e) {
            return new ReasoningLogWriteResult(
                    ReasoningLogWriteResult.LogWriteStatus.FAILED,
                    cycleLog.getLogFilePath(),
                    entry.getSequence(),
                    e.getMessage()
            );
        }
    }

    /**
     * Close Cycle Log.
     *
     * @param cycleLog the reasoning cycle log instance
     */
    @Override
    public void closeCycleLog(ReasoningCycleLog cycleLog) {
        cycleLog.setClosedAt(Instant.now());
    }

     
    private String allocateLogFilename(Path logDirectory, String sourcePath) throws InterruptedException {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        String timestamp = FILENAME_FORMATTER.format(now);
        String fileNameBeingProcessed = ModelRequestResponseLogger.extractFileNameBeingProcessed(sourcePath);
        String filename = timestamp + "-" + fileNameBeingProcessed;
        Path fullPath = logDirectory.resolve(filename + LOG_EXTENSION);

        
        if (!Files.exists(fullPath)) {
            return filename;
        }

        
        Thread.sleep(1000);
        return allocateLogFilename(logDirectory, sourcePath);
    }

     
    private String serializeEntry(ReasoningLogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp").append(FIELD_SEPARATOR).append(entry.getTimestamp()).append("\n");
        sb.append("direction").append(FIELD_SEPARATOR).append(entry.getDirection().name()).append("\n");
        sb.append("role").append(FIELD_SEPARATOR).append(entry.getRole()).append("\n");
        sb.append("sequence").append(FIELD_SEPARATOR).append(entry.getSequence()).append("\n");
        sb.append(ENTRY_SEPARATOR).append("\n");
        sb.append(entry.getBody()).append("\n");
        sb.append("\n");

        return sb.toString();
    }
}
