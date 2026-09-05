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

package br.com.dizeno.reins.run;

import br.com.dizeno.reins.compilation.CompilationService;
import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.CycleWorkSetEntry;
import br.com.dizeno.reins.compilation.DefaultReprocessingPreFilterService;
import br.com.dizeno.reins.compilation.PreFilterResult;
import br.com.dizeno.reins.compilation.ProcessingResult;
import br.com.dizeno.reins.compilation.ReprocessingPreFilterService;
import br.com.dizeno.reins.compilation.SourceFileProcessor;
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.compilation.tracking.CleanupOutcomeSummary;
import br.com.dizeno.reins.compilation.tracking.CleanupReporter;
import br.com.dizeno.reins.compilation.tracking.CleanupService;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.run.mojo.ExplicitSourceResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.source.scanner.CoderMdScanner;
import br.com.dizeno.reins.source.scanner.SourceDiscoveryMode;
import br.com.dizeno.reins.util.PathNormalizer;
import br.com.dizeno.reins.util.PathLogFormatter;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ReinsRunner is part of the entry points and integrations for running reins
 * via Mojo, CLI, or library programmatic access in the reins architecture.
 * Coordinates cleanup, workspace checks, and compilation runs across CLI, Mojo,
 * and library entry points.
 */
public class ReinsRunner {

    private CompilationService compilationService = new CompilationService();
    private ReprocessingPreFilterService preFilterService = new DefaultReprocessingPreFilterService();

    /**
     * Sets the compilation service.
     *
     * @param compilationService the compilation service
     */
    public void setCompilationService(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    /**
     * Sets the pre filter service.
     *
     * @param preFilterService the pre filter service
     */
    public void setPreFilterService(ReprocessingPreFilterService preFilterService) {
        this.preFilterService = preFilterService;
    }

    /**
     * Compile.
     *
     * @param config  the Reins configuration settings
     * @param baseDir the base dir
     * @param log     the logger instance
     * @return the resulting summary
     */
    public CompilationSummary compile(ReinsConfig config, File baseDir, Log log) throws Exception {
        Path projectRoot = baseDir.toPath().toAbsolutePath().normalize();
        if (config.getTracking().isFreezeState()) {
            log.info("[tracking] Freeze mode ENABLED — tracking files will be read but NOT updated.");
        } else {
            log.debug("[tracking] Freeze mode DISABLED — tracking files will be updated normally.");
        }

        new ConfigValidator().validate(config, baseDir, msg -> log.warn(msg));

        String configuredScriptDir = config.getReasoning() == null ? null : config.getReasoning().getScriptsPath();
        File resolvedScriptDir = resolveScriptDir(configuredScriptDir, baseDir);
        ScriptRegistry scriptRegistry = ScriptRegistry.build(new ScriptResolver(resolvedScriptDir, null),
                resolvedScriptDir);
        scriptRegistry.validateAll();
        log.debug("[reins-script] Script registry ready with "
                + scriptRegistry.getAllScripts().size() + " scripts.");

        RecompileOnSettings ros = config.getRecompileOn();
        log.info("recompileOn: markdownReferences=" + ros.isMarkdownReferences()
                + ", inspectedFiles=" + ros.isInspectedFiles()
                + ", compiledFiles=" + ros.isCompiledFiles());

        PathValidator pathValidator = new PathValidator(projectRoot);
        List<File> files;
        SourceDiscoveryMode discoveryMode = SourceDiscoveryMode.FULL_SCAN;
        if (config.isExplicitSourceMode()) {
            discoveryMode = SourceDiscoveryMode.EXPLICIT_SOURCE;
            ExplicitSourceResolver.ResolutionResult resolution = new ExplicitSourceResolver().resolve(
                    config.getSource(),
                    projectRoot,
                    config.getSourceBases(),
                    config.getIncludePattern(),
                    pathValidator);

            String displayPaths = resolution.resolvedPaths().stream()
                    .map(p -> PathLogFormatter.formatPath(p, projectRoot))
                    .collect(java.util.stream.Collectors.joining(", "));
            log.info("Explicit source mode active: " + displayPaths);

            files = resolution.files();
            if (resolution.directory() && files.isEmpty()) {
                log.warn("Explicit source directory contains zero eligible files: " + displayPaths);
                log.info(
                        "Summary: discovered=0, dependencyLinks=0, processed=0, compiled=0, noChange=0, skipped=0, failed=0, reprocessedDueToStaleness=0, reprocessedDueToChildChange=0");
                return new CompilationSummary();
            }
        } else {
            files = new CoderMdScanner().scan(
                    config.getScanRoots(),
                    config.getIncludePattern(),
                    pathValidator,
                    discoveryMode);
        }

        if (files.isEmpty()) {
            log.info("No source instruction files found in scan roots. No inference will be performed.");
            return new CompilationSummary();
        }

        PreFilterResult preFilterResult;
        if (discoveryMode == SourceDiscoveryMode.EXPLICIT_SOURCE) {
            preFilterResult = new PreFilterResult(files, buildExplicitWorkSet(files, projectRoot), List.of());
            log.info("Explicit source mode processed files: " + preFilterResult.getFilesToProcess().size());
        } else {
            preFilterResult = preFilterService.filter(files, config, projectRoot, log);
            if (config.getLog() != null && config.getLog().isSkipped()) {
                preFilterResult.getSkipDecisions().forEach(
                        skip -> log.info("Pre-filter: skipping " + PathLogFormatter.formatPath(skip.filePath(), projectRoot) + " [" + skip.reason() + "]"));
            }
        }

        CompilationSummary summary = compilationService.processFiles(preFilterResult, config, projectRoot, log);

        if (summary == null) {
            summary = new CompilationSummary();
        }

        log.info("Summary: discovered=" + summary.getDiscovered()
                + ", dependencyLinks=" + summary.getDependencyLinks()
                + ", processed=" + summary.getProcessed()
                + ", compiled=" + summary.getCompiled()
                + ", noChange=" + summary.getNoChange()
                + ", skipped=" + summary.getSkipped()
                + ", failed=" + summary.getFailed()
                + ", reprocessedDueToStaleness=" + summary.getReprocessedDueToStaleness()
                + ", reprocessedDueToChildChange=" + summary.getReprocessedDueToChildChange()
                + ", validateAllPromoted=" + summary.getValidateAllPromoted());

        if (config.getTracking().isFreezeState()) {
            log.info("[tracking] Freeze mode ACTIVE — Trackings NOT persisted during this run.");
        }

        return summary;
    }

    /**
     * Clean.
     *
     * @param config  the Reins configuration settings
     * @param baseDir the base dir
     * @param log     the logger instance
     * @return the resulting summary
     */
    public CleanupOutcomeSummary clean(ReinsConfig config, File baseDir, Log log) throws Exception {
        Path projectRoot = baseDir.toPath().toAbsolutePath().normalize();
        log.info("[ReinsRunner] Cleanup started for project root: " + projectRoot);

        CleanupService cleanupService = new CleanupService();
        PathValidator validator = new PathValidator(projectRoot);

        List<File> targetSourceFiles = null;
        if (config.isExplicitSourceMode() || (config.getSource() != null && !config.getSource().isBlank())) {
            ExplicitSourceResolver.ResolutionResult resolution = new ExplicitSourceResolver().resolve(
                    config.getSource(),
                    projectRoot,
                    config.getSourceBases(),
                    config.getIncludePattern(),
                    validator);
            targetSourceFiles = resolution.files();
            String displayPaths = resolution.resolvedPaths().stream()
                    .map(p -> PathLogFormatter.formatPath(p, projectRoot))
                    .collect(java.util.stream.Collectors.joining(", "));
            log.info("Explicit source mode active for cleanup: " + displayPaths);
        }

        CleanupOutcomeSummary summary = cleanupService.executeCleanup(projectRoot, validator, targetSourceFiles);
        CleanupReporter reporter = new CleanupReporter(log);
        reporter.reportOutcome(summary, config.isVerbose());

        if (summary.hasFailures() && config.isFailOnError()) {
            throw new RuntimeException(
                    "Cleanup failed with " + summary.getStatistics().getFailedDeletion() + " error(s)");
        }

        if (summary.hasFailures()) {
            log.warn("[ReinsRunner] Cleanup completed with failures but failOnError=false");
        } else {
            log.info("[ReinsRunner] Cleanup completed successfully");
        }

        return summary;
    }

    /**
     * Add Note.
     *
     * @param config  the Reins configuration settings
     * @param baseDir the base dir
     * @param source  the source or comma-separated list of sources
     * @param note    the note
     * @param origin  the origin
     * @param log     the logger instance
     * @return the numeric value
     */
    public int addNote(ReinsConfig config, File baseDir, String source, String note, ReasoningNote.Origin origin,
            Log log) throws Exception {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("'source' parameter is required and must not be blank.");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("'note' parameter is required and must not be blank.");
        }

        Path projectRoot = baseDir.toPath().toAbsolutePath().normalize();
        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        SourceTrackingManager manager = new SourceTrackingManager();
        PathValidator validator = new PathValidator(projectRoot);

        List<File> targetFiles = null;
        try {
            ExplicitSourceResolver.ResolutionResult resolution = new ExplicitSourceResolver().resolve(
                    source,
                    projectRoot,
                    config != null ? config.getSourceBases() : null,
                    config != null ? config.getIncludePattern() : null,
                    validator);
            targetFiles = resolution.files();
        } catch (Exception ignored) {
        }

        int lastNoteCount = 0;
        if (targetFiles != null && !targetFiles.isEmpty()) {
            for (File file : targetFiles) {
                String sourcePath = PathNormalizer.toForwardSlashes(
                        projectRoot.toAbsolutePath().normalize()
                                .relativize(file.toPath().toAbsolutePath().normalize()).toString());
                String canonicalSourcePath = trackingStore.canonicalizePath(sourcePath);
                lastNoteCount = manager.appendNote(
                        projectRoot,
                        canonicalSourcePath,
                        note,
                        origin,
                        trackingStore);
                log.info("[ReinsRunner] Note appended to: " + PathLogFormatter.formatPath(canonicalSourcePath, projectRoot)
                        + " (total notes: " + lastNoteCount + ")");
            }
        } else {
            List<String> sourceList = new br.com.dizeno.reins.source.validation.ExplicitSourceValidator().validateSourceInputs(source);
            for (String singleSource : sourceList) {
                String canonicalSourcePath = trackingStore.canonicalizePath(singleSource);
                lastNoteCount = manager.appendNote(
                        projectRoot,
                        canonicalSourcePath,
                        note,
                        origin,
                        trackingStore);
                log.info("[ReinsRunner] Note appended to: " + PathLogFormatter.formatPath(canonicalSourcePath, projectRoot)
                        + " (total notes: " + lastNoteCount + ")");
            }
        }
        return lastNoteCount;
    }

    /**
     * List Notes.
     *
     * @param config  the Reins configuration settings
     * @param baseDir the base dir
     * @param source  the source or comma-separated list of sources (optional, null or blank lists all scanned sources)
     * @param log     the logger instance
     * @return map of source path to list of reasoning notes
     */
    public java.util.Map<String, List<ReasoningNote>> listNotes(ReinsConfig config, File baseDir, String source, Log log) throws Exception {
        Path projectRoot = baseDir.toPath().toAbsolutePath().normalize();
        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        PathValidator validator = new PathValidator(projectRoot);

        List<String> targetSourcePaths = collectTargetSourcePaths(config, baseDir, source, projectRoot, trackingStore, validator);

        java.util.Map<String, List<ReasoningNote>> result = new java.util.LinkedHashMap<>();
        for (String sourcePath : targetSourcePaths) {
            String canonicalSourcePath = trackingStore.canonicalizePath(sourcePath);
            java.util.Optional<SourceTrackingRecord> recordOpt = trackingStore.load(projectRoot, canonicalSourcePath);
            if (recordOpt.isPresent()) {
                List<ReasoningNote> notes = recordOpt.get().getNotes();
                if (notes != null && !notes.isEmpty()) {
                    result.put(canonicalSourcePath, new ArrayList<>(notes));
                }
            }
        }

        if (result.isEmpty()) {
            log.info("[ReinsRunner] No notes found.");
        } else {
            log.info("[ReinsRunner] Found notes for " + result.size() + " source(s):");
            for (java.util.Map.Entry<String, List<ReasoningNote>> entry : result.entrySet()) {
                String src = entry.getKey();
                List<ReasoningNote> notes = entry.getValue();
                log.info("  Source: " + PathLogFormatter.formatPath(src, projectRoot) + " (" + notes.size() + " note(s)):");
                for (ReasoningNote note : notes) {
                    log.info("    - [" + note.getOrigin() + " | " + note.getCreatedAt() + "] " + note.getText());
                }
            }
        }
        return result;
    }

    /**
     * Clear Notes.
     *
     * @param config  the Reins configuration settings
     * @param baseDir the base dir
     * @param source  the source or comma-separated list of sources (optional, null or blank clears all scanned sources)
     * @param log     the logger instance
     * @return the total number of notes cleared
     */
    public int clearNotes(ReinsConfig config, File baseDir, String source, Log log) throws Exception {
        Path projectRoot = baseDir.toPath().toAbsolutePath().normalize();
        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        SourceTrackingManager manager = new SourceTrackingManager();
        PathValidator validator = new PathValidator(projectRoot);

        List<String> targetSourcePaths = collectTargetSourcePaths(config, baseDir, source, projectRoot, trackingStore, validator);

        int totalCleared = 0;
        int clearedFilesCount = 0;
        for (String sourcePath : targetSourcePaths) {
            String canonicalSourcePath = trackingStore.canonicalizePath(sourcePath);
            int cleared = manager.clearNotes(projectRoot, canonicalSourcePath, trackingStore);
            if (cleared > 0) {
                totalCleared += cleared;
                clearedFilesCount++;
                log.info("[ReinsRunner] Cleared " + cleared + " note(s) from: " + PathLogFormatter.formatPath(canonicalSourcePath, projectRoot));
            }
        }

        if (totalCleared == 0) {
            log.info("[ReinsRunner] No notes to clear.");
        } else {
            log.info("[ReinsRunner] Cleared a total of " + totalCleared + " note(s) across " + clearedFilesCount + " source file(s).");
        }

        return totalCleared;
    }

    private List<String> collectTargetSourcePaths(ReinsConfig config, File baseDir, String source, Path projectRoot, CompilationTrackingStore trackingStore, PathValidator validator) {
        java.util.Set<String> candidatePaths = new java.util.LinkedHashSet<>();
        if (source != null && !source.isBlank()) {
            List<File> targetFiles = null;
            try {
                ExplicitSourceResolver.ResolutionResult resolution = new ExplicitSourceResolver().resolve(
                        source,
                        projectRoot,
                        config != null ? config.getSourceBases() : null,
                        config != null ? config.getIncludePattern() : null,
                        validator);
                targetFiles = resolution.files();
            } catch (Exception ignored) {
            }

            if (targetFiles != null && !targetFiles.isEmpty()) {
                for (File file : targetFiles) {
                    String sourcePath = PathNormalizer.toForwardSlashes(
                            projectRoot.toAbsolutePath().normalize()
                                    .relativize(file.toPath().toAbsolutePath().normalize()).toString());
                    candidatePaths.add(trackingStore.canonicalizePath(sourcePath));
                }
            } else {
                List<String> sourceList = new br.com.dizeno.reins.source.validation.ExplicitSourceValidator().validateSourceInputs(source);
                for (String singleSource : sourceList) {
                    candidatePaths.add(trackingStore.canonicalizePath(singleSource));
                }
            }
        } else {
            try {
                List<String> tracked = trackingStore.listAllTrackedSourcePaths(projectRoot);
                for (String t : tracked) {
                    candidatePaths.add(trackingStore.canonicalizePath(t));
                }
            } catch (Exception ignored) {
            }
            try {
                List<File> scanRoots = config != null ? config.getScanRoots() : null;
                String includePattern = config != null ? config.getIncludePattern() : null;
                List<File> scannedFiles = new CoderMdScanner().scan(
                        scanRoots,
                        includePattern,
                        validator,
                        SourceDiscoveryMode.FULL_SCAN);
                for (File file : scannedFiles) {
                    String sourcePath = PathNormalizer.toForwardSlashes(
                            projectRoot.toAbsolutePath().normalize()
                                    .relativize(file.toPath().toAbsolutePath().normalize()).toString());
                    candidatePaths.add(trackingStore.canonicalizePath(sourcePath));
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(candidatePaths);
    }

    private List<CycleWorkSetEntry> buildExplicitWorkSet(List<File> files, Path projectRoot) {
        List<CycleWorkSetEntry> entries = new ArrayList<>();
        for (File file : files) {
            String sourcePath = PathNormalizer.toForwardSlashes(
                    projectRoot.toAbsolutePath().normalize()
                            .relativize(file.toPath().toAbsolutePath().normalize()).toString());
            CycleWorkSetEntry entry = new CycleWorkSetEntry(file, sourcePath, SourceProcessingStatus.COMPILE);
            entry.setSelectionReason(br.com.dizeno.reins.compilation.tracking.ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD);
            entries.add(entry);
        }
        return entries;
    }

    private File resolveScriptDir(String scriptDirParam, File basedir) {
        if (scriptDirParam == null || scriptDirParam.isBlank()) {
            return null;
        }
        File f = new File(scriptDirParam);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(basedir, scriptDirParam);
    }

    private String formatDisplayPath(Path absolutePath, Path mainBase, Path testBase) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        Path mainNormalized = mainBase.toAbsolutePath().normalize();
        Path testNormalized = testBase.toAbsolutePath().normalize();

        if (normalized.startsWith(mainNormalized)) {
            Path relPath = mainNormalized.relativize(normalized);
            return "main:" + relPath;
        } else if (normalized.startsWith(testNormalized)) {
            Path relPath = testNormalized.relativize(normalized);
            return "test:" + relPath;
        } else {
            return "unknown:" + absolutePath;
        }
    }
}
