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

/**
 * ReasoningPhase is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public interface ReasoningPhase {
    /**
     * Executes the operation.
     *
     * @param context the context
     * @return the resolved or constructed object
     */
    ReasoningPhase execute(ReasoningContext context) throws Exception;
}
