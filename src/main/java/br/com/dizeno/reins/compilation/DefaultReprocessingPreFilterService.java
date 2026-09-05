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

package br.com.dizeno.reins.compilation;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.util.PathNormalizer;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DefaultReprocessingPreFilterService is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class DefaultReprocessingPreFilterService implements ReprocessingPreFilterService {

    private final RecompilationDecider recompilationDecider;
    private final CompilationTrackingStore trackingStore;
    private final SourceFingerprintService fingerprintService;

    /**
     * Constructs a new instance of {@link DefaultReprocessingPreFilterService}.
     *
     * @param recompilationDecider the decider for recompilation needs
     * @param trackingStore the persistence store for file tracking records
     * @param fingerprintService the service used to calculate file fingerprints
     */
    public DefaultReprocessingPreFilterService(RecompilationDecider recompilationDecider,
                                               CompilationTrackingStore trackingStore,
                                               SourceFingerprintService fingerprintService) {
        this.recompilationDecider = recompilationDecider;
        this.trackingStore = trackingStore;
        this.fingerprintService = fingerprintService;
    }

    /**
     * Constructs a new instance of {@link DefaultReprocessingPreFilterService}.
     */
    public DefaultReprocessingPreFilterService() {
        this(new RecompilationDecider(),
             new CompilationTrackingStore(),
             new SourceFingerprintService());
    }

    /**
     * Filter.
     *
     * @param sourceFiles the list of source files to process
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param log the logger instance
     * @return the resulting result
     */
    @Override
    public PreFilterResult filter(List<File> sourceFiles,
                                  ReinsConfig config,
                                  Path projectRoot,
                                  Log log) throws Exception {
        List<PreFilterSkipDecision> skipDecisions = new ArrayList<>();
        List<CycleWorkSetEntry> workSetEntries = new ArrayList<>();
        int validateAllPromotedCount = 0;

        for (File sourceFile : sourceFiles) {
            String relPath = PathNormalizer.toForwardSlashes(
                    projectRoot.toAbsolutePath().normalize()
                               .relativize(sourceFile.toPath().toAbsolutePath().normalize()).toString());

            if (config != null && config.isFreshCompilation()) {
                CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.COMPILE);
                entry.setSelectionReason(ReprocessingDecision.ReprocessingReason.FRESH_COMPILATION);
                workSetEntries.add(entry);
                continue;
            }

            SourceTrackingRecord previous;
            try {
                previous = trackingStore.load(projectRoot, relPath).orElse(null);
            } catch (Exception ex) {
                CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.COMPILE);
                entry.setSelectionReason(ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD);
                workSetEntries.add(entry);
                continue;
            }
            if (previous == null) {
                CycleWorkSetEntry entry = new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.COMPILE);
                entry.setSelectionReason(ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD);
                workSetEntries.add(entry);
                continue;
            }

            String sourceText;
            try {
                sourceText = Files.readString(sourceFile.toPath());
            } catch (Exception ex) {
                workSetEntries.add(new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.COMPILE));
                continue;
            }
            String sourceHash = fingerprintService.sha256(sourceText);

            Set<String> markdownRefPaths = previous.getMarkdownReferences() != null
                    ? previous.getMarkdownReferences().keySet() : Set.of();
            Set<String> compiledPaths = previous.getCompiledFiles() != null
                    ? previous.getCompiledFiles().keySet() : Set.of();
            Set<String> inspectedPaths = previous.getInspectedFiles() != null
                    ? previous.getInspectedFiles().keySet() : Set.of();

            String sourceCategory = resolveSourceCategory(sourceFile.toPath(), config, projectRoot);
            String resolvedTargetRoot = PathNormalizer.toForwardSlashes(
                    projectRoot.relativize(
                        BasePathMappingSet.forScope(config, projectRoot, sourceCategory).getTargetRoot()
                    ).toString());

            MtimeSnapshotUtil.SnapshotResult markdownSnapshot = MtimeSnapshotUtil.snapshot(projectRoot, markdownRefPaths, resolvedTargetRoot);
            MtimeSnapshotUtil.SnapshotResult compiledSnapshot = MtimeSnapshotUtil.snapshot(projectRoot, compiledPaths, resolvedTargetRoot);
            MtimeSnapshotUtil.SnapshotResult inspectedSnapshot = MtimeSnapshotUtil.snapshot(projectRoot, inspectedPaths, resolvedTargetRoot);

            Set<String> missingOrUnreadable = new LinkedHashSet<>();
            missingOrUnreadable.addAll(markdownSnapshot.missingOrUnreadablePaths());
            missingOrUnreadable.addAll(compiledSnapshot.missingOrUnreadablePaths());
            missingOrUnreadable.addAll(inspectedSnapshot.missingOrUnreadablePaths());

            long markdownFileMtime = sourceFile.lastModified();
            List<String> outputPathStrings = new ArrayList<>(compiledPaths);

            ReprocessingDecision decision = recompilationDecider.evaluate(
                    relPath,
                    previous,
                    sourceHash,
                    List.of(sourceHash),
                    outputPathStrings,
                    projectRoot,
                    markdownFileMtime,
                    Set.of(),
                    resolvedTargetRoot,
                    new RecompilationDecider.EvaluationInputs(
                            markdownSnapshot.mtimes(),
                            compiledSnapshot.mtimes(),
                            inspectedSnapshot.mtimes(),
                            Set.of(),
                            Map.of(),
                            Set.of(),
                            Map.of(),
                            missingOrUnreadable
                    ),
                    config.getRecompileOn()
            );

            if (decision.shouldReprocess()) {
                CycleWorkSetEntry compileEntry = new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.COMPILE);
                compileEntry.setSelectionReason(decision.reason());
                workSetEntries.add(compileEntry);
            } else {
                workSetEntries.add(new CycleWorkSetEntry(sourceFile, relPath, SourceProcessingStatus.SKIP));
                skipDecisions.add(new PreFilterSkipDecision(relPath, decision.reason().name()));
            }

            if (config != null && config.isValidateAll()
                    && previous != null
                    && isValidateAllEligible(previous.getLastStatus())) {
                CycleWorkSetEntry latestEntry = workSetEntries.get(workSetEntries.size() - 1);
                if (latestEntry.markValidateIfSkippable("validate-all")
                        && latestEntry.getStatus() == SourceProcessingStatus.VALIDATE) {
                    validateAllPromotedCount++;
                }
            }
        }

        return new PreFilterResult(sourceFiles, workSetEntries, skipDecisions, validateAllPromotedCount);
    }

    private boolean isValidateAllEligible(String lastStatus) {
        if (lastStatus == null) {
            return false;
        }
        return "success".equalsIgnoreCase(lastStatus)
                || "skipped".equalsIgnoreCase(lastStatus)
                || "no-change".equalsIgnoreCase(lastStatus)
                || "nochanges".equalsIgnoreCase(lastStatus)
                || "validated".equalsIgnoreCase(lastStatus);
    }

    private String resolveSourceCategory(Path sourcePath, ReinsConfig config, Path projectRoot) {
        Path normalized = sourcePath.toAbsolutePath().normalize();
        if (config != null && config.getSourceBases() != null) {
            for (Map.Entry<String, File> entry : config.getSourceBases().entrySet()) {
                if (entry.getValue() != null) {
                    Path baseRoot = entry.getValue().toPath().toAbsolutePath().normalize();
                    if (normalized.startsWith(baseRoot)) {
                        return entry.getKey().toLowerCase(java.util.Locale.ROOT);
                    }
                }
            }
        }
        return SourceScope.UNCLASSIFIED.value();
    }
}
