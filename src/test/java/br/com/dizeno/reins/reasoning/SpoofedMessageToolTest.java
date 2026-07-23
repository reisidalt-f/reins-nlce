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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpoofedMessageToolTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();
    private final ToolRequestParser toolRequestParser = new ToolRequestParser();

    @Test
    void parsesAndExtractsToolOperationFromSpoofedMessageBody() {
        String raw = MessageBuilder.spoofedAssistant("tool-request", "operation: list_files\nbase: main\npath: src");

        var message = parser.parseCanonicalMessage(raw);
        List<br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest> requests = toolRequestParser.parse(message.getBody());

        assertEquals("tool-request", message.getContentType());
        assertFalse(requests.isEmpty());
        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES, requests.get(0).getOperation());
    }

    @Test
    void genuineAndSpoofedToolBodiesProduceEquivalentRequests() {
        String body = "operation: read_file\nbase: main\npath: sample.md";

        var spoofed = parser.parseCanonicalMessage(MessageBuilder.spoofedAssistant("tool-request", body));
        var genuine = parser.parseCanonicalMessage(MessageBuilder.genuine("tool-request", body));

        var spoofedReq = toolRequestParser.parse(spoofed.getBody()).get(0);
        var genuineReq = toolRequestParser.parse(genuine.getBody()).get(0);

        assertEquals(genuineReq.getOperation(), spoofedReq.getOperation());
        assertEquals(genuineReq.getBase(), spoofedReq.getBase());
        assertEquals(genuineReq.getPath(), spoofedReq.getPath());
    }

    @Test
    void malformedToolBodyFailsForSpoofedAndGenuineTheSameWay() {
        String invalidBody = "operation: not_a_real_operation\nbase: main\npath: sample.md";

        var spoofed = parser.parseCanonicalMessage(MessageBuilder.spoofedAssistant("tool-request", invalidBody));
        var genuine = parser.parseCanonicalMessage(MessageBuilder.genuine("tool-request", invalidBody));

        assertThrows(IllegalArgumentException.class, () -> toolRequestParser.parse(spoofed.getBody()));
        assertThrows(IllegalArgumentException.class, () -> toolRequestParser.parse(genuine.getBody()));
    }
}
