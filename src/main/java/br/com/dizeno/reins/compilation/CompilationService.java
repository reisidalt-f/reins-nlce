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

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.compilation.tracking.TrackingCommitStrategy;
import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.run.SourceTagLogger;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmEmptyResponseException;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.DefaultReasoningService;
import br.com.dizeno.reins.reasoning.EagerlyProvideResult;
import br.com.dizeno.reins.reasoning.EagerlyProvideService;
import br.com.dizeno.reins.reasoning.CompiledSourceGroup;
import br.com.dizeno.reins.reasoning.ReasoningPromptBuilder;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.reasoning.ReferenceTreeContextService;
import br.com.dizeno.reins.reasoning.DefaultReasoningServiceFactory;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.PathNormalizer;
import br.com.dizeno.reins.util.PathLogFormatter;
import org.apache.maven.plugin.logging.Log;

import br.com.dizeno.reins.compilation.pipeline.CompilationPhase;
import br.com.dizeno.reins.compilation.pipeline.PhaseChain;
import br.com.dizeno.reins.compilation.pipeline.SourceCompilationContext;
import br.com.dizeno.reins.compilation.pipeline.TrackingRecordHelper;
import br.com.dizeno.reins.compilation.pipeline.PathHelper;
import br.com.dizeno.reins.compilation.pipeline.ReasoningHelper;
import br.com.dizeno.reins.compilation.pipeline.SourceReadPhase;
import br.com.dizeno.reins.compilation.pipeline.TrackingLoadPhase;
import br.com.dizeno.reins.compilation.pipeline.EagerlyProvidePhase;
import br.com.dizeno.reins.compilation.pipeline.ReasoningRequestBuildPhase;
import br.com.dizeno.reins.compilation.pipeline.ReasoningExecutionPhase;
import br.com.dizeno.reins.compilation.pipeline.OutcomeBranchPhase;
import br.com.dizeno.reins.compilation.pipeline.ResultPrintPhase;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * CompilationService is part of the core compilation lifecycle management,
 * orchestrating file discovery, dependency resolution, and pipeline execution
 * in the reins architecture.
 * Coordinates the entire reins compilation lifecycle, including dependency
 * graph analysis, change detection, pipeline execution, and target file
 * writing.
 */
public class CompilationService {
    private final InferenceService inferenceService;
    private final ResultPrinter resultPrinter;
    private final CompilationTrackingStore trackingStore;
    private final SourceFingerprintService fingerprintService;
    private final RecompilationDecider recompilationDecider;
    private final MarkdownDependencyGraphBuilder graphBuilder;
    private final ProcessingOrderResolver processingOrderResolver;
    private final ReasoningService reasoningService;
    private final ProjectContextService projectContextService;
    private final TrackingCommitStrategy trackingCommitStrategy = new TrackingCommitStrategy();
    private final SourceTrackingManager sourceTrackingManager = new SourceTrackingManager(trackingCommitStrategy);
    private EagerlyProvideService eagerlyProvideService = new EagerlyProvideService();

    void setEagerlyProvideService(EagerlyProvideService svc) {
        this.eagerlyProvideService = svc;
    }

    /**
     * Constructs a new instance of {@link CompilationService}.
     */
    public CompilationService() {
        this(new InferenceService(),
                new OutputWriter(),
                new ResultPrinter(),
                new CompilationTrackingStore(),
                new SourceFingerprintService(),
                new RecompilationDecider(),
                new MarkdownDependencyGraphBuilder(),
                new ProcessingOrderResolver(),
                DefaultReasoningServiceFactory.createDefault(),
                new ProjectContextService());
    }

    public CompilationService(InferenceService inferenceService,
            OutputWriter outputWriter,
            ResultPrinter resultPrinter,
            CompilationTrackingStore trackingStore,
            SourceFingerprintService fingerprintService,
            RecompilationDecider recompilationDecider,
            MarkdownDependencyGraphBuilder graphBuilder,
            ProcessingOrderResolver processingOrderResolver,
            ReasoningService reasoningService) {
        this(inferenceService, outputWriter, resultPrinter,
                trackingStore, fingerprintService, recompilationDecider,
                graphBuilder, processingOrderResolver, reasoningService,
                new ProjectContextService());
    }

    public CompilationService(InferenceService inferenceService,
            OutputWriter outputWriter,
            ResultPrinter resultPrinter,
            CompilationTrackingStore trackingStore,
            SourceFingerprintService fingerprintService,
            RecompilationDecider recompilationDecider,
            MarkdownDependencyGraphBuilder graphBuilder,
            ProcessingOrderResolver processingOrderResolver,
            ReasoningService reasoningService,
            ProjectContextService projectContextService) {
        this.inferenceService = inferenceService;
        this.resultPrinter = resultPrinter;
        this.trackingStore = trackingStore;
        this.fingerprintService = fingerprintService;
        this.recompilationDecider = recompilationDecider;
        this.graphBuilder = graphBuilder;
        this.processingOrderResolver = processingOrderResolver;
        this.reasoningService = reasoningService;
        this.projectContextService = projectContextService;
    }

    /**
     * Processes the source files.
     *
     * @param sourceFiles the list of source files to process
     * @param config      the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param log         the logger instance
     * @return the resulting summary
     */
    public CompilationSummary processFiles(List<File> sourceFiles,
            ReinsConfig config,
            Path projectRoot,
            Log log) throws Exception {
        return processFiles(
                new PreFilterResult(
                        sourceFiles,
                        buildCompileWorkSetEntries(sourceFiles, projectRoot),
                        List.of()),
                config,
                projectRoot,
                log);
    }

    /**
     * Processes the source files.
     *
     * @param preFilterResult the pre-filtered list of files and work sets
     * @param config          the Reins configuration settings
     * @param projectRoot     the root path of the project
     * @param log             the logger instance
     * @return the resulting summary
     */
    public CompilationSummary processFiles(PreFilterResult preFilterResult,
            ReinsConfig config,
            Path projectRoot,
            Log log) throws Exception {
        inferenceService.setLog(log);
        CompilationSummary summary = new CompilationSummary();
        summary.addValidateAllPromoted(preFilterResult.getValidateAllPromotedCount());
        PathValidator validator = new PathValidator(projectRoot);
        Map<String, Path> sourceBasesMap = new LinkedHashMap<>();
        if (config != null && config.getSourceBases() != null) {
            for (Map.Entry<String, File> entry : config.getSourceBases().entrySet()) {
                if (entry.getValue() != null) {
                    sourceBasesMap.put(entry.getKey(), entry.getValue().toPath().toAbsolutePath().normalize());
                }
            }
        }
        List<File> sourceFiles = preFilterResult.getSourceFiles();
        MarkdownDependencyGraph graph = graphBuilder.build(sourceFiles, projectRoot, sourceBasesMap, validator);
        List<String> processingOrder = processingOrderResolver.resolve(graph);
        Map<String, CycleWorkSetEntry> workSetByPath = preFilterResult.getWorkSetEntries().stream()
                .collect(Collectors.toMap(
                        CycleWorkSetEntry::getSourcePath,
                        entry -> entry,
                        (left, right) -> left,
                        LinkedHashMap::new));
        Set<String> requestedSourcePaths = new LinkedHashSet<>(workSetByPath.keySet());
        List<String> executionOrder = requestedSourcePaths.isEmpty()
                ? processingOrder
                : processingOrder.stream()
                        .filter(requestedSourcePaths::contains)
                        .toList();
        if (!requestedSourcePaths.isEmpty() && executionOrder.size() < requestedSourcePaths.size()) {
            List<String> mergedExecutionOrder = new ArrayList<>(executionOrder);
            for (String requestedPath : requestedSourcePaths) {
                if (!mergedExecutionOrder.contains(requestedPath)) {
                    mergedExecutionOrder.add(requestedPath);
                }
            }
            executionOrder = List.copyOf(mergedExecutionOrder);
        }
        assignOrderIndexes(executionOrder, workSetByPath);
        Map<String, Integer> orderIndex = processingOrderResolver.orderIndexMap(executionOrder);
        LogSettings logSettings = config.getLog();
        LoggingSettings loggingSettings = config.getLogging();
        boolean processingOrderLoggingEnabled = logSettings != null && logSettings.isProcessingOrder();
        boolean skippedLoggingEnabled = logSettings != null && logSettings.isSkipped();
        boolean selectionReasonLoggingEnabled = loggingSettings != null && loggingSettings.isSelectionReason();
        summary.setDiscovered(graph.size());
        summary.setDependencyLinks(graph.getDependencyLinkCount());

        log.info("Dependency graph: nodes=" + graph.size() + ", links=" + graph.getDependencyLinkCount());
        if (processingOrderLoggingEnabled) {
            log.info("Processing order: " + String.join(" -> ", executionOrder));
        }

        ReferenceDepthPolicy referenceDepthPolicy = config.getContext() == null || config.getContext().getReferencesTree() == null
                ? ReferenceDepthPolicy.defaultPolicy()
                : config.getContext().getReferencesTree().resolveReferenceDepthPolicy();
        boolean includeReferencedAttachments = config.getContext() != null
                && config.getContext().getReferencesTree() != null
                && config.getContext().getReferencesTree().isAttachFiles();
        log.info("Reference tree depth: " + referenceDepthPolicy.describeForLog());

        Set<Path> contextScanRoots = buildContextScanRoots(config);

        CompilationBackgroundPayload compilationBackgroundPayload = CompilationBackgroundPayload.empty();

        if (config.getContext() != null && config.getContext().getSources() != null) {
            Set<Path> alreadyVisited = compilationBackgroundPayload.getFiles().stream()
                    .map(CompilationBackgroundFile::getAbsolutePath)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            ProjectContextService.SourcesLoadResult sourcesResult = projectContextService.loadSources(
                    config.getContext().getSources(),
                    projectRoot,
                    contextScanRoots,
                    alreadyVisited,
                    referenceDepthPolicy,
                    includeReferencedAttachments);
            if (sourcesResult == null) {
                sourcesResult = ProjectContextService.SourcesLoadResult.empty();
            }
            for (String expr : sourcesResult.getBlankExprs()) {
                log.warn("[context.sources] Source file is blank, skipping: " + PathLogFormatter.formatPath(expr, projectRoot));
            }
            for (String expr : sourcesResult.getLoadedExprs()) {
                log.info("[context.sources] Loaded: " + PathLogFormatter.formatPath(expr, projectRoot));
            }
            if (!sourcesResult.getCombinedFiles().isEmpty()) {
                List<CompilationBackgroundFile> merged = new ArrayList<>(compilationBackgroundPayload.getFiles());
                merged.addAll(sourcesResult.getCombinedFiles());
                compilationBackgroundPayload = new CompilationBackgroundPayload(merged);
            }
        }

        ReasoningService effectiveReasoningService = reasoningService;
        if (reasoningService instanceof DefaultReasoningService defaultReasoningService) {
            File customScriptDir = null;
            String scriptDir = config.getReasoning() == null ? null : config.getReasoning().getScriptsPath();
            if (scriptDir != null && !scriptDir.isBlank()) {
                File configured = new File(scriptDir);
                customScriptDir = configured.isAbsolute() ? configured : new File(projectRoot.toFile(), scriptDir);
            }
            ScriptRegistry scriptRegistry = ScriptRegistry.build(new ScriptResolver(customScriptDir, null),
                    customScriptDir);
            scriptRegistry.validateAll();
            boolean scriptsEventsEnabled = config.getLogging() != null && config.getLogging().isScriptsEvents();
            BasePathMappingSet baseMappings = BasePathMappingSet.fromConfig(config, projectRoot);
            BasePathResolver basePathResolver = new BasePathResolver(baseMappings, validator);
            ScriptEvaluator scriptEvaluator = new ScriptEvaluator(scriptRegistry, log, scriptsEventsEnabled, basePathResolver);
            effectiveReasoningService = defaultReasoningService.withScriptEvaluator(scriptEvaluator);
        }

        br.com.dizeno.reins.reasoning.tooling.ToolingService queueToolingService = null;
        if (effectiveReasoningService instanceof DefaultReasoningService drs) {
            queueToolingService = drs.getToolingService();
        }

        SourceProcessingQueue queue = new SourceProcessingQueue(
                buildOrderedQueueEntries(executionOrder, workSetByPath));

        List<CompilationPhase> phases = List.of(
                new SourceReadPhase(),
                new TrackingLoadPhase(),
                new EagerlyProvidePhase(),
                new ReasoningRequestBuildPhase(),
                new ReasoningExecutionPhase(),
                new OutcomeBranchPhase(),
                new ResultPrintPhase());

        int compilationThreads = config != null
                ? config.getCompilationThreads()
                : 1;

        if (compilationThreads <= 1) {
            CycleWorkSetEntry workSetEntry;
            while ((workSetEntry = queue.nextExecutable()) != null) {
                executeSingleWorkSetEntry(workSetEntry, graph, config, projectRoot, log, summary,
                        compilationBackgroundPayload, executionOrder, workSetByPath, orderIndex,
                        referenceDepthPolicy, validator, effectiveReasoningService, queueToolingService,
                        queue, phases, loggingSettings, skippedLoggingEnabled, selectionReasonLoggingEnabled);
            }
        } else {
            processEntriesConcurrently(compilationThreads, executionOrder, workSetByPath, graph, config, projectRoot,
                    log, summary, compilationBackgroundPayload, orderIndex, referenceDepthPolicy, validator,
                    effectiveReasoningService, queueToolingService, queue, phases, loggingSettings,
                    skippedLoggingEnabled, selectionReasonLoggingEnabled);
        }


        if (preFilterResult != null && preFilterResult.getWorkSetEntries() != null) {
            for (CycleWorkSetEntry entry : preFilterResult.getWorkSetEntries()) {
                if (entry.getStatus() == SourceProcessingStatus.SKIP) {
                    String canonicalSourcePath = trackingStore.canonicalizePath(entry.getSourcePath());
                    try {
                        SourceTrackingRecord record = trackingStore.load(projectRoot, canonicalSourcePath).orElse(null);
                        if (record != null) {
                            TrackingRecordHelper.physicallyTouchOutputs(canonicalSourcePath, record, projectRoot,
                                    config, log, skippedLoggingEnabled);
                        }
                    } catch (Exception ex) {
                        log.warn("Could not process touch operation for skipped source " + entry.getSourcePath() + ": "
                                + ex.getMessage());
                    }
                }
            }
        }

        try {
            for (String trackedPath : trackingStore.listAllTrackedSourcePaths(projectRoot)) {
                SourceTrackingRecord trackedRecord = trackingStore.load(projectRoot, trackedPath).orElse(null);
                Path resolvedTrackedPath = TrackedPathResolver.resolveTrackedPath(
                        projectRoot,
                        trackedPath,
                        trackedRecord == null ? null : trackedRecord.getResolvedTargetRoot());
                if (!Files.exists(resolvedTrackedPath)) {
                    if (config.getTracking().isFreezeState()) {
                        log.info("[tracking] Freeze mode: stale tracking file NOT removed for " + trackedPath
                                + " (would be removed in mutable mode)");
                    } else {
                        trackingStore.delete(projectRoot, trackedPath);
                        log.info("Removed stale tracking file for deleted source: " + trackedPath);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Stale tracking cleanup failed: " + ex.getMessage());
        }

        return summary;
    }

    private CompilationOutput initOutput(MarkdownSourceNode node,
            String relativeSourcePath,
            MarkdownDependencyGraph graph,
            String sourceCategory,
            int processingIndex,
            int processingTotal,
            CycleWorkSetEntry workSetEntry) {
        CompilationOutput output = new CompilationOutput();
        output.setSourcePath(node.absolutePath().toString());
        output.setSourceCategory(sourceCategory);
        output.setProcessingIndex(processingIndex);
        output.setProcessingTotal(processingTotal);
        output.setDependencyChildren(graph.getChildren(relativeSourcePath));
        output.setResolutionDiagnostics(graph.getResolutionDiagnostics(relativeSourcePath));
        output.setWinningStrategies(graph.getWinningStrategies(relativeSourcePath));
        output.setProcessingState(workSetEntry.getStatus().name().toLowerCase());
        output.setTransitionCauses(workSetEntry.getTransitionCauses());
        return output;
    }

    private List<CycleWorkSetEntry> buildCompileWorkSetEntries(List<File> sourceFiles, Path projectRoot) {
        List<CycleWorkSetEntry> entries = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            String relativeSourcePath = PathNormalizer.toForwardSlashes(
                    projectRoot.toAbsolutePath().normalize()
                            .relativize(sourceFile.toPath().toAbsolutePath().normalize()).toString());
            entries.add(new CycleWorkSetEntry(sourceFile, relativeSourcePath, SourceProcessingStatus.COMPILE));
        }
        return entries;
    }

    private void assignOrderIndexes(List<String> executionOrder,
            Map<String, CycleWorkSetEntry> workSetByPath) {
        for (int index = 0; index < executionOrder.size(); index++) {
            CycleWorkSetEntry entry = workSetByPath.get(executionOrder.get(index));
            if (entry != null) {
                entry.setOrderIndex(index);
            }
        }
    }

    private List<CycleWorkSetEntry> buildOrderedQueueEntries(List<String> executionOrder,
            Map<String, CycleWorkSetEntry> workSetByPath) {
        List<CycleWorkSetEntry> orderedEntries = new ArrayList<>();
        Set<String> addedPaths = new LinkedHashSet<>();
        for (String sourcePath : executionOrder) {
            CycleWorkSetEntry entry = workSetByPath.get(sourcePath);
            if (entry != null && addedPaths.add(sourcePath)) {
                orderedEntries.add(entry);
            }
        }
        for (Map.Entry<String, CycleWorkSetEntry> entry : workSetByPath.entrySet()) {
            if (addedPaths.add(entry.getKey())) {
                orderedEntries.add(entry.getValue());
            }
        }
        return orderedEntries;
    }

    private static Set<Path> buildContextScanRoots(ReinsConfig config) {
        Set<Path> roots = new LinkedHashSet<>();
        if (config != null && config.getScanRoots() != null) {
            for (File root : config.getScanRoots()) {
                if (root != null) {
                    roots.add(root.toPath().toAbsolutePath().normalize());
                }
            }
        }
        return roots;
    }

    private void executeSingleWorkSetEntry(
            CycleWorkSetEntry workSetEntry,
            MarkdownDependencyGraph graph,
            ReinsConfig config,
            Path projectRoot,
            Log log,
            CompilationSummary summary,
            CompilationBackgroundPayload compilationBackgroundPayload,
            List<String> executionOrder,
            Map<String, CycleWorkSetEntry> workSetByPath,
            Map<String, Integer> orderIndex,
            ReferenceDepthPolicy referenceDepthPolicy,
            PathValidator validator,
            ReasoningService effectiveReasoningService,
            br.com.dizeno.reins.reasoning.tooling.ToolingService queueToolingService,
            SourceProcessingQueue queue,
            List<CompilationPhase> phases,
            LoggingSettings loggingSettings,
            boolean skippedLoggingEnabled,
            boolean selectionReasonLoggingEnabled) throws Exception {
        String relativeSourcePath = workSetEntry.getSourcePath();
        MarkdownSourceNode node = graph.getNodes().get(relativeSourcePath);
        String sourceCategory = PathHelper.resolveSourceCategory(node.absolutePath(), config, projectRoot);
        CompilationBackgroundPayload sourceCycleContextPayload = PathHelper.isMainOrTestScope(sourceCategory)
                ? compilationBackgroundPayload
                : CompilationBackgroundPayload.empty();
        if (sourceCycleContextPayload != null && !sourceCycleContextPayload.isEmpty()) {
            List<CompilationBackgroundFile> fileMatched = sourceCycleContextPayload.getFiles().stream()
                    .filter(f -> f.matchesTargetFile(relativeSourcePath))
                    .toList();
            sourceCycleContextPayload = new CompilationBackgroundPayload(fileMatched);
        }
        String resolvedTargetRoot = PathHelper.resolveTargetRootForSourceCategory(config, projectRoot,
                sourceCategory);
        String canonicalSourcePath = trackingStore.canonicalizePath(relativeSourcePath);
        long start = System.currentTimeMillis();
        int processingIndex = workSetEntry.getOrderIndex() + 1;
        CompilationOutput output = initOutput(node, relativeSourcePath, graph, sourceCategory, processingIndex,
                executionOrder.size(), workSetEntry);
        String processingStatus = workSetEntry.getStatus().name();

        log.info("**");
        log.info("** " + processingStatus + ": " + PathHelper.formatBaseRelativePath(node.absolutePath(), config, projectRoot));
        if (selectionReasonLoggingEnabled) {
            ReprocessingDecision.ReprocessingReason reason = workSetEntry.getSelectionReason();
            if (reason == null) {
                try {
                    if (trackingStore.load(projectRoot, canonicalSourcePath).isEmpty()) {
                        reason = ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD;
                    }
                } catch (Exception e) {
                    // ignore, keep null / UNKNOWN
                }
            }
            String reasonLabel = reason != null
                    ? reason.name()
                    : "UNKNOWN";
            log.info("Selection reason: " + reasonLabel);
        }

        boolean validateOnly = workSetEntry.getStatus() == SourceProcessingStatus.VALIDATE;
        if (validateOnly) {
            summary.incrementReprocessedDueToChildChange();
        }

        SourceCompilationContext ctx = new SourceCompilationContext();
        ctx.setWorkSetEntry(workSetEntry);
        ctx.setNode(node);
        ctx.setRelativeSourcePath(relativeSourcePath);
        ctx.setCanonicalSourcePath(canonicalSourcePath);
        ctx.setSourceCategory(sourceCategory);
        ctx.setResolvedTargetRoot(resolvedTargetRoot);
        ctx.setValidateOnly(validateOnly);
        ctx.setOutput(output);
        ctx.setStartTimeMs(start);

        Log cycleLog = log;
        if (loggingSettings != null && loggingSettings.isSourceTag()) {
            String simpleFileName = new File(relativeSourcePath).getName();
            cycleLog = new SourceTagLogger(log, simpleFileName);
        }

        ctx.setConfig(config);
        ctx.setProjectRoot(projectRoot);
        ctx.setLog(cycleLog);
        ctx.setGraph(graph);
        ctx.setSummary(summary);
        ctx.setTrackingStore(trackingStore);
        ctx.setSourceTrackingManager(sourceTrackingManager);
        ctx.setFingerprintService(fingerprintService);
        ctx.setEagerlyProvideService(eagerlyProvideService);
        ctx.setEffectiveReasoningService(effectiveReasoningService);
        ctx.setQueueToolingService(queueToolingService);
        ctx.setQueue(queue);
        ctx.setWorkSetByPath(workSetByPath);
        ctx.setOrderIndex(orderIndex);
        ctx.setValidator(validator);
        ctx.setReferenceDepthPolicy(referenceDepthPolicy);
        ctx.setCompilationBackgroundPayload(sourceCycleContextPayload);
        ctx.setResultPrinter(resultPrinter);
        ctx.setProcessingOrderResolver(processingOrderResolver);
        ctx.setSkippedLoggingEnabled(skippedLoggingEnabled);
        ctx.setSelectionReasonLoggingEnabled(selectionReasonLoggingEnabled);

        try {
            new PhaseChain(phases).execute(ctx);
        } catch (LlmEmptyResponseException ex) {
            summary.incrementFailed();
            output.setStatus("failed-empty-response-exhausted");
            output.setMessage(ex.getMessage());
            output.setRetryAttemptCount(ex.getTotalAttempts() > 0 ? ex.getTotalAttempts() : 1);
            output.setNoUsableContentCount(ex.getNoUsableContentCount() > 0 ? ex.getNoUsableContentCount() : 1);
            if (!config.getTracking().isFreezeState() && !config.isExplicitSourceMode()) {
                try {
                    SourceTrackingRecord failRecord = trackingStore.load(projectRoot, canonicalSourcePath)
                            .orElseGet(SourceTrackingRecord::new);
                    failRecord.setSourcePath(canonicalSourcePath);
                    failRecord.setSourceCategory(sourceCategory);
                    failRecord.setLastStatus("failed-empty-response-exhausted");
                    failRecord.setLastCompiledAt(Instant.now().toString());
                    sourceTrackingManager.commit(projectRoot, canonicalSourcePath, failRecord, trackingStore);
                } catch (Exception trackEx) {
                    log.warn("Could not write fail tracking file for " + relativeSourcePath + ": "
                            + trackEx.getMessage());
                }
            } else if (config.isExplicitSourceMode()) {
                log.info("[tracking] Explicit source mode: preserving prior tracking file after failure for "
                        + canonicalSourcePath);
            }
            workSetEntry.markProcessed();
            output.setDurationMs(System.currentTimeMillis() - start);
            resultPrinter.print(log, output, projectRoot);

            if (!queue.getNewlyNotedSinceLastPoll().isEmpty()) {
                log.info("[notes] Source failed (empty-response) but note-handoff is active for "
                        + queue.getNewlyNotedSinceLastPoll().size() + " source(s); continuing queue.");
                return;
            }
            throw ex;
        } catch (Exception ex) {
            summary.incrementFailed();
            output.setStatus("failed");
            output.setMessage(ex.getMessage());
            if (!config.getTracking().isFreezeState() && !config.isExplicitSourceMode()) {
                try {
                    SourceTrackingRecord failRecord = trackingStore.load(projectRoot, canonicalSourcePath)
                            .orElseGet(SourceTrackingRecord::new);
                    failRecord.setSourcePath(canonicalSourcePath);
                    failRecord.setSourceCategory(sourceCategory);
                    failRecord.setLastStatus("failed");
                    failRecord.setLastCompiledAt(Instant.now().toString());
                    sourceTrackingManager.commit(projectRoot, canonicalSourcePath, failRecord, trackingStore);
                } catch (Exception trackEx) {
                    log.warn("Could not write fail tracking file for " + relativeSourcePath + ": "
                            + trackEx.getMessage());
                }
            } else if (config.isExplicitSourceMode()) {
                log.info("[tracking] Explicit source mode: preserving prior tracking file after failure for "
                        + canonicalSourcePath);
            }
            workSetEntry.markProcessed();
            output.setDurationMs(System.currentTimeMillis() - start);
            resultPrinter.print(log, output, projectRoot);

            if (!queue.getNewlyNotedSinceLastPoll().isEmpty()) {
                log.info("[notes] Source failed but note-handoff is active for "
                        + queue.getNewlyNotedSinceLastPoll().size() + " source(s); continuing queue.");
                return;
            }
            if (config.isFailOnError()) {
                throw ex;
            }
        }
    }

    private void processEntriesConcurrently(
            int compilationThreads,
            List<String> executionOrder,
            Map<String, CycleWorkSetEntry> workSetByPath,
            MarkdownDependencyGraph graph,
            ReinsConfig config,
            Path projectRoot,
            Log log,
            CompilationSummary summary,
            CompilationBackgroundPayload compilationBackgroundPayload,
            Map<String, Integer> orderIndex,
            ReferenceDepthPolicy referenceDepthPolicy,
            PathValidator validator,
            ReasoningService effectiveReasoningService,
            br.com.dizeno.reins.reasoning.tooling.ToolingService queueToolingService,
            SourceProcessingQueue queue,
            List<CompilationPhase> phases,
            LoggingSettings loggingSettings,
            boolean skippedLoggingEnabled,
            boolean selectionReasonLoggingEnabled) throws Exception {

        log.info("Executing concurrent compilation with threads: " + compilationThreads);

        Map<String, Set<String>> pendingDeps = new ConcurrentHashMap<>();
        Map<String, Set<String>> reverseDeps = new ConcurrentHashMap<>();
        Set<String> executablePaths = ConcurrentHashMap.newKeySet();
        Set<String> failedPaths = ConcurrentHashMap.newKeySet();

        for (String sourcePath : executionOrder) {
            CycleWorkSetEntry entry = workSetByPath.get(sourcePath);
            if (entry != null && entry.getStatus().shouldExecute()) {
                executablePaths.add(sourcePath);
            }
        }

        if (executablePaths.isEmpty()) {
            return;
        }

        for (String sourcePath : executablePaths) {
            Set<String> deps = ConcurrentHashMap.newKeySet();
            collectExecutableDependencies(sourcePath, graph, executablePaths, new LinkedHashSet<>(), deps);
            for (String dep : deps) {
                reverseDeps.computeIfAbsent(dep, k -> ConcurrentHashMap.newKeySet()).add(sourcePath);
            }
            pendingDeps.put(sourcePath, deps);
        }

        ConcurrentLinkedQueue<String> readyQueue = new ConcurrentLinkedQueue<>();
        for (String sourcePath : executablePaths) {
            if (pendingDeps.get(sourcePath).isEmpty()) {
                readyQueue.add(sourcePath);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(compilationThreads);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger pendingCount = new java.util.concurrent.atomic.AtomicInteger(executablePaths.size());
        Object lock = new Object();

        Runnable workerTask = new Runnable() {
            @Override
            public void run() {
                while (firstError.get() == null) {
                    String sourcePath = readyQueue.poll();
                    if (sourcePath == null) {
                        break;
                    }
                    CycleWorkSetEntry workSetEntry = workSetByPath.get(sourcePath);
                    if (workSetEntry != null) {
                        long failedBefore = summary.getFailed();
                        try {
                            executeSingleWorkSetEntry(workSetEntry, graph, config, projectRoot, log, summary,
                                    compilationBackgroundPayload, executionOrder, workSetByPath, orderIndex,
                                    referenceDepthPolicy, validator, effectiveReasoningService, queueToolingService,
                                    queue, phases, loggingSettings, skippedLoggingEnabled, selectionReasonLoggingEnabled);
                        } catch (Throwable t) {
                            firstError.compareAndSet(null, t);
                        }
                        if (summary.getFailed() > failedBefore || firstError.get() != null) {
                            failedPaths.add(sourcePath);
                        }
                    }

                    int remaining = pendingCount.decrementAndGet();
                    boolean currentFailed = failedPaths.contains(sourcePath);
                    Set<String> parents = reverseDeps.getOrDefault(sourcePath, Set.of());
                    for (String parent : parents) {
                        if (currentFailed) {
                            failedPaths.add(parent);
                        }
                        Set<String> parentDeps = pendingDeps.get(parent);
                        if (parentDeps != null) {
                            parentDeps.remove(sourcePath);
                            if (parentDeps.isEmpty()) {
                                if (failedPaths.contains(parent)) {
                                    log.warn("[SKIP] Skipping compilation of dependent file " + parent + " because one of its dependencies failed to compile.");
                                    summary.incrementFailed();
                                    pendingCount.decrementAndGet();
                                } else {
                                    readyQueue.add(parent);
                                    executor.submit(this);
                                }
                            }
                        }
                    }
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                    if (remaining == 0) {
                        break;
                    }
                }
            }
        };

        int initialReadySize = readyQueue.size();
        for (int i = 0; i < initialReadySize; i++) {
            executor.submit(workerTask);
        }

        synchronized (lock) {
            while (pendingCount.get() > 0 && firstError.get() == null) {
                lock.wait(100);
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        if (firstError.get() != null) {
            Throwable err = firstError.get();
            if (err instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(err);
        }
    }

    private void collectExecutableDependencies(String sourcePath,
                                                MarkdownDependencyGraph graph,
                                                Set<String> executablePaths,
                                                Set<String> visited,
                                                Set<String> resultDeps) {
        if (graph == null) {
            return;
        }
        List<String> children = graph.getChildren(sourcePath);
        if (children == null) {
            return;
        }
        for (String child : children) {
            if (!visited.add(child)) {
                continue;
            }
            if (executablePaths.contains(child)) {
                resultDeps.add(child);
            } else {
                collectExecutableDependencies(child, graph, executablePaths, visited, resultDeps);
            }
        }
    }
}


