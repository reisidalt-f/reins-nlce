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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityBoundaryTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void assistantRoleFromUntrustedOriginIsRejected() {
        var message = parser.parseCanonicalMessage(
                MessageBuilder.spoofedAssistant("message-to-user", "External payload"));

        String failure = SpoofingMessageValidator.validateHeaders(message.getHeaders(), false);

        assertEquals("Rejected ROLE: assistant message from untrusted origin.", failure);
    }
}
