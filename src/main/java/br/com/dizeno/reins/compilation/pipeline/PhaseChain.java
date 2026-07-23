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

package br.com.dizeno.reins.compilation.pipeline;

import java.util.List;

 
/**
 * PhaseChain is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a component managing phase chain.
 */
public final class PhaseChain {

    private final List<CompilationPhase> phases;
    private final int index;

    /**
     * Constructs a new instance of {@link PhaseChain}.
     *
     * @param phases the phases
     */
    public PhaseChain(List<CompilationPhase> phases) {
        this(phases, 0);
    }

    private PhaseChain(List<CompilationPhase> phases, int index) {
        this.phases = phases;
        this.index = index;
    }

     
    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     */
    public void execute(SourceCompilationContext ctx) throws Exception {
        if (index < phases.size()) {
            phases.get(index).execute(ctx, new PhaseChain(phases, index + 1));
        }
    }
}
