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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.ToolOperationsReference;
import br.com.dizeno.reins.reasoning.ToolResultFormatter;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScriptBehaviorIT {

        @TempDir
        Path tempDir;

    @Test
        void systemContextScriptRendersExpectedStructuredSections() throws Exception {
        ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

        ReasoningScriptContext context = contextForPrompts("Implement feature", "root.md");

        String scriptOutput = evaluator.evaluate("system-context.ftl", context);
                String normalized = normalize(scriptOutput);
                org.junit.jupiter.api.Assertions.assertTrue(normalized.contains("MANDATORY RESPONSE FORMAT"));
                org.junit.jupiter.api.Assertions.assertTrue(normalized.contains("PROHIBITED"));
                org.junit.jupiter.api.Assertions.assertFalse(normalized.contains("compatibility fallback"));
    }

    @Test
        void maxTurnGraceScriptRendersExpectedInstructions() throws Exception {
        ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

        ReasoningScriptContext context = contextForPrompts("Implement feature", "root.md\n├── a.md");

        String scriptOutput = evaluator.evaluate("max-turn-grace-prompt.ftl", context);
                String normalized = normalize(scriptOutput);
                org.junit.jupiter.api.Assertions.assertTrue(normalized.contains("The reasoning cycle reached the configured max turns"));
                org.junit.jupiter.api.Assertions.assertFalse(normalized.contains("compatibility fallback"));
    }

            @Test
            void contextBuildSourceNotesScriptRendersTrackingNotes() throws Exception {
                ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

                ReasoningScriptContext context = ReasoningScriptContext.builder()
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
                        .cycle(new ReasoningScriptViews.CycleView("cycle-1", 1, 5, "IN_PROGRESS", null))
                        .policy(new ReasoningScriptViews.PolicyView(
                                List.of("main", "test", "target"),
                                List.of("main", "test", "target"),
                                List.of("target"),
                                List.of("target"),
                                List.of("target"),
                                List.of("main", "test", "target"),
                                false,
                                ToolOperationsReference.build(FilePolicy.allPermissive(), false, true, true, false)))
                        .tracking(new ReasoningScriptViews.TrackingView(
                                "hash",
                                List.of(),
                                "success",
                                "2026-05-13T21:00:00Z",
                                List.of("Review compiled output paths before continuing.")))
                        .referenceTree("root.md")
                        .build();

                String scriptOutput = evaluator.evaluate("context-build.ftl", context, java.util.Map.of("step", "source-notes"));
                String normalized = normalize(scriptOutput);

                org.junit.jupiter.api.Assertions.assertTrue(normalized.contains("Corrective notes for this source:"));
                org.junit.jupiter.api.Assertions.assertTrue(normalized.contains("Review compiled output paths before continuing."));
            }

    @Test
    void mcpResultScriptMatchesLegacyFormatterOutput() throws Exception {
        ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                "main:src/main/nl/source.md",
                "ok");
        result.setResolvedBase("main");
        result.setResolvedPath("/tmp/src/main/nl/source.md");
        result.setStarted(true);
        result.setStdout("stdout");
        result.setStderr("stderr");
        result.setContent("file-content");
        result.setListedPaths(List.of("main:a.md", "main:b.md"));

        ReasoningScriptContext context = contextForPrompts("Implement feature", "root.md");
        context = ReasoningScriptContext.builder()
                .source(context.getSource())
                .fileBases(context.getFileBases())
                .inference(context.getInference())
                .attachments(context.getAttachments())
                .config(context.getConfig())
                .cycle(context.getCycle())
                .policy(context.getPolicy())
                .referenceTree(context.getReferenceTree())
                .currentToolResult(toView(result))
                .build();

        String scriptOutput = evaluator.evaluate("tool-result.ftl", context);
        String legacyOutput = new ToolResultFormatter().format(result);

        assertEquals(normalize(legacyOutput), normalize(scriptOutput));
    }

        @Test
        void inferencePipelineDefaultCycleUsesValidateProcessingStatusOnFirstTurn() throws Exception {
                ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

                ReasoningScriptContext base = contextForPrompts("Implement feature", "root.md");
                ReasoningScriptContext context = ReasoningScriptContext.builder()
                                .source(base.getSource())
                                .fileBases(base.getFileBases())
                                .inference(base.getInference())
                                .attachments(base.getAttachments())
                                .config(base.getConfig())
                                .cycle(base.getCycle())
                                .policy(base.getPolicy())
                                .referenceTree(base.getReferenceTree())
                                .pipeline(new ReasoningScriptViews.PipelineView(
                                                "default-cycle",
                                                0,
                                                1,
                                                List.of("default-cycle"),
                                                "VALIDATE",
                                                null,
                                                null,
                                                null,
                                                null,
                                                false))
                                .build();

                String scriptOutput = evaluator.evaluatePhase("reasoning-pipeline.ftl", "default-cycle", context, java.util.Map.of());
                String normalized = normalize(scriptOutput);

                assertTrue(normalized.contains("Revalidate the files previously compiled from the main source markdown main:source.md"));
                assertTrue(normalized.contains("Do not perform a full recompilation when patching is sufficient."));
        }

        @Test
        void inferencePipelineDefaultCycleDoesNotDoublePrefixCanonicalSourcePath() throws Exception {
                ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

                ReasoningScriptContext base = contextForPrompts("Implement feature", "root.md");
                ReasoningScriptContext context = ReasoningScriptContext.builder()
                                .source(new ReasoningScriptViews.SourceView(
                                                "main:com/example/design.md",
                                                "/tmp/source.md",
                                                "# source",
                                                "hash",
                                                "main",
                                                List.of()))
                                .fileBases(base.getFileBases())
                                .inference(base.getInference())
                                .attachments(base.getAttachments())
                                .config(base.getConfig())
                                .cycle(base.getCycle())
                                .policy(base.getPolicy())
                                .referenceTree(base.getReferenceTree())
                                .pipeline(new ReasoningScriptViews.PipelineView(
                                                "default-cycle",
                                                0,
                                                1,
                                                List.of("default-cycle"),
                                                "COMPILE",
                                                null,
                                                null,
                                                null,
                                                null,
                                                false))
                                .build();

                String scriptOutput = evaluator.evaluatePhase("reasoning-pipeline.ftl", "default-cycle", context, java.util.Map.of());
                String normalized = normalize(scriptOutput);

                assertTrue(normalized.contains("Compile the main source file:\nmain:com/example/design.md"));
                assertTrue(normalized.contains("main:com/example/design.md"));
                org.junit.jupiter.api.Assertions.assertFalse(normalized.contains("main:main:"));
        }

        @Test
        void nonBlankCustomOverrideStillWins() throws Exception {
                Files.writeString(tempDir.resolve("max-turn-grace-prompt.ftl"), "CUSTOM_NON_BLANK_OVERRIDE");

                ScriptEvaluator evaluator = new ScriptEvaluator(
                                ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null), tempDir.toFile()),
                                null);

                String scriptOutput = evaluator.evaluate("max-turn-grace-prompt.ftl", contextForPrompts("Implement feature", "root.md"));
                assertEquals("CUSTOM_NON_BLANK_OVERRIDE", normalize(scriptOutput));
        }

    private ReasoningScriptContext contextForPrompts(String message, String referenceTree) {
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
                message,
                List.of(),
                List.of(),
                List.of(),
                List.of());

        ReasoningScriptViews.ConfigView config = new ReasoningScriptViews.ConfigView(
                "gemini-2.5-pro",
                5,
                true,
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
                List.of("main", "test", "target"),
                List.of("main", "test", "target"),
                List.of("target"),
                List.of("target"),
                List.of("target"),
                List.of("main", "test", "target"),
                false,
                ToolOperationsReference.build(FilePolicy.allPermissive(), false, true, true, false));

        return ReasoningScriptContext.builder()
                .source(source)
                .fileBases(fileBases)
                .inference(inference)
                .attachments(List.of())
                .config(config)
                .cycle(cycle)
                .policy(policy)
                .referenceTree(referenceTree)
                .build();
    }

        private ReasoningScriptViews.ToolResultView toView(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result) {
        boolean readFileSuccess = result.getOperation() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE
                && result.getStatus() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS;

        return new ReasoningScriptViews.ToolResultView(
                result.getStatus().name(),
                result.getOperation().name(),
                result.getQualifiedPath(),
                result.getResolvedBase(),
                result.getFailureReason(),
                result.getPolicyCode(),
                result.getResolvedPath(),
                result.getExitCode(),
                result.isStarted(),
                result.isTruncated(),
                result.getStdout(),
                result.getStderr(),
                result.getContent(),
                readFileSuccess,
                result.getListedPaths(),
                List.of(),
                List.of(),
                result.getExcludedPaths(),
                result.getExclusionReason());
    }

        private String normalize(String value) {
                return value.replace("\r\n", "\n").trim();
    }
}
