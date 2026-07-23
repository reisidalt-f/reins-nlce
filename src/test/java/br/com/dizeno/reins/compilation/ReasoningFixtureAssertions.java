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

package br.com.dizeno.reins.compilation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReasoningFixtureAssertions {
    private ReasoningFixtureAssertions() {
    }

    static void assertCompiledFile(Path path) throws IOException {
        assertTrue(Files.exists(path), "Expected compiled file to exist: " + path);
        assertFalse(Files.readString(path).isBlank(), "Expected compiled file to be non-empty: " + path);
    }

    static void assertCompiledContentEquals(Path path, String expected) throws IOException {
        assertCompiledFile(path);
        assertEquals(expected.strip(), Files.readString(path).strip());
    }
}