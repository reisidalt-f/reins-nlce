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
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.ReasoningPipelineExecutor;
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PipelineOrchestratorService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class PipelineOrchestratorService {
    private final ScriptContextAssembler contextAssembler;
    private final ScriptEvaluator scriptEvaluator;
    private final ReasoningPipelineExecutor inferencePipelineExecutor;

    /**
     * Constructs a new instance of {@link PipelineOrchestratorService}.
     *
     * @param contextAssembler the context assembler
     * @param scriptEvaluator the script evaluator instance
     * @param inferencePipelineExecutor the inference pipeline executor
     */
    public PipelineOrchestratorService(ScriptContextAssembler contextAssembler,
                                       ScriptEvaluator scriptEvaluator,
                                       ReasoningPipelineExecutor inferencePipelineExecutor) {
        this.contextAssembler = contextAssembler;
        this.scriptEvaluator = scriptEvaluator;
        this.inferencePipelineExecutor = inferencePipelineExecutor;
    }

    /**
     * Resolves the configured value or path per source phase names.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @return the string result
     */
    public List<String> resolvePerSourcePhaseNames(ReasoningRequest request,
                                                   ReinsConfig config,
                                                   ReasoningCycle cycle,
                                                   FilePolicy policy,
                                                   boolean scriptRunnerEnabled) {
        if (request != null && request.getPhaseOrderOverride() != null && !request.getPhaseOrderOverride().isEmpty()) {
            List<String> overridden = request.getPhaseOrderOverride().stream()
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .toList();
            PhaseListValidator.validate(overridden, scriptEvaluator);
            return overridden;
        }
        if (scriptEvaluator == null || contextAssembler == null) {
            throw new IllegalStateException("Script evaluator is not configured for per-source phase resolution.");
        }
        ReasoningScriptContext context = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                null,
                List.of(),
                List.of(),
                List.of(),
                request != null && request.isProjectInferenceCycle());
        String rendered;
        try {
            boolean scriptsEventsEnabled = config != null
                    && config.getLogging() != null
                    && config.getLogging().isScriptsEvents();
            rendered = scriptEvaluator.evaluate("phase-list.ftl", context, scriptsEventsEnabled);
        } catch (ScriptEvaluationException e) {
            String msg = "Error in phase-list script: " + e.getMessage()
                    + " sourceIdentifier=" + (e.getSourceIdentifier() == null ? "unknown" : e.getSourceIdentifier())
                    + " sourceIndex=" + (e.getSourceIndex() == null ? "unknown" : e.getSourceIndex())
                    + " terminalReason=" + (e.getTerminalReason() == null ? "unknown" : e.getTerminalReason().name())
                    + " failureCategory=" + (e.getFailureCategory() == null ? "unknown" : e.getFailureCategory().name());
            throw new IllegalStateException(msg, e);
        }
        List<String> parsed = parsePhaseList(rendered);
        PhaseListValidator.validate(parsed, scriptEvaluator);
        return parsed;
    }

    /**
     * Resolves the configured value or path inference pipeline plan.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @param conversationHistory the conversation history
     * @param phaseNames the phase names
     * @param currentPhase the current phase
     * @param lastDirectiveIntent the last directive intent
     * @param lastDirectiveContentType the last directive content type
     * @param lastFailureClass the last failure class
     * @param currentToolResultAvailable the current tool result available
     * @param currentToolResult the current tool result
     * @return the resolved or constructed object
     */
    public ReasoningPipelinePlan resolveInferencePipelinePlan(ReasoningRequest request,
                                                              ReinsConfig config,
                                                              ReasoningCycle cycle,
                                                              FilePolicy policy,
                                                              boolean scriptRunnerEnabled,
                                                              List<String> inspectedPaths,
                                                              List<String> compiledPaths,
                                                              List<AttachedFilePayload> attachments,
                                                              List<ConversationMessage> conversationHistory,
                                                              List<String> phaseNames,
                                                              String currentPhase,
                                                              String lastDirectiveIntent,
                                                              String lastDirectiveContentType,
                                                              String lastFailureClass,
                                                              boolean currentToolResultAvailable,
                                                              ToolExecutionResult currentToolResult) {
        ReasoningScriptContext baseContext = contextAssembler.buildScriptBaseContext(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                null,
                inspectedPaths,
                compiledPaths,
                attachments,
                request != null && request.isProjectInferenceCycle());
        if (baseContext == null) {
            throw new IllegalStateException("Unable to build script context for inference pipeline plan.");
        }
        if (currentToolResult != null) {
            baseContext = contextAssembler.getScriptContextFactory().withToolResult(baseContext, currentToolResult);
        }
        List<String> safePhaseNames = phaseNames == null ? List.of() : phaseNames;
        int currentPhaseOrdinal = -1;
        if (currentPhase != null && !safePhaseNames.isEmpty()) {
            currentPhaseOrdinal = safePhaseNames.indexOf(currentPhase);
        }
        baseContext = contextAssembler.getScriptContextFactory().withPipelineState(
                baseContext,
                conversationHistory,
                request == null ? null : request.getMessage(),
                currentPhase,
                currentPhaseOrdinal,
                safePhaseNames,
                lastDirectiveIntent,
                lastDirectiveContentType,
                null,
                lastFailureClass,
                currentToolResultAvailable);
        try {
            return inferencePipelineExecutor.resolvePlan(
                    request == null ? null : request.getSourcePath(),
                    baseContext,
                    request == null ? null : request.getPhaseOrderOverride());
        } catch (ScriptEvaluationException ex) {
            throw new IllegalStateException("Failed to resolve inference pipeline phases.", ex);
        }
    }

    /**
     * Render Pipeline Gemini Message.
     *
     * @param currentPhase the current phase
     * @param pipelinePlan the pipeline plan
     * @param pipelinePhaseIndex the pipeline phase index
     * @param messageContext the message context
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @param conversationHistory the conversation history
     * @param lastDirectiveIntent the last directive intent
     * @param lastDirectiveContentType the last directive content type
     * @param lastDirectiveBody the last directive body
     * @param lastFailureClass the last failure class
     * @param currentToolResultAvailable the current tool result available
     * @param currentToolResult the current tool result
     * @return the string result
     */
    public String renderPipelineGeminiMessage(String currentPhase,
                                              ReasoningPipelinePlan pipelinePlan,
                                              int pipelinePhaseIndex,
                                              String messageContext,
                                              ReasoningRequest request,
                                              ReinsConfig config,
                                              ReasoningCycle cycle,
                                              FilePolicy policy,
                                              boolean scriptRunnerEnabled,
                                              String referenceTree,
                                              List<String> inspectedPaths,
                                              List<String> compiledPaths,
                                              List<AttachedFilePayload> attachments,
                                              List<ConversationMessage> conversationHistory,
                                              String lastDirectiveIntent,
                                              String lastDirectiveContentType,
                                              String lastDirectiveBody,
                                              String lastFailureClass,
                                              boolean currentToolResultAvailable,
                                              ToolExecutionResult currentToolResult) {
        if (currentPhase == null || currentPhase.isBlank()) {
            return null;
        }
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
                request != null && request.isProjectInferenceCycle());
        if (baseContext == null) {
            return null;
        }
        if (currentToolResult != null) {
            baseContext = contextAssembler.getScriptContextFactory().withToolResult(baseContext, currentToolResult);
        }
        baseContext = contextAssembler.getScriptContextFactory().withPipelineState(
                baseContext,
                conversationHistory,
                messageContext,
                currentPhase,
                pipelinePhaseIndex,
                pipelinePlan == null
                        ? List.of()
                        : pipelinePlan.getPhases().stream().map(p -> p.getName()).toList(),
                lastDirectiveIntent,
                lastDirectiveContentType,
                lastDirectiveBody,
                lastFailureClass,
                currentToolResultAvailable);
        try {
            String renderedMessage = inferencePipelineExecutor.renderPhaseRawMessage(currentPhase, baseContext, Map.of());
            PipelineExchangeMessage exchangeMessage = inferencePipelineExecutor.parsePhaseMessage(renderedMessage);
            if (!exchangeMessage.isValid()) {
                throw new ScriptEvaluationService.PipelineMessageParseException(currentPhase, exchangeMessage.getFailureReason(), renderedMessage);
            }
            if (exchangeMessage.getMessageToUser() != null
                    && !exchangeMessage.getMessageToUser().isBlank()
                    && request != null
                    && request.getUserMessageListener() != null
                    && "user-progress".equalsIgnoreCase(exchangeMessage.getContentType())) {
                request.getUserMessageListener().accept(exchangeMessage.getMessageToUser().trim());
            }
            if (exchangeMessage.isSpoofedAssistantRole()) {
                return toCanonicalMessage(exchangeMessage);
            }
            return exchangeMessage.getMessageToGemini();
        } catch (ScriptEvaluationException ex) {
            throw new IllegalStateException("Failed to render inference pipeline message.", ex);
        } catch (ScriptEvaluationService.PipelineMessageParseException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected failure while rendering inference pipeline message for phase: " + currentPhase, ex);
        }
    }

    private String toCanonicalMessage(PipelineExchangeMessage message) {
        StringBuilder builder = new StringBuilder();
        if (message.getRole() != null && !message.getRole().isBlank()) {
            builder.append("ROLE: ").append(message.getRole().trim()).append("\n");
        }
        if (message.getIntent() != null && !message.getIntent().isBlank()) {
            builder.append("INTENT: ").append(message.getIntent().trim()).append("\n");
        }
        if (message.getContentType() != null && !message.getContentType().isBlank()) {
            builder.append("CONTENT_TYPE: ").append(message.getContentType().trim()).append("\n");
        }
        builder.append("\n").append(message.getBody() == null ? "" : message.getBody());
        return builder.toString();
    }

    private List<String> parsePhaseList(String rendered) {
        if (rendered == null || rendered.isBlank()) {
            return List.of();
        }
        java.util.LinkedHashSet<String> phases = new java.util.LinkedHashSet<>();
        for (String line : rendered.split("\\R")) {
            if (line == null) {
                continue;
            }
            String normalized = line.trim();
            if (!normalized.isEmpty()) {
                phases.add(normalized);
            }
        }
        return new ArrayList<>(phases);
    }
}
