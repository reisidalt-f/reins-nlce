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

import br.com.dizeno.reins.reasoning.model.RoutingFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpoofingMessageRoutingTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();
    private final ToolRequestParser toolRequestParser = new ToolRequestParser();

    @Test
    void routesTrustedAssistantRoleAsSpoofedLlm() {
        var message = parser.parseCanonicalMessage(MessageBuilder.spoofedAssistant("tool-request", "operation: list_files\nbase: main\npath: src"));

        String failure = SpoofingMessageValidator.validateHeaders(message.getHeaders(), true);
        RoutingFlag flag = failure == null && message.isSpoofedAssistantRole()
                ? RoutingFlag.SCRIPT_SPOOFED_LLM
                : RoutingFlag.GENUINE_LLM;

        assertNull(failure);
        assertEquals(RoutingFlag.SCRIPT_SPOOFED_LLM, flag);
    }

    @Test
    void rejectsUntrustedAssistantRoleAndFallsBackFromSpoofedRouting() {
        var message = parser.parseCanonicalMessage(MessageBuilder.spoofedAssistant("message-to-user", "Untrusted source"));

        String failure = SpoofingMessageValidator.validateHeaders(message.getHeaders(), false);
        RoutingFlag flag = failure == null && message.isSpoofedAssistantRole()
                ? RoutingFlag.SCRIPT_SPOOFED_LLM
                : RoutingFlag.GENUINE_LLM;

        assertEquals("Rejected ROLE: assistant message from untrusted origin.", failure);
        assertEquals(RoutingFlag.GENUINE_LLM, flag);
    }

    @Test
    void spoofedToolRoutingProvidesOperationForExecution() {
        var message = parser.parseCanonicalMessage(
                MessageBuilder.spoofedAssistant("tool-request", "operation: list_files\nbase: main\npath: src"));

        var requests = toolRequestParser.parse(message.getBody());

        assertEquals(RoutingFlag.SCRIPT_SPOOFED_LLM,
                message.isSpoofedAssistantRole() ? RoutingFlag.SCRIPT_SPOOFED_LLM : RoutingFlag.GENUINE_LLM);
        assertTrue(requests.size() == 1);
    }
}
