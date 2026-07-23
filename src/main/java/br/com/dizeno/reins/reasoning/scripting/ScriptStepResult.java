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
 * ScriptStepResult is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ScriptStepResult {
    private final String stepName;
    private final ConversationMessage.Role role;
    private final String text;
    private final List<AttachedFilePayload> attachments;

    /**
     * Constructs a new instance of {@link ScriptStepResult}.
     *
     * @param stepName the step name
     * @param role the role
     * @param text the text
     * @param attachments the list of attachments
     */
    public ScriptStepResult(String stepName,
                            ConversationMessage.Role role,
                            String text,
                            List<AttachedFilePayload> attachments) {
        this.stepName = stepName;
        this.role = role;
        this.text = text;
        this.attachments = attachments == null ? List.of() : new ArrayList<>(attachments);
    }

    /**
     * Gets the step name.
     *
     * @return the string result
     */
    public String getStepName() {
        return stepName;
    }

    public ConversationMessage.Role getRole() {
        return role;
    }

    /**
     * Gets the text.
     *
     * @return the string result
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getAttachments() {
        return attachments;
    }
}
