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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;

import java.util.List;

/**
 * ToolResultFormatter is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing tool result formatter.
 */
public class ToolResultFormatter {
    public static final String WRITE_SCOPE_POLICY_REASON =
        "Write operations are allowed only under the configured target base.";
    public static final String LIST_EXCLUSION_REASON =
        "Compiled files outside active output base scope are excluded.";

    /**
     * Format.
     *
     * @param result the result
     * @return the string result
     */
    public String format(ToolExecutionResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("TOOL_RESULT\n");
        sb.append("status: ").append(result.getStatus()).append("\n");
        if (result.getOperation() != null) {
            sb.append("operation: ").append(result.getOperation()).append("\n");
        }
        if (result.getQualifiedPath() != null) {
            sb.append("path: ").append(result.getQualifiedPath()).append("\n");
        }
        if (result.getResolvedBase() != null && !result.getResolvedBase().isBlank()) {
            sb.append("resolved_base: ").append(result.getResolvedBase()).append("\n");
        }
        if (result.getFailureReason() != null && !result.getFailureReason().isBlank()) {
            sb.append("failure: ").append(result.getFailureReason()).append("\n");
        }
        if (result.getPolicyCode() != null && !result.getPolicyCode().isBlank()) {
            sb.append("policy_code: ").append(result.getPolicyCode()).append("\n");
        }
        if (result.getResolvedPath() != null && !result.getResolvedPath().isBlank()) {
            sb.append("resolved_path: ").append(result.getResolvedPath()).append("\n");
        }
        if (result.getExitCode() != null) {
            sb.append("exit_code: ").append(result.getExitCode()).append("\n");
        }
        sb.append("started: ").append(result.isStarted()).append("\n");
        if (result.isTruncated()) {
            sb.append("truncated: true\n");
        }
        if (result.getStdout() != null) {
            sb.append("stdout:\n").append(result.getStdout()).append("\n");
        }
        if (result.getStderr() != null) {
            sb.append("stderr:\n").append(result.getStderr()).append("\n");
        }
        boolean suppressReadContent = result.getOperation() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE
                && result.getStatus() == ToolExecutionResult.Status.SUCCESS;
        if (!suppressReadContent && result.getContent() != null && !result.getContent().isBlank()) {
            sb.append("content:\n").append(result.getContent()).append("\n");
        }
        if (suppressReadContent) {
            sb.append("content: [attached]\n");
        }
        if (result.getListedPaths() != null && !result.getListedPaths().isEmpty()) {
            sb.append("listed:\n");
            for (String listedPath : result.getListedPaths()) {
                sb.append("- ").append(listedPath).append("\n");
            }
        }
        if (result.getCompiledFileStatuses() != null && !result.getCompiledFileStatuses().isEmpty()) {
            sb.append("compiled_files:\n");
            for (ToolExecutionResult.CompiledFileStatus status : result.getCompiledFileStatuses()) {
                sb.append("- path: ").append(status.getQualifiedPath()).append("\n");
                sb.append("  attach_status: ").append(status.getAttachStatus()).append("\n");
                if (status.getReason() != null && !status.getReason().isBlank()) {
                    sb.append("  reason: ").append(status.getReason()).append("\n");
                }
            }
        }
        if (result.getReadFileStatuses() != null && !result.getReadFileStatuses().isEmpty()) {
            sb.append("read_files:\n");
            for (ToolExecutionResult.ReadFileStatus status : result.getReadFileStatuses()) {
                sb.append("- path: ").append(status.getQualifiedPath()).append("\n");
                sb.append("  attach_status: ").append(status.getAttachStatus()).append("\n");
                if (status.getReason() != null && !status.getReason().isBlank()) {
                    sb.append("  reason: ").append(status.getReason()).append("\n");
                }
            }
        }
        if (result.getExcludedPaths() != null && !result.getExcludedPaths().isEmpty()) {
            sb.append("excluded_paths:\n");
            for (String excludedPath : result.getExcludedPaths()) {
                sb.append("- ").append(excludedPath).append("\n");
            }
        }
        if (result.getExclusionReason() != null && !result.getExclusionReason().isBlank()) {
            sb.append("exclusion_reason: ").append(result.getExclusionReason()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format Batch.
     *
     * @param results the results
     * @return the string result
     */
    public String formatBatch(List<ToolExecutionResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("TOOL_BATCH_RESULT\n");
        sb.append("count: ").append(results == null ? 0 : results.size()).append("\n");
        if (results == null || results.isEmpty()) {
            return sb.toString();
        }
        for (int i = 0; i < results.size(); i++) {
            sb.append("\n--- result ").append(i + 1).append(" ---\n");
            sb.append(format(results.get(i)));
        }
        return sb.toString();
    }

}
