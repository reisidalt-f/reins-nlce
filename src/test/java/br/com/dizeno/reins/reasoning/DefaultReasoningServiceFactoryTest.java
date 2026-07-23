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

import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultReasoningServiceFactoryTest {

    @Test
    void createDefault_returnsConfiguredService() {
        DefaultReasoningService service = DefaultReasoningServiceFactory.createDefault();
        assertNotNull(service);
    }

    @Test
    void createWithCustomScriptEvaluator_returnsConfiguredService() {
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());
        registry.validateAll();
        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null);

        DefaultReasoningService service = DefaultReasoningServiceFactory.createWithCustomScriptEvaluator(evaluator);
        assertNotNull(service);
    }
}
