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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinsConfigProjectInferenceTest {

    

    @Test
    void enableProjectInference_defaultsToFalse() {
        ReinsConfig config = new ReinsConfig();
        assertFalse(config.isEnableProjectInference(),
                "enableProjectInference should default to false for safe backward-compat");
    }

    @Test
    void enableProjectInference_canBeSetToTrue() {
        ReinsConfig config = new ReinsConfig();
        config.setEnableProjectInference(true);
        assertTrue(config.isEnableProjectInference());
    }

    @Test
    void enableProjectInference_canBeToggledBackToFalse() {
        ReinsConfig config = new ReinsConfig();
        config.setEnableProjectInference(true);
        config.setEnableProjectInference(false);
        assertFalse(config.isEnableProjectInference());
    }
}
