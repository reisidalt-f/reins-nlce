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
    void attachReferencedFiles_defaultsToTrue() {
        ContextSettings settings = new ContextSettings();
        assertTrue(settings.isAttachReferencedFiles());
    }

    @Test
    void attachReferencedFiles_canBeDisabled() {
        ContextSettings settings = new ContextSettings();
        settings.setAttachReferencedFiles(false);
        assertFalse(settings.isAttachReferencedFiles());
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
