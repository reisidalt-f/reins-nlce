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

import br.com.dizeno.reins.reasoning.ReasoningRequest;

 
/**
 * ReasoningRequestBuildPhase is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public final class ReasoningRequestBuildPhase implements CompilationPhase {

    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     * @param next the next
     */
    @Override
    public void execute(SourceCompilationContext ctx, PhaseChain next) throws Exception {
        ReasoningRequest reasoningRequest = ReasoningHelper.buildReasoningRequestFor(
                ctx.getCanonicalSourcePath(),
                ctx.getSourceCategory(),
                ctx.getSourceHash(),
                ctx.getConfig(),
                ctx.getProjectRoot(),
                ctx.getCompilationBackgroundPayload(),
                ctx.getLog(),
                ctx.getWorkSetEntry().getStatus());

        reasoningRequest.setReferenceDepthPolicy(ctx.getReferenceDepthPolicy());
        reasoningRequest.setEagerlyProvide(ctx.getEagerlyProvide());
        reasoningRequest.setPriorRecord(ctx.getPriorRecord());

        ctx.setReasoningRequest(reasoningRequest);

        next.execute(ctx);
    }
}
