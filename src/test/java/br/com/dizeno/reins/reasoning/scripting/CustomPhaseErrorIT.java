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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPhaseErrorIT {

    @TempDir
    Path tempDir;

    @Test
    void unknownPhaseInCustomPhaseListFailsBeforeInference() throws Exception {
        Files.writeString(tempDir.resolve("phase-list.ftl"), "system-context\nunknown-phase\nproject-context");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile());
        registry.validateAll();

        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);
        List<String> phases = evaluator.evaluate("phase-list.ftl", ReasoningScriptContext.builder().build())
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> PhaseListValidator.validate(phases, evaluator));

        assertTrue(error.getMessage().contains("Unknown phase name in custom phase list: unknown-phase"));
    }
}
