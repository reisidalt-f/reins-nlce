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

import br.com.dizeno.reins.compilation.MtimeSnapshotUtil;
import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.source.domain.FileReference;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TrackingRecordHelper is part of the sequential execution of compilation
 * phases (reading, tracking, LLM reasoning, writing, and printing) in the reins
 * architecture.
 * Acts as a helper utility assisting in its prefix tasks.
 */
public final class TrackingRecordHelper {

    private TrackingRecordHelper() {
    }

    /**
     * TrackingParams is part of the sequential execution of compilation phases
     * (reading, tracking, LLM reasoning, writing, and printing) in the reins
     * architecture.
     * Acts as a component managing tracking params.
     */
    public record TrackingParams(
            String sourcePath, String sourceCategory, String sourceHash, Long sourceModificationTime,
            List<String> fingerprints, List<String> compiledPaths,
            List<String> inspectedPaths,
            Map<String, String> compiledFileCategories,
            Map<String, String> inspectedFileCategories,
            Map<String, Long> postMtimes,
            Map<String, Long> inspectedMtimes,
            Map<String, Long> markdownReferenceMtimes, ReinsConfig config, String outputPolicy,
            String resolvedTargetRoot) {
    }

    /**
     * Builds the configured target tracking record.
     *
     * @param p                  the p
     * @param fingerprintService the service used to calculate file fingerprints
     * @return the resulting record
     */
    public static SourceTrackingRecord buildTrackingRecord(TrackingParams p,
            SourceFingerprintService fingerprintService) {
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(p.sourcePath());
        record.setSourceCategory(p.sourceCategory());
        record.setSourceHash(p.sourceHash());
        record.setSourceModificationTime(p.sourceModificationTime());
        record.setBlockFingerprints(p.fingerprints());
        record.setMarkdownReferences(p.markdownReferenceMtimes().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new FileTrackingDetails(entry.getKey(), p.sourceCategory(), entry.getValue()),
                (a, b) -> b,
                LinkedHashMap::new)));
        record.setInferenceFingerprint(
                fingerprintService.sha256(p.sourceHash() + "\n" + String.join("\n", p.fingerprints())));
        record.setModel(p.config().resolveModel());
        record.setOutputPolicy(p.outputPolicy());
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");
        record.setResolvedTargetRoot(p.resolvedTargetRoot());

        record.setCompiledFiles(p.compiledPaths().stream().collect(Collectors.toMap(
                path -> path,
                path -> new FileTrackingDetails(path, p.compiledFileCategories().get(path),
                        p.postMtimes().getOrDefault(path, null)),
                (a, b) -> b,
                LinkedHashMap::new)));

        record.setInspectedFiles(p.inspectedPaths().stream().collect(Collectors.toMap(
                path -> path,
                path -> new FileTrackingDetails(path, p.inspectedFileCategories().get(path),
                        p.inspectedMtimes().getOrDefault(path, null)),
                (a, b) -> b,
                LinkedHashMap::new)));

        return record;
    }

    /**
     * Copy Tracking Record.
     *
     * @param source the source
     * @return the resulting record
     */
    public static SourceTrackingRecord copyTrackingRecord(SourceTrackingRecord source) {
        SourceTrackingRecord copy = new SourceTrackingRecord();
        if (source == null) {
            return copy;
        }
        copy.setSourcePath(source.getSourcePath());
        copy.setSourceCategory(source.getSourceCategory());
        copy.setSourceHash(source.getSourceHash());
        copy.setSourceModificationTime(source.getSourceModificationTime());
        copy.setBlockFingerprints(source.getBlockFingerprints());
        copy.setMarkdownReferences(source.getMarkdownReferences());
        copy.setInferenceFingerprint(source.getInferenceFingerprint());
        copy.setModel(source.getModel());
        copy.setOutputPolicy(source.getOutputPolicy());
        copy.setLastCompiledAt(source.getLastCompiledAt());
        copy.setLastStatus(source.getLastStatus());
        copy.setResolvedTargetRoot(source.getResolvedTargetRoot());
        copy.setCompiledFiles(source.getCompiledFiles());
        copy.setInspectedFiles(source.getInspectedFiles());
        copy.setNotes(source.getNotes());
        return copy;
    }

    /**
     * Refresh Tracked Mtimes.
     *
     * @param record             the tracking record
     * @param projectRoot        the root path of the project
     * @param resolvedTargetRoot the resolved target root
     * @param sourceCategory     the category of the source file (e.g. main or test)
     */
    public static void refreshTrackedMtimes(SourceTrackingRecord record,
            Path projectRoot,
            String resolvedTargetRoot,
            String sourceCategory) {
        if (record == null) {
            return;
        }
        record.setMarkdownReferences(refreshTrackingMapMtimes(
                record.getMarkdownReferences(),
                projectRoot,
                resolvedTargetRoot,
                sourceCategory));
        record.setCompiledFiles(refreshTrackingMapMtimes(
                record.getCompiledFiles(),
                projectRoot,
                resolvedTargetRoot,
                sourceCategory));
        record.setInspectedFiles(refreshTrackingMapMtimes(
                record.getInspectedFiles(),
                projectRoot,
                resolvedTargetRoot,
                sourceCategory));
    }

    /**
     * Refresh Tracking Map Mtimes.
     *
     * @param trackedFiles       the tracked files
     * @param projectRoot        the root path of the project
     * @param resolvedTargetRoot the resolved target root
     * @param sourceCategory     the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static Map<String, FileTrackingDetails> refreshTrackingMapMtimes(
            Map<String, FileTrackingDetails> trackedFiles,
            Path projectRoot,
            String resolvedTargetRoot,
            String sourceCategory) {
        if (trackedFiles == null || trackedFiles.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Set<String> trackedPaths = new LinkedHashSet<>(trackedFiles.keySet());
        Map<String, Long> currentMtimes = MtimeSnapshotUtil.snapshot(projectRoot, trackedPaths, resolvedTargetRoot)
                .mtimes();

        LinkedHashMap<String, FileTrackingDetails> refreshed = new LinkedHashMap<>();
        for (Map.Entry<String, FileTrackingDetails> entry : trackedFiles.entrySet()) {
            String trackedPath = entry.getKey();
            FileTrackingDetails details = entry.getValue();
            String tracking = details == null || details.getTracking() == null
                    ? trackedPath
                    : details.getTracking();
            String category = details == null || details.getCategory() == null
                    ? sourceCategory
                    : details.getCategory();
            Long modificationTime = currentMtimes.get(trackedPath);
            refreshed.put(trackedPath, new FileTrackingDetails(tracking, category, modificationTime));
        }
        return refreshed;
    }

    /**
     * Sanitize Stored Tracking Record.
     *
     * @param record     the tracking record
     * @param sourcePath the path of the source file
     * @param log        the logger instance
     * @return the resulting record
     */
    public static SourceTrackingRecord sanitizeStoredTrackingRecord(SourceTrackingRecord record,
            String sourcePath,
            Log log) {
        if (record == null) {
            return null;
        }

        SourceTrackingRecord sanitized = copyTrackingRecord(record);
        sanitized.setCompiledFiles(filterCanonicalTrackedFiles(record.getCompiledFiles(), sourcePath, "compiled", log));
        sanitized.setInspectedFiles(
                filterCanonicalTrackedFiles(record.getInspectedFiles(), sourcePath, "inspected", log));
        return sanitized;
    }

    /**
     * Filter Canonical Tracked Files.
     *
     * @param files      the list of files
     * @param sourcePath the path of the source file
     * @param category   the category
     * @param log        the logger instance
     * @return the string result
     */
    public static Map<String, FileTrackingDetails> filterCanonicalTrackedFiles(
            Map<String, FileTrackingDetails> files,
            String sourcePath,
            String category,
            Log log) {
        LinkedHashMap<String, FileTrackingDetails> filtered = new LinkedHashMap<>();
        if (files == null || files.isEmpty()) {
            return filtered;
        }

        for (Map.Entry<String, FileTrackingDetails> entry : files.entrySet()) {
            String trackedPath = entry.getKey();
            if (!TrackedPathResolver.looksCanonical(trackedPath)) {
                log.warn("Dropping non-canonical " + category + " tracked path for " + sourcePath + ": " + trackedPath);
                continue;
            }
            filtered.put(FileReference.fromCanonical(trackedPath).toCanonicalString(), entry.getValue());
        }
        return filtered;
    }

    /**
     * Physically Touch Outputs.
     *
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param record              the tracking record
     * @param projectRoot         the root path of the project
     * @param config              the Reins configuration settings
     * @param log                 the logger instance
     * @param verboseLogging      the verbose logging
     */
    public static void physicallyTouchOutputs(String canonicalSourcePath,
            SourceTrackingRecord record,
            Path projectRoot,
            ReinsConfig config,
            Log log,
            boolean verboseLogging) {
        if (record == null || record.getCompiledFiles() == null || record.getCompiledFiles().isEmpty()) {
            return;
        }

        boolean dryRun = config.isDryRun();

        for (String trackedPath : record.getCompiledFiles().keySet()) {
            try {
                Path absolutePath = TrackedPathResolver.resolveTrackedPath(projectRoot, trackedPath,
                        record.getResolvedTargetRoot());
                if (Files.isRegularFile(absolutePath)) {
                    if (dryRun) {
                        log.info("[dry-run] Would touch output file: " + trackedPath);
                    } else {
                        Files.setLastModifiedTime(absolutePath, java.nio.file.attribute.FileTime.from(Instant.now()));
                        if (verboseLogging) {
                            log.info("Touched output file: " + trackedPath);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not touch output file " + trackedPath + " for source " + canonicalSourcePath + ": "
                        + ex.getMessage());
            }
        }
    }

    /**
     * Preserve Existing Compiled Files.
     *
     * @param projectRoot            the root path of the project
     * @param previous               the previous
     * @param resolvedTargetRoot     the resolved target root
     * @param sourceCategory         the category of the source file (e.g. main or
     *                               test)
     * @param compiled               the compiled
     * @param compiledFileCategories the compiled file categories
     */
    public static void preserveExistingCompiledFiles(Path projectRoot,
            SourceTrackingRecord previous,
            String resolvedTargetRoot,
            String sourceCategory,
            List<String> compiled,
            Map<String, String> compiledFileCategories) {
        if (previous == null || previous.getCompiledFiles() == null || previous.getCompiledFiles().isEmpty()) {
            return;
        }

        for (Map.Entry<String, FileTrackingDetails> entry : previous.getCompiledFiles().entrySet()) {
            String trackedPath = FileReference.fromCanonical(entry.getKey()).toCanonicalString();
            if (compiledFileCategories.containsKey(trackedPath)
                    || !trackedPathStillExists(projectRoot, trackedPath, resolvedTargetRoot)) {
                continue;
            }
            compiled.add(trackedPath);
            FileTrackingDetails details = entry.getValue();
            String category = details != null && details.getCategory() != null && !details.getCategory().isBlank()
                    ? details.getCategory()
                    : sourceCategory;
            compiledFileCategories.put(trackedPath, category);
        }
    }

    /**
     * Cleanup Stale Compiled Files.
     *
     * @param projectRoot          the root path of the project
     * @param previous             the previous
     * @param currentCompiledPaths the current compiled paths
     * @param resolvedTargetRoot   the resolved target root
     * @param log                  the logger instance
     */
    public static void cleanupStaleCompiledFiles(Path projectRoot,
            SourceTrackingRecord previous,
            List<String> currentCompiledPaths,
            String resolvedTargetRoot,
            Log log) {
        if (previous == null || previous.getCompiledFiles() == null || previous.getCompiledFiles().isEmpty()) {
            return;
        }
        Set<String> current = new LinkedHashSet<>(currentCompiledPaths);
        for (String priorPath : previous.getCompiledFiles().keySet()) {
            String canonical = FileReference.fromCanonical(priorPath).toCanonicalString();
            if (current.contains(canonical)) {
                continue;
            }
            try {
                Path stalePath = TrackedPathResolver.resolveTrackedPath(projectRoot, canonical, resolvedTargetRoot);
                Files.deleteIfExists(stalePath);
                log.info("Deleted stale compiled file: " + canonical);
            } catch (Exception ex) {
                log.warn("Could not delete stale compiled file " + canonical + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Snapshot Output Mtimes.
     *
     * @param projectRoot        the root path of the project
     * @param outputPaths        the output paths
     * @param resolvedTargetRoot the resolved target root
     * @return the string result
     */
    public static Map<String, Long> snapshotOutputMtimes(Path projectRoot,
            List<String> outputPaths,
            String resolvedTargetRoot) throws Exception {
        Map<String, Long> mtimes = new LinkedHashMap<>();
        for (String outputPath : outputPaths) {
            Path absolutePath = TrackedPathResolver.resolveTrackedPath(projectRoot, outputPath, resolvedTargetRoot);
            if (Files.exists(absolutePath)) {
                mtimes.put(outputPath, Files.getLastModifiedTime(absolutePath).toMillis());
            }
        }
        return mtimes;
    }

    /**
     * Outputs Changed.
     *
     * @param previous      the previous
     * @param compiledPaths the compiled paths
     * @param preMtimes     the pre mtimes
     * @param postMtimes    the post mtimes
     * @return true if successful or matching, false otherwise
     */
    public static boolean outputsChanged(SourceTrackingRecord previous,
            List<String> compiledPaths,
            Map<String, Long> preMtimes,
            Map<String, Long> postMtimes) {
        Set<String> currentPaths = new LinkedHashSet<>(compiledPaths);
        Set<String> previousPaths = previous == null || previous.getCompiledFiles() == null
                ? Set.of()
                : new LinkedHashSet<>(previous.getCompiledFiles().keySet());
        if (previous == null || !previousPaths.equals(currentPaths)) {
            return true;
        }
        for (String path : compiledPaths) {
            if (!java.util.Objects.equals(preMtimes.get(path), postMtimes.get(path))) {
                return true;
            }
        }
        return false;
    }

    private static boolean trackedPathStillExists(Path projectRoot,
            String trackedPath,
            String resolvedTargetRoot) {
        try {
            return Files.isRegularFile(
                    TrackedPathResolver.resolveTrackedPath(projectRoot, trackedPath, resolvedTargetRoot));
        } catch (Exception ex) {
            return false;
        }
    }
}
