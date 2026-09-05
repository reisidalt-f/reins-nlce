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

package br.com.dizeno.reins.run.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReinsCliNotesTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path mainNl = tempDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        Files.writeString(mainNl.resolve("Item.md"), "# Item Spec");
    }

    @Test
    public void testCliNotesCommands() {
        String source = tempDir.resolve("src/main/nl/Item.md").toString();

        // 1. Add Note via CLI
        int exitCodeAdd = ReinsCli.runCli(new String[]{"add-note", "--source", source, "--note", "CLI note content"});
        assertEquals(0, exitCodeAdd);

        // 2. List Notes with explicit source
        int exitCodeListSource = ReinsCli.runCli(new String[]{"list-notes", "--source", source});
        assertEquals(0, exitCodeListSource);

        // 3. List Notes scanning all sources
        int exitCodeListAll = ReinsCli.runCli(new String[]{"list-notes"});
        assertEquals(0, exitCodeListAll);

        // 4. Clear Notes scanning all sources
        int exitCodeClearAll = ReinsCli.runCli(new String[]{"clear-notes"});
        assertEquals(0, exitCodeClearAll);

        // 5. List Notes after clear
        int exitCodeListPostClear = ReinsCli.runCli(new String[]{"list-notes"});
        assertEquals(0, exitCodeListPostClear);
    }
}
