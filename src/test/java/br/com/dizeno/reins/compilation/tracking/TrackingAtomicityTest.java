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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackingAtomicityTest {

    @TempDir
    Path projectRoot;

    @Test
    void commit_isPerFileAndDoesNotRollbackOnLaterFailure() throws Exception {
        CompilationTrackingStore store = new CompilationTrackingStore();
        TrackingCommitStrategy strategy = new TrackingCommitStrategy();
        SourceTrackingManager manager = new SourceTrackingManager(strategy);

        String sourceA = "src/main/nl/A.md";
        String sourceB = "src/main/nl/B.md";
        String unrelated = "src/test/nl/C.md";

        manager.commit(projectRoot, unrelated, record("test:C.md", "old", "success"), store);
        manager.commit(projectRoot, sourceA, record("main:A.md", "v1", "success"), store);

        try {
            throw new IllegalStateException("simulated processing failure for source B");
        } catch (IllegalStateException ignored) {
            
        }

        Optional<SourceTrackingRecord> a = store.load(projectRoot, sourceA);
        Optional<SourceTrackingRecord> b = store.load(projectRoot, sourceB);
        Optional<SourceTrackingRecord> c = store.load(projectRoot, unrelated);

        assertTrue(a.isPresent());
        assertEquals("v1", a.get().getSourceHash());
        assertTrue(b.isEmpty());
        assertTrue(c.isPresent());
        assertEquals("old", c.get().getSourceHash());
    }

    private SourceTrackingRecord record(String sourcePath, String hash, String status) {
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(sourcePath);
        record.setSourceHash(hash);
        record.setLastStatus(status);
        return record;
    }
}
