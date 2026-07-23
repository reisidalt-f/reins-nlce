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

class ReasoningRequestProjectCycleFieldTest {

    

    @Test
    void projectInferenceCycle_defaultsToFalse() {
        ReasoningRequest request = new ReasoningRequest();
        assertFalse(request.isProjectInferenceCycle(),
                "projectInferenceCycle should default to false for backward-compat");
    }

    @Test
    void projectInferenceCycle_canBeSetToTrue() {
        ReasoningRequest request = new ReasoningRequest();
        request.setProjectInferenceCycle(true);
        assertTrue(request.isProjectInferenceCycle());
    }

    @Test
    void projectInferenceCycle_canBeToggledBackToFalse() {
        ReasoningRequest request = new ReasoningRequest();
        request.setProjectInferenceCycle(true);
        request.setProjectInferenceCycle(false);
        assertFalse(request.isProjectInferenceCycle());
    }

    @Test
    void useCachedContent_defaultsToTrue() {
        ReasoningRequest request = new ReasoningRequest();
        assertTrue(request.isUseCachedContent());
    }

    @Test
    void cachedContentFields_canBeSet() {
        ReasoningRequest request = new ReasoningRequest();
        request.setUseCachedContent(false);
        request.setCachedContentId("cachedContents/abc");
        request.setMainSourceQualifiedPath("main:domain/entities.md");

        assertFalse(request.isUseCachedContent());
        assertTrue("cachedContents/abc".equals(request.getCachedContentId()));
        assertTrue("main:domain/entities.md".equals(request.getMainSourceQualifiedPath()));
    }
}
