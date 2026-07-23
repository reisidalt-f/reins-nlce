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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RecompilationDeciderTest {

    @TempDir
    Path projectDir;

    private final RecompilationDecider decider = new RecompilationDecider();

    

    @Test
    void returnsTargetRootChanged_whenStoredRootDiffersFromCurrent() {
        SourceTrackingRecord old = record("src/main/nl/Foo.md", "hash1", "src/main/java");

        ReprocessingDecision decision = decider.evaluate(
                "src/main/nl/Foo.md", old,
                "hash1", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason());
    }

    @Test
    void doesNotReturnTargetRootChanged_whenStoredRootEqualsCurrentRoot() {
        SourceTrackingRecord old = record("src/main/nl/Foo.md", "hash1", "src/main/java");

        ReprocessingDecision decision = decider.evaluate(
                "src/main/nl/Foo.md", old,
                "hash1", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src/main/java",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason());
        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, decision.reason());
    }

    @Test
    void skipsTargetRootCheck_whenStoredResolvedTargetRootIsNull() {
        
        SourceTrackingRecord old = record("src/main/nl/Foo.md", "hash1", null);

        ReprocessingDecision decision = decider.evaluate(
                "src/main/nl/Foo.md", old,
                "hash1", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src/main/java",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason(),
                "null stored resolvedTargetRoot must not trigger TARGET_ROOT_CHANGED (backward compat)");
    }

    @Test
    void skipsTargetRootCheck_whenCurrentResolvedTargetRootIsNull() {
        
        SourceTrackingRecord old = record("src/main/nl/Foo.md", "hash1", "src/main/java");

        ReprocessingDecision decision = decider.evaluate(
                "src/main/nl/Foo.md", old,
                "hash1", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                null,
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason(),
                "null current resolvedTargetRoot must not trigger TARGET_ROOT_CHANGED");
    }

    

    @Test
    void returnsTargetRootChanged_whenTestScopeRootDiffersFromCurrent() {
        SourceTrackingRecord old = record("src/test/nl/FooTest.md", "hash2", "src/test/java");

        ReprocessingDecision decision = decider.evaluate(
                "src/test/nl/FooTest.md", old,
                "hash2", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason());
    }

    @Test
    void doesNotReturnTargetRootChanged_whenTestScopeRootIsUnchanged() {
        SourceTrackingRecord old = record("src/test/nl/FooTest.md", "hash2", "src/test/java");

        ReprocessingDecision decision = decider.evaluate(
                "src/test/nl/FooTest.md", old,
                "hash2", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src/test/java",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, decision.reason());
    }

    @Test
    void skipsTargetRootCheck_whenOldRecordHasNullAndTestScopeIsCurrent() {
        
        SourceTrackingRecord old = record("src/test/nl/FooTest.md", "hash2", null);

        ReprocessingDecision decision = decider.evaluate(
                "src/test/nl/FooTest.md", old,
                "hash2", List.of("fp"), List.of(),
                projectDir, RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                "src/test/java",
                RecompilationDecider.EvaluationInputs.empty(),
                null
        );

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, decision.reason(),
                "old records must not be invalidated on first upgrade run");
    }

    

        private SourceTrackingRecord record(String sourcePath, String sourceHash, String resolvedTargetRoot) {
                SourceTrackingRecord r = new SourceTrackingRecord();
                r.setSourcePath(sourcePath);
                r.setSourceHash(sourceHash);
                r.setSourceModificationTime(1L);
                r.setBlockFingerprints(List.of("fp"));
                r.setCompiledFiles(new java.util.LinkedHashMap<>());
                r.setMarkdownReferences(new java.util.LinkedHashMap<>());
                r.setResolvedTargetRoot(resolvedTargetRoot);
                return r;
        }
}
