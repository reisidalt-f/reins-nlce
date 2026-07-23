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

package br.com.dizeno.reins.compilation;

 
/**
 * QueuePointerState is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing queue pointer state.
 */
public final class QueuePointerState {
    private final int pointer;
    private final int totalNewlyNoted;

    /**
     * Constructs a new instance of {@link QueuePointerState}.
     *
     * @param pointer the pointer
     * @param totalNewlyNoted the total newly noted
     */
    public QueuePointerState(int pointer, int totalNewlyNoted) {
        this.pointer = pointer;
        this.totalNewlyNoted = totalNewlyNoted;
    }

    /**
     * Gets the pointer.
     *
     * @return the numeric value
     */
    public int getPointer() {
        return pointer;
    }

    /**
     * Gets the total newly noted.
     *
     * @return the numeric value
     */
    public int getTotalNewlyNoted() {
        return totalNewlyNoted;
    }
}
