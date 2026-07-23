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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ReasoningRequest is part of the orchestration of conversational reasoning
 * loops, prompt construction, and tool instruction mapping in the reins
 * architecture.
 * Acts as a component managing reasoning request.
 */
public class ReasoningRequest {
    private String sourcePath;
    private String sourceScope;
    private String sourceHash;
    private String message;
    private Path projectRoot;
    private BasePathMappingSet baseMappings;
    private GeminiSettings modelConfigSnapshot;
    private List<AttachedFilePayload> attachments = new ArrayList<>();
    private Consumer<String> userMessageListener;

    /**
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Sets the source path.
     *
     * @param sourcePath the path of the source file
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * Gets the source scope.
     *
     * @return the string result
     */
    public String getSourceScope() {
        return sourceScope;
    }

    /**
     * Sets the source scope.
     *
     * @param sourceScope the source scope
     */
    public void setSourceScope(String sourceScope) {
        this.sourceScope = sourceScope;
    }

    /**
     * Gets the source hash.
     *
     * @return the string result
     */
    public String getSourceHash() {
        return sourceHash;
    }

    /**
     * Sets the source hash.
     *
     * @param sourceHash the source hash
     */
    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }

    /**
     * Gets the message.
     *
     * @return the string result
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Sets the project root.
     *
     * @param projectRoot the root path of the project
     */
    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /**
     * Gets the base mappings.
     *
     * @return the collection of elements
     */
    public BasePathMappingSet getBaseMappings() {
        return baseMappings;
    }

    /**
     * Sets the base mappings.
     *
     * @param baseMappings the base mappings
     */
    public void setBaseMappings(BasePathMappingSet baseMappings) {
        this.baseMappings = baseMappings;
    }

    /**
     * Gets the model config snapshot.
     *
     * @return the collection of elements
     */
    public GeminiSettings getModelConfigSnapshot() {
        return modelConfigSnapshot;
    }

    /**
     * Sets the model config snapshot.
     *
     * @param modelConfigSnapshot the model config snapshot
     */
    public void setModelConfigSnapshot(GeminiSettings modelConfigSnapshot) {
        this.modelConfigSnapshot = modelConfigSnapshot;
    }

    /**
     * Gets the attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getAttachments() {
        return attachments;
    }

    /**
     * Sets the attachments.
     *
     * @param attachments the list of attachments
     */
    public void setAttachments(List<AttachedFilePayload> attachments) {
        this.attachments = attachments;
    }

    /**
     * Gets the user message listener.
     *
     * @return the string result
     */
    public Consumer<String> getUserMessageListener() {
        return userMessageListener;
    }

    /**
     * Sets the user message listener.
     *
     * @param userMessageListener the user message listener
     */
    public void setUserMessageListener(Consumer<String> userMessageListener) {
        this.userMessageListener = userMessageListener;
    }

    private CompilationBackgroundPayload compilationBackgroundPayload;

    /**
     * Gets the compilation background payload.
     *
     * @return the resulting payload
     */
    public CompilationBackgroundPayload getCompilationBackgroundPayload() {
        return compilationBackgroundPayload;
    }

    /**
     * Sets the compilation background payload.
     *
     * @param compilationBackgroundPayload the compilation background payload
     */
    public void setCompilationBackgroundPayload(CompilationBackgroundPayload compilationBackgroundPayload) {
        this.compilationBackgroundPayload = compilationBackgroundPayload;
    }

    private boolean projectInferenceCycle = false;

    /**
     * Checks if the component is project inference cycle.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isProjectInferenceCycle() {
        return projectInferenceCycle;
    }

    /**
     * Sets the project inference cycle.
     *
     * @param projectInferenceCycle the project inference cycle
     */
    public void setProjectInferenceCycle(boolean projectInferenceCycle) {
        this.projectInferenceCycle = projectInferenceCycle;
    }

    private EagerlyProvideResult eagerlyProvide;
    private ReferenceDepthPolicy referenceDepthPolicy;
    private boolean useCachedContent = true;
    private String cachedContentId;
    private String mainSourceQualifiedPath;
    private Consumer<String> operationLogger;
    private List<String> phaseOrderOverride = new ArrayList<>();
    private SourceProcessingStatus processingStatus;

    /**
     * Gets the eagerly provide.
     *
     * @return the resulting result
     */
    public EagerlyProvideResult getEagerlyProvide() {
        return eagerlyProvide;
    }

    /**
     * Sets the eagerly provide.
     *
     * @param eagerlyProvide the eagerly provide
     */
    public void setEagerlyProvide(EagerlyProvideResult eagerlyProvide) {
        this.eagerlyProvide = eagerlyProvide;
    }

    /**
     * Gets the reference depth policy.
     *
     * @return the resolved or constructed object
     */
    public ReferenceDepthPolicy getReferenceDepthPolicy() {
        return referenceDepthPolicy;
    }

    /**
     * Sets the reference depth policy.
     *
     * @param referenceDepthPolicy the reference depth policy
     */
    public void setReferenceDepthPolicy(ReferenceDepthPolicy referenceDepthPolicy) {
        this.referenceDepthPolicy = referenceDepthPolicy;
    }

    /**
     * Checks if the component is use cached content.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isUseCachedContent() {
        return useCachedContent;
    }

    /**
     * Sets the use cached content.
     *
     * @param useCachedContent the use cached content
     */
    public void setUseCachedContent(boolean useCachedContent) {
        this.useCachedContent = useCachedContent;
    }

    /**
     * Gets the cached content id.
     *
     * @return the string result
     */
    public String getCachedContentId() {
        return cachedContentId;
    }

    /**
     * Sets the cached content id.
     *
     * @param cachedContentId the cached content id
     */
    public void setCachedContentId(String cachedContentId) {
        this.cachedContentId = cachedContentId;
    }

    /**
     * Gets the main source qualified path.
     *
     * @return the string result
     */
    public String getMainSourceQualifiedPath() {
        return mainSourceQualifiedPath;
    }

    /**
     * Sets the main source qualified path.
     *
     * @param mainSourceQualifiedPath the main source qualified path
     */
    public void setMainSourceQualifiedPath(String mainSourceQualifiedPath) {
        this.mainSourceQualifiedPath = mainSourceQualifiedPath;
    }

    /**
     * Gets the operation logger.
     *
     * @return the string result
     */
    public Consumer<String> getOperationLogger() {
        return operationLogger;
    }

    /**
     * Sets the operation logger.
     *
     * @param operationLogger the operation logger
     */
    public void setOperationLogger(Consumer<String> operationLogger) {
        this.operationLogger = operationLogger;
    }

    /**
     * Gets the phase order override.
     *
     * @return the string result
     */
    public List<String> getPhaseOrderOverride() {
        return phaseOrderOverride;
    }

    /**
     * Sets the phase order override.
     *
     * @param phaseOrderOverride the phase order override
     */
    public void setPhaseOrderOverride(List<String> phaseOrderOverride) {
        this.phaseOrderOverride = phaseOrderOverride == null ? new ArrayList<>() : new ArrayList<>(phaseOrderOverride);
    }

    /**
     * Gets the processing status.
     *
     * @return the resulting status
     */
    public SourceProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    /**
     * Sets the processing status.
     *
     * @param processingStatus the processing status
     */
    public void setProcessingStatus(SourceProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    private SourceTrackingRecord priorRecord;

    /**
     * Gets the prior record.
     *
     * @return the resulting record
     */
    public SourceTrackingRecord getPriorRecord() {
        return priorRecord;
    }

    /**
     * Sets the prior record.
     *
     * @param priorRecord the prior record
     */
    public void setPriorRecord(SourceTrackingRecord priorRecord) {
        this.priorRecord = priorRecord;
    }
}
