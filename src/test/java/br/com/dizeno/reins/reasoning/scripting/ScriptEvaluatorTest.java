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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.ToolOperationsReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptEvaluatorTest {

        @TempDir
        Path tempDir;

        @Test
        void throwsWhenScriptIsMissingFromRegistry() {
                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                ScriptEvaluationException ex = assertThrows(
                                ScriptEvaluationException.class,
                                () -> evaluator.evaluate("missing-script.ftl", minimalContext()));

                assertEquals("missing-script.ftl", ex.getScriptName());
                assertTrue(ex.getMessage().contains("Script not found"));
        }

        @Test
        void translatesTemplateEvaluationFailureToScriptEvaluationException() throws IOException {
                Files.writeString(tempDir.resolve("system-context.ftl"), "${1?api}");

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null));
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                ScriptEvaluationException ex = assertThrows(
                                ScriptEvaluationException.class,
                                () -> evaluator.evaluate("system-context.ftl", minimalContext()));

                assertEquals("system-context.ftl", ex.getScriptName());
                assertEquals(PhaseType.PROMPT_ASSEMBLY, ex.getPhaseType());
                assertTrue(ex.getResolvedLocation().endsWith("system-context.ftl"));
                assertTrue(ex.getMessage().contains("Failed to evaluate script"));
                assertTrue(ex.getCause() != null);
        }

        @Test
        void fallsBackToBundledSourceWhenCustomSourceRendersBlank() throws Exception {
                Files.writeString(tempDir.resolve("custom-classpath-only.ftl"), "   \n\t  ");

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), "custom-scripts"));
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                ScriptChainEvaluationResult result = evaluator.evaluateWithOutcome("custom-classpath-only.ftl",
                                minimalContext());

                assertEquals(ScriptTerminalReason.NON_BLANK_SELECTED, result.terminalReason());
                assertNotNull(result.diagnostics());
                assertEquals(ScriptSource.CUSTOM_CLASSPATH.name(), result.diagnostics().sourceIdentifier());
                assertEquals(1, result.diagnostics().sourceIndex());
                assertEquals(ScriptTerminalReason.NON_BLANK_SELECTED, result.diagnostics().terminalReason());
                assertTrue(result.renderedOutput() != null && !result.renderedOutput().isBlank());
        }

        @Test
        void reportsNoUsableOutputWhenAllCandidatesAreBlank() throws Exception {
                Files.writeString(tempDir.resolve("custom-all-blank.ftl"), " \n\t\n ");

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null));
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                ScriptEvaluationException ex = assertThrows(
                                ScriptEvaluationException.class,
                                () -> evaluator.evaluate("custom-all-blank.ftl", minimalContext()));

                assertEquals(ScriptFailureCategory.NO_USABLE_OUTPUT, ex.getFailureCategory());
                assertEquals(ScriptTerminalReason.NO_USABLE_OUTPUT, ex.getTerminalReason());
                assertEquals(ScriptSource.CUSTOM_FILESYSTEM.name(), ex.getSourceIdentifier());
                assertEquals(0, ex.getSourceIndex());
        }

        @Test
        void includeFailureIsFatalAndDoesNotFallback() throws Exception {
                Files.writeString(tempDir.resolve("max-turn-grace-prompt.ftl"), "<#include \"missing.ftl\">\nCUSTOM");

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null));
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                ScriptEvaluationException ex = assertThrows(
                                ScriptEvaluationException.class,
                                () -> evaluator.evaluate("max-turn-grace-prompt.ftl", minimalContext()));

                assertEquals(ScriptFailureCategory.INCLUDE_FAILURE, ex.getFailureCategory());
                assertEquals(ScriptTerminalReason.FATAL_ERROR, ex.getTerminalReason());
                assertEquals(ScriptSource.CUSTOM_FILESYSTEM.name(), ex.getSourceIdentifier());
                assertEquals(0, ex.getSourceIndex());
        }

        @Test
        void inferencePipelineDoesNotLockSourceAfterListPhases() throws Exception {
                Files.writeString(tempDir.resolve("reasoning-pipeline.ftl"),
                                "<#if phase == \"list-phases\">\n" +
                                                "custom-phase\n" +
                                                "<#elseif phase == \"custom-phase\">\n" +
                                                "INTENT: waiting-for-next-message\n" +
                                                "CONTENT_TYPE: gemini-message\n" +
                                                "\n" +
                                                "Custom phase output\n" +
                                                "</#if>");

                ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(tempDir.toFile(), null),
                                tempDir.toFile());
                ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

                String phaseList = evaluator.evaluatePhase("reasoning-pipeline.ftl", "list-phases", minimalContext(),
                                java.util.Map.of());
                assertEquals("custom-phase", phaseList.trim());

                String customPhaseOutput = evaluator.evaluatePhase("reasoning-pipeline.ftl", "custom-phase",
                                minimalContext(), java.util.Map.of());
                assertTrue(customPhaseOutput.contains("Custom phase output"));

                String unknownPhaseOutput = evaluator.evaluatePhase("reasoning-pipeline.ftl", "unknown-phase",
                                minimalContext(), java.util.Map.of());
                assertTrue(unknownPhaseOutput.contains("Unknown inference pipeline phase"));
        }

        @Test
        void inferencePipelineSwitchesBetweenDedicatedNotesAndFallbackGuidance() throws Exception {
                ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

                ReasoningScriptContext enabledContext = noteHandoffContext(true);
                String enabledOutput = normalize(evaluator.evaluatePhase("reasoning-pipeline.ftl", "default-cycle",
                                enabledContext, java.util.Map.of()));
                assertTrue(enabledOutput.contains("Use add_reasoning_note to capture corrective context"));
                assertFalse(enabledOutput.contains("finish the pipeline with intent `finish-error`"));

                ReasoningScriptContext disabledContext = noteHandoffContext(false);
                String disabledOutput = normalize(evaluator.evaluatePhase("reasoning-pipeline.ftl", "default-cycle",
                                disabledContext, java.util.Map.of()));
                assertTrue(disabledOutput.contains("Finish the pipeline with intent `finish-error`"));
                assertTrue(disabledOutput
                                .contains("Group any pending notes by canonical recipient key format `base:path`"));
                assertFalse(disabledOutput.contains("Use add_reasoning_note to capture corrective context"));
        }

        private ReasoningScriptContext minimalContext() {
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
                                                "context",
                                                List.of(),
                                                List.of(),
                                                List.of(),
                                                List.of()))
                                .build();
        }

        private ReasoningScriptContext noteHandoffContext(boolean addReasoningNotesEnabled) {
                ReinsConfig config = new ReinsConfig();
                config.getTooling().setMain("read");
                config.getTooling().setTest("read");
                config.getTooling().setTarget("read");
                config.getTooling().setAddReasoningNotes(addReasoningNotesEnabled);

                FilePolicy policy = new FilePolicy(config.getTooling());

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
                                                "context",
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
                                                addReasoningNotesEnabled,
                                                null))
                                .cycle(new ReasoningScriptViews.CycleView("cycle-1", 1, 5, "IN_PROGRESS", null))
                                .policy(new ReasoningScriptViews.PolicyView(
                                                List.of("main", "test"),
                                                List.of("main", "test"),
                                                List.of("target"),
                                                List.of("target"),
                                                List.of("target"),
                                                List.of("main", "test"),
                                                false,
                                                ToolOperationsReference.build(FilePolicy.allPermissive(), false, true,
                                                                true, false)))
                                .pipeline(new ReasoningScriptViews.PipelineView(
                                                "default-cycle",
                                                0,
                                                1,
                                                List.of("default-cycle"),
                                                "COMPILE",
                                                "continue",
                                                "gemini-message",
                                                "previous cycle",
                                                null,
                                                false))
                                .build();
        }

        private String normalize(String value) {
                return value == null ? null : value.replace("\r\n", "\n").trim();
        }
}
