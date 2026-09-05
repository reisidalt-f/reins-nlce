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

package br.com.dizeno.reins.reasoning.tooling.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilePatchApplierTolerantPatchTest {

    private FilePatchApplier applier;

    @BeforeEach
    void setUp() {
        applier = new FilePatchApplier();
    }

    @Test
    void testStandardPatchAppliesSuccessfully() {
        String original = "line 1\nline 2\nline 3\n";
        String diff = "@@ -1,3 +1,3 @@\n line 1\n-line 2\n+line 2 modified\n line 3\n";

        String result = applier.apply(original, diff);

        assertEquals("line 1\nline 2 modified\nline 3\n", result);
    }

    @Test
    void testPatchWithIncorrectHeaderLineCountSucceeds() {
        String original = "public class Foo {\n    private int count_value;\n    public void run() {}\n}\n";
        // Header claims 4 lines (@@ -1,4 +1,4 @@) but body only has 3 lines (1 context, 1 minus, 1 plus)
        String diff = "@@ -1,4 +1,4 @@\n public class Foo {\n-    private int count_value;\n+    private int countValue;\n";

        String result = applier.apply(original, diff);

        assertTrue(result.contains("private int countValue;"));
        assertFalse(result.contains("count_value"));
    }

    @Test
    void testPatchWithWhitespaceVariancesInContextSucceeds() {
        String original = "public class Bar {\n        private String name_val;\n        public String getName() {\n            return name_val;\n        }\n}\n";
        // Diff context lines have 1 leading space instead of 8 leading spaces
        String diff = "@@ -2,4 +2,4 @@\n private String name_val;\n-        public String getName() {\n+        public String getNameValue() {\n             return name_val;\n";

        String result = applier.apply(original, diff);

        assertTrue(result.contains("public String getNameValue() {"));
    }
}
