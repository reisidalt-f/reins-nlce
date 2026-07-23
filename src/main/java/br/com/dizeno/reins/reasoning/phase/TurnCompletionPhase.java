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
import br.com.dizeno.reins.reasoning.ResponseDirective;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.PipelineRuntimeOutcome;
import java.time.Instant;
import java.util.ArrayList;

/**
 * TurnCompletionPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class TurnCompletionPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link TurnCompletionPhase}.
     *
     * @param coordinator the coordinator
     */
    public TurnCompletionPhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        ReasoningTurn inbound = context.getTurnInbound();
        ResponseDirective directive = context.getTurnDirective();

        inbound.setFulfilled(true);
        inbound.setFailureClass(null);
        int closedTurns = context.getClosedTurns() + 1;
        context.setClosedTurns(closedTurns);
        context.getCycle().setClosedTurnCount(closedTurns);
        context.getCycle().resetRetryStreakCount();

        coordinator.cycleStateManager.logAttemptEvent(context.getCycleLog(), inbound, "turn-closed", closedTurns, context.getCycle().getMaxTurns(),
                context.getCycle().getRetryStreakCount(),
                directive.getIntent() == null ? "turn fulfilled" : directive.getIntent().name());

        if (directive.getIntent() == ResponseDirective.Intent.FINISH_SUCCESS) {
            PipelineRuntimeOutcome pipelineOutcome = coordinator.inferencePipelineExecutor.outcomeForDirective(
                    context.getPipelinePlan(),
                    context.getPipelinePhaseIndex(),
                    directive);
            int nextPhaseIndex = coordinator.inferencePipelineExecutor.advancePhaseIndex(context.getPipelinePlan(), context.getPipelinePhaseIndex());
            if (pipelineOutcome == null && nextPhaseIndex >= 0) {
                context.setPipelinePhaseIndex(nextPhaseIndex);
                String currentPipelinePhase = coordinator.inferencePipelineExecutor.currentPhaseName(context.getPipelinePlan(), nextPhaseIndex);
                context.setCurrentPipelinePhase(currentPipelinePhase);
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "pipeline-phase-transition: " + currentPipelinePhase);

                String nextMessage = coordinator.renderPipelineGeminiMessageWithFailureLogging(
                        context.getCycleLog(),
                        inbound.getSequence(),
                        currentPipelinePhase,
                        context.getPipelinePlan(),
                        nextPhaseIndex,
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
                        context.getTurnParseResult().getBody(),
                        null,
                        false,
                        null);
                context.setNextMessage(nextMessage);
                context.setNextMessageFromScript(true);

                context.setLogicalTurn(closedTurns + 1);
                context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
                context.setAttemptIndex(1);
                return new TurnOutboundPrepPhase(coordinator); 
            }

            context.getCycle().setStatus(ReasoningCycle.Status.FINISHED_SUCCESS);
            context.getCycle().setCompletedAt(Instant.now());
            context.getCycle().resetRetryStreakCount();

            ReasoningResult result = coordinator.cycleStateManager.buildResult(context.getCycle(), directive.getIntent(),
                    "finish_success",
                    "Reasoning finished successfully.",
                    context.getUserFacingMessages(),
                    context.getToolInfoPhrases(),
                    coordinator.scriptSelectionService.applyFileListScript(new java.util.ArrayList<>(context.getWrittenPaths()), context.getConfig()),
                    new java.util.ArrayList<>(context.getInspectedPaths()),
                    new java.util.LinkedHashMap<>(context.getWrittenMtimes()),
                    new java.util.LinkedHashMap<>(context.getInspectedMtimes()),
                    new java.util.ArrayList<>(context.getReadMarkdownPaths()),
                    context.getFirstTurnReferenceTree());
            context.setResult(result);
            return null; 
        }

        if (directive.getIntent() == ResponseDirective.Intent.FINISH_ERROR) {
            context.getCycle().setStatus(ReasoningCycle.Status.FINISHED_ERROR);
            context.getCycle().setCompletedAt(Instant.now());

            ReasoningResult result = coordinator.cycleStateManager.buildResult(context.getCycle(), directive.getIntent(),
                    "finish_error",
                    "Reasoning finished with explicit finish-error intent.",
                    context.getUserFacingMessages(),
                    context.getToolInfoPhrases(),
                    coordinator.scriptSelectionService.applyFileListScript(new java.util.ArrayList<>(context.getWrittenPaths()), context.getConfig()),
                    new java.util.ArrayList<>(context.getInspectedPaths()),
                    new java.util.LinkedHashMap<>(context.getWrittenMtimes()),
                    new java.util.LinkedHashMap<>(context.getInspectedMtimes()),
                    new java.util.ArrayList<>(context.getReadMarkdownPaths()),
                    context.getFirstTurnReferenceTree());
            context.setResult(result);
            return null; 
        }

        context.setLogicalTurn(closedTurns + 1);
        context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
        context.setAttemptIndex(1);

        return new TurnOutboundPrepPhase(coordinator); 
    }
}
