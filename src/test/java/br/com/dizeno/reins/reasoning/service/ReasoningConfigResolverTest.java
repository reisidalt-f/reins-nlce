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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReasoningConfigResolverTest {

    private final ReasoningConfigResolver resolver = new ReasoningConfigResolver();

    @Test
    void resolveMaxTurns_usesReasoningFallbackWhenGeminiMaximumTurnsMissing() {
        ReinsConfig config = new ReinsConfig();
        ReasoningSettings reasoningSettings = new ReasoningSettings();
        reasoningSettings.setMaxTurns(7);
        config.setReasoning(reasoningSettings);

        assertEquals(7, resolver.resolveMaxTurns(config));
    }

    @Test
    void resolveSourceBase_defaultsToMain() {
        assertEquals("main", resolver.resolveSourceBase(null));
        assertEquals("main", resolver.resolveSourceBase(""));
        assertEquals("main", resolver.resolveSourceBase("main"));
        assertEquals("test", resolver.resolveSourceBase("test"));
    }

    @Test
    void resolveProjectRoot_usesRequestRootWhenProvided() {
        ReasoningRequest request = new ReasoningRequest();
        request.setProjectRoot(Path.of("/tmp/project-root"));

        assertEquals(Path.of("/tmp/project-root"), resolver.resolveProjectRoot(request));
    }

}
