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
import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiEmptyResponseException;
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
    private final ProjectReasoningCycleService projectInferenceCycleService;
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
                new ProjectContextService(),
                null);
    }

    /**
     * Constructs a new instance of {@link CompilationService}.
     *
     * @param inferenceService        the service invoking LLM endpoints
     * @param outputWriter            the writer for compilation output files
     * @param resultPrinter           the printer component for compilation outcomes
     * @param trackingStore           the persistence store for file tracking
     *                                records
     * @param fingerprintService      the service used to calculate file
     *                                fingerprints
     * @param recompilationDecider    the decider for recompilation needs
     * @param graphBuilder            the dependency graph builder instance
     * @param processingOrderResolver the processing order resolver
     * @param reasoningService        the LLM reasoning service component
     */
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
                new ProjectContextService(), null);
    }

    /**
     * Constructs a new instance of {@link CompilationService}.
     *
     * @param inferenceService        the service invoking LLM endpoints
     * @param outputWriter            the writer for compilation output files
     * @param resultPrinter           the printer component for compilation outcomes
     * @param trackingStore           the persistence store for file tracking
     *                                records
     * @param fingerprintService      the service used to calculate file
     *                                fingerprints
     * @param recompilationDecider    the decider for recompilation needs
     * @param graphBuilder            the dependency graph builder instance
     * @param processingOrderResolver the processing order resolver
     * @param reasoningService        the LLM reasoning service component
     * @param projectContextService   the service managing project execution context
     */
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
        this(inferenceService, outputWriter, resultPrinter,
                trackingStore, fingerprintService, recompilationDecider,
                graphBuilder, processingOrderResolver, reasoningService,
                projectContextService, null);
    }

    /**
     * Constructs a new instance of {@link CompilationService}.
     *
     * @param inferenceService             the service invoking LLM endpoints
     * @param outputWriter                 the writer for compilation output files
     * @param resultPrinter                the printer component for compilation
     *                                     outcomes
     * @param trackingStore                the persistence store for file tracking
     *                                     records
     * @param fingerprintService           the service used to calculate file
     *                                     fingerprints
     * @param recompilationDecider         the decider for recompilation needs
     * @param graphBuilder                 the dependency graph builder instance
     * @param processingOrderResolver      the processing order resolver
     * @param reasoningService             the LLM reasoning service component
     * @param projectContextService        the service managing project execution
     *                                     context
     * @param projectInferenceCycleService the project inference cycle service
     */
    public CompilationService(InferenceService inferenceService,
            OutputWriter outputWriter,
            ResultPrinter resultPrinter,
            CompilationTrackingStore trackingStore,
            SourceFingerprintService fingerprintService,
            RecompilationDecider recompilationDecider,
            MarkdownDependencyGraphBuilder graphBuilder,
            ProcessingOrderResolver processingOrderResolver,
            ReasoningService reasoningService,
            ProjectContextService projectContextService,
            ProjectReasoningCycleService projectInferenceCycleService) {
        this.inferenceService = inferenceService;
        this.resultPrinter = resultPrinter;
        this.trackingStore = trackingStore;
        this.fingerprintService = fingerprintService;
        this.recompilationDecider = recompilationDecider;
        this.graphBuilder = graphBuilder;
        this.processingOrderResolver = processingOrderResolver;
        this.reasoningService = reasoningService;
        this.projectContextService = projectContextService;
        this.projectInferenceCycleService = projectInferenceCycleService;
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
        return processFiles(sourceFiles, true, config, projectRoot, log);
    }

    /**
     * Processes the source files.
     *
     * @param sourceFiles         the list of source files to process
     * @param runProjectInference the run project inference
     * @param config              the Reins configuration settings
     * @param projectRoot         the root path of the project
     * @param log                 the logger instance
     * @return the resulting summary
     */
    public CompilationSummary processFiles(List<File> sourceFiles,
            boolean runProjectInference,
            ReinsConfig config,
            Path projectRoot,
            Log log) throws Exception {
        return processFiles(
                new PreFilterResult(
                        sourceFiles,
                        buildCompileWorkSetEntries(sourceFiles, projectRoot),
                        runProjectInference,
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
        List<File> sourceFiles = preFilterResult.getSourceFiles();
        MarkdownDependencyGraph graph = graphBuilder.build(sourceFiles, projectRoot, validator);
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

        ReferenceDepthPolicy referenceDepthPolicy = config.getContext() == null
                ? ReferenceDepthPolicy.defaultPolicy()
                : config.getContext().resolveReferenceDepthPolicy();
        boolean includeReferencedAttachments = config.getContext() == null
                || config.getContext().isAttachReferencedFiles();
        log.info("Reference tree depth: " + referenceDepthPolicy.describeForLog());

        Set<Path> contextScanRoots = buildContextScanRoots(config);

        CompilationBackgroundPayload projectInferencePayload = CompilationBackgroundPayload.empty();
        if (config.isEnableProjectInference()) {
            projectInferencePayload = projectContextService.load(
                    config.getProjectContextFile(),
                    projectRoot,
                    contextScanRoots,
                    referenceDepthPolicy,
                    includeReferencedAttachments);
            if (!projectInferencePayload.isEmpty()) {
                log.info("Compilation background loaded for inference: " + config.getProjectContextFile().getName());
            }
        }

        CompilationBackgroundPayload compilationBackgroundPayload;
        if (config.getContext().isIncludeProjectFiles()) {
            compilationBackgroundPayload = projectInferencePayload;
        } else {
            if (config.getProjectContextFile() != null
                    && Files.exists(config.getProjectContextFile().toPath().toAbsolutePath().normalize())) {
                log.warn("Compilation background file '" + config.getProjectContextFile().getName()
                        + "' detected but context.includeProjectFiles=false. "
                        + "Set <context><includeProjectFiles>true</includeProjectFiles></context> to enable context injection.");
            }
            compilationBackgroundPayload = CompilationBackgroundPayload.empty();
        }

        {
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

        if (preFilterResult.isRunProjectInference() && config.isEnableProjectInference()
                && !projectInferencePayload.isEmpty()) {
            resolveProjectInferenceCycleService(log, projectRoot).run(projectInferencePayload, config);
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
            ScriptEvaluator scriptEvaluator = new ScriptEvaluator(scriptRegistry, log, scriptsEventsEnabled);
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

        CycleWorkSetEntry workSetEntry;
        while ((workSetEntry = queue.nextExecutable()) != null) {
            String relativeSourcePath = workSetEntry.getSourcePath();
            MarkdownSourceNode node = graph.getNodes().get(relativeSourcePath);
            String sourceCategory = PathHelper.resolveSourceCategory(node.absolutePath(), projectRoot);
            CompilationBackgroundPayload sourceCycleContextPayload = PathHelper.isMainOrTestScope(sourceCategory)
                    ? compilationBackgroundPayload
                    : CompilationBackgroundPayload.empty();
            String resolvedTargetRoot = PathHelper.resolveTargetRootForSourceCategory(config, projectRoot,
                    sourceCategory);
            String canonicalSourcePath = trackingStore.canonicalizePath(relativeSourcePath);
            long start = System.currentTimeMillis();
            int processingIndex = workSetEntry.getOrderIndex() + 1;
            CompilationOutput output = initOutput(node, relativeSourcePath, graph, sourceCategory, processingIndex,
                    executionOrder.size(), workSetEntry);
            String processingStatus = workSetEntry.getStatus().name();

            log.info("**");
            log.info("** " + processingStatus + ": " + sourceCategory + ":" + relativeSourcePath);
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

            ctx.setConfig(config);
            ctx.setProjectRoot(projectRoot);
            ctx.setLog(log);
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
            } catch (GeminiEmptyResponseException ex) {
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
                    continue;
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
                    continue;
                }
                if (config.isFailOnError()) {
                    throw ex;
                }
            }
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
                            if (!config.isDryRun() && !config.getTracking().isFreezeState()) {
                                SourceTrackingRecord recordToUpdate = TrackingRecordHelper.copyTrackingRecord(record);
                                String targetRoot = recordToUpdate.getResolvedTargetRoot();
                                if (targetRoot == null || targetRoot.isBlank()) {
                                    targetRoot = PathHelper.resolveTargetRootForSourceCategory(config, projectRoot,
                                            recordToUpdate.getSourceCategory());
                                }
                                TrackingRecordHelper.refreshTrackedMtimes(recordToUpdate, projectRoot, targetRoot,
                                        recordToUpdate.getSourceCategory());
                                sourceTrackingManager.commit(projectRoot, canonicalSourcePath, recordToUpdate,
                                        trackingStore);
                                if (config.getLogging() != null && config.getLogging().isTrackingFile()) {
                                    log.info("Tracking file updated (touched): " + canonicalSourcePath);
                                }
                            }
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

    private ProjectReasoningCycleService resolveProjectInferenceCycleService(Log log, Path projectRoot) {
        if (projectInferenceCycleService != null) {
            return projectInferenceCycleService;
        }
        return new DefaultProjectReasoningCycleService(
                reasoningService,
                trackingStore,
                recompilationDecider,
                fingerprintService,
                new ReferenceTreeContextService(),
                new ReasoningPromptBuilder(),
                log,
                projectRoot);
    }

}
