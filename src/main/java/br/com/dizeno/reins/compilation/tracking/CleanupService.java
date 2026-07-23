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

import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.util.PathNormalizer;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * CleanupService is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class CleanupService {

    /**
     * TargetEntry is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    private static class TargetEntry {
        private final CleanupTarget target;

        private TargetEntry(CleanupTarget target) {
            this.target = target;
        }
    }
    
     
    /**
     * Executes the operation cleanup.
     *
     * @param projectRoot the root path of the project
     * @param validator the path validator for security boundary checks
     * @return the resulting summary
     */
    public CleanupOutcomeSummary executeCleanup(Path projectRoot,
                                                PathValidator validator) {
        Instant completedAt = Instant.now();
        List<CleanupOutcome> outcomes = new ArrayList<>();
        List<TargetEntry> targets = new ArrayList<>();
        Set<String> seenCleanupPaths = new HashSet<>();
        CompilationTrackingStore trackingStore = new CompilationTrackingStore();

        try {
            
            List<String> trackedSources = trackingStore.listAllTrackedSourcePaths(projectRoot);
            for (String sourcePath : trackedSources) {
                Optional<SourceTrackingRecord> record = trackingStore.load(projectRoot, sourcePath);
                if (record.isPresent()) {
                    collectCompiledTargetsFromRecord(projectRoot, record.get(), targets, seenCleanupPaths, outcomes);
                }
            }

            
            collectTrackingArtifactTargets(projectRoot, trackingStore, trackedSources, targets, seenCleanupPaths);

            CleanupTargetValidator targetValidator = new CleanupTargetValidator(validator);
            CleanupExecutor executor = new CleanupExecutor(targetValidator, projectRoot);

            for (TargetEntry entry : targets) {
                CleanupOutcome outcome = executor.deleteTarget(entry.target);
                outcomes.add(outcome);
            }

            completedAt = Instant.now();
            CleanupStatistics statistics = buildStatistics(outcomes, 0);
            CleanupOutcomeSummary.OverallStatus status = statistics.hasFailures()
                    ? CleanupOutcomeSummary.OverallStatus.PARTIAL_FAILURE
                    : CleanupOutcomeSummary.OverallStatus.SUCCESS;

            return new CleanupOutcomeSummary.Builder(projectRoot)
                    .completedAt(completedAt)
                    .outcomes(outcomes)
                    .overallStatus(status)
                    .statistics(statistics)
                    .build();
        } catch (Exception e) {
            CleanupTarget target = new CleanupTarget("target:.reins/compilation-tracking", CleanupTarget.Type.DIRECTORY,
                    "system", 0L, Instant.now());
            outcomes.add(new CleanupOutcome(
                    target,
                    CleanupOutcome.Status.FAILED_DELETION,
                    "Cleanup execution failed: " + e.getMessage(),
                    e,
                    Instant.now()
            ));
            CleanupStatistics statistics = buildStatistics(outcomes, 0);
            return new CleanupOutcomeSummary.Builder(projectRoot)
                    .completedAt(Instant.now())
                    .outcomes(outcomes)
                    .overallStatus(CleanupOutcomeSummary.OverallStatus.PARTIAL_FAILURE)
                    .statistics(statistics)
                    .build();
        }
    }

    private void collectCompiledTargetsFromRecord(Path projectRoot,
                                                   SourceTrackingRecord record,
                                                   List<TargetEntry> targets,
                                                   Set<String> seenCleanupPaths,
                                                   List<CleanupOutcome> outcomes) {
        if (record == null || record.getCompiledFiles() == null || record.getCompiledFiles().isEmpty()) {
            return;
        }
        for (Map.Entry<String, FileTrackingDetails> compiled : record.getCompiledFiles().entrySet()) {
            String trackedPath = compiled.getKey();
            try {
                Path absolutePath = resolveCompiledPath(projectRoot, trackedPath, record);
                String cleanupPath = toCleanupCanonicalPath(projectRoot, absolutePath);
                if (!seenCleanupPaths.add(cleanupPath)) {
                    continue;
                }
                CleanupTarget target = new CleanupTarget(
                        cleanupPath,
                        CleanupTarget.Type.FILE,
                        record.getSourcePath(),
                        0L,
                        Instant.now()
                );
                targets.add(new TargetEntry(target));
            } catch (Exception ex) {
                String fallback = TrackedPathResolver.looksCanonical(trackedPath)
                        ? trackedPath
                        : "target:" + trackedPath;
                CleanupTarget target = new CleanupTarget(
                        fallback,
                        CleanupTarget.Type.FILE,
                        record.getSourcePath(),
                        0L,
                        Instant.now()
                );
                outcomes.add(new CleanupOutcome(
                        target,
                        CleanupOutcome.Status.REJECTED,
                        "Invalid tracked compiled path: " + ex.getMessage(),
                        null,
                        Instant.now()
                ));
            }
        }
    }

    private void collectTrackingArtifactTargets(Path projectRoot,
                                                   CompilationTrackingStore trackingStore,
                                                   List<String> trackedSources,
                                                   List<TargetEntry> targets,
                                                   Set<String> seenCleanupPaths) {
        for (String sourcePath : trackedSources) {
            Path trackingFile = trackingStore.trackingFilePath(projectRoot, sourcePath);
            addTrackingTarget(projectRoot, trackingFile, targets, seenCleanupPaths);
        }
    }

    private void addTrackingTarget(Path projectRoot,
                                      Path absolutePath,
                                      List<TargetEntry> targets,
                                      Set<String> seenCleanupPaths) {
        String cleanupPath = toCleanupCanonicalPath(projectRoot, absolutePath);
        if (!seenCleanupPaths.add(cleanupPath)) {
            return;
        }
        CleanupTarget target = new CleanupTarget(
                cleanupPath,
                CleanupTarget.Type.FILE,
                "system",
                0L,
                Instant.now()
        );
        targets.add(new TargetEntry(target));
    }

    private String toCleanupCanonicalPath(Path projectRoot, Path absolutePath) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        Path relative = projectRoot.toAbsolutePath().normalize().relativize(normalized);
        return "target:" + PathNormalizer.toForwardSlashes(relative.toString());
    }

    private Path resolveCompiledPath(Path projectRoot,
                                      String trackedPath,
                                      SourceTrackingRecord record) {
        try {
            return TrackedPathResolver.resolveTrackedPath(projectRoot, trackedPath, record.getResolvedTargetRoot());
        } catch (IllegalArgumentException ex) {
            String normalized = trackedPath == null ? "" : trackedPath.trim().replace('\\', '/');
            if (normalized.startsWith("target:")) {
                String relative = normalized.substring("target:".length());
                if (relative.startsWith("src/") || relative.startsWith(".reins/") || relative.startsWith("target/")) {
                    return projectRoot.resolve(relative).normalize();
                }
                if (relative.startsWith("main/") || relative.startsWith("test/")) {
                    return projectRoot.resolve("src/").resolve(relative).normalize();
                }
            }
            throw ex;
        }
    }

    private CleanupStatistics buildStatistics(List<CleanupOutcome> outcomes, int recordsUpdated) {
        int deleted = 0;
        int alreadyMissing = 0;
        int rejected = 0;
        int failed = 0;

        for (CleanupOutcome outcome : outcomes) {
            switch (outcome.getStatus()) {
                case DELETED -> deleted++;
                case ALREADY_MISSING -> alreadyMissing++;
                case REJECTED -> rejected++;
                case FAILED_DELETION -> failed++;
                default -> {
                }
            }
        }

        return new CleanupStatistics(
                outcomes.size(),
                deleted,
                alreadyMissing,
                rejected,
                failed,
                recordsUpdated
            );
    }
}
