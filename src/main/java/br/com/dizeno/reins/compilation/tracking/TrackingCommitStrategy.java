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

import java.io.IOException;
import java.nio.file.Path;

/**
 * TrackingCommitStrategy is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing tracking commit strategy.
 */
public class TrackingCommitStrategy {

    /**
     * Commits the changes.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @param record the tracking record
     * @param trackingStore the persistence store for file tracking records
     */
    public void commit(Path projectRoot,
                       String sourcePath,
                       SourceTrackingRecord record,
                       CompilationTrackingStore trackingStore) throws IOException {
        trackingStore.save(projectRoot, sourcePath, record);
    }
}
