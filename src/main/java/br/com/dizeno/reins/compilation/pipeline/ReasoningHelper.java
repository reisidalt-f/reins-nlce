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
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ReasoningHelper is part of the sequential execution of compilation phases
 * (reading, tracking, LLM reasoning, writing, and printing) in the reins
 * architecture.
 * Acts as a helper utility assisting in its prefix tasks.
 */
public final class ReasoningHelper {

    private ReasoningHelper() {
    }

    /**
     * Builds the configured target reasoning message.
     *
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param processingStatus    the processing status
     * @return the string result
     */
    public static String buildReasoningMessage(String canonicalSourcePath,
            SourceProcessingStatus processingStatus) {
        if (processingStatus == SourceProcessingStatus.VALIDATE) {
            return "Revalidate the files previously compiled from the main source markdown "
                    + canonicalSourcePath
                    + " and the files it references. Apply only the targeted fixes needed to keep compiled outputs consistent with referenced-source changes. Do not perform a full recompilation when patching is sufficient.";
        }
        return "Build the files necessary to implement the design found in the main source markdown "
                + canonicalSourcePath
                + " and the files it references.";
    }

    /**
     * Builds the configured target reasoning request for.
     *
     * @param canonicalSourcePath          the canonicalized path of the source file
     * @param sourceCategory               the category of the source file (e.g.
     *                                     main or test)
     * @param sourceHash                   the source hash
     * @param config                       the Reins configuration settings
     * @param projectRoot                  the root path of the project
     * @param compilationBackgroundPayload the compilation background payload
     * @param log                          the logger instance
     * @param processingStatus             the processing status
     * @return the resolved or constructed object
     */
    public static ReasoningRequest buildReasoningRequestFor(Path sourcePath,
            String canonicalSourcePath,
            String sourceCategory,
            String sourceHash,
            ReinsConfig config,
            Path projectRoot,
            CompilationBackgroundPayload compilationBackgroundPayload,
            Log log,
            SourceProcessingStatus processingStatus) {
        ReasoningRequest request = new ReasoningRequest();
        String qualifiedPath = sourcePath != null
                ? PathHelper.formatBaseRelativePath(sourcePath, config, projectRoot)
                : PathHelper.resolveMainSourceQualifiedPath(canonicalSourcePath, sourceCategory, config, projectRoot);

        request.setSourcePath(canonicalSourcePath);
        request.setSourceScope(sourceCategory);
        request.setSourceHash(sourceHash);
        request.setMessage(buildReasoningMessage(qualifiedPath, processingStatus));
        request.setProcessingStatus(processingStatus);
        request.setProjectRoot(projectRoot);
        request.setBaseMappings(BasePathMappingSet.forScope(config, projectRoot, sourceCategory));
        request.setMainSourceQualifiedPath(qualifiedPath);
        request.setModelConfigSnapshot(config != null ? config.resolveActiveModelSettings() : null);
        request.setAttachments(List.of());
        request.setUserMessageListener(log::info);
        request.setOperationLogger(phrase -> {
            if (shouldLogToolPhrase(config, phrase)) {
                log.info(phrase);
            }
        });
        request.setCompilationBackgroundPayload(
                compilationBackgroundPayload == null
                        ? CompilationBackgroundPayload.empty()
                        : compilationBackgroundPayload);
        return request;
    }

    public static ReasoningRequest buildReasoningRequestFor(String canonicalSourcePath,
            String sourceCategory,
            String sourceHash,
            ReinsConfig config,
            Path projectRoot,
            CompilationBackgroundPayload compilationBackgroundPayload,
            Log log,
            SourceProcessingStatus processingStatus) {
        return buildReasoningRequestFor(null, canonicalSourcePath, sourceCategory, sourceHash, config, projectRoot, compilationBackgroundPayload, log, processingStatus);
    }

    /**
     * Should Log Tool Phrase.
     *
     * @param config the Reins configuration settings
     * @param phrase the phrase
     * @return true if successful or matching, false otherwise
     */
    public static boolean shouldLogToolPhrase(ReinsConfig config, String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return false;
        }
        LoggingSettings logging = config == null ? null : config.getLogging();
        if (logging == null) {
            return false;
        }

        String normalized = phrase.trim();
        if (normalized.startsWith("Read of file ")
                || normalized.startsWith("List of files ")
                || normalized.startsWith("List of compiled files ")) {
            return logging.isFileListingAndReading();
        }
        if (normalized.startsWith("Write of file ")
                || normalized.startsWith("Patch of file ")
                || normalized.startsWith("Delete of file ")) {
            return logging.isFileMutating();
        }
        if (normalized.startsWith("Run of script ")) {
            return logging.isScriptRun();
        }
        return false;
    }

    /**
     * Apply Reasoning Output Fields.
     *
     * @param output the output
     * @param result the result
     */
    public static void applyReasoningOutputFields(CompilationOutput output, ReasoningResult result) {
        output.setCycleId(result.getCycleId());
        output.setCycleTurns(result.getTurnCount());
        output.setCycleIntent(result.getFinalIntent());
        output.setCycleUserMessages(toPluginOutputMessages(result));
        output.setCycleToolInfoPhrases(result.getToolInfoPhrases());
    }

    /**
     * To Plugin Output Messages.
     *
     * @param reasoningResult the reasoning result
     * @return the string result
     */
    public static List<String> toPluginOutputMessages(ReasoningResult reasoningResult) {
        if (reasoningResult == null || reasoningResult.getUserFacingMessages() == null) {
            return List.of();
        }
        List<String> userMessages = reasoningResult.getUserFacingMessages();
        if (userMessages.isEmpty()) {
            return List.of();
        }
        String finalIntent = reasoningResult.getFinalIntent();
        boolean hasFinishIntent = "finish_success".equalsIgnoreCase(finalIntent)
                || "finish_error".equalsIgnoreCase(finalIntent);
        if (!hasFinishIntent || userMessages.size() == 1) {
            return new ArrayList<>(userMessages);
        }

        List<String> pluginMessages = new ArrayList<>(userMessages.size());
        for (int i = 0; i < userMessages.size() - 1; i++) {
            pluginMessages.add("Understood.");
        }
        pluginMessages.add(userMessages.get(userMessages.size() - 1));
        return pluginMessages;
    }
}
