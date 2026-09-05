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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.reasoning.model.RoutingFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReasoningExecutionCoordinatorTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void computesSpoofedRoutingFlagForTrustedScriptAssistantMessage() {
        var message = parser.parseCanonicalMessage(MessageBuilder.spoofedAssistant("message-to-user", "ok"));
        String failure = SpoofingMessageValidator.validateHeaders(message.getHeaders(), true);
        RoutingFlag flag = failure == null && message.isSpoofedAssistantRole()
                ? RoutingFlag.SCRIPT_SPOOFED_LLM
                : RoutingFlag.GENUINE_LLM;

        assertEquals(RoutingFlag.SCRIPT_SPOOFED_LLM, flag);
    }

    @Test
    void preservesIntentSemanticsAcrossSpoofedAndGenuineMessages() {
        var spoofed = parser.parseCanonicalMessage(
                "ROLE: assistant\nINTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone");
        var genuine = parser.parseCanonicalMessage(
                "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nDone");

        assertTrue(spoofed.isFinishSuccess());
        assertTrue(genuine.isFinishSuccess());
        assertEquals(genuine.getIntent(), spoofed.getIntent());
    }

    @Test
    void formatsMutationBlockedMessageWithReasonDetails() {
        ReferenceMutationDecision decision = ReferenceMutationDecision.blockReferencedSource(2, 1);

        String message = DefaultReasoningExecutionCoordinator.formatReferenceMutationBlockedMessage(
                "reason=" + decision.getReasonCode() + " ignoredSelfReferences=" + decision.getIgnoredSelfReferences());

        assertTrue(message.contains("Mutation of artifacts compiled by referenced markdown is not allowed."));
        assertTrue(message.contains("Use add_reasoning_note on the artifact you attempted to mutate"));
        assertTrue(message.contains("describe the reason for the block"));
        assertTrue(message.contains("BLOCK_REFERENCED_SOURCE"));
        assertTrue(message.contains("ignoredSelfReferences=1"));
    }

    @Test
    void formatsMutationBlockedMessageWhenAddReasoningNotesDisabled() {
        ReferenceMutationDecision decision = ReferenceMutationDecision.blockReferencedSource(2, 1);

        String message = DefaultReasoningExecutionCoordinator.formatReferenceMutationBlockedMessage(
                "reason=" + decision.getReasonCode() + " ignoredSelfReferences=" + decision.getIgnoredSelfReferences(), false);

        assertTrue(message.contains("Mutation of artifacts compiled by referenced markdown is not allowed."));
        assertTrue(!message.contains("add_reasoning_note"));
        assertTrue(message.contains("Describe the reason for the block and what needs to change in your final response message."));
        assertTrue(message.contains("BLOCK_REFERENCED_SOURCE"));
    }

    @Test
    void formatsMutationBlockedLogFieldsWithReasonCodeAndIgnoredCount() {
        ReferenceMutationDecision decision = ReferenceMutationDecision.blockReferencedCompiled(3, 1);

        String logFields = DefaultReasoningExecutionCoordinator.formatReferenceMutationLogFields(decision);

        assertTrue(logFields.contains("referenceMutationBlocked=true"));
        assertTrue(logFields.contains("reason=BLOCK_REFERENCED_COMPILED"));
        assertTrue(logFields.contains("ignoredSelfReferences=1"));
    }

    @Test
    void stateMachineExecutesPhasesInSequenceUntilNull() throws Exception {
        java.util.List<String> order = new java.util.ArrayList<>();
        
        ReasoningPhase phase2 = context -> {
            order.add("phase2");
            return null;
        };
        ReasoningPhase phase1 = context -> {
            order.add("phase1");
            return phase2;
        };

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("test");
        ReasoningContext context = new ReasoningContext(request, null, null, null);

        ReasoningPhase current = phase1;
        while (current != null) {
            current = current.execute(context);
        }

        assertEquals(java.util.List.of("phase1", "phase2"), order);
    }
}
