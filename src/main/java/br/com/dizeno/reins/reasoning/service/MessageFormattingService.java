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

package br.com.dizeno.reins.reasoning.service;

import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MessageFormattingService is part of the extracted reasoning helper services for scripts, attachments, MCP tools, configuration, and state in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class MessageFormattingService {

    /**
     * Append System Context.
     *
     * @param baseContext the base context
     * @param migratedPayload the migrated payload
     * @return the string result
     */
    public String appendSystemContext(String baseContext, String migratedPayload) {
        String base = baseContext == null ? "" : baseContext.trim();
        String payload = migratedPayload == null ? "" : migratedPayload.trim();
        if (payload.isBlank()) {
            return base;
        }
        if (base.isBlank()) {
            return payload;
        }
        return base + "\n\n" + payload;
    }

    /**
     * With Turn Count Note.
     *
     * @param message the message content
     * @param turnIndex the turn index
     * @param maxTurns the maximum turn limit for the reasoning cycle
     * @param enabled the enabled
     * @return the string result
     */
    public String withTurnCountNote(String message,
                                    int turnIndex,
                                    int maxTurns,
                                    boolean enabled) {
        if (!enabled || turnIndex <= 0 || maxTurns <= 0) {
            return message;
        }
        String base = message == null ? "" : message.trim();
        String note = "Turn " + turnIndex + "/" + maxTurns;
        if (turnIndex >= maxTurns) {
            note = note + "\n"
                    + "Respond with a finish-success message requesting the MCP operations needed to complete the task or a finish-error with a message to the user indicating the failure. You should not wait for the results of the MCP operations, the task will be considered complete if all MCP operations succeed.";
        }
        if (base.isBlank()) {
            return note;
        }
        return base + "\n\n" + note;
    }

    /**
     * Join Qualified Paths For Log.
     *
     * @param attachments the list of attachments
     * @return the string result
     */
    public String joinQualifiedPathsForLog(List<AttachedFilePayload> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "<none>";
        }
        return attachments.stream()
                .map(AttachedFilePayload::getQualifiedPath)
                .filter(path -> path != null && !path.isBlank())
                .limit(20)
                .collect(Collectors.joining(", "));
    }

    /**
     * Preview For Log.
     *
     * @param payload the message payload text
     * @return the string result
     */
    public String previewForLog(String payload) {
        if (payload == null || payload.isBlank()) {
            return "<empty>";
        }
        String normalized = payload.replace('\n', ' ').trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240) + "...";
    }

    /**
     * Parse Path List.
     *
     * @param rendered the rendered
     * @return the string result
     */
    public List<String> parsePathList(String rendered) {
        if (rendered == null || rendered.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String line : rendered.split("\\R")) {
            if (line == null) {
                continue;
            }
            String normalized = line.trim();
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return new ArrayList<>(values);
    }

    /**
     * Summarize Prepend Messages.
     *
     * @param messages the messages
     * @return the string result
     */
    public String summarizePrependMessages(List<ConversationMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "prepend-messages: <none>";
        }
        StringBuilder summary = new StringBuilder("prepend-messages: count=")
                .append(messages.size());
        int idx = 1;
        for (ConversationMessage message : messages) {
            summary.append("\n")
                    .append(idx++)
                    .append(") role=")
                    .append(message.getRole())
                    .append(" preview=")
                    .append(previewForLog(message.getText()));
        }
        return summary.toString();
    }

    /**
     * Builds the configured target outbound log body.
     *
     * @param messageContent the message content
     * @param outboundAttachments the outbound attachments
     * @return the string result
     */
    public String buildOutboundLogBody(String messageContent, List<AttachedFilePayload> outboundAttachments) {
        String body = messageContent == null ? "" : messageContent;
        if (outboundAttachments == null || outboundAttachments.isEmpty()) {
            return body;
        }

        String attachmentLines = outboundAttachments.stream()
                .map(this::toAttachmentLogReference)
                .distinct()
                .collect(Collectors.joining("\n"));

        if (attachmentLines.isBlank()) {
            return body;
        }

        if (body.isBlank()) {
            return "attachments:\n" + attachmentLines;
        }
        return body + "\n\nattachments:\n" + attachmentLines;
    }

    /**
     * Canonicalize Context Attachment Path.
     *
     * @param displayPath the display path
     * @return the string result
     */
    public String canonicalizeContextAttachmentPath(String displayPath) {
        if (displayPath == null || displayPath.isBlank()) {
            return "main:unknown";
        }
        String normalized = displayPath.replace('\\', '/');
        if (normalized.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return "test:" + normalized.substring((ProjectDirectoryPaths.TEST_NL_ROOT + "/").length());
        }
        if (normalized.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return "main:" + normalized.substring((ProjectDirectoryPaths.MAIN_NL_ROOT + "/").length());
        }
        return "main:" + normalized;
    }

    /**
     * Safe.
     *
     * @param value the value
     * @return the string result
     */
    public String safe(String value) {
        return value == null ? "" : value;
    }

    private String toAttachmentLogReference(AttachedFilePayload attachment) {
        if (attachment == null) {
            return "- unknown:unknown";
        }
        String base = attachment.getBase() == null || attachment.getBase().isBlank()
                ? "main"
                : attachment.getBase().trim();
        String relativePath = attachment.getRelativePath() == null ? "" : attachment.getRelativePath().trim();
        if (relativePath.isBlank()) {
            return "- " + (attachment.getQualifiedPath() == null || attachment.getQualifiedPath().isBlank()
                    ? base + ":unknown"
                    : attachment.getQualifiedPath().trim());
        }
        return "- " + base + ":" + relativePath;
    }
}
