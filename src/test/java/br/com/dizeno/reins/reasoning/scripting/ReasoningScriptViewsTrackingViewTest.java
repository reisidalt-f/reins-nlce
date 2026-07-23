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

package br.com.dizeno.reins.reasoning.scripting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReasoningScriptViewsTrackingViewTest {

    @Test
    void compiledRelativePathsStripKnownBasePrefixes() {
        ReasoningScriptViews.TrackingView view = new ReasoningScriptViews.TrackingView(
                "hash",
                List.of(
                        "main:src/main/java/com/example/A.java",
                        "test:src/test/java/com/example/ATest.java",
                        "target:compiled/B.java",
                        "main-source:docs/spec.md",
                        "script:inference/context-build.ftl"
                ),
                "success",
                "2026-05-14T00:00:00Z",
                List.of());

        assertEquals(
                List.of(
                        "src/main/java/com/example/A.java",
                        "src/test/java/com/example/ATest.java",
                        "compiled/B.java",
                        "docs/spec.md",
                        "inference/context-build.ftl"
                ),
                view.getCompiledRelativePaths());
    }

    @Test
    void compiledRelativePathsKeepUnknownPrefixesUnchanged() {
        ReasoningScriptViews.TrackingView view = new ReasoningScriptViews.TrackingView(
                "hash",
                List.of(
                        "custom-base:path.txt",
                        "src/main/java/com/example/A.java",
                        "C:/workspace/project/file.txt"
                ),
                "success",
                "2026-05-14T00:00:00Z",
                List.of());

        assertEquals(
                List.of(
                        "custom-base:path.txt",
                        "src/main/java/com/example/A.java",
                        "C:/workspace/project/file.txt"
                ),
                view.getCompiledRelativePaths());
    }
}