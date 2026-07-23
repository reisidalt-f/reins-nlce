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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPhaseListIT {

    @TempDir
    Path tempDir;

    @Test
    void customPhaseListIncludesAdditionalPhaseInExpectedPosition() throws Exception {
        Files.writeString(tempDir.resolve("phase-list.ftl"), "system-context\nvalidation\nproject-context");
        Files.writeString(tempDir.resolve("validation.ftl"), "VALIDATION_PHASE");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile());
        registry.validateAll();

        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);
        List<String> phases = evaluator.evaluate("phase-list.ftl", ReasoningScriptContext.builder().build())
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        PhaseListValidator.validate(phases, evaluator);

        assertEquals(List.of("system-context", "validation", "project-context"), phases);
        assertTrue(evaluator.evaluate("validation.ftl", ReasoningScriptContext.builder().build()).contains("VALIDATION_PHASE"));
    }
}
