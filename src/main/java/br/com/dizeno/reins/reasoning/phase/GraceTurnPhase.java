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

/**
 * GraceTurnPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class GraceTurnPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link GraceTurnPhase}.
     *
     * @param coordinator the coordinator
     */
    public GraceTurnPhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        ReasoningResult result = coordinator.graceTurnExecutor.run(
                context.getCycle(),
                context.getCycleLog(),
                context.getRequest(),
                context.getConversationHistory(),
                context.getConfig(),
                context.getUserFacingMessages(),
                context.getToolInfoPhrases(),
                new java.util.ArrayList<>(context.getWrittenPaths()),
                new java.util.ArrayList<>(context.getInspectedPaths()),
                new java.util.LinkedHashMap<>(context.getWrittenMtimes()),
                new java.util.LinkedHashMap<>(context.getInspectedMtimes()),
                new java.util.ArrayList<>(context.getReadMarkdownPaths()),
                context.getActivePerSourcePhases(),
                context.getFirstTurnReferenceTree()
        );
        context.setResult(result);
        return null;
    }
}
