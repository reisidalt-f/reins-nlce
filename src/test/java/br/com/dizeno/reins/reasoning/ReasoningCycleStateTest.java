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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasoningCycleStateTest {

    @Test
    void defaultsGraceTurnStateToFalse() {
        ReasoningCycle cycle = new ReasoningCycle();

        assertFalse(cycle.isGraceTurnUsed());
        assertEquals(0, cycle.getMaxTurnAt());
        assertEquals(0, cycle.getClosedTurnCount());
        assertEquals(1, cycle.getCurrentTurnIndex());
    }

    @Test
    void tracksGraceTurnUsageMetadata() {
        ReasoningCycle cycle = new ReasoningCycle();
        cycle.setMaxTurns(10);
        cycle.setGraceTurnUsed(true);
        cycle.setMaxTurnAt(10);
        cycle.setClosedTurnCount(3);
        cycle.setCurrentTurnIndex(4);

        assertTrue(cycle.isGraceTurnUsed());
        assertEquals(10, cycle.getMaxTurnAt());
        assertEquals(3, cycle.getClosedTurnCount());
        assertEquals(4, cycle.getCurrentTurnIndex());
    }
}
