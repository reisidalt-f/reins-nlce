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

import br.com.dizeno.reins.compilation.CycleWorkSetEntry;
import br.com.dizeno.reins.compilation.MtimeSnapshotUtil;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.util.PathLogFormatter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

 
/**
 * OutcomeBranchPhase is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public final class OutcomeBranchPhase implements CompilationPhase {

    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     * @param next the next
     */
    @Override
    public void execute(SourceCompilationContext ctx, PhaseChain next) throws Exception {
        ReasoningResult reasoningResult = ctx.getReasoningResult();

        
        if (reasoningResult != null
                && "finish_success".equalsIgnoreCase(reasoningResult.getFinalIntent())
                && reasoningResult.getWrittenPaths().isEmpty()) {
            handleNoopOutcome(ctx);
            
            return;
        }

        
        if (reasoningResult != null
                && "finish_error".equalsIgnoreCase(reasoningResult.getFinalIntent())) {
            String reason = reasoningResult.getTerminalReasonMessage();
            if (reason == null || reason.isBlank()) {
                reason = "Reasoning ended with an error.";
            }
            throw new IllegalArgumentException(reason);
        }

        
        handleSuccessOutcome(ctx);

        next.execute(ctx);
    }

    

    private void handleNoopOutcome(SourceCompilationContext ctx) {
        boolean validateOnly = ctx.isValidateOnly();
        ctx.getOutput().setStatus(validateOnly ? "validated" : "no-change");
        ctx.getOutput().setMessage(validateOnly ? "validated-no-change" : "no-change");
        ctx.getSummary().incrementProcessed();
        if (!validateOnly) {
            ctx.getSummary().incrementNoChange();
        }
        if (ctx.getPriorRecord() != null) {
            TrackingRecordHelper.physicallyTouchOutputs(ctx.getCanonicalSourcePath(), ctx.getPriorRecord(), ctx.getProjectRoot(), ctx.getConfig(), ctx.getLog(), ctx.isSkippedLoggingEnabled());
        }
        if (validateOnly || ctx.getPriorRecord() != null) {
            if (ctx.getConfig().getTracking().isFreezeState()) {
                ctx.getLog().info("[tracking] Freeze mode: tracking record NOT written for " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()));
            } else if (!ctx.getConfig().isDryRun()) {
                try {
                    SourceTrackingRecord skipRecord = TrackingRecordHelper.copyTrackingRecord(ctx.getPriorRecord());
                    skipRecord.setSourcePath(ctx.getCanonicalSourcePath());
                    skipRecord.setSourceCategory(ctx.getSourceCategory());
                    skipRecord.setSourceHash(ctx.getSourceHash());
                    skipRecord.setBlockFingerprints(List.of(ctx.getSourceHash()));
                    skipRecord.setSourceModificationTime(ctx.getNode().lastModifiedMillis());
                    skipRecord.setLastStatus(validateOnly ? "validated" : "no-change");
                    skipRecord.setLastCompiledAt(Instant.now().toString());
                    ctx.getSourceTrackingManager().commit(ctx.getProjectRoot(), ctx.getCanonicalSourcePath(), skipRecord, ctx.getTrackingStore());
                    if (ctx.getConfig().getLogging() != null && ctx.getConfig().getLogging().isTrackingFile()) {
                        ctx.getLog().info("Tracking file written (" + (validateOnly ? "validated" : "no-change") + "): " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()));
                    }
                } catch (Exception ex) {
                    ctx.getLog().warn("Could not write tracking file for " + PathLogFormatter.formatPath(ctx.getRelativeSourcePath(), ctx.getProjectRoot()) + ": " + ex.getMessage());
                }
            }
        }
        ctx.getWorkSetEntry().markProcessed();
        ctx.getOutput().setDurationMs(System.currentTimeMillis() - ctx.getStartTimeMs());
        if (ctx.getConfig().getLogging() != null && ctx.getConfig().getLogging().isResult()) {
            ctx.getResultPrinter().print(ctx.getLog(), ctx.getOutput(), ctx.getProjectRoot());
        }
    }

    /* package */

    private void handleSuccessOutcome(SourceCompilationContext ctx) throws Exception {
        ReasoningResult reasoningResult = ctx.getReasoningResult();
        boolean validateOnly = ctx.isValidateOnly();
        String sourceHash = ctx.getSourceHash();
        String sourceCategory = ctx.getSourceCategory();
        String resolvedTargetRoot = ctx.getResolvedTargetRoot();

        List<String> fingerprints = List.of(sourceHash);
        List<String> outputPathStrings = reasoningResult != null
            ? PathHelper.canonicalizeTrackedArtifactPaths(reasoningResult.getWrittenPaths(), ctx.getConfig(), ctx.getProjectRoot(), sourceCategory)
            : new ArrayList<>();
        List<String> inspectedTrackedPaths = reasoningResult != null
            ? PathHelper.canonicalizeTrackedArtifactPaths(reasoningResult.getInspectedPaths(), ctx.getConfig(), ctx.getProjectRoot(), sourceCategory)
            : new ArrayList<>();
        ctx.getOutput().setOutputPaths(outputPathStrings);
        ctx.getOutput().setOutputPath(String.join(",", outputPathStrings));
        ctx.getOutput().setOutputPolicy(resolvedTargetRoot);

        SourceTrackingRecord previous = TrackingRecordHelper.sanitizeStoredTrackingRecord(
            ctx.getTrackingStore().load(ctx.getProjectRoot(), ctx.getCanonicalSourcePath()).orElse(null),
            ctx.getCanonicalSourcePath(),
            ctx.getLog());

        if (ctx.getConfig() != null && ctx.getConfig().isFreshCompilation() && previous != null && previous.getCompiledFiles() != null) {
            previous.getCompiledFiles().clear();
        }

        Set<String> currentMarkdownReferencePaths = new LinkedHashSet<>(ctx.getGraph().getChildren(ctx.getRelativeSourcePath()));
        if (reasoningResult != null && reasoningResult.getReadMarkdownPaths() != null
                && !reasoningResult.getReadMarkdownPaths().isEmpty()) {
            currentMarkdownReferencePaths = new LinkedHashSet<>(reasoningResult.getReadMarkdownPaths());
        }
        currentMarkdownReferencePaths = currentMarkdownReferencePaths.stream()
                .map(ctx.getTrackingStore()::canonicalizePath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> currentInspectedTrackedPaths = new LinkedHashSet<>();
        if (reasoningResult != null && reasoningResult.getInspectedPaths() != null) {
            currentInspectedTrackedPaths.addAll(inspectedTrackedPaths);
        } else if (previous != null && previous.getInspectedFiles() != null) {
            currentInspectedTrackedPaths.addAll(previous.getInspectedFiles().keySet());
        }

        MtimeSnapshotUtil.SnapshotResult markdownSnapshot = MtimeSnapshotUtil.snapshot(ctx.getProjectRoot(), currentMarkdownReferencePaths, resolvedTargetRoot);

        ctx.getSummary().incrementProcessed();

        Map<String, Long> preMtimes = TrackingRecordHelper.snapshotOutputMtimes(ctx.getProjectRoot(), outputPathStrings, resolvedTargetRoot);

        List<String> compiled = new ArrayList<>();
        Map<String, String> compiledFileCategories = new LinkedHashMap<>();
        List<String> inspected = new ArrayList<>();
        Map<String, String> inspectedFileCategories = new LinkedHashMap<>();

        
        if (reasoningResult != null) {
            for (String canonicalPath : outputPathStrings) {
                if (!compiledFileCategories.containsKey(canonicalPath)) {
                    compiled.add(canonicalPath);
                    compiledFileCategories.put(canonicalPath, sourceCategory);
                }
            }

            for (String canonicalPath : inspectedTrackedPaths) {
                if (!inspectedFileCategories.containsKey(canonicalPath)) {
                    inspected.add(canonicalPath);
                    inspectedFileCategories.put(canonicalPath, sourceCategory);
                }
            }
        }

        boolean cleanupStaleCompiledFiles = ctx.getConfig().getTracking() != null && ctx.getConfig().getTracking().isCleanupStaleCompiledFiles();

        if (!cleanupStaleCompiledFiles && !(ctx.getConfig() != null && ctx.getConfig().isFreshCompilation())) {
            TrackingRecordHelper.preserveExistingCompiledFiles(ctx.getProjectRoot(), previous, resolvedTargetRoot, sourceCategory,
                compiled, compiledFileCategories);
        }

        Map<String, Long> postMtimes = TrackingRecordHelper.snapshotOutputMtimes(ctx.getProjectRoot(), compiled, resolvedTargetRoot);

        Map<String, Long> inspectedMtimes = TrackingRecordHelper.snapshotOutputMtimes(ctx.getProjectRoot(), inspected, resolvedTargetRoot);

        boolean compiledOutputsChanged = TrackingRecordHelper.outputsChanged(previous, compiled, preMtimes, postMtimes);
        if (compiledOutputsChanged) {
            flagReferencingSourcesForValidation(ctx);
        }

        String effectiveStatus = validateOnly ? "validated" : (compiledOutputsChanged ? "compiled" : "no-change");
        String trackingStatus = validateOnly ? "validated" : (compiledOutputsChanged ? "success" : "no-change");

        SourceTrackingRecord record = TrackingRecordHelper.buildTrackingRecord(new TrackingRecordHelper.TrackingParams(
            ctx.getCanonicalSourcePath(),
            sourceCategory,
            sourceHash,
            ctx.getNode().lastModifiedMillis(),
            fingerprints,
            compiled,
            inspected,
            compiledFileCategories,
            inspectedFileCategories,
            postMtimes,
            inspectedMtimes,
            markdownSnapshot.mtimes(),
            ctx.getConfig(),
            resolvedTargetRoot,
            resolvedTargetRoot,
            trackingStatus
        ), ctx.getFingerprintService());
        if (ctx.getConfig().getTracking().isFreezeState()) {
            ctx.getLog().info("[tracking] Freeze mode: tracking record NOT written for " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()));
        } else {
            ctx.getSourceTrackingManager().commit(ctx.getProjectRoot(), ctx.getCanonicalSourcePath(), record, ctx.getTrackingStore());
            if (ctx.getConfig().getLogging() != null && ctx.getConfig().getLogging().isTrackingFile()) {
                ctx.getLog().info("Tracking file written: " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()));
            }
            if (cleanupStaleCompiledFiles) {
                TrackingRecordHelper.cleanupStaleCompiledFiles(ctx.getProjectRoot(), previous, compiled, resolvedTargetRoot, ctx.getLog());
            }
        }

        ctx.getOutput().setStatus(effectiveStatus);
        ctx.getOutput().setOutputPath(String.join(",", compiled));
        ctx.getOutput().setMessage(validateOnly
                ? (compiledOutputsChanged ? "validated-with-fixes" : "validated-no-change")
                : (compiledOutputsChanged ? "ok" : "no-change"));
        if (compiledOutputsChanged) {
            ctx.getSummary().incrementCompiled();
        } else if (!validateOnly) {
            ctx.getSummary().incrementNoChange();
        }
        
        
        if (!ctx.getConfig().getTracking().isFreezeState()) {
            try {
                int notesCleared = ctx.getSourceTrackingManager().clearNotes(ctx.getProjectRoot(), ctx.getCanonicalSourcePath(), ctx.getTrackingStore());
                if (notesCleared > 0) {
                    ctx.getLog().info("[notes] Cleared " + notesCleared + " note(s) after successful compilation: " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()));
                }
            } catch (Exception noteEx) {
                ctx.getLog().warn("[notes] Could not clear notes for " + PathLogFormatter.formatPath(ctx.getCanonicalSourcePath(), ctx.getProjectRoot()) + ": " + noteEx.getMessage());
            }
        }
        ctx.getWorkSetEntry().markProcessed();
    }

    

    private void flagReferencingSourcesForValidation(SourceCompilationContext ctx) {
        List<String> orderedParents = ctx.getProcessingOrderResolver().resolveOrderedParents(
                ctx.getRelativeSourcePath(), ctx.getGraph(), ctx.getOrderIndex());
        if (orderedParents == null || orderedParents.isEmpty()) {
            orderedParents = ctx.getGraph().getDirectReferencingSources(ctx.getRelativeSourcePath());
        }
        for (String parentPath : orderedParents) {
            CycleWorkSetEntry parentEntry = ctx.getWorkSetByPath().get(parentPath);
            if (parentEntry == null || parentEntry.isProcessed()) {
                continue;
            }
            boolean changed = parentEntry.markValidateIfSkippable(ctx.getRelativeSourcePath());
            if (changed) {
                ctx.getOutput().getDiagnostics().add("flagged=" + parentPath + " cause=" + ctx.getRelativeSourcePath());
            }
        }
    }
}
