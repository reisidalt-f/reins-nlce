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
import br.com.dizeno.reins.reasoning.ReasoningPipelineExecutor;
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.ContextStepScriptResolver;

import java.util.List;

/**
 * ScriptEvaluationService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ScriptEvaluationService {

    /**
     * PipelineMessageParseException is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a exception representing errors in its prefix operations.
     */
    public static class PipelineMessageParseException extends IllegalStateException {
        private final String phase;
        private final String failureReason;
        private final String renderedMessage;

        /**
         * Constructs a new instance of {@link PipelineMessageParseException}.
         *
         * @param phase the phase
         * @param failureReason the failure reason
         * @param renderedMessage the rendered message
         */
        public PipelineMessageParseException(String phase, String failureReason, String renderedMessage) {
            super("Failed to parse inference pipeline message for phase '"
                    + (phase == null ? "unknown" : phase)
                    + "': " + (failureReason == null ? "unknown parsing failure" : failureReason));
            this.phase = phase;
            this.failureReason = failureReason;
            this.renderedMessage = renderedMessage;
        }

        /**
         * Gets the phase.
         *
         * @return the string result
         */
        public String getPhase() {
            return phase;
        }

        /**
         * Gets the failure reason.
         *
         * @return the string result
         */
        public String getFailureReason() {
            return failureReason;
        }

        /**
         * Gets the rendered message.
         *
         * @return the string result
         */
        public String getRenderedMessage() {
            return renderedMessage;
        }
    }

    private final ScriptContextAssembler contextAssembler;
    private final SafeTemplateExecutor templateExecutor;
    private final PromptAssemblyService promptAssemblyService;
    private final PipelineOrchestratorService pipelineOrchestratorService;

    
    private final ScriptEvaluator scriptEvaluator;
    private final ReasoningScriptContextFactory scriptContextFactory;
    private final ContextStepScriptResolver contextStepScriptResolver;
    private final ReasoningPipelineExecutor inferencePipelineExecutor;
    private final MessageFormattingService messageFormattingService;

    /**
     * Constructs a new instance of {@link ScriptEvaluationService}.
     *
     * @param scriptEvaluator the script evaluator instance
     * @param scriptContextFactory the script context factory
     * @param contextStepScriptResolver the context step script resolver
     * @param inferencePipelineExecutor the inference pipeline executor
     * @param messageFormattingService the message formatting service
     */
    public ScriptEvaluationService(ScriptEvaluator scriptEvaluator,
                                   ReasoningScriptContextFactory scriptContextFactory,
                                   ContextStepScriptResolver contextStepScriptResolver,
                                   ReasoningPipelineExecutor inferencePipelineExecutor,
                                   MessageFormattingService messageFormattingService) {
        this.scriptEvaluator = scriptEvaluator;
        this.scriptContextFactory = scriptContextFactory;
        this.contextStepScriptResolver = contextStepScriptResolver;
        this.inferencePipelineExecutor = inferencePipelineExecutor;
        this.messageFormattingService = messageFormattingService;

        this.contextAssembler = new ScriptContextAssembler(scriptEvaluator, scriptContextFactory);
        this.templateExecutor = new SafeTemplateExecutor(scriptEvaluator, messageFormattingService);
        this.promptAssemblyService = new PromptAssemblyService(
                contextAssembler,
                templateExecutor,
                scriptEvaluator,
                contextStepScriptResolver
        );
        this.pipelineOrchestratorService = new PipelineOrchestratorService(
                contextAssembler,
                scriptEvaluator,
                inferencePipelineExecutor
        );
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
        return promptAssemblyService.evaluateContextMessageStepScript(
                step, request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.evaluateSystemContextScript(
                request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.buildFirstTurnPayloadWithScripts(
                nextMessage, firstTurnReferenceTree, request, config, cycle, policy, scriptRunnerEnabled,
                activePerSourcePhases, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.evaluateCustomPromptPhase(
                phaseName, request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.evaluateReferenceTreeScript(
                request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.evaluateProjectContextScript(
                request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.evaluateGracePromptScript(
                request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return promptAssemblyService.formatToolResultWithScript(
                result, request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments
        );
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
        return pipelineOrchestratorService.resolvePerSourcePhaseNames(
                request, config, cycle, policy, scriptRunnerEnabled
        );
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
        return pipelineOrchestratorService.resolveInferencePipelinePlan(
                request, config, cycle, policy, scriptRunnerEnabled, inspectedPaths, compiledPaths, attachments,
                conversationHistory, phaseNames, currentPhase, lastDirectiveIntent, lastDirectiveContentType,
                lastFailureClass, currentToolResultAvailable, currentToolResult
        );
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
        return pipelineOrchestratorService.renderPipelineGeminiMessage(
                currentPhase, pipelinePlan, pipelinePhaseIndex, messageContext, request, config, cycle, policy,
                scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments, conversationHistory,
                lastDirectiveIntent, lastDirectiveContentType, lastDirectiveBody, lastFailureClass,
                currentToolResultAvailable, currentToolResult
        );
    }

    /**
     * Evaluate Script Safely.
     *
     * @param scriptName the script name
     * @param context the context
     * @param config the Reins configuration settings
     * @param scriptContext the script context
     * @return the string result
     */
    public String evaluateScriptSafely(String scriptName,
                                       ReasoningScriptContext context,
                                       ReinsConfig config,
                                       String scriptContext) {
        return templateExecutor.evaluateScriptSafely(scriptName, context, config, scriptContext);
    }

    /**
     * Builds the configured target script base context.
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
     * @param isProjectCycle the is project cycle
     * @return the resulting context
     */
    public ReasoningScriptContext buildScriptBaseContext(ReasoningRequest request,
                                                         ReinsConfig config,
                                                         ReasoningCycle cycle,
                                                         FilePolicy policy,
                                                         boolean scriptRunnerEnabled,
                                                         String referenceTree,
                                                         List<String> inspectedPaths,
                                                         List<String> compiledPaths,
                                                         List<AttachedFilePayload> attachments,
                                                         boolean isProjectCycle) {
        return contextAssembler.buildScriptBaseContext(
                request, config, cycle, policy, scriptRunnerEnabled, referenceTree, inspectedPaths, compiledPaths, attachments, isProjectCycle
        );
    }
}
