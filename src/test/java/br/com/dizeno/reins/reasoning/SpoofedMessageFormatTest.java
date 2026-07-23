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

class SpoofedMessageFormatTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void validatesAllContractScenarios() {
        var scenarios = ParsingScenarioFixtures.allScenarios();

        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_assistant_in_process")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_assistant_external")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_user")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_unknown")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("no_role")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_intent")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_content_type")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_blank_line")).isValid());
    }
}
