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

package br.com.dizeno.reins.reasoning.scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsReadyRegistryWithAllRequiredScripts() {
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());

        assertTrue(registry.isReady());
        assertTrue(registry.getValidationErrors().isEmpty());
        assertEquals(11, registry.getAllScripts().size());
        assertEquals(10, ScriptRegistry.requiredScriptsForCycle(false).size());
        assertEquals(4, ScriptRegistry.requiredScriptsForCycle(true).size());
        assertTrue(ScriptRegistry.requiredScriptsForCycle(false).containsKey("phase-list.ftl"));
        assertFalse(ScriptRegistry.requiredScriptsForCycle(false).containsKey("source-base-phase-list.ftl"));
        assertTrue(ScriptRegistry.requiredScriptsForCycle(true).containsKey("source-base-phase-list.ftl"));
        assertNotNull(registry.getScript("system-context.ftl"));
    }

    @Test
    void collectsValidationErrorWhenCustomScriptCannotBeParsed() throws IOException {
        Files.writeString(tempDir.resolve("system-context.ftl"), "${1?api");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile());

        assertFalse(registry.isReady());
        assertFalse(registry.getValidationErrors().isEmpty());
        assertTrue(registry.getValidationErrors().stream()
            .anyMatch(msg -> msg.contains("Syntax error in custom script system-context.ftl")));
        }

        @Test
        void reportsInvalidCustomScriptDirectory() {
        Path missingDir = tempDir.resolve("missing-custom-scripts");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(missingDir.toFile(), null), missingDir.toFile());

        assertFalse(registry.isReady());
        assertTrue(registry.getValidationErrors().stream()
            .anyMatch(msg -> msg.contains("Cannot resolve custom script directory: " + missingDir)));
    }
}
