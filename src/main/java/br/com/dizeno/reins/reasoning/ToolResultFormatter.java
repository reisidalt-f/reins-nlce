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
     * Format a single result into a --reins-boundary multipart document.
     *
     * @param result the tool execution result
     * @return formatted multipart result string
     */
    public String format(ToolExecutionResult result) {
        if (result == null) {
            return "--reins-boundary\nEMPTY\n--reins-boundary--\n";
        }
        return formatBatch(List.of(result));
    }

    /**
     * Format a list of results into a unified --reins-boundary multipart document.
     *
     * @param results list of execution results
     * @return formatted multipart batch result string
     */
    public String formatBatch(List<ToolExecutionResult> results) {
        StringBuilder sb = new StringBuilder();
        if (results == null || results.isEmpty()) {
            sb.append("--reins-boundary\nEMPTY\n--reins-boundary--\n");
            return sb.toString();
        }

        for (ToolExecutionResult result : results) {
            sb.append("--reins-boundary\n");
            formatSingleResultBody(sb, result);
        }
        sb.append("--reins-boundary--\n");
        return sb.toString();
    }

    private void formatSingleResultBody(StringBuilder sb, ToolExecutionResult result) {
        sb.append("status: ").append(result.getStatus()).append("\n");
        if (result.getOperation() != null) {
            sb.append("operation: ").append(result.getOperation()).append("\n");
        }
        if (result.getQualifiedPath() != null) {
            sb.append("path: ").append(result.getQualifiedPath()).append("\n");
        }
        if (result.getExitCode() != null) {
            sb.append("exit_code: ").append(result.getExitCode()).append("\n");
        }
        if (result.getFailureReason() != null && !result.getFailureReason().isBlank()) {
            sb.append("failure: ").append(result.getFailureReason()).append("\n");
        }
        if (result.getPolicyCode() != null && !result.getPolicyCode().isBlank()) {
            sb.append("policy_code: ").append(result.getPolicyCode()).append("\n");
        }

        boolean suppressReadContent = result.getOperation() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE
                && result.getStatus() == ToolExecutionResult.Status.SUCCESS;
        if (suppressReadContent) {
            sb.append("content: [attached]\n");
        } else if (result.getContent() != null && !result.getContent().isBlank()) {
            sb.append("content:\n").append(result.getContent()).append("\n");
        }

        if (result.getListedPaths() != null && !result.getListedPaths().isEmpty()) {
            sb.append("listed:\n");
            for (String listedPath : result.getListedPaths()) {
                sb.append("- ").append(listedPath).append("\n");
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
        if (result.getStdout() != null && !result.getStdout().isBlank()) {
            sb.append("stdout:\n").append(result.getStdout()).append("\n");
        }
        if (result.getStderr() != null && !result.getStderr().isBlank()) {
            sb.append("stderr:\n").append(result.getStderr()).append("\n");
        }
    }
}
