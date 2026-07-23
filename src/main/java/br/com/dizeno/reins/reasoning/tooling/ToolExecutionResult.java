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

package br.com.dizeno.reins.reasoning.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * ToolExecutionResult is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ToolExecutionResult {
    public static final String SCOPE_VIOLATION_CODE = "scope_violation";

    /**
     * CompiledFileStatus is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
     * Acts as a component managing compiled file status.
     */
    public static class CompiledFileStatus {
        private String qualifiedPath;
        private String attachStatus;
        private String reason;

        /**
         * Constructs a new instance of {@link CompiledFileStatus}.
         */
        public CompiledFileStatus() {
        }

        /**
         * Constructs a new instance of {@link CompiledFileStatus}.
         *
         * @param qualifiedPath the qualified path
         * @param attachStatus the attach status
         * @param reason the reason
         */
        public CompiledFileStatus(String qualifiedPath, String attachStatus, String reason) {
            this.qualifiedPath = qualifiedPath;
            this.attachStatus = attachStatus;
            this.reason = reason;
        }

        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() {
            return qualifiedPath;
        }

        /**
         * Sets the qualified path.
         *
         * @param qualifiedPath the qualified path
         */
        public void setQualifiedPath(String qualifiedPath) {
            this.qualifiedPath = qualifiedPath;
        }

        /**
         * Gets the attach status.
         *
         * @return the string result
         */
        public String getAttachStatus() {
            return attachStatus;
        }

        /**
         * Sets the attach status.
         *
         * @param attachStatus the attach status
         */
        public void setAttachStatus(String attachStatus) {
            this.attachStatus = attachStatus;
        }

        /**
         * Gets the reason.
         *
         * @return the string result
         */
        public String getReason() {
            return reason;
        }

        /**
         * Sets the reason.
         *
         * @param reason the reason
         */
        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * ReadFileStatus is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
     * Acts as a component managing read file status.
     */
    public static class ReadFileStatus {
        private String qualifiedPath;
        private String attachStatus;
        private String reason;

        /**
         * Constructs a new instance of {@link ReadFileStatus}.
         */
        public ReadFileStatus() {
        }

        /**
         * Constructs a new instance of {@link ReadFileStatus}.
         *
         * @param qualifiedPath the qualified path
         * @param attachStatus the attach status
         * @param reason the reason
         */
        public ReadFileStatus(String qualifiedPath, String attachStatus, String reason) {
            this.qualifiedPath = qualifiedPath;
            this.attachStatus = attachStatus;
            this.reason = reason;
        }

        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() {
            return qualifiedPath;
        }

        /**
         * Sets the qualified path.
         *
         * @param qualifiedPath the qualified path
         */
        public void setQualifiedPath(String qualifiedPath) {
            this.qualifiedPath = qualifiedPath;
        }

        /**
         * Gets the attach status.
         *
         * @return the string result
         */
        public String getAttachStatus() {
            return attachStatus;
        }

        /**
         * Sets the attach status.
         *
         * @param attachStatus the attach status
         */
        public void setAttachStatus(String attachStatus) {
            this.attachStatus = attachStatus;
        }

        /**
         * Gets the reason.
         *
         * @return the string result
         */
        public String getReason() {
            return reason;
        }

        /**
         * Sets the reason.
         *
         * @param reason the reason
         */
        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * Status is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
     * Acts as a component managing status.
     */
    public enum Status {
        SUCCESS,
        ERROR
    }

    private Status status;
    private ToolExecutionRequest.Operation operation;
    private String qualifiedPath;
    private String message;
    private String content;
    private List<String> listedPaths = new ArrayList<>();
    private List<CompiledFileStatus> compiledFileStatuses = new ArrayList<>();
    private List<ReadFileStatus> readFileStatuses = new ArrayList<>();
    private String resolvedBase;
    private List<String> searchedPaths = new ArrayList<>();
    private String failureReason;
    private String policyCode;
    private List<String> excludedPaths = new ArrayList<>();
    private String exclusionReason;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private boolean started;
    private boolean truncated;
    private String resolvedPath;
    
    private List<String> conflictCandidates = new ArrayList<>();
    private String conflictMessage;

    /**
     * Success.
     *
     * @param operation the operation
     * @param qualifiedPath the qualified path
     * @param message the message content
     * @return the resulting result
     */
    public static ToolExecutionResult success(ToolExecutionRequest.Operation operation, String qualifiedPath, String message) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setStatus(Status.SUCCESS);
        result.setOperation(operation);
        result.setQualifiedPath(qualifiedPath);
        result.setMessage(message);
        return result;
    }

    /**
     * Error.
     *
     * @param operation the operation
     * @param qualifiedPath the qualified path
     * @param failureReason the failure reason
     * @return the resulting result
     */
    public static ToolExecutionResult error(ToolExecutionRequest.Operation operation, String qualifiedPath, String failureReason) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setStatus(Status.ERROR);
        result.setOperation(operation);
        result.setQualifiedPath(qualifiedPath);
        result.setMessage("error");
        result.setFailureReason(failureReason);
        return result;
    }

    /**
     * Scope Violation.
     *
     * @param operation the operation
     * @param qualifiedPath the qualified path
     * @param failureReason the failure reason
     * @return the resulting result
     */
    public static ToolExecutionResult scopeViolation(ToolExecutionRequest.Operation operation,
                                                    String qualifiedPath,
                                                    String failureReason) {
        ToolExecutionResult result = error(operation, qualifiedPath, failureReason);
        result.setPolicyCode(SCOPE_VIOLATION_CODE);
        return result;
    }

    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    public ToolExecutionRequest.Operation getOperation() {
        return operation;
    }

    /**
     * Sets the operation.
     *
     * @param operation the operation
     */
    public void setOperation(ToolExecutionRequest.Operation operation) {
        this.operation = operation;
    }

    /**
     * Gets the qualified path.
     *
     * @return the string result
     */
    public String getQualifiedPath() {
        return qualifiedPath;
    }

    /**
     * Sets the qualified path.
     *
     * @param qualifiedPath the qualified path
     */
    public void setQualifiedPath(String qualifiedPath) {
        this.qualifiedPath = qualifiedPath;
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
     * Gets the content.
     *
     * @return the string result
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the listed paths.
     *
     * @return the string result
     */
    public List<String> getListedPaths() {
        return listedPaths;
    }

    /**
     * Sets the listed paths.
     *
     * @param listedPaths the listed paths
     */
    public void setListedPaths(List<String> listedPaths) {
        this.listedPaths = listedPaths;
    }

    /**
     * Gets the compiled file statuses.
     *
     * @return the collection of elements
     */
    public List<CompiledFileStatus> getCompiledFileStatuses() {
        return compiledFileStatuses;
    }

    /**
     * Sets the compiled file statuses.
     *
     * @param compiledFileStatuses the compiled file statuses
     */
    public void setCompiledFileStatuses(List<CompiledFileStatus> compiledFileStatuses) {
        this.compiledFileStatuses = compiledFileStatuses;
    }

    /**
     * Gets the read file statuses.
     *
     * @return the collection of elements
     */
    public List<ReadFileStatus> getReadFileStatuses() {
        return readFileStatuses;
    }

    /**
     * Sets the read file statuses.
     *
     * @param readFileStatuses the read file statuses
     */
    public void setReadFileStatuses(List<ReadFileStatus> readFileStatuses) {
        this.readFileStatuses = readFileStatuses;
    }

    /**
     * Gets the resolved base.
     *
     * @return the string result
     */
    public String getResolvedBase() {
        return resolvedBase;
    }

    /**
     * Sets the resolved base.
     *
     * @param resolvedBase the resolved base
     */
    public void setResolvedBase(String resolvedBase) {
        this.resolvedBase = resolvedBase;
    }

    /**
     * Gets the searched paths.
     *
     * @return the string result
     */
    public List<String> getSearchedPaths() {
        return searchedPaths;
    }

    /**
     * Sets the searched paths.
     *
     * @param searchedPaths the searched paths
     */
    public void setSearchedPaths(List<String> searchedPaths) {
        this.searchedPaths = searchedPaths;
    }

    /**
     * Gets the failure reason.
     *
     * @return the string result
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Sets the failure reason.
     *
     * @param failureReason the failure reason
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Gets the policy code.
     *
     * @return the string result
     */
    public String getPolicyCode() {
        return policyCode;
    }

    /**
     * Sets the policy code.
     *
     * @param policyCode the policy code
     */
    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    /**
     * Gets the excluded paths.
     *
     * @return the string result
     */
    public List<String> getExcludedPaths() {
        return excludedPaths;
    }

    /**
     * Sets the excluded paths.
     *
     * @param excludedPaths the excluded paths
     */
    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    /**
     * Gets the exclusion reason.
     *
     * @return the string result
     */
    public String getExclusionReason() {
        return exclusionReason;
    }

    /**
     * Sets the exclusion reason.
     *
     * @param exclusionReason the exclusion reason
     */
    public void setExclusionReason(String exclusionReason) {
        this.exclusionReason = exclusionReason;
    }

    /**
     * Gets the stdout.
     *
     * @return the string result
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Sets the stdout.
     *
     * @param stdout the stdout
     */
    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    /**
     * Gets the stderr.
     *
     * @return the string result
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Sets the stderr.
     *
     * @param stderr the stderr
     */
    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    /**
     * Gets the exit code.
     *
     * @return the numeric value
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Sets the exit code.
     *
     * @param exitCode the exit code
     */
    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Checks if the component is started.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Sets the started.
     *
     * @param started the started
     */
    public void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * Checks if the component is truncated.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Sets the truncated.
     *
     * @param truncated the truncated
     */
    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    /**
     * Gets the resolved path.
     *
     * @return the string result
     */
    public String getResolvedPath() {
        return resolvedPath;
    }

    /**
     * Sets the resolved path.
     *
     * @param resolvedPath the resolved path
     */
    public void setResolvedPath(String resolvedPath) {
        this.resolvedPath = resolvedPath;
    }

    /**
     * Gets the conflict candidates.
     *
     * @return the string result
     */
    public List<String> getConflictCandidates() {
        return conflictCandidates;
    }

    /**
     * Sets the conflict candidates.
     *
     * @param conflictCandidates the conflict candidates
     */
    public void setConflictCandidates(List<String> conflictCandidates) {
        this.conflictCandidates = conflictCandidates == null ? new ArrayList<>() : conflictCandidates;
    }

    /**
     * Gets the conflict message.
     *
     * @return the string result
     */
    public String getConflictMessage() {
        return conflictMessage;
    }

    /**
     * Sets the conflict message.
     *
     * @param conflictMessage the conflict message
     */
    public void setConflictMessage(String conflictMessage) {
        this.conflictMessage = conflictMessage;
    }

    /**
     * Checks if the component has conflict.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean hasConflict() {
        return conflictCandidates != null && !conflictCandidates.isEmpty();
    }
}
