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
import br.com.dizeno.reins.run.config.settings.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingSettingsTest {

    @Test
    void freezeState_defaultsToFalse() {
        TrackingSettings settings = new TrackingSettings();
        assertFalse(settings.isFreezeState());
    }

    @Test
    void freezeState_canBeEnabled() {
        TrackingSettings settings = new TrackingSettings();
        settings.setFreezeState(true);
        assertTrue(settings.isFreezeState());
    }

    @Test
    void freezeState_canBeDisabledExplicitly() {
        TrackingSettings settings = new TrackingSettings();
        settings.setFreezeState(false);
        assertFalse(settings.isFreezeState());
    }

    @Test
    void pluginConfig_trackingDefaultsNotNull() {
        ReinsConfig config = new ReinsConfig();
        assertNotNull(config.getTracking());
    }

    @Test
    void pluginConfig_trackingFreezeState_defaultsFalse() {
        ReinsConfig config = new ReinsConfig();
        assertFalse(config.getTracking().isFreezeState());
    }

    @Test
    void pluginConfig_setTracking_nullSafetyCreatesDefault() {
        ReinsConfig config = new ReinsConfig();
        config.setTracking(null);
        assertNotNull(config.getTracking());
        assertFalse(config.getTracking().isFreezeState());
    }
}
