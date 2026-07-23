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

package br.com.dizeno.reins.run.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReinsConfigSemverStabilityTest {

    @Test
    void inferenceScriptsPathIsOptionalAndDefaultsRemainStable() {
        ReinsConfig config = new ReinsConfig();

        
        assertNull(config.getReasoning().getScriptsPath());

        
        assertEquals("**/*.md", config.getIncludePattern());
        assertNotNull(config.getGemini());
    }

    @Test
    void inferenceScriptsPathCanBeConfiguredWithoutAffectingExistingSettings() {
        ReinsConfig config = new ReinsConfig();
        String previousPattern = config.getIncludePattern();

        config.getReasoning().setScriptsPath("src/main/reins-scripts");

        assertEquals("src/main/reins-scripts", config.getReasoning().getScriptsPath());
        assertEquals(previousPattern, config.getIncludePattern());
    }
}
