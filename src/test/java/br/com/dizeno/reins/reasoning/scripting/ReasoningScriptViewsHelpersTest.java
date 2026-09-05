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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasoningScriptViewsHelpersTest {

    @Test
    void testPolicyViewToolGroupHelpers() {
        ReasoningScriptViews.PolicyView policyEmpty = new ReasoningScriptViews.PolicyView(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, "");
        assertFalse(policyEmpty.isHasListFiles());
        assertFalse(policyEmpty.isHasReadFiles());
        assertFalse(policyEmpty.isHasWriteFiles());
        assertFalse(policyEmpty.isHasPatchFiles());
        assertFalse(policyEmpty.isHasDeleteFiles());
        assertFalse(policyEmpty.isHasListCompiledFiles());
        assertFalse(policyEmpty.isScriptRunnerEnabled());
        assertFalse(policyEmpty.isHasAnyReadOrList());
        assertFalse(policyEmpty.isHasAnyMutation());
        assertFalse(policyEmpty.isHasAnyToolEnabled());

        ReasoningScriptViews.PolicyView policyRead = new ReasoningScriptViews.PolicyView(
                List.of("main"), List.of("main"), List.of(), List.of(), List.of(), List.of(), false, "");
        assertTrue(policyRead.isHasListFiles());
        assertTrue(policyRead.isHasReadFiles());
        assertFalse(policyRead.isHasWriteFiles());
        assertTrue(policyRead.isHasAnyReadOrList());
        assertFalse(policyRead.isHasAnyMutation());
        assertTrue(policyRead.isHasAnyToolEnabled());

        ReasoningScriptViews.PolicyView policyMutation = new ReasoningScriptViews.PolicyView(
                List.of(), List.of(), List.of("target"), List.of("target"), List.of("target"), List.of(), false, "");
        assertTrue(policyMutation.isHasWriteFiles());
        assertTrue(policyMutation.isHasPatchFiles());
        assertTrue(policyMutation.isHasDeleteFiles());
        assertFalse(policyMutation.isHasAnyReadOrList());
        assertTrue(policyMutation.isHasAnyMutation());
        assertTrue(policyMutation.isHasAnyToolEnabled());
    }

    @Test
    void testPipelineViewHelpers() {
        ReasoningScriptViews.PipelineView initialPipeline = new ReasoningScriptViews.PipelineView(
                "default-cycle", 0, 1, List.of("default-cycle"), "COMPILE", null, null, null, null, false);
        assertTrue(initialPipeline.isFirstMessage());
        assertTrue(initialPipeline.isFirstPhase());

        ReasoningScriptViews.PipelineView activePipeline = new ReasoningScriptViews.PipelineView(
                "default-cycle", 1, 2, List.of("default-cycle"), "COMPILE", "waiting-for-next-message", "tool-request", "", null, true);
        assertFalse(activePipeline.isFirstMessage());
        assertFalse(activePipeline.isFirstPhase());
    }

    @Test
    void testTrackingViewFormattedListHelpers() {
        ReasoningScriptViews.TrackingView emptyTracking = new ReasoningScriptViews.TrackingView(
                "hash", List.of(), "SUCCESS", "2026-08-13", List.of());
        assertEquals("- none", emptyTracking.getNotesOrNone());
        assertEquals("- none", emptyTracking.getCompiledPathsOrNone());

        ReasoningScriptViews.TrackingView trackingWithItems = new ReasoningScriptViews.TrackingView(
                "hash", List.of("target:Out1.java", "target:Out2.java"), "SUCCESS", "2026-08-13", List.of("Note 1", "Note 2"));
        assertEquals("- Note 1\n- Note 2", trackingWithItems.getNotesOrNone());
        assertEquals("- target:Out1.java\n- target:Out2.java", trackingWithItems.getCompiledPathsOrNone());
    }

    @Test
    void testInferenceStateViewHelpers() {
        ReasoningScriptViews.InferenceStateView emptyInference = new ReasoningScriptViews.InferenceStateView(
                "context", List.of(), List.of(), List.of(), List.of());
        assertEquals("- none", emptyInference.getInspectedFilesOrNone());
        assertFalse(emptyInference.isHasHistory());
        assertTrue(emptyInference.isFirstTurn());

        ReasoningScriptViews.TurnView turn = new ReasoningScriptViews.TurnView(1, "user", "hello", List.of());
        ReasoningScriptViews.InferenceStateView inferenceWithHistory = new ReasoningScriptViews.InferenceStateView(
                "context", List.of(turn), List.of("file.java"), List.of(), List.of());
        assertEquals("- file.java", inferenceWithHistory.getInspectedFilesOrNone());
        assertTrue(inferenceWithHistory.isHasHistory());
        assertFalse(inferenceWithHistory.isFirstTurn());
    }
}
