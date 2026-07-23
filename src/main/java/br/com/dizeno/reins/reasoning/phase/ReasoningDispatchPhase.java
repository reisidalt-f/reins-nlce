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
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.ReasoningTurn;
import br.com.dizeno.reins.reasoning.ReasoningLogEntry;
import br.com.dizeno.reins.reasoning.SpoofingMessageValidator;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import br.com.dizeno.reins.reasoning.model.RoutingFlag;
import java.util.ArrayList;

/**
 * ReasoningDispatchPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class ReasoningDispatchPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link ReasoningDispatchPhase}.
     *
     * @param coordinator the coordinator
     */
    public ReasoningDispatchPhase(DefaultReasoningExecutionCoordinator coordinator) {
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

        String messageContent = context.getTurnMessageContent();
        var outboundAttachments = context.getTurnOutboundAttachments();

        if (!(spoofedScriptMessage && spoofValidationFailure == null)) {
            context.getConversationHistory().add(new ConversationMessage(ConversationMessage.Role.USER, messageContent, outboundAttachments));
        }

        context.getRequest().setCachedContentId(context.getCycleCachedContentId());
        context.getRequest().setUseCachedContent(context.isCachedContentEnabled() && context.getCycleCachedContentId() != null);
        if (context.getRequest().isUseCachedContent()) {
            coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-used: " + context.getCycleCachedContentId() + " turn=" + context.getLogicalTurn() + " attempt=" + context.getAttemptIndex());
        }

        MarkdownInferenceRequest inferenceRequest = coordinator.graceTurnExecutor.buildInferenceRequestWithHistory(context.getRequest(), context.getConversationHistory(), context.getConfig());

        ReasoningTurn outbound = new ReasoningTurn();
        outbound.setSequence(context.getCycle().getTurns().size() + 1);
        outbound.setDirection(ReasoningTurn.Direction.OUTBOUND);
        outbound.setTurnIndex(context.getLogicalTurn());
        outbound.setAttemptIndex(context.getAttemptIndex());
        outbound.setPayload(messageContent);
        outbound.setAttachedFiles(new ArrayList<>(outboundAttachments));
        context.getCycle().getTurns().add(outbound);
        context.setTurnOutbound(outbound);

        MarkdownInferenceResponse response;
        if (spoofedScriptMessage && spoofValidationFailure == null) {
            response = new MarkdownInferenceResponse();
            response.setRawResponseText(context.getNextMessage());
            response.setDurationMs(0L);
            coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.OUTBOUND, "user", outbound.getSequence(),
                "routingFlag=" + RoutingFlag.SCRIPT_SPOOFED_LLM + "\n"
                    + "trustedScriptOrigin=true\n"
                    + "spoofedMessageBypassedInference=true");
        } else {
            coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.OUTBOUND, "user", outbound.getSequence(),
                coordinator.messageFormattingService.buildOutboundLogBody(messageContent, outboundAttachments));
            response = coordinator.inferenceService.infer(inferenceRequest, context.getConfig());
        }
        context.setTurnResponse(response);

        context.getConversationHistory().add(new ConversationMessage(ConversationMessage.Role.MODEL, response.getRawResponseText()));

        ReasoningTurn inbound = new ReasoningTurn();
        inbound.setSequence(context.getCycle().getTurns().size() + 1);
        inbound.setDirection(ReasoningTurn.Direction.INBOUND);
        inbound.setTurnIndex(context.getLogicalTurn());
        inbound.setAttemptIndex(context.getAttemptIndex());
        inbound.setPayload(response.getRawResponseText());
        context.getCycle().getTurns().add(inbound);
        context.setTurnInbound(inbound);

        coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.INBOUND, "assistant", inbound.getSequence(),
                "routingFlag=" + (spoofedScriptMessage && spoofValidationFailure == null
                    ? RoutingFlag.SCRIPT_SPOOFED_LLM
                    : RoutingFlag.GENUINE_LLM)
                    + "\n"
                    + response.getRawResponseText());

        return new DirectiveEvaluationPhase(coordinator);
    }
}
