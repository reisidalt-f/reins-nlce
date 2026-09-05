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

import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReinsLibraryNotesTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path mainNl = tempDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        Files.writeString(mainNl.resolve("Product.md"), "# Product Spec");
    }

    @Test
    public void testLibraryListAndClearNotes() throws Exception {
        File baseDir = tempDir.toFile();
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "stub");

        String source = "src/main/nl/Product.md";

        int added = ReinsLibrary.addNote(baseDir, config, source, "Library Note", ReasoningNote.Origin.LIBRARY);
        assertEquals(1, added);

        Map<String, List<ReasoningNote>> listed = ReinsLibrary.listNotes(baseDir, config, null);
        assertNotNull(listed);
        assertEquals(1, listed.size());

        int cleared = ReinsLibrary.clearNotes(baseDir, config, null);
        assertEquals(1, cleared);

        Map<String, List<ReasoningNote>> listedEmpty = ReinsLibrary.listNotes(baseDir, config, source);
        assertTrue(listedEmpty.isEmpty());
    }
}
