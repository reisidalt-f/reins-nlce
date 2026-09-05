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

class PartialCustomOverrideIT {

    @TempDir
    Path tempDir;

    @Test
    void onlySpecifiedCustomScriptOverridesAndOthersRemainBundled() throws Exception {
        Files.writeString(tempDir.resolve("retry-message.ftl"), "CUSTOM_RETRY_MESSAGE");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile());
        registry.validateAll();
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

        String retryMessage = evaluator.evaluate("retry-message.ftl", context());
        String toolResult = evaluator.evaluate("tool-result.ftl", contextWithResult());

        assertEquals("CUSTOM_RETRY_MESSAGE", retryMessage.trim());
        assertTrue(toolResult.contains("SUCCESS READ_FILE"));
        assertTrue(toolResult.contains("status: SUCCESS") || toolResult.contains("SUCCESS"));
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
                .config(new ReasoningScriptViews.ConfigView(new br.com.dizeno.reins.run.config.ReinsConfig()))
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

    private ReasoningScriptContext contextWithResult() {
        return ReasoningScriptContext.builder()
                .source(context().getSource())
                .fileBases(context().getFileBases())
                .inference(context().getInference())
                .config(context().getConfig())
                .cycle(context().getCycle())
                .policy(context().getPolicy())
                .referenceTree(context().getReferenceTree())
                .currentToolResult(new ReasoningScriptViews.ToolResultView(
                        "SUCCESS",
                        "READ_FILE",
                        "main:src/main/nl/source.md",
                        "main",
                        null,
                        null,
                        "/tmp/src/main/nl/source.md",
                        null,
                        true,
                        false,
                        "",
                        "",
                        "",
                        true,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        null))
                .build();
    }
}
