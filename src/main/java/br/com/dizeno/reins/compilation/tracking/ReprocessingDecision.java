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

package br.com.dizeno.reins.compilation.tracking;

/**
 * ReprocessingDecision is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public record ReprocessingDecision(String sourcePath,
                                   boolean shouldReprocess,
                                   ReprocessingReason reason) {
    /**
     * ReprocessingReason is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing reprocessing reason.
     */
    public enum ReprocessingReason {
        NO_PRIOR_RECORD,
        SOURCE_HASH_CHANGED,
        OUTPUT_MISSING,
        OUTPUT_PATHS_CHANGED,
         
        TARGET_ROOT_CHANGED,
        SOURCE_RECORD_TIMESTAMP_ADVANCED,
        SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED,
        SOURCE_DEPENDENCY_SET_CHANGED,
        PROJECT_RECORD_TIMESTAMP_ADVANCED,
        PROJECT_TRACKED_SET_CHANGED,
        TRACKED_FILE_MTIME_MISSING,
        TRACKED_FILE_MISSING_OR_UNREADABLE,
        SOURCE_NEWER_THAN_OUTPUTS,
        CHILD_OUTPUT_CHANGED,
         
        PRIOR_RECORD_PENDING_NOTES,
        SKIP_UP_TO_DATE,
         
        NOTES_BACKTRACK
    }
}