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
import br.com.dizeno.reins.reasoning.SpoofingMessageValidator;
import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * TurnOutboundPrepPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class TurnOutboundPrepPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link TurnOutboundPrepPhase}.
     *
     * @param coordinator the coordinator
     */
    public TurnOutboundPrepPhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        if (context.getClosedTurns() >= context.getCycle().getMaxTurns()) {
            return new GraceTurnPhase(coordinator);
        }

        PipelineExchangeMessage scriptMessageCandidate = context.isNextMessageFromScript()
                ? coordinator.directiveParser.parseCanonicalMessage(context.getNextMessage())
                : null;
        boolean spoofedScriptMessage = context.isNextMessageFromScript()
                && scriptMessageCandidate != null
                && scriptMessageCandidate.isValid()
                && scriptMessageCandidate.isSpoofedAssistantRole();
        String spoofValidationFailure = spoofedScriptMessage
                ? SpoofingMessageValidator.validateHeaders(scriptMessageCandidate.getHeaders(), true)
                : null;

        List<AttachedFilePayload> outboundAttachments;
        String messageContent;

        int summarizeTurns = context.getConfig() != null && context.getConfig().getReasoning() != null
                ? context.getConfig().getReasoning().getSummarizeCycleTurns()
                : 1;
        boolean shouldSummarize = summarizeTurns > 0 && context.getLogicalTurn() % summarizeTurns == 0;

        if (spoofedScriptMessage && spoofValidationFailure == null) {
            outboundAttachments = List.of();
            messageContent = context.getNextMessage();
            context.setSourceAttachmentSent(true);
        } else if (!context.isSourceAttachmentSent() && context.getSourceAttachment() != null) {
            outboundAttachments = List.of();
            messageContent = context.getNextMessage();
            context.setSourceAttachmentSent(true);
        } else {
            outboundAttachments = context.getPromptAttachments() == null
                    ? List.of()
                    : new ArrayList<>(context.getPromptAttachments());
            String promptText = coordinator.promptBuilder.buildPrompt(context.getCycle(), context.getNextMessage(), outboundAttachments, false);
            if (shouldSummarize) {
                promptText = coordinator.messageFormattingService.withSummarizationInstruction(promptText);
            }
            messageContent = coordinator.formatNextMessage(
                    promptText,
                    context.getLogicalTurn(),
                    context.getCycle().getMaxTurns(),
                    coordinator.configResolver.isTurnCountNoteEnabled(context.getConfig()),
                    context.isNextMessageFromScript());
        }

        context.setTurnMessageContent(messageContent);
        context.setTurnOutboundAttachments(outboundAttachments);

        return new ReasoningDispatchPhase(coordinator);
    }
}
