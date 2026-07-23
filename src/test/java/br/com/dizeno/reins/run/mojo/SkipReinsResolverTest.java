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

package br.com.dizeno.reins.run.mojo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkipReinsResolverTest {

    private final SkipReinsResolver resolver = new SkipReinsResolver();

    @Test
    void resolve_nullValue_executes() {
        SkipDecision decision = resolver.resolve(null);

        assertEquals(SkipDecision.Decision.EXECUTE, decision.getDecision());
        assertEquals(SkipReasonCode.SKIP_DISABLED, decision.getReasonCode());
    }

    @Test
    void resolve_emptyValue_skips() {
        SkipDecision decision = resolver.resolve(" ");

        assertEquals(SkipDecision.Decision.SKIP, decision.getDecision());
        assertEquals("true", decision.getNormalizedValue());
    }

    @Test
    void resolve_trueValue_skips() {
        SkipDecision decision = resolver.resolve("TrUe");

        assertEquals(SkipDecision.Decision.SKIP, decision.getDecision());
        assertEquals("true", decision.getNormalizedValue());
    }

    @Test
    void resolve_falseValue_executes() {
        SkipDecision decision = resolver.resolve("FALSE");

        assertEquals(SkipDecision.Decision.EXECUTE, decision.getDecision());
        assertEquals("false", decision.getNormalizedValue());
    }

    @Test
    void resolve_invalidValue_fails() {
        SkipDecision decision = resolver.resolve("maybe");

        assertTrue(decision.shouldFail());
        assertEquals(SkipReasonCode.SKIP_INVALID_VALUE, decision.getReasonCode());
    }
}
