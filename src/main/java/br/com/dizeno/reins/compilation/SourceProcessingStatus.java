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
 * SourceProcessingStatus is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing source processing status.
 */
public enum SourceProcessingStatus {
    SKIP,
    COMPILE,
    VALIDATE;

    /**
     * Should Execute.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean shouldExecute() {
        return this != SKIP;
    }
}