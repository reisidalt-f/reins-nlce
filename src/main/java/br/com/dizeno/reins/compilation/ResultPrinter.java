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

import org.apache.maven.plugin.logging.Log;
import br.com.dizeno.reins.util.LogSanitizer;
import br.com.dizeno.reins.util.PathLogFormatter;

import java.nio.file.Path;
import java.util.StringJoiner;

/**
 * ResultPrinter is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a helper utility for printing or formatting its prefix output.
 */
public class ResultPrinter {
    /**
     * Print.
     *
     * @param log the logger instance
     * @param output the output
     * @param projectRoot the project root path
     */
    public void print(Log log, CompilationOutput output, Path projectRoot) {
        String outputPaths = output.getOutputPaths().isEmpty()
                ? output.getOutputPath()
                : String.join(",", output.getOutputPaths());
        String outputCategories = output.getOutputCategories().isEmpty()
                ? ""
                : String.join(",", output.getOutputCategories());
        String dependencyChildren = output.getDependencyChildren().isEmpty()
                ? ""
                : String.join(",", output.getDependencyChildren());
        String cycleMetadata = compactCycleMetadata(output);
        String cycleUserMessages = output.getCycleUserMessages().isEmpty()
                ? ""
                : String.join(" || ", output.getCycleUserMessages());
        String winningStrategies = output.getWinningStrategies().isEmpty()
            ? ""
            : String.join(",", output.getWinningStrategies());
                String retrySummary = output.getRetryAttemptCount() > 0
                                ? "attempts=" + output.getRetryAttemptCount()
                                    + ",noUsableContent=" + output.getNoUsableContentCount()
                                : "";
        String resolutionDiagnostics = output.getResolutionDiagnostics().isEmpty()
            ? ""
            : String.join(" | ", output.getResolutionDiagnostics());
        String transitionCauses = output.getTransitionCauses().isEmpty()
            ? ""
            : String.join(",", output.getTransitionCauses());
        StringJoiner diagnostics = new StringJoiner(" | ");
        for (String diagnostic : output.getDiagnostics()) {
            diagnostics.add(diagnostic);
        }
        
        String formattedSource = PathLogFormatter.formatPath(output.getSourcePath(), projectRoot);
        String formattedOutputs = PathLogFormatter.formatPath(outputPaths, projectRoot);
        String formattedChildren = PathLogFormatter.formatPath(dependencyChildren, projectRoot);

        String message = String.format("[%s] source=%s category=%s policy=%s output=%s order=%d/%d durationMs=%d message=%s",
                output.getStatus(),
                formattedSource,
                output.getSourceCategory(),
                output.getOutputPolicy(),
                formattedOutputs,
                output.getProcessingIndex(),
                output.getProcessingTotal(),
                output.getDurationMs(),
                compactMessage(output.getMessage(), output.getReprocessingReason(), outputCategories,
                    formattedChildren, cycleMetadata, cycleUserMessages, winningStrategies, retrySummary,
                    resolutionDiagnostics, diagnostics.toString(), output.getProcessingState(), transitionCauses));
        if ("failed".equalsIgnoreCase(output.getStatus())) {
            log.error(message);
        } else {
            log.info(message);
        }
    }

    private String compactMessage(String baseMessage,
                                  String reprocessingReason,
                                  String outputCategories,
                                  String dependencyChildren,
                                  String cycleMetadata,
                                  String cycleUserMessages,
                                  String winningStrategies,
                                  String retrySummary,
                                  String resolutionDiagnostics,
                                  String diagnostics,
                                  String processingState,
                                  String transitionCauses) {
        StringJoiner joiner = new StringJoiner("; ");
        addIfNotBlank(joiner, null, baseMessage);
        addIfNotBlank(joiner, "reason", reprocessingReason);
        addIfNotBlank(joiner, "processingState", processingState);
        addIfNotBlank(joiner, "transitionCauses", transitionCauses);
        addIfNotBlank(joiner, "fileCategories", outputCategories);
        addIfNotBlank(joiner, "children", dependencyChildren);
        addIfNotBlank(joiner, null, cycleMetadata);
        addIfNotBlank(joiner, "userMessages", cycleUserMessages == null ? null : LogSanitizer.sanitizeForLog(cycleUserMessages));
        addIfNotBlank(joiner, "winnerStrategies", winningStrategies);
        addIfNotBlank(joiner, "retrySummary", retrySummary);
        addIfNotBlank(joiner, "resolver", resolutionDiagnostics == null ? null : LogSanitizer.sanitizeForLog(resolutionDiagnostics));
        addIfNotBlank(joiner, "diagnostics", diagnostics == null ? null : LogSanitizer.sanitizeForLog(diagnostics));
        return joiner.toString();
    }

    private void addIfNotBlank(StringJoiner joiner, String key, String value) {
        if (value != null && !value.isBlank()) {
            joiner.add(key == null ? value : key + "=" + value);
        }
    }

    private String compactCycleMetadata(CompilationOutput output) {
        if (output.getCycleId() == null || output.getCycleId().isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("cycleId=").append(output.getCycleId());
        if (output.getCycleTurns() > 0) {
            sb.append(",cycleTurns=").append(output.getCycleTurns());
        }
        if (output.getCycleIntent() != null && !output.getCycleIntent().isBlank()) {
            sb.append(",cycleIntent=").append(output.getCycleIntent());
        }
        return sb.toString();
    }
}
