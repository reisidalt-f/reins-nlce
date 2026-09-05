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
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluationService;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningCycle;

import java.util.List;

/**
 * ReasoningPipelineOrchestrator is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing inference pipeline orchestrator.
 */
public class ReasoningPipelineOrchestrator {
    
    private final ScriptEvaluationService scriptEvaluationService;

    /**
     * Constructs a new instance of {@link ReasoningPipelineOrchestrator}.
     *
     * @param scriptEvaluationService the script evaluation service
     */
    public ReasoningPipelineOrchestrator(ScriptEvaluationService scriptEvaluationService) {
        this.scriptEvaluationService = scriptEvaluationService;
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
        return scriptEvaluationService.buildFirstTurnPayloadWithScripts(
                nextMessage,
                firstTurnReferenceTree,
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                activePerSourcePhases,
                inspectedPaths,
                compiledPaths,
                attachments);
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
        return scriptEvaluationService.resolvePerSourcePhaseNames(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled);
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
        return scriptEvaluationService.resolveInferencePipelinePlan(
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                inspectedPaths,
                compiledPaths,
                attachments,
                conversationHistory,
                phaseNames,
                currentPhase,
                lastDirectiveIntent,
                lastDirectiveContentType,
                lastFailureClass,
                currentToolResultAvailable,
                currentToolResult);
    }

    /**
     * Render Pipeline Message To Model.
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
    public String renderPipelineMessageToModel(String currentPhase,
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
        return scriptEvaluationService.renderPipelineMessageToModel(
                currentPhase,
                pipelinePlan,
                pipelinePhaseIndex,
                messageContext,
                request,
                config,
                cycle,
                policy,
                scriptRunnerEnabled,
                referenceTree,
                inspectedPaths,
                compiledPaths,
                attachments,
                conversationHistory,
                lastDirectiveIntent,
                lastDirectiveContentType,
                lastDirectiveBody,
                lastFailureClass,
                currentToolResultAvailable,
                currentToolResult);
    }
}
