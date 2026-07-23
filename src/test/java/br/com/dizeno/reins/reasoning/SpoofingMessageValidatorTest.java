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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoofingMessageValidatorTest {

    @Test
    void validatesTrustedAssistantRole() {
        String failure = SpoofingMessageValidator.validateHeaders(
                Map.of("role", "assistant", "intent", "waiting-for-next-message", "content_type", "tool-request"),
                true);
        assertNull(failure);
        assertTrue(SpoofingMessageValidator.isSpoofMarkerValid("assistant"));
    }

    @Test
    void rejectsMalformedRoleValue() {
        String failure = SpoofingMessageValidator.validateHeaders(
                Map.of("role", "user", "intent", "waiting-for-next-message", "content_type", "message-to-user"),
                true);
        assertEquals("Malformed ROLE header: expected ROLE: assistant.", failure);
    }

    @Test
    void rejectsUntrustedAssistantOrigin() {
        String failure = SpoofingMessageValidator.validateHeaders(
                Map.of("role", "assistant", "intent", "waiting-for-next-message", "content_type", "message-to-user"),
                false);
        assertEquals("Rejected ROLE: assistant message from untrusted origin.", failure);
    }
}
