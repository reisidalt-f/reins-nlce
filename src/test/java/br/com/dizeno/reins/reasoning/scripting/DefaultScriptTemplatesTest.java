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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScriptTemplatesTest {

    @Test
    void loadsAllRequiredDefaultScripts() {
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());
        assertTrue(registry.isReady());
        assertEquals(11, registry.getAllScripts().size());
        assertEquals(10, ScriptRegistry.requiredScriptCountForCycle(false));
        assertEquals(4, ScriptRegistry.requiredScriptCountForCycle(true));
    }

    @Test
    void rendersAllDefaultScriptsWithStructuredOutput() throws Exception {
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

        ReasoningScriptContext context = buildContext();

        for (String scriptName : registry.getAllScripts().keySet()) {
            String rendered = evaluator.evaluate(scriptName, context);
            assertNotNull(rendered, "Rendered output should not be null for " + scriptName);
            assertFalse(rendered.isBlank(), "Rendered output should not be blank for " + scriptName);
        }

        String phaseList = evaluator.evaluate("phase-list.ftl", context);
        List<String> phaseNames = phaseList.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
        assertTrue(phaseNames.contains("system-context"));
        assertTrue(phaseNames.contains("tool-result"));

        String toolResult = evaluator.evaluate("tool-result.ftl", context);
        assertTrue(toolResult.contains("MCP_RESULT"));
        assertTrue(toolResult.contains("status: SUCCESS"));

        String attachmentList = evaluator.evaluate("attachment-list.ftl", context);
        assertTrue(attachmentList.contains("main:src/main/nl/source.md"));

        String fileList = evaluator.evaluate("file-list.ftl", context);
        assertTrue(fileList.contains("src/main/java/Compiled.java"));
    }

    private ReasoningScriptContext buildContext() {
        ReasoningScriptViews.SourceView source = new ReasoningScriptViews.SourceView(
                "src/main/nl/source.md",
                "/tmp/source.md",
                "# source",
                "hash",
                "main",
                List.of());

        ReasoningScriptViews.FileBasesView fileBases = new ReasoningScriptViews.FileBasesView(
                "/tmp/main",
                "/tmp/test",
                "/tmp/target",
                "/tmp");

        ReasoningScriptViews.InferenceStateView inference = new ReasoningScriptViews.InferenceStateView(
                "Implement feature",
                List.of(),
                List.of("src/main/java/Existing.java"),
                List.of("src/main/java/Compiled.java"),
                List.of());

        ReasoningScriptViews.AttachmentView attachment = new ReasoningScriptViews.AttachmentView(
                "main:src/main/nl/source.md",
                "content",
                "text/plain");

        ReasoningScriptViews.ConfigView config = new ReasoningScriptViews.ConfigView(
                "gemini-2.5-pro",
                5,
                false,
                false,
                false,
                true,
                false,
                null);

        ReasoningScriptViews.CycleView cycle = new ReasoningScriptViews.CycleView(
                "cycle-1",
                1,
                5,
                "IN_PROGRESS",
                null);

        ReasoningScriptViews.PolicyView policy = new ReasoningScriptViews.PolicyView(
                List.of("main", "test"),
                List.of("main", "test"),
                List.of("target"),
                List.of("target"),
                List.of("target"),
                List.of("main", "test"),
                false,
                ToolOperationsReference.build(FilePolicy.allPermissive(), false, true, true, false));

        ReasoningScriptViews.ToolResultView mcpResult = new ReasoningScriptViews.ToolResultView(
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
                "content",
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);

        return ReasoningScriptContext.builder()
                .source(source)
                .fileBases(fileBases)
                .inference(inference)
                .attachments(List.of(attachment))
                .config(config)
                .cycle(cycle)
                .policy(policy)
                .referenceTree("source.md\n├── ref.md")
                .currentToolResult(mcpResult)
                .build();
    }
}
