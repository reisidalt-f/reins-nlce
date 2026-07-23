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

package br.com.dizeno.reins.reasoning.service;

import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormattingServiceTest {

    private final MessageFormattingService service = new MessageFormattingService();

    @Test
    void appendSystemContext_combinesBaseAndPayload() {
        String result = service.appendSystemContext("base", "payload");
        assertEquals("base\n\npayload", result);
    }

    @Test
    void withTurnCountNote_addsTerminalGuidanceOnLastTurn() {
        String result = service.withTurnCountNote("compile", 3, 3, true);
        assertTrue(result.contains("Turn 3/3"));
        assertTrue(result.contains("finish-success"));
    }

    @Test
    void parsePathList_trimsAndDeduplicates() {
        List<String> parsed = service.parsePathList("a\n\n a \n b \n");
        assertEquals(List.of("a", "b"), parsed);
    }

    @Test
    void buildOutboundLogBody_appendsAttachmentReferences() {
        AttachedFilePayload payload = new AttachedFilePayload();
        payload.setBase("main");
        payload.setRelativePath("foo.md");

        String body = service.buildOutboundLogBody("msg", List.of(payload));
        assertTrue(body.contains("attachments:"));
        assertTrue(body.contains("main:foo.md"));
    }
}
