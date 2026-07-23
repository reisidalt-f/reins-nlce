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

class DynamicPhaseSequenceIT {

    @TempDir
    Path tempDir;

    @Test
    void differentCustomPhaseListsPreserveDifferentExecutionOrders() throws Exception {
        Path first = tempDir.resolve("first");
        Path second = tempDir.resolve("second");
        Files.createDirectories(first);
        Files.createDirectories(second);

        Files.writeString(first.resolve("phase-list.ftl"), "system-context\nreference-tree\nproject-context");

        Files.writeString(second.resolve("phase-list.ftl"), "system-context\nproject-context\nreference-tree");

        ScriptEvaluator firstEvaluator = new ScriptEvaluator(
                ScriptRegistry.build(new ScriptResolver(first.toFile(), null), first.toFile()),
                null);
        ScriptEvaluator secondEvaluator = new ScriptEvaluator(
                ScriptRegistry.build(new ScriptResolver(second.toFile(), null), second.toFile()),
                null);

        List<String> firstPhases = firstEvaluator.evaluate("phase-list.ftl", ReasoningScriptContext.builder().build())
                .lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        List<String> secondPhases = secondEvaluator.evaluate("phase-list.ftl", ReasoningScriptContext.builder().build())
                .lines().map(String::trim).filter(line -> !line.isEmpty()).toList();

        PhaseListValidator.validate(firstPhases, firstEvaluator);
        PhaseListValidator.validate(secondPhases, secondEvaluator);

                                assertEquals(List.of("system-context", "reference-tree", "project-context"), firstPhases);
                                assertEquals(List.of("system-context", "project-context", "reference-tree"), secondPhases);
    }
}
