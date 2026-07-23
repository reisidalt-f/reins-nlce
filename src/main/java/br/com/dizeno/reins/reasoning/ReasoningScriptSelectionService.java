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
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluationService;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReasoningScriptSelectionService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ReasoningScriptSelectionService {
    private final ScriptEvaluationService scriptEvaluationService;
    private final MessageFormattingService messageFormattingService;

    /**
     * Constructs a new instance of {@link ReasoningScriptSelectionService}.
     *
     * @param scriptEvaluationService the script evaluation service
     * @param messageFormattingService the message formatting service
     */
    public ReasoningScriptSelectionService(ScriptEvaluationService scriptEvaluationService,
                                           MessageFormattingService messageFormattingService) {
        this.scriptEvaluationService = scriptEvaluationService;
        this.messageFormattingService = messageFormattingService;
    }

    /**
     * Apply Attachment List Script.
     *
     * @param attachments the list of attachments
     * @param config the Reins configuration settings
     * @return the collection of elements
     */
    public List<AttachedFilePayload> applyAttachmentListScript(List<AttachedFilePayload> attachments,
                                                               ReinsConfig config) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        ReasoningScriptContext context = scriptEvaluationService.buildScriptBaseContext(
                null,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                List.of(),
                attachments,
                false);
        String rendered = scriptEvaluationService.evaluateScriptSafely(
                "attachment-list.ftl",
                context,
                config,
                "attachment-list");
        List<String> selectedPaths = messageFormattingService.parsePathList(rendered);
        if (selectedPaths.isEmpty()) {
            return List.of();
        }

        Map<String, AttachedFilePayload> byPath = new LinkedHashMap<>();
        for (AttachedFilePayload attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            if (attachment.getQualifiedPath() != null && !attachment.getQualifiedPath().isBlank()) {
                byPath.putIfAbsent(attachment.getQualifiedPath(), attachment);
            }
            if (attachment.getRelativePath() != null && !attachment.getRelativePath().isBlank()) {
                byPath.putIfAbsent(attachment.getRelativePath(), attachment);
            }
            String base = attachment.getBase() == null ? "main" : attachment.getBase();
            String rel = attachment.getRelativePath() == null ? "" : attachment.getRelativePath();
            String canonical = base + ":" + rel;
            if (!canonical.equals(":")) {
                byPath.putIfAbsent(canonical, attachment);
            }
        }

        List<AttachedFilePayload> filtered = new ArrayList<>();
        for (String selectedPath : selectedPaths) {
            AttachedFilePayload match = byPath.get(selectedPath);
            if (match != null) {
                filtered.add(match);
            }
        }
        return filtered;
    }

    /**
     * Apply File List Script.
     *
     * @param compiledPaths the compiled paths
     * @param config the Reins configuration settings
     * @return the string result
     */
    public List<String> applyFileListScript(List<String> compiledPaths,
                                            ReinsConfig config) {
        List<String> current = compiledPaths == null ? List.of() : compiledPaths;
        ReasoningScriptContext context = scriptEvaluationService.buildScriptBaseContext(
                null,
                null,
                null,
                null,
                false,
                null,
                List.of(),
                current,
                List.of(),
                false);
        String rendered = scriptEvaluationService.evaluateScriptSafely("file-list.ftl", context, config, "file-list");
        List<String> parsed = messageFormattingService.parsePathList(rendered);
        return parsed.isEmpty() ? List.of() : parsed;
    }
}
