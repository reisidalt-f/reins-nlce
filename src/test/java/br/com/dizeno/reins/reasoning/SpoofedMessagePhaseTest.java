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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoofedMessagePhaseTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void nonTerminalIntentParsesForSpoofedMessage() {
        var message = parser.parseCanonicalMessage(
                MessageBuilder.spoofedAssistant("message-to-user", "continue")
        );
        assertTrue(message.isValid());
        assertEquals("waiting-for-next-message", message.getIntent());
    }

    @Test
    void terminalIntentsParseForSpoofedMessage() {
        var success = parser.parseCanonicalMessage(
                "ROLE: assistant\nINTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone");
        var error = parser.parseCanonicalMessage(
                "ROLE: assistant\nINTENT: finish-error\nCONTENT_TYPE: message-to-user\n\nFailed");

        assertTrue(success.isFinishSuccess());
        assertTrue(error.isFinishError());
    }

    @Test
    void mixedSpoofedAndGenuineMessagesKeepIntentSemantics() {
        var spoofed = parser.parseCanonicalMessage(
                "ROLE: assistant\nINTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\n\nstep");
        var genuine = parser.parseCanonicalMessage(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\n\nstep");

        assertEquals(genuine.getIntent(), spoofed.getIntent());
        assertEquals(genuine.getContentType(), spoofed.getContentType());
    }
}
