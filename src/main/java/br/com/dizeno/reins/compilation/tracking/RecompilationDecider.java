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

package br.com.dizeno.reins.compilation.tracking;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RecompilationDecider is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Evaluates file modification times, fingerprints, and dependency changes to decide whether a file needs recompilation.
 */
public class RecompilationDecider {
     
    private static final Path UNKNOWN_PROJECT_ROOT = Path.of("").toAbsolutePath();
     
    public static final long MTIME_UNAVAILABLE = -1L;

    /**
     * EvaluationInputs is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing evaluation inputs.
     */
    public record EvaluationInputs(
            Map<String, Long> currentMarkdownReferenceMtimes,
            Map<String, Long> currentCompiledFileMtimes,
            Map<String, Long> currentInspectedFileMtimes,
            Set<String> currentProjectSourcePaths,
            Map<String, Long> currentProjectSourceMtimes,
            Set<String> currentProjectFilePaths,
            Map<String, Long> currentProjectFileMtimes,
            Set<String> missingOrUnreadablePaths
    ) {
        /**
         * Empty.
         *
         * @return the resolved or constructed object
         */
        public static EvaluationInputs empty() {
            return new EvaluationInputs(
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Set.of(),
                    Map.of(),
                    Set.of(),
                    Map.of(),
                    Set.of()
            );
        }
    }

    /**
     * Should Recompile.
     *
     * @param oldRecord the old record
     * @param sourceHash the source hash
     * @param blockFingerprints the block fingerprints
     * @param expectedOutputs the expected outputs
     * @return true if successful or matching, false otherwise
     */
    public boolean shouldRecompile(SourceTrackingRecord oldRecord,
                                    String sourceHash,
                                    List<String> blockFingerprints,
                                    List<String> expectedOutputs) {
        return evaluate(
                oldRecord == null ? "" : oldRecord.getSourcePath(),
                oldRecord,
                sourceHash,
                blockFingerprints,
                expectedOutputs,
                UNKNOWN_PROJECT_ROOT,
                MTIME_UNAVAILABLE,
                Set.of(),
                null,
                EvaluationInputs.empty(),
                null
        ).shouldReprocess();
    }

    /**
     * Evaluate.
     *
     * @param sourcePath the path of the source file
     * @param oldRecord the old record
     * @param sourceHash the source hash
     * @param blockFingerprints the block fingerprints
     * @param expectedOutputs the expected outputs
     * @param projectRoot the root path of the project
     * @param markdownFileMtime the markdown file mtime
     * @param changedChildSources the changed child sources
     * @param resolvedTargetRoot the resolved target root
     * @param inputs the inputs
     * @param recompileOn the recompile on
     * @return the resolved or constructed object
     */
    public ReprocessingDecision evaluate(String sourcePath,
                                         SourceTrackingRecord oldRecord,
                                         String sourceHash,
                                         List<String> blockFingerprints,
                                         List<String> expectedOutputs,
                                         Path projectRoot,
                                         long markdownFileMtime,
                                         Set<String> changedChildSources,
                                         String resolvedTargetRoot,
                                         EvaluationInputs inputs,
                                         RecompileOnSettings recompileOn) {
        RecompileOnSettings settings =
                recompileOn != null ? recompileOn : new RecompileOnSettings();
        if (oldRecord == null) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD);
        }

        
        String lastStatus = oldRecord.getLastStatus();
        if ("note-pending".equals(lastStatus) || "failed".equals(lastStatus)) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.PRIOR_RECORD_PENDING_NOTES);
        }
        if (!sourceHash.equals(oldRecord.getSourceHash()) || !blockFingerprints.equals(oldRecord.getBlockFingerprints())) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED);
        }
        if (!expectedOutputs.equals(new ArrayList<>(oldRecord.getCompiledFiles().keySet()))) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.OUTPUT_PATHS_CHANGED);
        }
        if (oldRecord.getResolvedTargetRoot() != null && resolvedTargetRoot != null
                && !resolvedTargetRoot.equals(oldRecord.getResolvedTargetRoot())) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED);
        }

        if (hasExtendedInputs(inputs)) {
            if (isSourceTimestampAdvanced(markdownFileMtime, oldRecord.getSourceModificationTime())) {
                return decision(sourcePath, ReprocessingDecision.ReprocessingReason.SOURCE_RECORD_TIMESTAMP_ADVANCED);
            }

            ReprocessingDecision.ReprocessingReason sourceDependencyDecision = evaluateSourceDependencies(oldRecord, inputs, settings);
            if (sourceDependencyDecision != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE) {
                return decision(sourcePath, sourceDependencyDecision);
            }

            if (isProjectRecord(oldRecord.getSourceCategory())) {
                ReprocessingDecision.ReprocessingReason projectDecision = evaluateProjectTrackedSets(oldRecord, inputs);
                if (projectDecision != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE) {
                    return decision(sourcePath, projectDecision);
                }
            }
        }

        if (hasMissingOutputs(expectedOutputs, projectRoot, resolvedTargetRoot)) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.OUTPUT_MISSING);
        }
        if (markdownFileMtime >= 0
                && isSourceNewerThanOutputs(markdownFileMtime, expectedOutputs, projectRoot, resolvedTargetRoot)) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.SOURCE_NEWER_THAN_OUTPUTS);
        }
        if (oldRecord.getMarkdownReferences().keySet().stream().anyMatch(changedChildSources::contains)) {
            return decision(sourcePath, ReprocessingDecision.ReprocessingReason.CHILD_OUTPUT_CHANGED);
        }
        return decision(sourcePath, ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE);
    }

    private boolean isProjectRecord(String sourceCategory) {
        return sourceCategory != null && "project".equalsIgnoreCase(sourceCategory);
    }

    private ReprocessingDecision.ReprocessingReason evaluateSourceDependencies(SourceTrackingRecord oldRecord,
                                                                              EvaluationInputs inputs,
                                                                              RecompileOnSettings settings) {
        if ((settings.isMarkdownReferences() && hasMembershipChange(oldRecord.getMarkdownReferences().keySet(), inputs.currentMarkdownReferenceMtimes().keySet()))
                || (settings.isCompiledFiles() && hasMembershipChange(oldRecord.getCompiledFiles().keySet(), inputs.currentCompiledFileMtimes().keySet()))
                || (settings.isInspectedFiles() && hasMembershipChange(oldRecord.getInspectedFiles().keySet(), inputs.currentInspectedFileMtimes().keySet()))) {
            return ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED;
        }

        if (settings.isMarkdownReferences()) {
            ReprocessingDecision.ReprocessingReason markdownReason = evaluateTrackedMtimeRule(
                    oldRecord.getMarkdownReferences(),
                    inputs.currentMarkdownReferenceMtimes(),
                    inputs.missingOrUnreadablePaths(),
                    ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED
            );
            if (markdownReason != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE) {
                return markdownReason;
            }
        }

        if (settings.isCompiledFiles()) {
            ReprocessingDecision.ReprocessingReason compiledReason = evaluateTrackedMtimeRule(
                    oldRecord.getCompiledFiles(),
                    inputs.currentCompiledFileMtimes(),
                    inputs.missingOrUnreadablePaths(),
                    ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED
            );
            if (compiledReason != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE) {
                return compiledReason;
            }
        }

        if (settings.isInspectedFiles()) {
            return evaluateTrackedMtimeRule(
                    oldRecord.getInspectedFiles(),
                    inputs.currentInspectedFileMtimes(),
                    inputs.missingOrUnreadablePaths(),
                    ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED
            );
        }

        return ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE;
    }

    private ReprocessingDecision.ReprocessingReason evaluateProjectTrackedSets(SourceTrackingRecord oldRecord,
                                                                              EvaluationInputs inputs) {
        if (hasMembershipChange(oldRecord.getMarkdownReferences().keySet(), inputs.currentProjectSourcePaths())
                || hasMembershipChange(oldRecord.getInspectedFiles().keySet(), inputs.currentProjectFilePaths())) {
            return ReprocessingDecision.ReprocessingReason.PROJECT_TRACKED_SET_CHANGED;
        }

        ReprocessingDecision.ReprocessingReason sourceReason = evaluateTrackedMtimeRule(
                oldRecord.getMarkdownReferences(),
                inputs.currentProjectSourceMtimes(),
                inputs.missingOrUnreadablePaths(),
                ReprocessingDecision.ReprocessingReason.PROJECT_RECORD_TIMESTAMP_ADVANCED
        );
        if (sourceReason != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE) {
            return sourceReason;
        }

        return evaluateTrackedMtimeRule(
                oldRecord.getInspectedFiles(),
                inputs.currentProjectFileMtimes(),
                inputs.missingOrUnreadablePaths(),
                ReprocessingDecision.ReprocessingReason.PROJECT_RECORD_TIMESTAMP_ADVANCED
        );
    }

    private ReprocessingDecision.ReprocessingReason evaluateTrackedMtimeRule(Map<String, FileTrackingDetails> recorded,
                                                                            Map<String, Long> current,
                                                                            Set<String> missingOrUnreadablePaths,
                                                                            ReprocessingDecision.ReprocessingReason advancedReason) {
        for (Map.Entry<String, FileTrackingDetails> entry : recorded.entrySet()) {
            String path = entry.getKey();
            Long recordedMtime = entry.getValue() == null ? null : normalizeMillis(entry.getValue().getModificationTime());
            if (recordedMtime == null) {
                return ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MTIME_MISSING;
            }
            if (missingOrUnreadablePaths.contains(path)) {
                return ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MISSING_OR_UNREADABLE;
            }
            Long currentMtime = normalizeMillis(current.get(path));
            if (currentMtime == null) {
                return ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MISSING_OR_UNREADABLE;
            }
            if (currentMtime > recordedMtime) {
                return advancedReason;
            }
        }
        return ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE;
    }

    private boolean hasMembershipChange(Set<String> oldPaths, Set<String> currentPaths) {
        return !oldPaths.equals(currentPaths);
    }

    private boolean hasExtendedInputs(EvaluationInputs inputs) {
        return !(inputs.currentMarkdownReferenceMtimes().isEmpty()
                && inputs.currentCompiledFileMtimes().isEmpty()
                && inputs.currentInspectedFileMtimes().isEmpty()
                && inputs.currentProjectSourcePaths().isEmpty()
                && inputs.currentProjectSourceMtimes().isEmpty()
                && inputs.currentProjectFilePaths().isEmpty()
                && inputs.currentProjectFileMtimes().isEmpty()
                && inputs.missingOrUnreadablePaths().isEmpty());
    }

    private boolean isSourceTimestampAdvanced(long currentMtime, Long recordedMtime) {
        if (currentMtime < 0) {
            return false;
        }
        Long normalizedRecorded = normalizeMillis(recordedMtime);
        if (normalizedRecorded == null) {
            return true;
        }
        long normalizedCurrent = normalizeMillis(currentMtime);
        return normalizedCurrent > normalizedRecorded;
    }

    private Long normalizeMillis(Long value) {
        return value;
    }

    private long normalizeMillis(long value) {
        return value;
    }

    private boolean hasMissingOutputs(List<String> expectedOutputs, Path projectRoot, String resolvedTargetRoot) {
        for (String path : expectedOutputs) {
            if (!Files.exists(resolveOutputPath(projectRoot, path, resolvedTargetRoot))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSourceNewerThanOutputs(long markdownFileMtime,
                                             List<String> expectedOutputs,
                                             Path projectRoot,
                                             String resolvedTargetRoot) {
        for (String path : expectedOutputs) {
            try {
                long outputMtime = Files.getLastModifiedTime(
                        resolveOutputPath(projectRoot, path, resolvedTargetRoot)
                ).toMillis();
                if (markdownFileMtime > outputMtime) {
                    return true;
                }
            } catch (IOException ex) {
                return true;
            }
        }
        return false;
    }

    private Path resolveOutputPath(Path projectRoot, String outputPath, String resolvedTargetRoot) {
        return TrackedPathResolver.resolveTrackedPath(projectRoot, outputPath, resolvedTargetRoot);
    }

    private ReprocessingDecision decision(String sourcePath, ReprocessingDecision.ReprocessingReason reason) {
        return new ReprocessingDecision(sourcePath, reason != ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, reason);
    }
}
