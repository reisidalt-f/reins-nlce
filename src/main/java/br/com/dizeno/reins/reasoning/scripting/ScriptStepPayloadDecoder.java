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

package br.com.dizeno.reins.reasoning.scripting;

import java.util.ArrayList;
import java.util.List;

 
/**
 * ScriptStepPayloadDecoder is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing script step payload decoder.
 */
public class ScriptStepPayloadDecoder {

    /**
     * Decode.
     *
     * @param stepName the step name
     * @param payload the message payload text
     * @return the resulting result
     */
    public ScriptStepResult decode(String stepName, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Script step '" + stepName + "' produced empty payload.");
        }

        String[] lines = payload.split("\\R", -1);
        int idx = 0;
        while (idx < lines.length && lines[idx].isBlank()) {
            idx++;
        }
        if (idx >= lines.length) {
            throw new IllegalStateException("Script step '" + stepName + "' payload has no role line.");
        }

        ConversationMessage.Role role = parseRole(lines[idx].trim(), stepName);
        idx++;

        if (idx >= lines.length || !lines[idx].isBlank()) {
            throw new IllegalStateException("Script step '" + stepName + "' payload must contain a blank separator line after role.");
        }
        idx++;

        List<AttachedFilePayload> attachments = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        for (; idx < lines.length; idx++) {
            String line = lines[idx];
            if (line.startsWith("ATTACH ")) {
                AttachedFilePayload payloadAttachment = decodeAttachmentLine(line.substring("ATTACH ".length()).trim());
                if (payloadAttachment != null) {
                    attachments.add(payloadAttachment);
                }
                continue;
            }
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(line);
        }

        return new ScriptStepResult(stepName, role, body.toString().strip(), attachments);
    }

    private ConversationMessage.Role parseRole(String roleName, String stepName) {
        try {
            return ConversationMessage.Role.valueOf(roleName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Script step '" + stepName + "' produced invalid role '" + roleName + "'.", ex);
        }
    }

    private AttachedFilePayload decodeAttachmentLine(String rawValue) {
        if (rawValue.isBlank()) {
            return null;
        }
        String[] parts = rawValue.split(":", 2);
        AttachedFilePayload attachment = new AttachedFilePayload();
        if (parts.length == 2) {
            attachment.setBase(parts[0].trim());
            attachment.setRelativePath(parts[1].trim());
            attachment.setQualifiedPath(parts[0].trim() + ":" + parts[1].trim());
        } else {
            attachment.setQualifiedPath(rawValue);
        }
        return attachment;
    }
}
