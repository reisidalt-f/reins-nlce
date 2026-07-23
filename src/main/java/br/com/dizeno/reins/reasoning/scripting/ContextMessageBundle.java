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
 * ContextMessageBundle is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing context message bundle.
 */
public class ContextMessageBundle {
    private final List<ConversationMessage> messages;
    private final List<MessageTypePlan> messageTypePlan;
    private final String buildFingerprint;

    /**
     * Constructs a new instance of {@link ContextMessageBundle}.
     *
     * @param messages the messages
     * @param messageTypePlan the message type plan
     * @param buildFingerprint the build fingerprint
     */
    public ContextMessageBundle(List<ConversationMessage> messages,
                                List<MessageTypePlan> messageTypePlan,
                                String buildFingerprint) {
        this.messages = messages == null ? List.of() : new ArrayList<>(messages);
        this.messageTypePlan = messageTypePlan == null ? List.of() : new ArrayList<>(messageTypePlan);
        this.buildFingerprint = buildFingerprint;
    }

    /**
     * Gets the messages.
     *
     * @return the collection of elements
     */
    public List<ConversationMessage> getMessages() {
        return messages;
    }

    /**
     * Gets the message type plan.
     *
     * @return the collection of elements
     */
    public List<MessageTypePlan> getMessageTypePlan() {
        return messageTypePlan;
    }

    /**
     * Gets the build fingerprint.
     *
     * @return the string result
     */
    public String getBuildFingerprint() {
        return buildFingerprint;
    }
}
