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

import br.com.dizeno.reins.reasoning.*;
import br.com.dizeno.reins.reasoning.phase.TurnCompletionPhase;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.run.config.ReinsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GotoPhaseExecutionTest {

    @TempDir
    Path tempDir;

    @Test
    void executorFindsPhaseIndexInPipelinePlan() {
        ReasoningPipelineExecutor executor = new ReasoningPipelineExecutor(null, new ResponseDirectiveParser());

        List<ReasoningPhaseDescriptor> descriptors = List.of(
                new ReasoningPhaseDescriptor("initial-context", 0, ReasoningPhaseDescriptor.State.PENDING),
                new ReasoningPhaseDescriptor("code-gen", 1, ReasoningPhaseDescriptor.State.PENDING),
                new ReasoningPhaseDescriptor("verification-phase", 2, ReasoningPhaseDescriptor.State.PENDING)
        );
        ReasoningPipelinePlan plan = new ReasoningPipelinePlan("main:Test.md", descriptors, ReasoningPipelinePlan.PlanStatus.VALID);

        assertEquals(0, executor.findPhaseIndex(plan, "initial-context"));
        assertEquals(1, executor.findPhaseIndex(plan, "code-gen"));
        assertEquals(2, executor.findPhaseIndex(plan, "verification-phase"));
        assertEquals(-1, executor.findPhaseIndex(plan, "non-existent"));
    }

    @Test
    void turnCompletionPhaseExecutesGotoPhaseTransition() throws Exception {
        DefaultReasoningExecutionCoordinator coordinator = new DefaultReasoningExecutionCoordinator();
        TurnCompletionPhase turnCompletionPhase = new TurnCompletionPhase(coordinator);

        ReasoningRequest request = new ReasoningRequest();
        request.setSourcePath("main:Test.md");
        ReinsConfig config = new ReinsConfig();
        ReasoningCycle cycle = new ReasoningCycle();
        ReasoningContext context = new ReasoningContext(request, config, cycle, tempDir);

        List<ReasoningPhaseDescriptor> descriptors = List.of(
                new ReasoningPhaseDescriptor("initial-context", 0, ReasoningPhaseDescriptor.State.PENDING),
                new ReasoningPhaseDescriptor("verification-phase", 1, ReasoningPhaseDescriptor.State.PENDING)
        );
        ReasoningPipelinePlan plan = new ReasoningPipelinePlan("main:Test.md", descriptors, ReasoningPipelinePlan.PlanStatus.VALID);

        context.setPipelinePlan(plan);
        context.setPipelinePhaseIndex(0);
        context.setCurrentPipelinePhase("initial-context");

        ReasoningTurn inbound = new ReasoningTurn();
        inbound.setSequence(1);
        inbound.setPayload("GOTO_PHASE: verification-phase\nCONTENT_TYPE: user-progress\n\nJumping to verification.");
        context.setTurnInbound(inbound);

        ResponseDirective directive = new ResponseDirective();
        directive.setIntent(ResponseDirective.Intent.GOTO_PHASE);
        directive.setTargetPhase("verification-phase");
        directive.setValid(true);
        context.setTurnDirective(directive);

        ResponseDirectiveParser.ParseResult parseResult = new ResponseDirectiveParser.ParseResult(directive, "Jumping to verification.");
        context.setTurnParseResult(parseResult);

        ReasoningPhase nextPhase = turnCompletionPhase.execute(context);

        assertNotNull(nextPhase);
        assertEquals("verification-phase", context.getCurrentPipelinePhase());
        assertEquals(1, context.getPipelinePhaseIndex());
        assertEquals(1, context.getAttemptIndex());
        assertTrue(context.isNextMessageFromScript());
    }
}
