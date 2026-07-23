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
 * ConversationMessage is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing conversation message.
 */
public class ConversationMessage {
    /**
     * Role is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing role.
     */
    public enum Role {
        SYSTEM, USER, MODEL
    }

    private final Role role;
    private final String text;
    private final List<AttachedFilePayload> attachments;

    /**
     * Constructs a new instance of {@link ConversationMessage}.
     *
     * @param role the role
     * @param text the text
     */
    public ConversationMessage(Role role, String text) {
        this(role, text, List.of());
    }

    /**
     * Constructs a new instance of {@link ConversationMessage}.
     *
     * @param role the role
     * @param text the text
     * @param attachments the list of attachments
     */
    public ConversationMessage(Role role, String text, List<AttachedFilePayload> attachments) {
        this.role = role;
        this.text = text;
        this.attachments = attachments == null ? List.of() : new ArrayList<>(attachments);
    }

    /**
     * Gets the role.
     *
     * @return the resolved or constructed object
     */
    public Role getRole() {
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

    /**
     * Gets the role string.
     *
     * @return the string result
     */
    public String getRoleString() {
        if (role == Role.SYSTEM) {
            return "system";
        }
        return role == Role.USER ? "user" : "model";
    }
}
