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

import br.com.dizeno.reins.reasoning.ToolOperationsReference;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomScriptOverrideIT {

    @TempDir
    Path tempDir;

    @Test
    void customSystemContextOverridesDefaultWhileOtherScriptsUseFallback() throws Exception {
        Files.writeString(tempDir.resolve("system-context.ftl"), "CUSTOM_SYSTEM_CONTEXT");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile());
        registry.validateAll();
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

        String systemContext = evaluator.evaluate("system-context.ftl", context());
        String maxTurnPrompt = evaluator.evaluate("max-turn-grace-prompt.ftl", context());

        assertEquals("CUSTOM_SYSTEM_CONTEXT", systemContext.trim());
        assertTrue(maxTurnPrompt.contains("The reasoning cycle reached the configured max turns"));
    }

    private ReasoningScriptContext context() {
        return ReasoningScriptContext.builder()
                .source(new ReasoningScriptViews.SourceView(
                        "src/main/nl/source.md",
                        "/tmp/source.md",
                        "# source",
                        "hash",
                        "main",
                        List.of()))
                .fileBases(new ReasoningScriptViews.FileBasesView(
                        "/tmp/main",
                        "/tmp/test",
                        "/tmp/target",
                        "/tmp"))
                .inference(new ReasoningScriptViews.InferenceStateView(
                        "Implement feature",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()))
                .config(new ReasoningScriptViews.ConfigView(
                        "gemini-2.5-pro",
                        5,
                        true,
                        false,
                        false,
                        true,
                        false,
                        null))
                .cycle(new ReasoningScriptViews.CycleView("c1", 1, 5, "IN_PROGRESS", null))
                .policy(new ReasoningScriptViews.PolicyView(
                        List.of("main", "test", "target"),
                        List.of("main", "test", "target"),
                        List.of("target"),
                        List.of("target"),
                        List.of("target"),
                        List.of("main", "test", "target"),
                        false,
                        ToolOperationsReference.build(FilePolicy.allPermissive(), false, true, true, false)))
                .referenceTree("root.md")
                .build();
    }
}
