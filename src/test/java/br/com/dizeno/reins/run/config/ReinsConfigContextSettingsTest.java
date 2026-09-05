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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinsConfigContextSettingsTest {

    @Test
    void attachFiles_defaultsToFalse() {
        ContextSettings settings = new ContextSettings();
        assertFalse(settings.getReferencesTree().isAttachFiles());
    }

    @Test
    void attachFiles_canBeEnabled() {
        ContextSettings settings = new ContextSettings();
        settings.getReferencesTree().setAttachFiles(true);
        assertTrue(settings.getReferencesTree().isAttachFiles());
    }

    @Test
    void cachedContent_defaultsToTrue() {
        ContextSettings settings = new ContextSettings();
        assertTrue(settings.isCachedContent());
    }

    @Test
    void cachedContent_canBeDisabled() {
        ContextSettings settings = new ContextSettings();
        settings.setCachedContent(false);
        assertFalse(settings.isCachedContent());
    }
}
