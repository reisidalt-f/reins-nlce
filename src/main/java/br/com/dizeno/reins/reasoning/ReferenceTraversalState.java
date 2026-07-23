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

import java.util.HashMap;
import java.util.Map;

/**
 * ReferenceTraversalState is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reference traversal state.
 */
public class ReferenceTraversalState {
    private final Map<String, Integer> occurrences = new HashMap<>();

    /**
     * Increment.
     *
     * @param canonicalPath the canonical path
     * @return the numeric value
     */
    public int increment(String canonicalPath) {
        int next = occurrences.getOrDefault(canonicalPath, 0) + 1;
        occurrences.put(canonicalPath, next);
        return next;
    }
}
