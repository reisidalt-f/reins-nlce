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

package br.com.dizeno.reins.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathLogFormatterTest {

    @TempDir
    Path tempDir;

    @Test
    void testFormatAbsolutePathInsideProject() {
        Path projectRoot = tempDir;
        Path filePath = tempDir.resolve("src/main/nl/domain/note.md");
        
        String result = PathLogFormatter.formatPath(filePath, projectRoot);
        assertEquals("src/main/nl/domain/note.md", result);
    }

    @Test
    void testFormatRelativePathInsideProject() {
        Path projectRoot = tempDir;
        String pathStr = "src/main/nl/domain/note.md";
        
        String result = PathLogFormatter.formatPath(pathStr, projectRoot);
        assertEquals("src/main/nl/domain/note.md", result);
    }

    @Test
    void testFormatCanonicalPaths() {
        Path projectRoot = tempDir;
        
        // main: reference
        String resultMain = PathLogFormatter.formatPath("main:domain/note.md", projectRoot);
        assertEquals("src/main/nl/domain/note.md", resultMain);
        
        // test: reference
        String resultTest = PathLogFormatter.formatPath("test:domain/note_test.md", projectRoot);
        assertEquals("src/test/nl/domain/note_test.md", resultTest);
        
        // target: reference
        String resultTarget = PathLogFormatter.formatPath("target:src/main/java/App.java", projectRoot);
        assertEquals("src/main/java/App.java", resultTarget);
    }

    @Test
    void testFormatPathOutsideProject() {
        Path projectRoot = tempDir.resolve("project");
        Path filePath = tempDir.resolve("other/shared.md");
        
        String result = PathLogFormatter.formatPath(filePath, projectRoot);
        assertEquals("../other/shared.md", result);
    }

    @Test
    void testFormatStringPathOutsideProject() {
        Path projectRoot = tempDir.resolve("project");
        String pathStr = "../other/shared.md";
        
        String result = PathLogFormatter.formatPath(pathStr, projectRoot);
        assertEquals("../other/shared.md", result);
    }

    @Test
    void testFormatCanonicalList() {
        Path projectRoot = tempDir;
        String pathsList = "main:domain/note.md, test:domain/note_test.md";
        
        String result = PathLogFormatter.formatPath(pathsList, projectRoot);
        assertEquals("src/main/nl/domain/note.md,src/test/nl/domain/note_test.md", result);
    }

    @Test
    void testFormatEdgeCases() {
        Path projectRoot = tempDir;
        
        assertEquals("", PathLogFormatter.formatPath((Path) null, projectRoot));
        assertEquals("", PathLogFormatter.formatPath((File) null, projectRoot));
        assertEquals("", PathLogFormatter.formatPath((String) null, projectRoot));
        assertEquals("", PathLogFormatter.formatPath("   ", projectRoot));
    }
}
