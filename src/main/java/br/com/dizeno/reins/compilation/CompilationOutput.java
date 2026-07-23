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

import java.util.ArrayList;
import java.util.List;

/**
 * CompilationOutput is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing compilation output.
 */
public class CompilationOutput {
    private String sourcePath;
    private String sourceCategory;
    private String outputPath;
    private String outputPolicy;
    private List<String> outputPaths = new ArrayList<>();
    private List<String> outputCategories = new ArrayList<>();
    private List<String> diagnostics = new ArrayList<>();
    private String status;
    private String message;
    private long durationMs;
    private String reprocessingReason;
    private int processingIndex;
    private int processingTotal;
    private List<String> dependencyChildren = new ArrayList<>();
    private List<String> resolutionDiagnostics = new ArrayList<>();
    private List<String> winningStrategies = new ArrayList<>();
    private String cycleId;
    private int cycleTurns;
    private String cycleIntent;
    private List<String> cycleUserMessages = new ArrayList<>();
    private List<String> cycleToolInfoPhrases = new ArrayList<>();
    private int retryAttemptCount;
    private int noUsableContentCount;
    private String processingState;
    private List<String> transitionCauses = new ArrayList<>();

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
     * Gets the source category.
     *
     * @return the string result
     */
    public String getSourceCategory() {
        return sourceCategory;
    }

    /**
     * Sets the source category.
     *
     * @param sourceCategory the category of the source file (e.g. main or test)
     */
    public void setSourceCategory(String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    /**
     * Gets the output path.
     *
     * @return the string result
     */
    public String getOutputPath() {
        return outputPath;
    }

    /**
     * Sets the output path.
     *
     * @param outputPath the output path
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    /**
     * Gets the output policy.
     *
     * @return the string result
     */
    public String getOutputPolicy() {
        return outputPolicy;
    }

    /**
     * Sets the output policy.
     *
     * @param outputPolicy the output policy
     */
    public void setOutputPolicy(String outputPolicy) {
        this.outputPolicy = outputPolicy;
    }

    /**
     * Gets the output paths.
     *
     * @return the string result
     */
    public List<String> getOutputPaths() {
        return outputPaths;
    }

    /**
     * Sets the output paths.
     *
     * @param outputPaths the output paths
     */
    public void setOutputPaths(List<String> outputPaths) {
        this.outputPaths = outputPaths;
    }

    /**
     * Gets the output categories.
     *
     * @return the string result
     */
    public List<String> getOutputCategories() {
        return outputCategories;
    }

    /**
     * Sets the output categories.
     *
     * @param outputCategories the output categories
     */
    public void setOutputCategories(List<String> outputCategories) {
        this.outputCategories = outputCategories;
    }

    /**
     * Gets the diagnostics.
     *
     * @return the string result
     */
    public List<String> getDiagnostics() {
        return diagnostics;
    }

    /**
     * Sets the diagnostics.
     *
     * @param diagnostics the diagnostics
     */
    public void setDiagnostics(List<String> diagnostics) {
        this.diagnostics = diagnostics;
    }

    /**
     * Gets the status.
     *
     * @return the string result
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(String status) {
        this.status = status;
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
     * Gets the duration ms.
     *
     * @return the numeric value
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * Sets the duration ms.
     *
     * @param durationMs the duration ms
     */
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * Gets the reprocessing reason.
     *
     * @return the string result
     */
    public String getReprocessingReason() {
        return reprocessingReason;
    }

    /**
     * Sets the reprocessing reason.
     *
     * @param reprocessingReason the reprocessing reason
     */
    public void setReprocessingReason(String reprocessingReason) {
        this.reprocessingReason = reprocessingReason;
    }

    /**
     * Gets the processing index.
     *
     * @return the numeric value
     */
    public int getProcessingIndex() {
        return processingIndex;
    }

    /**
     * Sets the processing index.
     *
     * @param processingIndex the processing index
     */
    public void setProcessingIndex(int processingIndex) {
        this.processingIndex = processingIndex;
    }

    /**
     * Gets the processing total.
     *
     * @return the numeric value
     */
    public int getProcessingTotal() {
        return processingTotal;
    }

    /**
     * Sets the processing total.
     *
     * @param processingTotal the processing total
     */
    public void setProcessingTotal(int processingTotal) {
        this.processingTotal = processingTotal;
    }

    /**
     * Gets the dependency children.
     *
     * @return the string result
     */
    public List<String> getDependencyChildren() {
        return dependencyChildren;
    }

    /**
     * Sets the dependency children.
     *
     * @param dependencyChildren the dependency children
     */
    public void setDependencyChildren(List<String> dependencyChildren) {
        this.dependencyChildren = dependencyChildren;
    }

    /**
     * Gets the resolution diagnostics.
     *
     * @return the string result
     */
    public List<String> getResolutionDiagnostics() {
        return resolutionDiagnostics;
    }

    /**
     * Sets the resolution diagnostics.
     *
     * @param resolutionDiagnostics the resolution diagnostics
     */
    public void setResolutionDiagnostics(List<String> resolutionDiagnostics) {
        this.resolutionDiagnostics = resolutionDiagnostics;
    }

    /**
     * Gets the winning strategies.
     *
     * @return the string result
     */
    public List<String> getWinningStrategies() {
        return winningStrategies;
    }

    /**
     * Sets the winning strategies.
     *
     * @param winningStrategies the winning strategies
     */
    public void setWinningStrategies(List<String> winningStrategies) {
        this.winningStrategies = winningStrategies;
    }

    /**
     * Gets the cycle id.
     *
     * @return the string result
     */
    public String getCycleId() {
        return cycleId;
    }

    /**
     * Sets the cycle id.
     *
     * @param cycleId the cycle id
     */
    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    /**
     * Gets the cycle turns.
     *
     * @return the numeric value
     */
    public int getCycleTurns() {
        return cycleTurns;
    }

    /**
     * Sets the cycle turns.
     *
     * @param cycleTurns the cycle turns
     */
    public void setCycleTurns(int cycleTurns) {
        this.cycleTurns = cycleTurns;
    }

    /**
     * Gets the cycle intent.
     *
     * @return the string result
     */
    public String getCycleIntent() {
        return cycleIntent;
    }

    /**
     * Sets the cycle intent.
     *
     * @param cycleIntent the cycle intent
     */
    public void setCycleIntent(String cycleIntent) {
        this.cycleIntent = cycleIntent;
    }

    /**
     * Gets the cycle user messages.
     *
     * @return the string result
     */
    public List<String> getCycleUserMessages() {
        return cycleUserMessages;
    }

    /**
     * Sets the cycle user messages.
     *
     * @param cycleUserMessages the cycle user messages
     */
    public void setCycleUserMessages(List<String> cycleUserMessages) {
        this.cycleUserMessages = cycleUserMessages;
    }

    /**
     * Gets the cycle tool info phrases.
     *
     * @return the string result
     */
    public List<String> getCycleToolInfoPhrases() {
        return cycleToolInfoPhrases;
    }

    /**
     * Sets the cycle tool info phrases.
     *
     * @param cycleToolInfoPhrases the cycle tool info phrases
     */
    public void setCycleToolInfoPhrases(List<String> cycleToolInfoPhrases) {
        this.cycleToolInfoPhrases = cycleToolInfoPhrases;
    }

    /**
     * Gets the retry attempt count.
     *
     * @return the numeric value
     */
    public int getRetryAttemptCount() {
        return retryAttemptCount;
    }

    /**
     * Sets the retry attempt count.
     *
     * @param retryAttemptCount the retry attempt count
     */
    public void setRetryAttemptCount(int retryAttemptCount) {
        this.retryAttemptCount = retryAttemptCount;
    }

    /**
     * Gets the no usable content count.
     *
     * @return the numeric value
     */
    public int getNoUsableContentCount() {
        return noUsableContentCount;
    }

    /**
     * Sets the no usable content count.
     *
     * @param noUsableContentCount the no usable content count
     */
    public void setNoUsableContentCount(int noUsableContentCount) {
        this.noUsableContentCount = noUsableContentCount;
    }

    /**
     * Gets the processing state.
     *
     * @return the string result
     */
    public String getProcessingState() {
        return processingState;
    }

    /**
     * Sets the processing state.
     *
     * @param processingState the processing state
     */
    public void setProcessingState(String processingState) {
        this.processingState = processingState;
    }

    /**
     * Gets the transition causes.
     *
     * @return the string result
     */
    public List<String> getTransitionCauses() {
        return transitionCauses;
    }

    /**
     * Sets the transition causes.
     *
     * @param transitionCauses the transition causes
     */
    public void setTransitionCauses(List<String> transitionCauses) {
        this.transitionCauses = transitionCauses;
    }
}
