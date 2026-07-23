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
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.util.PathNormalizer;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DefaultReprocessingPreFilterService is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class DefaultReprocessingPreFilterService implements ReprocessingPreFilterService {

    private final RecompilationDecider recompilationDecider;
    private final CompilationTrackingStore trackingStore;
    private final SourceFingerprintService fingerprintService;
    private final ProjectContextService projectContextService;

    /**
     * Constructs a new instance of {@link DefaultReprocessingPreFilterService}.
     *
     * @param recompilationDecider the decider for recompilation needs
     * @param trackingStore the persistence store for file tracking records
     * @param fingerprintService the service used to calculate file fingerprints
     * @param projectContextService the service managing project execution context
     */
    public DefaultReprocessingPreFilterService(RecompilationDecider recompilationDecider,
                                               CompilationTrackingStore trackingStore,
                                               SourceFingerprintService fingerprintService,
                                               ProjectContextService projectContextService) {
        this.recompilationDecider = recompilationDecider;
        this.trackingStore = trackingStore;
        this.fingerprintService = fingerprintService;
        this.projectContextService = projectContextService;
    }

    /**
     * Constructs a new instance of {@link DefaultReprocessingPreFilterService}.
     */
    public DefaultReprocessingPreFilterService() {
        this(new RecompilationDecider(),
             new CompilationTrackingStore(),
             new SourceFingerprintService(),
             new ProjectContextService());
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

                String sourceCategory = resolveSourceCategory(sourceFile.toPath(), projectRoot);
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

        boolean runProjectInference = evaluateProjectFile(config, projectRoot, sourceFiles, log);

        return new PreFilterResult(sourceFiles, workSetEntries, runProjectInference, skipDecisions, validateAllPromotedCount);
    }

    private boolean isValidateAllEligible(String lastStatus) {
        if (lastStatus == null) {
            return false;
        }
        return "success".equalsIgnoreCase(lastStatus) || "skipped".equalsIgnoreCase(lastStatus);
    }

    private boolean evaluateProjectFile(ReinsConfig config,
                                        Path projectRoot,
                                        List<File> sourceFiles,
                                        Log log) {
        if (!config.isEnableProjectInference() || config.getProjectContextFile() == null) {
            return true;
        }
        try {
            Set<Path> contextScanRoots = buildContextScanRoots(config);
            CompilationBackgroundPayload payload = contextScanRoots.isEmpty()
                    ? projectContextService.load(config.getProjectContextFile(), projectRoot)
                    : projectContextService.load(config.getProjectContextFile(), projectRoot, contextScanRoots);
            if (payload.isEmpty()) {
                return true;
            }

            CompilationBackgroundFile projectFile = payload.getFiles().get(0);
            Path absPath = projectFile.getAbsolutePath().normalize();
            String sourcePath = PathNormalizer.toForwardSlashes(
                    projectRoot.toAbsolutePath().normalize().relativize(absPath.toAbsolutePath()).toString());

            SourceTrackingRecord previous;
            try {
                previous = trackingStore.load(projectRoot, sourcePath).orElse(null);
            } catch (Exception ex) {
                return true;
            }
            if (previous == null) {
                return true;
            }

            String sourceHash = fingerprintService.sha256(projectFile.getContent());
            long sourceMtime = readFileMtime(absPath);

            Set<String> currentProjectSourcePaths = sourceFiles.stream()
                    .map(f -> PathNormalizer.toForwardSlashes(
                            projectRoot.toAbsolutePath().normalize()
                                       .relativize(f.toPath().toAbsolutePath().normalize()).toString()))
                    .map(trackingStore::canonicalizePath)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

            
            
            
            
            
            
            Set<String> currentProjectFilePaths = new LinkedHashSet<>();
            for (CompilationBackgroundFile pf : payload.getFiles()) {
                currentProjectFilePaths.add(PathNormalizer.toForwardSlashes(
                        projectRoot.toAbsolutePath().normalize()
                                   .relativize(pf.getAbsolutePath().normalize().toAbsolutePath()).toString()));
            }
            currentProjectFilePaths.addAll(previous.getInspectedFiles().keySet());

                String resolvedTargetRoot = PathNormalizer.toForwardSlashes(
                    projectRoot.relativize(
                        BasePathMappingSet.fromConfig(config, projectRoot).getTargetRoot()
                    ).toString());

            MtimeSnapshotUtil.SnapshotResult sourceSnapshot = MtimeSnapshotUtil.snapshot(projectRoot, currentProjectSourcePaths, resolvedTargetRoot);
            MtimeSnapshotUtil.SnapshotResult projectSnapshot = MtimeSnapshotUtil.snapshot(projectRoot, currentProjectFilePaths, resolvedTargetRoot);

            Set<String> missingOrUnreadable = new LinkedHashSet<>();
            missingOrUnreadable.addAll(sourceSnapshot.missingOrUnreadablePaths());
            missingOrUnreadable.addAll(projectSnapshot.missingOrUnreadablePaths());

            ReprocessingDecision decision = recompilationDecider.evaluate(
                    sourcePath,
                    previous,
                    sourceHash,
                    List.of(sourceHash),
                    new ArrayList<>(previous.getCompiledFiles().keySet()),
                    projectRoot,
                    sourceMtime,
                    Set.of(),
                    resolvedTargetRoot,
                    new RecompilationDecider.EvaluationInputs(
                            sourceSnapshot.mtimes(),
                            Map.of(),
                            Map.of(),
                            currentProjectSourcePaths,
                            sourceSnapshot.mtimes(),
                            currentProjectFilePaths,
                            projectSnapshot.mtimes(),
                            missingOrUnreadable
                    ),
                    config.getRecompileOn()
            );

            return decision.shouldReprocess();
        } catch (Exception ex) {
            log.warn("Pre-filter: project file evaluation failed — including in run. Cause: " + ex.getMessage());
            return true;
        }
    }

    private long readFileMtime(Path absolutePath) {
        try {
            FileTime ft = Files.getLastModifiedTime(absolutePath);
            return ft.toMillis();
        } catch (Exception ex) {
            return RecompilationDecider.MTIME_UNAVAILABLE;
        }
    }

    private String resolveSourceCategory(Path sourcePath, Path projectRoot) {
        Path normalized = sourcePath.toAbsolutePath().normalize();
        if (normalized.startsWith(projectRoot.resolve(ProjectDirectoryPaths.MAIN_NL_ROOT).normalize())) {
            return SourceScope.MAIN.value();
        }
        if (normalized.startsWith(projectRoot.resolve(ProjectDirectoryPaths.TEST_NL_ROOT).normalize())) {
            return SourceScope.TEST.value();
        }
        return SourceScope.UNCLASSIFIED.value();
    }

    private Set<Path> buildContextScanRoots(ReinsConfig config) {
        Set<Path> roots = new LinkedHashSet<>();
        if (config.getScanRoots() != null && !config.getScanRoots().isEmpty()) {
            for (File root : config.getScanRoots()) {
                if (root != null) {
                    roots.add(root.toPath().toAbsolutePath().normalize());
                }
            }
            return roots;
        }
        if (config.getMainNlRoot() != null) {
            roots.add(config.getMainNlRoot().toPath().toAbsolutePath().normalize());
        }
        if (config.getTestNlRoot() != null) {
            roots.add(config.getTestNlRoot().toPath().toAbsolutePath().normalize());
        }
        return roots;
    }
}
