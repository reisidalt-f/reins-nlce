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
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningTurn;
import br.com.dizeno.reins.reasoning.ReasoningLogEntry;
import br.com.dizeno.reins.reasoning.ResponseDirective;
import br.com.dizeno.reins.reasoning.ResponseDirectiveParser;
import br.com.dizeno.reins.reasoning.SpoofingMessageValidator;
import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import java.util.ArrayList;

/**
 * DirectiveEvaluationPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class DirectiveEvaluationPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link DirectiveEvaluationPhase}.
     *
     * @param coordinator the coordinator
     */
    public DirectiveEvaluationPhase(DefaultReasoningExecutionCoordinator coordinator) {
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

        ResponseDirectiveParser.ParseResult parsed = coordinator.directiveParser.parse(context.getTurnResponse().getRawResponseText());
        ResponseDirective directive = parsed.getDirective();
        if (spoofedScriptMessage && spoofValidationFailure != null) {
            directive.setValid(false);
            directive.setFailureReason(spoofValidationFailure);
        }
        context.setTurnDirective(directive);
        context.setTurnParseResult(parsed);
        context.getTurnInbound().setDirective(directive);

        if (directive.isValid()
                && directive.getContentType() == ResponseDirective.ContentType.MESSAGE_TO_USER
                && parsed.getBody() != null
                && !parsed.getBody().isBlank()) {
            context.getUserFacingMessages().add(parsed.getBody().trim());
            if (context.getRequest().getUserMessageListener() != null) {
                try {
                    context.getRequest().getUserMessageListener().accept(parsed.getBody().trim());
                } catch (Exception ignored) {
                }
            }
        }

        if (!directive.isValid()) {
            context.getConversationHistory().remove(context.getConversationHistory().size() - 1);
            context.getTurnInbound().setFulfilled(false);
            context.getTurnInbound().setFailureClass("parse-error");

            String correctiveMessage = coordinator.renderPipelineGeminiMessageWithFailureLogging(
                    context.getCycleLog(),
                    context.getTurnInbound().getSequence(),
                    context.getCurrentPipelinePhase(),
                    context.getPipelinePlan(),
                    context.getPipelinePhaseIndex(),
                    context.getNextMessage(),
                    context.getRequest(),
                    context.getConfig(),
                    context.getCycle(),
                    context.getToolPermission(),
                    context.getScriptRunnerConfig().isEnabled(),
                    context.getFirstTurnReferenceTree(),
                    new ArrayList<>(context.getInspectedPaths()),
                    new ArrayList<>(context.getWrittenPaths()),
                    context.getPromptAttachments(),
                    context.getConversationHistory(),
                    null,
                    null,
                    null,
                    "parse-error",
                    false,
                    null);
            if (correctiveMessage == null || correctiveMessage.isBlank()) {
                correctiveMessage = coordinator.buildParseErrorRetryMessage(directive.getFailureReason(), context.getConfig());
            }
            correctiveMessage = coordinator.appendParseErrorDetails(correctiveMessage, directive.getFailureReason());
            context.getCycle().incrementRetryStreakCount();

            if (context.getConfig().getReasoning().isLogParseErrorRecovery()) {
                String effectiveReason = (directive.getFailureReason() == null || directive.getFailureReason().isBlank())
                        ? "The response could not be parsed." : directive.getFailureReason();
                String logBody = "lifecycle: parse-error-recovery\n"
                        + "retryStreakCount=" + context.getCycle().getRetryStreakCount() + "\n"
                        + "failure-reason=" + effectiveReason + "\n"
                        + "discarded-response=" + context.getTurnResponse().getRawResponseText() + "\n"
                        + "corrective-message=" + correctiveMessage;
                coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.INBOUND,
                        "parse-error-recovery", context.getTurnInbound().getSequence(), logBody);
            }

            coordinator.cycleStateManager.logAttemptEvent(context.getCycleLog(), context.getTurnInbound(), "parse-error", context.getClosedTurns(), context.getCycle().getMaxTurns(),
                    context.getCycle().getRetryStreakCount(),
                    directive.getFailureReason() == null ? "response parse failed" : directive.getFailureReason());

            context.setNextMessage(correctiveMessage);
            context.setNextMessageFromScript(false);
            context.setPromptAttachments(null);
            context.setAttemptIndex(context.getAttemptIndex() + 1);

            if (coordinator.configResolver.isNonFulfillmentAttemptBudgetExceeded(context.getConfig(), context.getAttemptIndex())) {
                ReasoningResult termResult = coordinator.terminateCycleOnNonFulfillmentAttemptExhaustion(
                        context.getCycle(),
                        context.getCycleLog(),
                        context.getRequest(),
                        context.getUserFacingMessages(),
                        context.getToolInfoPhrases(),
                        context.getWrittenPaths(),
                        context.getInspectedPaths(),
                        context.getWrittenMtimes(),
                        context.getInspectedMtimes(),
                        context.getReadMarkdownPaths(),
                        context.getFirstTurnReferenceTree(),
                        context.getLogicalTurn(),
                        "parse-error",
                        context.getCycleCachedContentId(),
                        context.getConfig());
                context.setResult(termResult);
                return null;
            }

            context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
            return new TurnOutboundPrepPhase(coordinator); 
        }

        if (directive.getContentType() == ResponseDirective.ContentType.TOOL_REQUEST) {
            return new ToolExecutionPhase(coordinator);
        } else {
            
            if (directive.isNonFinishUserMessage()) {
                String nextMessage = coordinator.renderPipelineGeminiMessageWithFailureLogging(
                        context.getCycleLog(),
                        context.getTurnInbound().getSequence(),
                        context.getCurrentPipelinePhase(),
                        context.getPipelinePlan(),
                        context.getPipelinePhaseIndex(),
                        context.getNextMessage(),
                        context.getRequest(),
                        context.getConfig(),
                        context.getCycle(),
                        context.getToolPermission(),
                        context.getScriptRunnerConfig().isEnabled(),
                        context.getFirstTurnReferenceTree(),
                        new ArrayList<>(context.getInspectedPaths()),
                        new ArrayList<>(context.getWrittenPaths()),
                        context.getPromptAttachments(),
                        context.getConversationHistory(),
                        directive.getIntent().name().toLowerCase(),
                        directive.getContentType() == null ? null : directive.getContentType().name().toLowerCase(),
                        parsed.getBody(),
                        null,
                        false,
                        null);
                if (nextMessage != null) {
                    nextMessage = nextMessage.trim();
                }
                context.setNextMessageFromScript(true);
                if (nextMessage == null || nextMessage.isBlank()) {
                    nextMessage = "Understood.";
                    context.setNextMessageFromScript(false);
                }
                context.setNextMessage(nextMessage);
            } else {
                context.setNextMessage(parsed.getBody());
                context.setNextMessageFromScript(false);
            }
            return new TurnCompletionPhase(coordinator);
        }
    }
}
