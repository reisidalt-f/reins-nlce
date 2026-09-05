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

package br.com.dizeno.reins.reasoning.logging;

import br.com.dizeno.reins.reasoning.FileReasoningLogService;
import br.com.dizeno.reins.reasoning.ReasoningCycleLog;
import br.com.dizeno.reins.reasoning.ReasoningLogEntry;
import br.com.dizeno.reins.reasoning.ReasoningLogWriteResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

 
public class FileReasoningLogServiceTest {
    private FileReasoningLogService logService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logService = new FileReasoningLogService();
    }

    @Test
    void testInitializeCycleLogCreatesDirectory() throws Exception {
        
        Path projectRoot = tempDir;
        assertFalse(Files.exists(projectRoot.resolve("reasoning-logs")));

        
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        assertTrue(Files.exists(projectRoot.resolve("reasoning-logs")));
        assertEquals(projectRoot.resolve("reasoning-logs"), cycleLog.getLogDirectory());
    }

    @Test
    void testInitializeCycleLogAllocatesTimestampFilename() throws Exception {
        
        Path projectRoot = tempDir;

        
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        String filename = cycleLog.getFileNameTimestamp();
        assertTrue(filename.matches("\\d{8}-\\d{6}-request\\.md"), 
                "Filename should match yyyyMMdd-HHmmss-request.md format");
    }

    @Test
    void testInitializeCycleLogWithSourcePath() throws Exception {
        Path projectRoot = tempDir;

        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot, "src/main/nl/domain/entities.md");

        String filename = cycleLog.getFileNameTimestamp();
        assertTrue(filename.matches("\\d{8}-\\d{6}-entities\\.md"), 
                "Filename should match yyyyMMdd-HHmmss-entities.md format");
        assertTrue(cycleLog.getLogFilePath().getFileName().toString().matches("\\d{8}-\\d{6}-entities\\.md\\.log"));
    }

    @Test
    void testWriteEntrySerializesFixedFields() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        ReasoningLogEntry entry = new ReasoningLogEntry();
        entry.setTimestamp(Instant.parse("2026-03-30T10:30:00Z"));
        entry.setDirection(ReasoningLogEntry.Direction.OUTBOUND);
        entry.setRole("user");
        entry.setSequence(1);
        entry.setBody("Hello, LLM!");

        ReasoningLogWriteResult result = logService.writeEntry(cycleLog, entry);

        
        assertTrue(result.isSuccess());
        
        
        String fileContent = Files.readString(cycleLog.getLogFilePath(), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("timestamp: 2026-03-30T10:30:00Z"));
        assertTrue(fileContent.contains("direction: OUTBOUND"));
        assertTrue(fileContent.contains("role: user"));
        assertTrue(fileContent.contains("sequence: 1"));
        assertTrue(fileContent.contains("---"));
        assertTrue(fileContent.contains("Hello, LLM!"));
    }

    @Test
    void testWriteEntryAppends() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        ReasoningLogEntry entry1 = new ReasoningLogEntry();
        entry1.setTimestamp(Instant.parse("2026-03-30T10:30:00Z"));
        entry1.setDirection(ReasoningLogEntry.Direction.OUTBOUND);
        entry1.setRole("user");
        entry1.setSequence(1);
        entry1.setBody("Message 1");

        ReasoningLogEntry entry2 = new ReasoningLogEntry();
        entry2.setTimestamp(Instant.parse("2026-03-30T10:30:01Z"));
        entry2.setDirection(ReasoningLogEntry.Direction.INBOUND);
        entry2.setRole("assistant");
        entry2.setSequence(2);
        entry2.setBody("Response 1");

        logService.writeEntry(cycleLog, entry1);
        logService.writeEntry(cycleLog, entry2);

        
        ReasoningLogAssertions.assertEntryOrder(cycleLog.getLogFilePath(), "OUTBOUND", "INBOUND");
        ReasoningLogAssertions.assertEntryCount(cycleLog.getLogFilePath(), 2);
    }

    @Test
    void testWriteEntryHandlesMultilineBody() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        String multilineBody = "Line 1\nLine 2\nLine 3";
        ReasoningLogEntry entry = new ReasoningLogEntry();
        entry.setTimestamp(Instant.now());
        entry.setDirection(ReasoningLogEntry.Direction.OUTBOUND);
        entry.setRole("user");
        entry.setSequence(1);
        entry.setBody(multilineBody);

        ReasoningLogWriteResult result = logService.writeEntry(cycleLog, entry);

        
        assertTrue(result.isSuccess());
        String fileContent = Files.readString(cycleLog.getLogFilePath(), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Line 1\nLine 2\nLine 3"));
    }

    @Test
    void testInitializeCycleLogHandlesCollisionByRetrying() throws Exception {
        // 1. Setup logs dir
        Path projectRoot = tempDir;
        Path logsDir = projectRoot.resolve("reasoning-logs");
        Files.createDirectories(logsDir);
        
        // Align to a second boundary to avoid sub-second rollover flakiness
        long timeToNextSecond = 1000 - (System.currentTimeMillis() % 1000);
        Thread.sleep(timeToNextSecond + 50); // wait until just after the second starts
        
        // 2. Create matching log file ahead of time
        String currentTimestamp = java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path existingFile = logsDir.resolve(currentTimestamp + "-request.md.log");
        Files.createFile(existingFile);

        // 3. Trigger initializeCycleLog (should collide and wait)
        long startTime = System.currentTimeMillis();
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-2", projectRoot);
        long endTime = System.currentTimeMillis();

        // 4. Assert wait and timestamp difference
        assertNotEquals(currentTimestamp + "-request.md", cycleLog.getFileNameTimestamp());
        assertTrue(endTime - startTime >= 1000, "Should have waited at least 1 second for collision");
    }

    @Test
    void testCloseCycleLogSetsClosedAt() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);
        assertNull(cycleLog.getClosedAt());

        
        logService.closeCycleLog(cycleLog);

        
        assertNotNull(cycleLog.getClosedAt());
    }

    @Test
    void testSequenceNumbersAreMonotonic() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        for (int i = 1; i <= 5; i++) {
            ReasoningLogEntry entry = new ReasoningLogEntry();
            entry.setTimestamp(Instant.now());
            entry.setDirection(i % 2 == 1 ? ReasoningLogEntry.Direction.OUTBOUND : ReasoningLogEntry.Direction.INBOUND);
            entry.setRole(i % 2 == 1 ? "user" : "assistant");
            entry.setSequence(i);
            entry.setBody("Message " + i);
            logService.writeEntry(cycleLog, entry);
        }

        
        ReasoningLogAssertions.assertSequencesAreMonotonic(cycleLog.getLogFilePath());
    }

    @Test
    void testFirstEntryCapturesToOutbound() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        ReasoningLogEntry contextEntry = new ReasoningLogEntry();
        contextEntry.setTimestamp(Instant.now());
        contextEntry.setDirection(ReasoningLogEntry.Direction.OUTBOUND);
        contextEntry.setRole("user");
        contextEntry.setSequence(1);
        contextEntry.setBody("Initial context message");

        logService.writeEntry(cycleLog, contextEntry);

        
        ReasoningLogAssertions.assertFirstEntryIsOutbound(cycleLog.getLogFilePath());
    }

    @Test
    void testWriteFailureReturnsFailedStatus() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);
        
        
        Path logFile = cycleLog.getLogFilePath();
        Files.write(logFile, "header\n".getBytes());
        logFile.toFile().setReadOnly();

        try {
            
            ReasoningLogEntry entry = new ReasoningLogEntry();
            entry.setTimestamp(Instant.now());
            entry.setDirection(ReasoningLogEntry.Direction.OUTBOUND);
            entry.setRole("user");
            entry.setSequence(1);
            entry.setBody("This should fail");

            ReasoningLogWriteResult result = logService.writeEntry(cycleLog, entry);

            
            assertFalse(result.isSuccess());
            assertEquals(ReasoningLogWriteResult.LogWriteStatus.FAILED, result.getStatus());
            assertNotNull(result.getErrorMessage());
        } finally {
            
            logFile.toFile().setWritable(true);
        }
    }

    @Test
    void testLogFilePathIsUnderInferenceLogsDirectory() throws Exception {
        
        Path projectRoot = tempDir;
        ReasoningCycleLog cycleLog = logService.initializeCycleLog("cycle-1", projectRoot);

        
        Path expectedDir = projectRoot.resolve("reasoning-logs");
        assertTrue(cycleLog.getLogFilePath().toString().startsWith(expectedDir.toString()),
                "Log file should be under reasoning-logs directory");
    }
}
