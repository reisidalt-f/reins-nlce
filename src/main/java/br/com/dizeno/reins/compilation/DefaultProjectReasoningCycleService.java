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
import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.ReasoningPromptBuilder;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.reasoning.ReferenceTreeContextService;
import br.com.dizeno.reins.util.PathLogFormatter;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContextFactory;
import br.com.dizeno.reins.reasoning.scripting.PhaseListValidator;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.PathNormalizer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DefaultProjectReasoningCycleService is part of the core compilation lifecycle
 * management, orchestrating file discovery, dependency resolution, and pipeline
 * execution in the reins architecture.
 * Acts as a service component responsible for managing and executing operations
 * related to its prefix.
 */
public class DefaultProjectReasoningCycleService implements ProjectReasoningCycleService {
    private final ReasoningService reasoningService;
    private final CompilationTrackingStore trackingStore;
    private final SourceFingerprintService fingerprintService;
    private final ReferenceTreeContextService referenceTreeContextService;
    private final Log log;
    private final Path projectRoot;
    private final PathValidator pathValidator;

    /**
     * Constructs a new instance of {@link DefaultProjectReasoningCycleService}.
     *
     * @param reasoningService            the LLM reasoning service component
     * @param trackingStore               the persistence store for file tracking
     *                                    records
     * @param recompilationDecider        the decider for recompilation needs
     * @param fingerprintService          the service used to calculate file
     *                                    fingerprints
     * @param referenceTreeContextService the reference tree context service
     * @param promptBuilder               the prompt builder
     * @param log                         the logger instance
     * @param projectRoot                 the root path of the project
     */
    public DefaultProjectReasoningCycleService(ReasoningService reasoningService,
            CompilationTrackingStore trackingStore,
            RecompilationDecider recompilationDecider,
            SourceFingerprintService fingerprintService,
            ReferenceTreeContextService referenceTreeContextService,
            ReasoningPromptBuilder promptBuilder,
            Log log,
            Path projectRoot) {
        this(reasoningService, trackingStore, recompilationDecider, fingerprintService,
                referenceTreeContextService, promptBuilder, log, projectRoot,
                new PathValidator(projectRoot.toAbsolutePath().normalize()));
    }

    /**
     * Constructs a new instance of {@link DefaultProjectReasoningCycleService}.
     *
     * @param reasoningService            the LLM reasoning service component
     * @param trackingStore               the persistence store for file tracking
     *                                    records
     * @param recompilationDecider        the decider for recompilation needs
     * @param fingerprintService          the service used to calculate file
     *                                    fingerprints
     * @param referenceTreeContextService the reference tree context service
     * @param promptBuilder               the prompt builder
     * @param log                         the logger instance
     * @param projectRoot                 the root path of the project
     * @param pathValidator               the path validator
     */
    public DefaultProjectReasoningCycleService(ReasoningService reasoningService,
            CompilationTrackingStore trackingStore,
            RecompilationDecider recompilationDecider,
            SourceFingerprintService fingerprintService,
            ReferenceTreeContextService referenceTreeContextService,
            ReasoningPromptBuilder promptBuilder,
            Log log,
            Path projectRoot,
            PathValidator pathValidator) {
        this.reasoningService = reasoningService;
        this.trackingStore = trackingStore;
        this.fingerprintService = fingerprintService;
        this.referenceTreeContextService = referenceTreeContextService;
        this.log = log;
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.pathValidator = pathValidator;
    }

    /**
     * Runs the execution cycle.
     *
     * @param compilationBackgroundPayload the compilation background payload
     * @param config                       the Reins configuration settings
     */
    @Override
    public void run(CompilationBackgroundPayload compilationBackgroundPayload,
            ReinsConfig config) throws MojoExecutionException {
        CompilationBackgroundFile projectFile = compilationBackgroundPayload.getFiles().get(0);
        Path absPath = projectFile.getAbsolutePath().normalize();
        String sourcePath = this.projectRoot.relativize(absPath).toString().replace('\\', '/');
        String content = projectFile.getContent();
        String sourceHash = fingerprintService.sha256(content);
        long sourceMtime = readFileMtime(absPath);

        Set<String> currentProjectSourcePaths = discoverCurrentSourceMarkdownPaths();
        Set<String> currentProjectFilePaths = discoverCurrentProjectFilePaths(compilationBackgroundPayload);
        MtimeSnapshotUtil.SnapshotResult currentProjectSourceSnapshot = snapshotTrackedMtimes(
                currentProjectSourcePaths);
        MtimeSnapshotUtil.SnapshotResult currentProjectFileSnapshot = snapshotTrackedMtimes(currentProjectFilePaths);
        Set<String> missingOrUnreadable = new LinkedHashSet<>();
        missingOrUnreadable.addAll(currentProjectSourceSnapshot.missingOrUnreadablePaths());
        missingOrUnreadable.addAll(currentProjectFileSnapshot.missingOrUnreadablePaths());

        Path targetRoot = BasePathMappingSet.fromConfig(config, projectRoot).getTargetRoot();
        Path scriptRoot = ScriptRunnerConfig.fromSettings(config, projectRoot).getScriptRoot();
        BasePathMappingSet projectMappings = BasePathMappingSet.forProjectInference(projectRoot, targetRoot,
                scriptRoot);
        String resolvedTargetRoot = PathNormalizer.toForwardSlashes(projectRoot.relativize(targetRoot).toString());
        ReasoningRequest request = new ReasoningRequest();
        request.setSourcePath(sourcePath);
        request.setSourceScope(SourceScope.PROJECT.value());
        request.setSourceHash(sourceHash);
        String canonicalDisplayPath = TrackedPathResolver.canonicalizeSourcePath(sourcePath);
        request.setMessage("Build the files necessary to implement the design found in the main source markdown "
                + canonicalDisplayPath + " and the files it references.");
        request.setProjectRoot(projectRoot);
        request.setBaseMappings(projectMappings);
        request.setMainSourceQualifiedPath("main:" + canonicalDisplayPath);
        request.setModelConfigSnapshot(config.getGemini());
        request.setProjectInferenceCycle(true);
        request.setReferenceDepthPolicy(resolveReferenceDepthPolicy(config));
        request.setAttachments(List.of());
        request.setUserMessageListener(body -> log.info(body));

        List<String> sourceBasePhases;
        try {
            sourceBasePhases = resolveSourceBasePhaseNames(request, config);
        } catch (Exception ex) {
            throw new MojoExecutionException("Project source-base phase resolution failed: " + ex.getMessage(), ex);
        }
        log.debug("[reins-script] source-base phases=" + sourceBasePhases);
        request.setPhaseOrderOverride(sourceBasePhases);

        BasePathResolver resolver = new BasePathResolver(
                projectMappings,
                pathValidator);
        Set<Path> excludedRoots = Set.of(
                projectRoot.resolve(ProjectDirectoryPaths.MAIN_NL_ROOT).normalize(),
                projectRoot.resolve(ProjectDirectoryPaths.TEST_NL_ROOT).normalize());
        try {
            referenceTreeContextService.build(request, config, resolver, excludedRoots);
        } catch (GraphProcessingException ex) {
            throw new MojoExecutionException(
                    "Project file reference tree validation failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new MojoExecutionException(
                    "Failed to build project file reference tree: " + ex.getMessage(), ex);
        }

        ReasoningResult result;
        try {
            result = reasoningService.runCycle(request, config);
        } catch (Exception ex) {
            throw new MojoExecutionException(
                    "Project file inference cycle failed: " + ex.getMessage(), ex);
        }

        List<String> writtenPaths = result.getWrittenPaths();
        Map<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        Map<String, Long> writtenMtimes = result.getWrittenMtimes();
        for (String p : writtenPaths) {
            compiledFiles.put(p, new FileTrackingDetails(sourcePath, "project", writtenMtimes.getOrDefault(p, null)));
        }
        List<String> inspectedPaths = result.getInspectedPaths();
        Map<String, FileTrackingDetails> inspectedFiles = new LinkedHashMap<>();
        Map<String, Long> inspectedMtimes = result.getInspectedMtimes();
        for (String p : inspectedPaths) {
            inspectedFiles.put(p,
                    new FileTrackingDetails(sourcePath, "project", inspectedMtimes.getOrDefault(p, null)));
        }

        Map<String, FileTrackingDetails> markdownReferences = new LinkedHashMap<>();
        for (String p : currentProjectSourcePaths) {
            markdownReferences.put(p,
                    new FileTrackingDetails(sourcePath, "project", currentProjectSourceSnapshot.mtimes().get(p)));
        }

        for (String p : currentProjectFilePaths) {
            inspectedFiles.putIfAbsent(p,
                    new FileTrackingDetails(sourcePath, "project", currentProjectFileSnapshot.mtimes().get(p)));
        }

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(sourcePath);
        record.setSourceCategory(SourceScope.PROJECT.value());
        record.setSourceHash(sourceHash);
        record.setSourceModificationTime(sourceMtime);
        record.setBlockFingerprints(List.of(sourceHash));
        record.setCompiledFiles(compiledFiles);
        record.setInspectedFiles(inspectedFiles);
        record.setMarkdownReferences(markdownReferences);
        record.setLastCompiledAt(Instant.now().toString());
        record.setLastStatus("success");
        record.setModel(config.resolveModel());
        record.setResolvedTargetRoot(resolvedTargetRoot);
        try {
            trackingStore.save(projectRoot, sourcePath, record);
        } catch (Exception ex) {
            throw new MojoExecutionException(
                    "Failed to save tracking file for project inference cycle: " + ex.getMessage(), ex);
        }

        String finalIntent = result.getFinalIntent();
        boolean isNoOutput = "finish_success".equalsIgnoreCase(finalIntent)
                && result.getWrittenPaths().isEmpty();
        if (config.getLogging() != null && config.getLogging().isResult()) {
            if (isNoOutput) {
                log.info("[project] reasoning-finish-no-output: " + PathLogFormatter.formatPath(sourcePath, projectRoot));
            } else {
                log.info("[project] compiled: " + PathLogFormatter.formatPath(sourcePath, projectRoot));
            }
        }
    }

    private MtimeSnapshotUtil.SnapshotResult snapshotTrackedMtimes(Set<String> trackedPaths) {
        return MtimeSnapshotUtil.snapshot(this.projectRoot, trackedPaths);
    }

    private long readFileMtime(Path absolutePath) {
        try {
            return Files.getLastModifiedTime(absolutePath).toMillis();
        } catch (Exception ex) {
            return RecompilationDecider.MTIME_UNAVAILABLE;
        }
    }

    private Set<String> discoverCurrentProjectFilePaths(CompilationBackgroundPayload compilationBackgroundPayload) {
        Set<String> paths = new LinkedHashSet<>();
        for (CompilationBackgroundFile file : compilationBackgroundPayload.getFiles()) {
            Path relative = projectRoot.relativize(file.getAbsolutePath().toAbsolutePath().normalize());
            paths.add(PathNormalizer.toForwardSlashes(relative.toString()));
        }
        return paths;
    }

    private Set<String> discoverCurrentSourceMarkdownPaths() {
        Set<String> sourcePaths = new LinkedHashSet<>();
        sourcePaths.addAll(scanMarkdownFiles(projectRoot.resolve(ProjectDirectoryPaths.MAIN_NL_ROOT)));
        sourcePaths.addAll(scanMarkdownFiles(projectRoot.resolve(ProjectDirectoryPaths.TEST_NL_ROOT)));
        return sourcePaths;
    }

    private Set<String> scanMarkdownFiles(Path root) {
        Set<String> paths = new LinkedHashSet<>();
        if (!Files.exists(root)) {
            return paths;
        }
        try (var stream = Files.walk(root)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .forEach(path -> paths
                            .add(PathNormalizer.toForwardSlashes(projectRoot.relativize(path).toString())));
        } catch (Exception ex) {

        }
        return paths;
    }

    private ReferenceDepthPolicy resolveReferenceDepthPolicy(ReinsConfig config) {
        if (config == null || config.getContext() == null) {
            return ReferenceDepthPolicy.defaultPolicy();
        }
        return config.getContext().resolveReferenceDepthPolicy();
    }

    private List<String> resolveSourceBasePhaseNames(ReasoningRequest request,
            ReinsConfig config) {
        Path scriptRoot = ScriptRunnerConfig.fromSettings(config, projectRoot).getScriptRoot();
        File customScriptDir = resolveCustomScriptDir(config);
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(customScriptDir, null), customScriptDir);
        registry.validateAll();

        boolean scriptsEventsEnabled = config != null
                && config.getLogging() != null
                && config.getLogging().isScriptsEvents();
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, log, scriptsEventsEnabled);
        ReasoningScriptContextFactory factory = new ReasoningScriptContextFactory();
        ReasoningScriptContext ctx = factory.buildBase(
                request,
                config,
                null,
                null,
                scriptRoot != null,
                null,
                List.of(),
                List.of(),
                List.of(),
                true);
        String rendered;
        try {
            rendered = evaluator.evaluate("source-base-phase-list.ftl", ctx, scriptsEventsEnabled);
        } catch (Exception ex) {
            throw new IllegalStateException("Error in source-base-phase-list script: " + ex.getMessage(), ex);
        }
        List<String> parsed = parsePhaseList(rendered);
        PhaseListValidator.validate(parsed, evaluator);
        for (String phase : parsed) {
            String scriptName = phase.endsWith(".ftl") ? phase : phase + ".ftl";
            if (!evaluator.hasScript(scriptName)) {
                throw new IllegalStateException("Unknown phase name in custom phase list: " + phase);
            }
        }
        return parsed;
    }

    private File resolveCustomScriptDir(ReinsConfig config) {
        if (config == null || config.getReasoning() == null
                || config.getReasoning().getScriptsPath() == null
                || config.getReasoning().getScriptsPath().isBlank()) {
            return null;
        }
        String configuredPath = config.getReasoning().getScriptsPath().trim();
        File configured = new File(configuredPath);
        return configured.isAbsolute() ? configured : projectRoot.resolve(configuredPath).toFile();
    }

    private List<String> parsePhaseList(String rendered) {
        if (rendered == null || rendered.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> phases = new LinkedHashSet<>();
        for (String line : rendered.split("\\R")) {
            if (line == null) {
                continue;
            }
            String phase = line.trim();
            if (!phase.isEmpty()) {
                phases.add(phase);
            }
        }
        return new ArrayList<>(phases);
    }

}
