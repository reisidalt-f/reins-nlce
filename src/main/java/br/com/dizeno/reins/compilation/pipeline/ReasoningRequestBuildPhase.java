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
import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import java.util.ArrayList;
import java.util.List;

 
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
                ctx.getNode() != null ? ctx.getNode().absolutePath() : null,
                ctx.getCanonicalSourcePath(),
                ctx.getSourceCategory(),
                ctx.getSourceHash(),
                ctx.getConfig(),
                ctx.getProjectRoot(),
                ctx.getCompilationBackgroundPayload(),
                ctx.getLog(),
                ctx.getWorkSetEntry().getStatus());

        SourceTrackingRecord priorRecord = ctx.getPriorRecord();
        if (ctx.getConfig() != null && ctx.getConfig().getNote() != null && !ctx.getConfig().getNote().isBlank()) {
            String noteText = ctx.getConfig().getNote().trim();
            if (priorRecord == null) {
                priorRecord = new SourceTrackingRecord();
            } else {
                SourceTrackingRecord copy = new SourceTrackingRecord();
                copy.setSourcePath(priorRecord.getSourcePath());
                copy.setSourceCategory(priorRecord.getSourceCategory());
                copy.setSourceHash(priorRecord.getSourceHash());
                copy.setSourceModificationTime(priorRecord.getSourceModificationTime());
                copy.setBlockFingerprints(priorRecord.getBlockFingerprints());
                copy.setMarkdownReferences(priorRecord.getMarkdownReferences());
                copy.setInferenceFingerprint(priorRecord.getInferenceFingerprint());
                copy.setModel(priorRecord.getModel());
                copy.setOutputPolicy(priorRecord.getOutputPolicy());
                copy.setLastCompiledAt(priorRecord.getLastCompiledAt());
                copy.setLastStatus(priorRecord.getLastStatus());
                copy.setResolvedTargetRoot(priorRecord.getResolvedTargetRoot());
                copy.setCompiledFiles(priorRecord.getCompiledFiles());
                copy.setInspectedFiles(priorRecord.getInspectedFiles());
                copy.setNotes(priorRecord.getNotes());
                priorRecord = copy;
            }
            List<ReasoningNote> updatedNotes = new ArrayList<>(priorRecord.getNotes());
            updatedNotes.add(new ReasoningNote(noteText, java.time.Instant.now().toString(), ReasoningNote.Origin.CLI));
            priorRecord.setNotes(updatedNotes);
        }

        reasoningRequest.setReferenceDepthPolicy(ctx.getReferenceDepthPolicy());
        reasoningRequest.setEagerlyProvide(ctx.getEagerlyProvide());
        reasoningRequest.setPriorRecord(priorRecord);

        ctx.setReasoningRequest(reasoningRequest);

        next.execute(ctx);
    }
}
