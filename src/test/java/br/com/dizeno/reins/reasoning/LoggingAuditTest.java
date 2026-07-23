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

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingAuditTest {

    @Test
    void routingFlagCanBeEmbeddedInLogEntries() {
        String spoofedLogLine = "routingFlag=" + RoutingFlag.SCRIPT_SPOOFED_LLM;
        String genuineLogLine = "routingFlag=" + RoutingFlag.GENUINE_LLM;

        assertTrue(spoofedLogLine.contains("SCRIPT_SPOOFED_LLM"));
        assertTrue(genuineLogLine.contains("GENUINE_LLM"));
    }
}
