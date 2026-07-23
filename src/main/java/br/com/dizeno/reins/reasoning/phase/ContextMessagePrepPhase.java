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

package br.com.dizeno.reins.reasoning.phase;

import br.com.dizeno.reins.reasoning.DefaultReasoningExecutionCoordinator;
import br.com.dizeno.reins.reasoning.ReasoningContext;
import br.com.dizeno.reins.reasoning.ReasoningPhase;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.MessageTypePlan;
import br.com.dizeno.reins.reasoning.scripting.ContextMessageBuildRequest;
import br.com.dizeno.reins.reasoning.scripting.ContextMessageBundle;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;
import java.util.ArrayList;
import java.util.List;

/**
 * ContextMessagePrepPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class ContextMessagePrepPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link ContextMessagePrepPhase}.
     *
     * @param coordinator the coordinator
     */
    public ContextMessagePrepPhase(DefaultReasoningExecutionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Executes the operation.
     *
     * @param context the context
     * @return the resolved or constructed object
     */
    @Override
    public ReasoningPhase execute(ReasoningContext context) throws Exception {
        List<AttachedFilePayload> firstTurnAttachments = coordinator.attachmentManagementService.buildFirstTurnAttachments(
                context.getRequest(),
                context.getSourceAttachment(),
                context.getFirstTurnReferenceTreeContext(),
                context.getProjectRoot(),
                context.getConfig(),
                context.getCycleLog());

        List<AttachedFilePayload> transformedSystemAttachments = context.getRequest().getAttachments() == null
                ? List.of()
                : new ArrayList<>(context.getRequest().getAttachments());
        List<AttachedFilePayload> firstTurnContextAttachments = coordinator.attachmentManagementService.mergeAttachments(
                transformedSystemAttachments,
                firstTurnAttachments);
        firstTurnContextAttachments = coordinator.scriptSelectionService.applyAttachmentListScript(firstTurnContextAttachments, context.getConfig());
        context.setFirstTurnContextAttachments(firstTurnContextAttachments);

        final String referenceTreeForContextMessages = context.getFirstTurnReferenceTree();
        final List<AttachedFilePayload> contextAttachmentsForContextMessages =
                firstTurnContextAttachments == null ? List.of() : List.copyOf(firstTurnContextAttachments);

        ReasoningScriptContext contextMessageScriptContext = coordinator.scriptEvaluationService.buildScriptBaseContext(
                context.getRequest(),
                context.getConfig(),
                context.getCycle(),
                context.getToolPermission(),
                context.getScriptRunnerConfig().isEnabled(),
                referenceTreeForContextMessages,
                new ArrayList<>(context.getInspectedPaths()),
                new ArrayList<>(context.getWrittenPaths()),
                contextAttachmentsForContextMessages,
                context.getRequest() != null && context.getRequest().isProjectInferenceCycle());

        ContextMessageBuildRequest contextMessageBuildRequest = new ContextMessageBuildRequest(
                context.getRequest() == null ? null : context.getRequest().getSourcePath(),
                context.getRequest() == null ? null : context.getRequest().getSourceHash(),
                coordinator.configResolver.buildConfigFingerprint(context.getConfig()),
                coordinator.configResolver.buildPolicyFingerprint(context.getToolPermission()),
                context.isCachedContentEnabled(),
                contextMessageScriptContext);
        ContextMessageBundle contextMessageBundle = coordinator.contextMessageBuilderService.build(
                contextMessageBuildRequest,
                step -> coordinator.scriptEvaluationService.evaluateContextMessageStepScript(
                        step,
                        context.getRequest(),
                        context.getConfig(),
                        context.getCycle(),
                        context.getToolPermission(),
                        context.getScriptRunnerConfig().isEnabled(),
                        referenceTreeForContextMessages,
                        new ArrayList<>(context.getInspectedPaths()),
                        new ArrayList<>(context.getWrittenPaths()),
                        contextAttachmentsForContextMessages));

        List<ConversationMessage> prependMessages = new ArrayList<>(
                contextMessageBundle.getMessages() == null ? List.of() : contextMessageBundle.getMessages());
        if (prependMessages.isEmpty()) {
            throw new IllegalStateException("Scripted context builder produced no prepend messages.");
        }

        List<MessageTypePlan> contextPlan = contextMessageBundle.getMessageTypePlan();
        for (int i = 0; i < prependMessages.size() && i < contextPlan.size(); i++) {
            if ("background-files".equals(contextPlan.get(i).getStepName())
                    && !firstTurnContextAttachments.isEmpty()) {
                ConversationMessage bg = prependMessages.get(i);
                prependMessages.set(i,
                        new ConversationMessage(bg.getRole(), bg.getText(), firstTurnContextAttachments));
                break;
            }
        }
        context.setPrependMessages(prependMessages);
        context.setContextPlan(contextPlan);

        String prependMessagesSummary = coordinator.messageFormattingService.summarizePrependMessages(prependMessages);
        context.setPrependMessagesSummary(prependMessagesSummary);
        String prependMessagesSafeSummary = "prepend-messages: count=" + prependMessages.size();
        context.setPrependMessagesSafeSummary(prependMessagesSafeSummary);

        String migratedFirstUserPayload = coordinator.renderPipelineGeminiMessageWithFailureLogging(
                context.getCycleLog(),
                0,
                context.getCurrentPipelinePhase(),
                context.getPipelinePlan(),
                context.getPipelinePhaseIndex(),
                context.getRequest().getMessage(),
                context.getRequest(),
                context.getConfig(),
                context.getCycle(),
                context.getToolPermission(),
                context.getScriptRunnerConfig().isEnabled(),
                context.getFirstTurnReferenceTree(),
                new ArrayList<>(context.getInspectedPaths()),
                new ArrayList<>(context.getWrittenPaths()),
                firstTurnContextAttachments,
                context.getConversationHistory(),
                null,
                null,
                null,
                null,
                false,
                null);
        if (migratedFirstUserPayload == null) {
            migratedFirstUserPayload = coordinator.inferencePipelineOrchestrator.buildFirstTurnPayloadWithScripts(
                    context.getRequest().getMessage(),
                    context.getFirstTurnReferenceTree(),
                    context.getRequest(),
                    context.getConfig(),
                    context.getCycle(),
                    context.getToolPermission(),
                    context.getScriptRunnerConfig().isEnabled(),
                    context.getActivePerSourcePhases(),
                    new ArrayList<>(context.getInspectedPaths()),
                    new ArrayList<>(context.getWrittenPaths()),
                    firstTurnContextAttachments);
        }
        String firstUserMessage = migratedFirstUserPayload == null ? "" : migratedFirstUserPayload.stripTrailing();
        context.setNextMessageFromScript(true);
        context.setNextMessage(coordinator.formatNextMessage(
                firstUserMessage,
                1,
                context.getCycle().getMaxTurns(),
                coordinator.configResolver.isTurnCountNoteEnabled(context.getConfig()),
                true));

        boolean logSystemContextEnabled = context.getConfig() != null
                && context.getConfig().getReasoning() != null
                && context.getConfig().getReasoning().isLogSystemContext();
        context.setLogSystemContextEnabled(logSystemContextEnabled);

        return new ContentCachingPhase(coordinator);
    }
}
