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

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParsingScenarioFixtures {
    private ParsingScenarioFixtures() {
    }

    public static Map<String, String> allScenarios() {
        Map<String, String> scenarios = new LinkedHashMap<>();
        scenarios.put("role_assistant_in_process", MessageBuilder.spoofedAssistant("tool-request", "operation: list_files\nbase: main\npath: src"));
        scenarios.put("role_assistant_external", MessageBuilder.spoofedAssistant("message-to-user", "Should be rejected by trust boundary validator."));
        scenarios.put("role_user", MessageBuilder.malformedRole("user", "message-to-user", "Regular script message."));
        scenarios.put("role_unknown", MessageBuilder.malformedRole("operator", "message-to-user", "Regular script message."));
        scenarios.put("no_role", MessageBuilder.genuine("message-to-user", "No role marker."));
        scenarios.put("missing_intent", "CONTENT_TYPE: message-to-user\n\nMissing intent");
        scenarios.put("missing_content_type", "INTENT: waiting-for-next-message\n\nMissing content type");
        scenarios.put("missing_blank_line", "INTENT: waiting-for-next-message\nCONTENT_TYPE: message-to-user\nBody without separator");
        return scenarios;
    }
}
