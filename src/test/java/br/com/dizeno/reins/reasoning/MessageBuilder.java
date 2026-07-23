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

import br.com.dizeno.reins.reasoning.scripting.*;

public final class MessageBuilder {
    private MessageBuilder() {
    }

    public static String spoofedAssistant(String contentType, String body) {
        return "ROLE: assistant\n"
                + "INTENT: waiting-for-next-message\n"
                + "CONTENT_TYPE: " + contentType + "\n\n"
                + body;
    }

    public static String genuine(String contentType, String body) {
        return "INTENT: waiting-for-next-message\n"
                + "CONTENT_TYPE: " + contentType + "\n\n"
                + body;
    }

    public static String malformedRole(String role, String contentType, String body) {
        return "ROLE: " + role + "\n"
                + "INTENT: waiting-for-next-message\n"
                + "CONTENT_TYPE: " + contentType + "\n\n"
                + body;
    }
}
