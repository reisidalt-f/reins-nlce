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

package br.com.dizeno.reins.run.library;

import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.tracking.CleanupOutcomeSummary;
import br.com.dizeno.reins.run.ReinsSystemOutLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ReinsLibraryTest {

    @TempDir
    Path tempDir;

    @Test
    public void testProgrammaticCompileWithMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "stub");
        map.put("verbose", "true");

        File baseDir = tempDir.toFile();
        CompilationSummary summary = ReinsLibrary.compile(baseDir, map);
        assertNotNull(summary);
        assertEquals(0, summary.getProcessed());
    }

    @Test
    public void testProgrammaticCompileWithProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("provider", "stub");
        props.setProperty("verbose", "false");

        File baseDir = tempDir.toFile();
        CompilationSummary summary = ReinsLibrary.compile(baseDir, props);
        assertNotNull(summary);
        assertEquals(0, summary.getProcessed());
    }

    @Test
    public void testProgrammaticCleanWithMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "stub");

        File baseDir = tempDir.toFile();
        CleanupOutcomeSummary summary = ReinsLibrary.clean(baseDir, map);
        assertNotNull(summary);
        assertFalse(summary.hasFailures());
    }

    @Test
    public void testProgrammaticCleanWithProperties() throws Exception {
        Properties props = new Properties();
        props.setProperty("provider", "stub");

        File baseDir = tempDir.toFile();
        CleanupOutcomeSummary summary = ReinsLibrary.clean(baseDir, props);
        assertNotNull(summary);
        assertFalse(summary.hasFailures());
    }
}
