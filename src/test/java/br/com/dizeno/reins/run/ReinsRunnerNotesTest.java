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

package br.com.dizeno.reins.run;

import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.run.config.ReinsConfig;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReinsRunnerNotesTest {

    @TempDir
    Path tempDir;

    private ReinsRunner runner;
    private ReinsConfig config;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        runner = new ReinsRunner();
        config = new ReinsConfig();
        log = new ReinsSystemOutLogger(true, true);

        // Create sample markdown source files in main and test source directories
        Path mainNl = tempDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        Files.writeString(mainNl.resolve("User.md"), "# User Domain");
        Files.writeString(mainNl.resolve("Order.md"), "# Order Domain");
    }

    @Test
    public void testAddListAndClearNotesForSpecificSource() throws Exception {
        File baseDir = tempDir.toFile();
        String source = "src/main/nl/User.md";

        // Add a note
        int count1 = runner.addNote(config, baseDir, source, "First note for User", ReasoningNote.Origin.CLI, log);
        assertEquals(1, count1);

        int count2 = runner.addNote(config, baseDir, source, "Second note for User", ReasoningNote.Origin.MAVEN_GOAL, log);
        assertEquals(2, count2);

        // List notes for specific source
        Map<String, List<ReasoningNote>> listed = runner.listNotes(config, baseDir, source, log);
        assertNotNull(listed);
        assertEquals(1, listed.size());
        String canonicalKey = listed.keySet().iterator().next();
        assertTrue(canonicalKey.endsWith("src/main/nl/User.md") || canonicalKey.equals("main:User.md") || canonicalKey.endsWith("User.md"));
        List<ReasoningNote> notes = listed.get(canonicalKey);
        assertEquals(2, notes.size());
        assertEquals("First note for User", notes.get(0).getText());
        assertEquals("Second note for User", notes.get(1).getText());

        // Clear notes for specific source
        int cleared = runner.clearNotes(config, baseDir, source, log);
        assertEquals(2, cleared);

        // Verify notes are cleared
        Map<String, List<ReasoningNote>> listedAfterClear = runner.listNotes(config, baseDir, source, log);
        assertTrue(listedAfterClear.isEmpty());
    }

    @Test
    public void testAddListAndClearNotesForAllScannedSources() throws Exception {
        File baseDir = tempDir.toFile();
        String source1 = "src/main/nl/User.md";
        String source2 = "src/main/nl/Order.md";

        runner.addNote(config, baseDir, source1, "Note User", ReasoningNote.Origin.CLI, log);
        runner.addNote(config, baseDir, source2, "Note Order", ReasoningNote.Origin.CLI, log);

        // List notes across all scanned sources (source = null)
        Map<String, List<ReasoningNote>> allListed = runner.listNotes(config, baseDir, null, log);
        assertNotNull(allListed);
        assertEquals(2, allListed.size());

        // Clear notes across all scanned sources (source = null)
        int totalCleared = runner.clearNotes(config, baseDir, null, log);
        assertEquals(2, totalCleared);

        // Verify notes are cleared
        Map<String, List<ReasoningNote>> listedAfterClear = runner.listNotes(config, baseDir, null, log);
        assertTrue(listedAfterClear.isEmpty());
    }
}
