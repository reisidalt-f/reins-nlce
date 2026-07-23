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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackwardCompatibilityTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void genuineLlmMessageRemainsValidWithoutRoleHeader() {
        var message = parser.parseCanonicalMessage(
                "INTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\n\nGenuine output");

        assertTrue(message.isValid());
        assertFalse(message.hasRoleHeader());
    }
}
