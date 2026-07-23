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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

 
public class ReasoningLogAssertions {
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "timestamp: ([^\n]+)\n" +
            "direction: ([^\n]+)\n" +
            "role: ([^\n]+)\n" +
            "sequence: (\\d+)\n" +
            "---\n" +
            "([\\s\\S]*?)(?=\n\ntimestamp:|$)",
            Pattern.MULTILINE | Pattern.DOTALL
    );

     
    public static void assertLogFileExists(Path logFilePath) {
        assertTrue(Files.exists(logFilePath), 
                "Log file should exist at: " + logFilePath);
    }

     
    public static void assertLogFileDoesNotExist(Path logFilePath) {
        assertFalse(Files.exists(logFilePath),
                "Log file should not exist at: " + logFilePath);
    }

     
    public static void assertEntryCount(Path logFilePath, int expectedCount) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        assertEquals(expectedCount, entries.size(),
                "Log file should contain " + expectedCount + " entries");
    }

     
    public static void assertEntryOrder(Path logFilePath, String... expectedDirections) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        
        assertEquals(expectedDirections.length, entries.size(),
                "Entry count mismatch");
        
        for (int i = 0; i < expectedDirections.length; i++) {
            assertEquals(expectedDirections[i], entries.get(i).direction,
                    "Entry " + (i + 1) + " should have direction: " + expectedDirections[i]);
        }
    }

     
    public static void assertEntryExists(Path logFilePath, String direction, String role) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        
        boolean found = entries.stream()
                .anyMatch(e -> e.direction.equals(direction) && e.role.equals(role));
        
        assertTrue(found,
                "Log file should contain entry with direction=" + direction + " and role=" + role);
    }

     
    public static void assertEntryBodyContains(Path logFilePath, int entryIndex, String expectedText) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        
        assertTrue(entryIndex >= 0 && entryIndex < entries.size(),
                "Entry index out of bounds");
        
        assertTrue(entries.get(entryIndex).body.contains(expectedText),
                "Entry " + entryIndex + " body should contain: " + expectedText);
    }

     
    public static void assertFirstEntryIsOutbound(Path logFilePath) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        
        assertFalse(entries.isEmpty(), "Log file should not be empty");
        assertEquals("OUTBOUND", entries.get(0).direction,
                "First entry should be OUTBOUND (initial context)");
    }

     
    public static void assertSequencesAreMonotonic(Path logFilePath) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        List<LogEntryData> entries = parseEntries(content);
        
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(i + 1, entries.get(i).sequence,
                    "Sequence at index " + i + " should be " + (i + 1));
        }
    }

     
    public static List<LogEntryData> getLogEntries(Path logFilePath) throws IOException {
        String content = Files.readString(logFilePath, StandardCharsets.UTF_8);
        return parseEntries(content);
    }

     
    private static List<LogEntryData> parseEntries(String content) {
        List<LogEntryData> entries = new ArrayList<>();
        Matcher matcher = ENTRY_PATTERN.matcher(content);

        while (matcher.find()) {
            LogEntryData entry = new LogEntryData();
            entry.timestamp = matcher.group(1);
            entry.direction = matcher.group(2);
            entry.role = matcher.group(3);
            entry.sequence = Integer.parseInt(matcher.group(4));
            entry.body = matcher.group(5).trim();
            entries.add(entry);
        }

        return entries;
    }

     
    public static class LogEntryData {
        public String timestamp;
        public String direction;
        public String role;
        public int sequence;
        public String body;

        @Override
        public String toString() {
            return "LogEntry{" +
                    "direction='" + direction + '\'' +
                    ", role='" + role + '\'' +
                    ", sequence=" + sequence +
                    ", body_length=" + body.length() +
                    '}';
        }
    }
}
