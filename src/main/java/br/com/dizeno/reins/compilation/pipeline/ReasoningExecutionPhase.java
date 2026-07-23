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

package br.com.dizeno.reins.compilation.pipeline;

import br.com.dizeno.reins.compilation.QueuePointerState;
import br.com.dizeno.reins.reasoning.ReasoningResult;

import java.util.Set;

 
/**
 * ReasoningExecutionPhase is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public final class ReasoningExecutionPhase implements CompilationPhase {

    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     * @param next the next
     */
    @Override
    public void execute(SourceCompilationContext ctx, PhaseChain next) throws Exception {
        ReasoningResult reasoningResult = null;
        try {
            reasoningResult = ctx.getEffectiveReasoningService().runCycle(ctx.getReasoningRequest(), ctx.getConfig());
        } finally {
            
            
            if (ctx.getQueueToolingService() != null) {
                Set<String> notedPaths = ctx.getQueueToolingService().getAndClearNotedSourcePaths();
                for (String notedPath : notedPaths) {
                    boolean queued = ctx.getQueue().markNote(notedPath);
                    QueuePointerState state = ctx.getQueue().getPointerState();
                    if (queued) {
                        ctx.getLog().info("[notes] Note-backtrack queued for " + notedPath
                                + " (pointer=" + state.getPointer()
                                + ", totalNoted=" + state.getTotalNewlyNoted() + ")");
                    } else {
                        ctx.getLog().warn("[notes] Ignored note target not present in current queue: " + notedPath);
                    }
                }
            }
        }

        ctx.setReasoningResult(reasoningResult);
        ReasoningHelper.applyReasoningOutputFields(ctx.getOutput(), reasoningResult);

        next.execute(ctx);
    }
}
