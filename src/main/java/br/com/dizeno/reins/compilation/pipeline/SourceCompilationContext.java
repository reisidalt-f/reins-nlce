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

import br.com.dizeno.reins.compilation.CompilationOutput;
import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.CycleWorkSetEntry;
import br.com.dizeno.reins.compilation.ResultPrinter;
import br.com.dizeno.reins.compilation.SourceProcessingQueue;
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.EagerlyProvideResult;
import br.com.dizeno.reins.reasoning.EagerlyProvideService;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import br.com.dizeno.reins.source.graph.ProcessingOrderResolver;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.Map;

 
/**
 * SourceCompilationContext is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a component managing source compilation context.
 */
public final class SourceCompilationContext {

    

    
    private CycleWorkSetEntry workSetEntry;
    private MarkdownSourceNode node;
    private String relativeSourcePath;
    private String canonicalSourcePath;
    private String sourceCategory;
    private String resolvedTargetRoot;
    private boolean validateOnly;
    private CompilationOutput output;
    private long startTimeMs;

    
    private ReinsConfig config;
    private Path projectRoot;
    private Log log;
    private MarkdownDependencyGraph graph;
    private CompilationSummary summary;
    private CompilationTrackingStore trackingStore;
    private SourceTrackingManager sourceTrackingManager;
    private SourceFingerprintService fingerprintService;
    private EagerlyProvideService eagerlyProvideService;
    private ReasoningService effectiveReasoningService;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService queueToolingService;
    private SourceProcessingQueue queue;
    private Map<String, CycleWorkSetEntry> workSetByPath;
    private Map<String, Integer> orderIndex;
    private PathValidator validator;
    private ReferenceDepthPolicy referenceDepthPolicy;
    private CompilationBackgroundPayload compilationBackgroundPayload;
    private ResultPrinter resultPrinter;
    private ProcessingOrderResolver processingOrderResolver;

    
    private boolean skippedLoggingEnabled;
    private boolean selectionReasonLoggingEnabled;

    

     
    private String sourceText;
     
    private String sourceHash;

     
    private SourceTrackingRecord priorRecord;

     
    private EagerlyProvideResult eagerlyProvide;

     
    private ReasoningRequest reasoningRequest;

     
    private ReasoningResult reasoningResult;

    

    /**
     * Gets the work set entry.
     *
     * @return the collection of elements
     */
    public CycleWorkSetEntry getWorkSetEntry() { return workSetEntry; }
    /**
     * Sets the work set entry.
     *
     * @param workSetEntry the entry representing the source file in the current cycle
     */
    public void setWorkSetEntry(CycleWorkSetEntry workSetEntry) { this.workSetEntry = workSetEntry; }

    /**
     * Gets the node.
     *
     * @return the resolved or constructed object
     */
    public MarkdownSourceNode getNode() { return node; }
    /**
     * Sets the node.
     *
     * @param node the source node in the dependency graph
     */
    public void setNode(MarkdownSourceNode node) { this.node = node; }

    /**
     * Gets the relative source path.
     *
     * @return the string result
     */
    public String getRelativeSourcePath() { return relativeSourcePath; }
    /**
     * Sets the relative source path.
     *
     * @param relativeSourcePath the relative path of the source file
     */
    public void setRelativeSourcePath(String relativeSourcePath) { this.relativeSourcePath = relativeSourcePath; }

    /**
     * Gets the canonical source path.
     *
     * @return the string result
     */
    public String getCanonicalSourcePath() { return canonicalSourcePath; }
    /**
     * Sets the canonical source path.
     *
     * @param canonicalSourcePath the canonicalized path of the source file
     */
    public void setCanonicalSourcePath(String canonicalSourcePath) { this.canonicalSourcePath = canonicalSourcePath; }

    /**
     * Gets the source category.
     *
     * @return the string result
     */
    public String getSourceCategory() { return sourceCategory; }
    /**
     * Sets the source category.
     *
     * @param sourceCategory the category of the source file (e.g. main or test)
     */
    public void setSourceCategory(String sourceCategory) { this.sourceCategory = sourceCategory; }

    /**
     * Gets the resolved target root.
     *
     * @return the string result
     */
    public String getResolvedTargetRoot() { return resolvedTargetRoot; }
    /**
     * Sets the resolved target root.
     *
     * @param resolvedTargetRoot the resolved target root
     */
    public void setResolvedTargetRoot(String resolvedTargetRoot) { this.resolvedTargetRoot = resolvedTargetRoot; }

    /**
     * Checks if the component is validate only.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValidateOnly() { return validateOnly; }
    /**
     * Sets the validate only.
     *
     * @param validateOnly the validate only
     */
    public void setValidateOnly(boolean validateOnly) { this.validateOnly = validateOnly; }

    /**
     * Gets the output.
     *
     * @return the resolved or constructed object
     */
    public CompilationOutput getOutput() { return output; }
    /**
     * Sets the output.
     *
     * @param output the output
     */
    public void setOutput(CompilationOutput output) { this.output = output; }

    /**
     * Gets the start time ms.
     *
     * @return the numeric value
     */
    public long getStartTimeMs() { return startTimeMs; }
    /**
     * Sets the start time ms.
     *
     * @param startTimeMs the start time ms
     */
    public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }

    /**
     * Gets the config.
     *
     * @return the resulting config
     */
    public ReinsConfig getConfig() { return config; }
    /**
     * Sets the config.
     *
     * @param config the Reins configuration settings
     */
    public void setConfig(ReinsConfig config) { this.config = config; }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() { return projectRoot; }
    /**
     * Sets the project root.
     *
     * @param projectRoot the root path of the project
     */
    public void setProjectRoot(Path projectRoot) { this.projectRoot = projectRoot; }

    /**
     * Gets the log.
     *
     * @return the resolved or constructed object
     */
    public Log getLog() { return log; }
    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) { this.log = log; }

    /**
     * Gets the graph.
     *
     * @return the resolved or constructed object
     */
    public MarkdownDependencyGraph getGraph() { return graph; }
    /**
     * Sets the graph.
     *
     * @param graph the markdown dependency graph
     */
    public void setGraph(MarkdownDependencyGraph graph) { this.graph = graph; }

    /**
     * Gets the summary.
     *
     * @return the resulting summary
     */
    public CompilationSummary getSummary() { return summary; }
    /**
     * Sets the summary.
     *
     * @param summary the summary
     */
    public void setSummary(CompilationSummary summary) { this.summary = summary; }

    /**
     * Gets the tracking store.
     *
     * @return the resolved or constructed object
     */
    public CompilationTrackingStore getTrackingStore() { return trackingStore; }
    /**
     * Sets the tracking store.
     *
     * @param trackingStore the persistence store for file tracking records
     */
    public void setTrackingStore(CompilationTrackingStore trackingStore) { this.trackingStore = trackingStore; }

    /**
     * Gets the source tracking manager.
     *
     * @return the resolved or constructed object
     */
    public SourceTrackingManager getSourceTrackingManager() { return sourceTrackingManager; }
    /**
     * Sets the source tracking manager.
     *
     * @param sourceTrackingManager the source tracking manager
     */
    public void setSourceTrackingManager(SourceTrackingManager sourceTrackingManager) { this.sourceTrackingManager = sourceTrackingManager; }

    /**
     * Gets the fingerprint service.
     *
     * @return the resolved or constructed object
     */
    public SourceFingerprintService getFingerprintService() { return fingerprintService; }
    /**
     * Sets the fingerprint service.
     *
     * @param fingerprintService the service used to calculate file fingerprints
     */
    public void setFingerprintService(SourceFingerprintService fingerprintService) { this.fingerprintService = fingerprintService; }

    /**
     * Gets the eagerly provide service.
     *
     * @return the resolved or constructed object
     */
    public EagerlyProvideService getEagerlyProvideService() { return eagerlyProvideService; }
    /**
     * Sets the eagerly provide service.
     *
     * @param eagerlyProvideService the service to eagerly resolve file attachments
     */
    public void setEagerlyProvideService(EagerlyProvideService eagerlyProvideService) { this.eagerlyProvideService = eagerlyProvideService; }

    /**
     * Gets the effective reasoning service.
     *
     * @return the resolved or constructed object
     */
    public ReasoningService getEffectiveReasoningService() { return effectiveReasoningService; }
    /**
     * Sets the effective reasoning service.
     *
     * @param effectiveReasoningService the effective reasoning service
     */
    public void setEffectiveReasoningService(ReasoningService effectiveReasoningService) { this.effectiveReasoningService = effectiveReasoningService; }

    public br.com.dizeno.reins.reasoning.tooling.ToolingService getQueueToolingService() { return queueToolingService; }
    /**
     * Sets the queue tooling service.
     *
     * @param queueToolingService the queue tooling service
     */
    public void setQueueToolingService(br.com.dizeno.reins.reasoning.tooling.ToolingService queueToolingService) { this.queueToolingService = queueToolingService; }

    /**
     * Gets the queue.
     *
     * @return the resolved or constructed object
     */
    public SourceProcessingQueue getQueue() { return queue; }
    /**
     * Sets the queue.
     *
     * @param queue the queue
     */
    public void setQueue(SourceProcessingQueue queue) { this.queue = queue; }

    /**
     * Gets the work set by path.
     *
     * @return the string result
     */
    public Map<String, CycleWorkSetEntry> getWorkSetByPath() { return workSetByPath; }
    /**
     * Sets the work set by path.
     *
     * @param workSetByPath the work set by path
     */
    public void setWorkSetByPath(Map<String, CycleWorkSetEntry> workSetByPath) { this.workSetByPath = workSetByPath; }

    /**
     * Gets the order index.
     *
     * @return the string result
     */
    public Map<String, Integer> getOrderIndex() { return orderIndex; }
    /**
     * Sets the order index.
     *
     * @param orderIndex the order index
     */
    public void setOrderIndex(Map<String, Integer> orderIndex) { this.orderIndex = orderIndex; }

    /**
     * Gets the validator.
     *
     * @return the resolved or constructed object
     */
    public PathValidator getValidator() { return validator; }
    /**
     * Sets the validator.
     *
     * @param validator the path validator for security boundary checks
     */
    public void setValidator(PathValidator validator) { this.validator = validator; }

    /**
     * Gets the reference depth policy.
     *
     * @return the resolved or constructed object
     */
    public ReferenceDepthPolicy getReferenceDepthPolicy() { return referenceDepthPolicy; }
    /**
     * Sets the reference depth policy.
     *
     * @param referenceDepthPolicy the reference depth policy
     */
    public void setReferenceDepthPolicy(ReferenceDepthPolicy referenceDepthPolicy) { this.referenceDepthPolicy = referenceDepthPolicy; }

    /**
     * Gets the compilation background payload.
     *
     * @return the resulting payload
     */
    public CompilationBackgroundPayload getCompilationBackgroundPayload() { return compilationBackgroundPayload; }
    /**
     * Sets the compilation background payload.
     *
     * @param compilationBackgroundPayload the compilation background payload
     */
    public void setCompilationBackgroundPayload(CompilationBackgroundPayload compilationBackgroundPayload) { this.compilationBackgroundPayload = compilationBackgroundPayload; }

    /**
     * Gets the result printer.
     *
     * @return the resolved or constructed object
     */
    public ResultPrinter getResultPrinter() { return resultPrinter; }
    /**
     * Sets the result printer.
     *
     * @param resultPrinter the printer component for compilation outcomes
     */
    public void setResultPrinter(ResultPrinter resultPrinter) { this.resultPrinter = resultPrinter; }

    /**
     * Gets the processing order resolver.
     *
     * @return the resolved or constructed object
     */
    public ProcessingOrderResolver getProcessingOrderResolver() { return processingOrderResolver; }
    /**
     * Sets the processing order resolver.
     *
     * @param processingOrderResolver the processing order resolver
     */
    public void setProcessingOrderResolver(ProcessingOrderResolver processingOrderResolver) { this.processingOrderResolver = processingOrderResolver; }

    /**
     * Checks if the component is skipped logging enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSkippedLoggingEnabled() { return skippedLoggingEnabled; }
    /**
     * Sets the skipped logging enabled.
     *
     * @param skippedLoggingEnabled the skipped logging enabled
     */
    public void setSkippedLoggingEnabled(boolean skippedLoggingEnabled) { this.skippedLoggingEnabled = skippedLoggingEnabled; }

    /**
     * Checks if the component is selection reason logging enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSelectionReasonLoggingEnabled() { return selectionReasonLoggingEnabled; }
    /**
     * Sets the selection reason logging enabled.
     *
     * @param selectionReasonLoggingEnabled the selection reason logging enabled
     */
    public void setSelectionReasonLoggingEnabled(boolean selectionReasonLoggingEnabled) { this.selectionReasonLoggingEnabled = selectionReasonLoggingEnabled; }

    /**
     * Gets the source text.
     *
     * @return the string result
     */
    public String getSourceText() { return sourceText; }
    /**
     * Sets the source text.
     *
     * @param sourceText the source text
     */
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }

    /**
     * Gets the source hash.
     *
     * @return the string result
     */
    public String getSourceHash() { return sourceHash; }
    /**
     * Sets the source hash.
     *
     * @param sourceHash the source hash
     */
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }

    /**
     * Gets the prior record.
     *
     * @return the resulting record
     */
    public SourceTrackingRecord getPriorRecord() { return priorRecord; }
    /**
     * Sets the prior record.
     *
     * @param priorRecord the prior record
     */
    public void setPriorRecord(SourceTrackingRecord priorRecord) { this.priorRecord = priorRecord; }

    /**
     * Gets the eagerly provide.
     *
     * @return the resulting result
     */
    public EagerlyProvideResult getEagerlyProvide() { return eagerlyProvide; }
    /**
     * Sets the eagerly provide.
     *
     * @param eagerlyProvide the eagerly provide
     */
    public void setEagerlyProvide(EagerlyProvideResult eagerlyProvide) { this.eagerlyProvide = eagerlyProvide; }

    /**
     * Gets the reasoning request.
     *
     * @return the resolved or constructed object
     */
    public ReasoningRequest getReasoningRequest() { return reasoningRequest; }
    /**
     * Sets the reasoning request.
     *
     * @param reasoningRequest the reasoning request
     */
    public void setReasoningRequest(ReasoningRequest reasoningRequest) { this.reasoningRequest = reasoningRequest; }

    /**
     * Gets the reasoning result.
     *
     * @return the resulting result
     */
    public ReasoningResult getReasoningResult() { return reasoningResult; }
    /**
     * Sets the reasoning result.
     *
     * @param reasoningResult the reasoning result
     */
    public void setReasoningResult(ReasoningResult reasoningResult) { this.reasoningResult = reasoningResult; }
}
