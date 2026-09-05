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

import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;

 
/**
 * TrackingLoadPhase is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public final class TrackingLoadPhase implements CompilationPhase {

    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     * @param next the next
     */
    @Override
    public void execute(SourceCompilationContext ctx, PhaseChain next) throws Exception {
        SourceTrackingRecord priorRecord = TrackingRecordHelper.sanitizeStoredTrackingRecord(
                ctx.getTrackingStore().load(ctx.getProjectRoot(), ctx.getCanonicalSourcePath()).orElse(null),
                ctx.getCanonicalSourcePath(),
                ctx.getLog());

        if (ctx.getConfig() != null && ctx.getConfig().isFreshCompilation() && priorRecord != null) {
            TrackingRecordHelper.deletePreviouslyGeneratedFiles(
                    ctx.getProjectRoot(),
                    priorRecord,
                    ctx.getLog());
            if (priorRecord.getCompiledFiles() != null) {
                priorRecord.getCompiledFiles().clear();
            }
            if (!ctx.getConfig().getTracking().isFreezeState() && !ctx.getConfig().isDryRun()) {
                try {
                    ctx.getSourceTrackingManager().commit(ctx.getProjectRoot(), ctx.getCanonicalSourcePath(), priorRecord, ctx.getTrackingStore());
                } catch (Exception ex) {
                    ctx.getLog().warn("Could not update tracking record after freshCompilation cleanup for "
                            + ctx.getCanonicalSourcePath() + ": " + ex.getMessage());
                }
            }
        }

        ctx.setPriorRecord(priorRecord);

        next.execute(ctx);
    }
}
