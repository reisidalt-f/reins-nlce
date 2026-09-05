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
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ContextStepScriptResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;

import java.util.Collections;
import java.util.List;

/**
 * PromptAssemblyService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class PromptAssemblyService {
    private final ScriptContextAssembler contextAssembler;
    private final SafeTemplateExecutor templateExecutor;
    private final ScriptEvaluator scriptEvaluator;
    private final ContextStepScriptResolver contextStepScriptResolver;

    /**
     * Constructs a new instance of {@link PromptAssemblyService}.
     *
     * @param contextAssembler the context assembler
     * @param templateExecutor the template executor
     * @param scriptEvaluator the script evaluator instance
     * @param contextStepScriptResolver the context step script resolver
     */
    public PromptAssemblyService(ScriptContextAssembler contextAssembler,
                                 SafeTemplateExecutor templateExecutor,
                                 ScriptEvaluator scriptEvaluator,
                                 ContextStepScriptResolver contextStepScriptResolver) {
        this.contextAssembler = contextAssembler;
        this.templateExecutor = templateExecutor;
        this.scriptEvaluator = scriptEvaluator;
        this.contextStepScriptResolver = contextStepScriptResolver;
    }

    /**
     * Evaluate Context Message Step Script.
     *
     * @param step the step
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateContextMessageStepScript(String step,
                                                   ReasoningRequest request,
                                                   ReinsConfig config,
                                                   ReasoningCycle cycle,
                                                   FilePolicy policy,
                                                   boolean scriptRunnerEnabled,
                                                   String referenceTree,
                                                   List<String> inspectedPaths,
                                                   List<String> compiledPaths,
                                                   List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            throw new IllegalStateException("Unable to build script context for step '" + step + "'.");
        }

        String scriptName = contextStepScriptResolver.resolveScriptName(step);
        try {
            boolean scriptsEventsEnabled = config != null
                    && config.getLogging() != null
                    && config.getLogging().isScriptsEvents();
            String rendered = scriptEvaluator.evaluate(
                    scriptName,
                    baseContext,
                    Collections.singletonMap("step", step),
                    scriptsEventsEnabled);
            if (rendered == null || rendered.isBlank()) {
                throw new IllegalStateException("Script step '" + step + "' produced blank output.");
            }
            return rendered;
        } catch (ScriptEvaluationException ex) {
            throw new IllegalStateException("Failed to evaluate context step '" + step + "'.", ex);
        }
    }

    /**
     * Evaluate System Context Script.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateSystemContextScript(ReasoningRequest request,
                                              ReinsConfig config,
                                              ReasoningCycle cycle,
                                              FilePolicy policy,
                                              boolean scriptRunnerEnabled,
                                              String referenceTree,
                                              List<String> inspectedPaths,
                                              List<String> compiledPaths,
                                              List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            throw new IllegalStateException("Unable to build script context for system-context phase.");
        }
        return templateExecutor.evaluateScriptSafely("system-context.ftl", baseContext, config, "system-context");
    }

    /**
     * Builds the configured target first turn payload with scripts.
     *
     * @param nextMessage the next message
     * @param firstTurnReferenceTree the first turn reference tree
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param activePerSourcePhases the active per source phases
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String buildFirstTurnPayloadWithScripts(String nextMessage,
                                                   String firstTurnReferenceTree,
                                                   ReasoningRequest request,
                                                   ReinsConfig config,
                                                   ReasoningCycle cycle,
                                                   FilePolicy policy,
                                                   boolean scriptRunnerEnabled,
                                                   List<String> activePerSourcePhases,
                                                   List<String> inspectedPaths,
                                                   List<String> compiledPaths,
                                                   List<AttachedFilePayload> attachments) {
        if (activePerSourcePhases == null || activePerSourcePhases.isEmpty()) {
            throw new IllegalStateException("Per-source phase list is required and must not be empty.");
        }
        List<String> phases = activePerSourcePhases;

        String effectiveReferenceTree = firstTurnReferenceTree;
        StringBuilder assembledPrompt = new StringBuilder(nextMessage == null ? "" : nextMessage.trim());
        boolean assembledAny = nextMessage != null && !nextMessage.isBlank();

        for (String phaseName : phases) {
            if ("reference-tree".equals(phaseName)) {
                String renderedReferenceTree = evaluateReferenceTreeScript(
                        request,
                        config,
                        cycle,
                        policy,
                        scriptRunnerEnabled,
                        effectiveReferenceTree,
                        inspectedPaths,
                        compiledPaths,
                        attachments);
                if (renderedReferenceTree != null) {
                    effectiveReferenceTree = renderedReferenceTree;
                }
                continue;
            }

            if ("project-context".equals(phaseName)) {
                String projectContext = evaluateProjectContextScript(
                        request,
                        config,
                        cycle,
                        policy,
                        scriptRunnerEnabled,
                        effectiveReferenceTree,
                        inspectedPaths,
                        compiledPaths,
                        attachments);
                assembledAny = appendPromptSection(assembledPrompt, projectContext) || assembledAny;
                continue;
            }

            if (isCustomPromptPhase(phaseName)) {
                String customPhaseText = evaluateCustomPromptPhase(
                        phaseName,
                        request,
                        config,
                        cycle,
                        policy,
                        scriptRunnerEnabled,
                        effectiveReferenceTree,
                        inspectedPaths,
                        compiledPaths,
                        attachments);
                assembledAny = appendPromptSection(assembledPrompt, customPhaseText) || assembledAny;
            }
        }

        if (!assembledAny) {
            throw new IllegalStateException("Per-source prompt assembly produced no scripted content.");
        }

        return assembledPrompt.toString();
    }

    /**
     * Evaluate Custom Prompt Phase.
     *
     * @param phaseName the phase name
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateCustomPromptPhase(String phaseName,
                                            ReasoningRequest request,
                                            ReinsConfig config,
                                            ReasoningCycle cycle,
                                            FilePolicy policy,
                                            boolean scriptRunnerEnabled,
                                            String referenceTree,
                                            List<String> inspectedPaths,
                                            List<String> compiledPaths,
                                            List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            return null;
        }

        ReasoningScriptViews.InferenceStateView state = baseContext.getInference();
        ReasoningScriptViews.InferenceStateView withMessage = new ReasoningScriptViews.InferenceStateView(
                state == null ? "" : state.getContext(),
                state == null ? List.of() : state.getConversationHistory(),
                state == null ? List.of() : state.getInspectedFiles(),
                state == null ? List.of() : state.getCompiledFiles(),
                state == null ? List.of() : state.getToolOperations());

        ReasoningScriptContext scriptContext = ReasoningScriptContext.builder()
                .source(baseContext.getSource())
                .fileBases(baseContext.getFileBases())
                .tracking(baseContext.getTracking())
                .referenceTree(baseContext.getReferenceTree())
                .inference(withMessage)
                .attachments(baseContext.getAttachments())
                .config(baseContext.getConfig())
                .cycle(baseContext.getCycle())
                .policy(baseContext.getPolicy())
                .currentToolResult(baseContext.getCurrentToolResult())
                .build();

        String scriptName = phaseName.endsWith(".ftl") ? phaseName : phaseName + ".ftl";
        return templateExecutor.evaluateScriptSafely(scriptName, scriptContext, config, phaseName);
    }

    /**
     * Evaluate Reference Tree Script.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateReferenceTreeScript(ReasoningRequest request,
                                              ReinsConfig config,
                                              ReasoningCycle cycle,
                                              FilePolicy policy,
                                              boolean scriptRunnerEnabled,
                                              String referenceTree,
                                              List<String> inspectedPaths,
                                              List<String> compiledPaths,
                                              List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            return null;
        }
        return templateExecutor.evaluateScriptSafely("reference-tree.ftl", baseContext, config, "reference-tree");
    }

    /**
     * Evaluate Project Context Script.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateProjectContextScript(ReasoningRequest request,
                                               ReinsConfig config,
                                               ReasoningCycle cycle,
                                               FilePolicy policy,
                                               boolean scriptRunnerEnabled,
                                               String referenceTree,
                                               List<String> inspectedPaths,
                                               List<String> compiledPaths,
                                               List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            return null;
        }
        return templateExecutor.evaluateScriptSafely("project-context.ftl", baseContext, config, "project-context");
    }

    /**
     * Evaluate Grace Prompt Script.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String evaluateGracePromptScript(ReasoningRequest request,
                                            ReinsConfig config,
                                            ReasoningCycle cycle,
                                            FilePolicy policy,
                                            boolean scriptRunnerEnabled,
                                            String referenceTree,
                                            List<String> inspectedPaths,
                                            List<String> compiledPaths,
                                            List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            return null;
        }
        return templateExecutor.evaluateScriptSafely("max-turn-grace-prompt.ftl", baseContext, config, "max-turn-grace-prompt");
    }

    /**
     * Format Tool Result With Script.
     *
     * @param result the result
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @return the string result
     */
    public String formatToolResultWithScript(ToolExecutionResult result,
                                            ReasoningRequest request,
                                            ReinsConfig config,
                                            ReasoningCycle cycle,
                                            FilePolicy policy,
                                            boolean scriptRunnerEnabled,
                                            String referenceTree,
                                            List<String> inspectedPaths,
                                            List<String> compiledPaths,
                                            List<AttachedFilePayload> attachments) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                false);
        if (baseContext == null) {
            throw new IllegalStateException("Script context is required for tool-result rendering.");
        }
        ReasoningScriptContext toolContext;
        try {
            toolContext = contextAssembler.getScriptContextFactory().withToolResult(baseContext, result);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to build script context for tool-result.ftl: " + e.getMessage(), e);
        }
        String scriptText = templateExecutor.evaluateScriptSafely("tool-result.ftl", toolContext, config, "tool-result");
        if (scriptText == null || scriptText.isBlank()) {
            throw new IllegalStateException("Required script 'tool-result.ftl' produced empty content.");
        }
        return scriptText;
    }

    private boolean appendPromptSection(StringBuilder assembledPrompt, String section) {
        if (section == null || section.isBlank()) {
            return false;
        }
        if (assembledPrompt.length() > 0) {
            assembledPrompt.append("\n\n");
        }
        assembledPrompt.append(section.strip());
        return true;
    }

    private boolean isCustomPromptPhase(String phaseName) {
        if (phaseName == null || phaseName.isBlank()) {
            return false;
        }
        return !List.of(
                "system-context",
                "reference-tree",
                "project-context",
                "attachment-list",
                "file-list",
                "tool-result",
                "max-turn-grace-prompt",
                "retry-message").contains(phaseName);
    }
}
