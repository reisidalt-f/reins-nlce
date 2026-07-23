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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResponseDirectiveParserTest {

    private final ResponseDirectiveParser parser = new ResponseDirectiveParser();

    @Test
    void parsesFinishSuccessDirective() {
        String raw = "INTENT: finish-success\n"
                + "CONTENT_TYPE: message-to-user\n\n"
                + "Completed";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);

        assertTrue(parsed.getDirective().isValid());
        assertEquals(ResponseDirective.Intent.FINISH_SUCCESS, parsed.getDirective().getIntent());
        assertEquals(ResponseDirective.ContentType.MESSAGE_TO_USER, parsed.getDirective().getContentType());
    }

    @Test
    void parsesWaitingDirective() {
        String raw = "INTENT: waiting-for-next-message\n"
                + "CONTENT_TYPE: tool-request\n\n"
                + "operation: list_files\nbase: main\npath: domain";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);

        assertTrue(parsed.getDirective().isValid());
        assertEquals(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE, parsed.getDirective().getIntent());
        assertEquals(ResponseDirective.ContentType.TOOL_REQUEST, parsed.getDirective().getContentType());
    }

        @Test
        void extractsGoalStrategProgressFromNonFinishUserMessage() {
        String raw = "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: message-to-user\n\n"
            + "GOAL: Implement the service layer for the task management system\n"
            + "STRATEGY: Inspect existing entities to align service signatures with the data model\n"
            + "PROGRESS: Identified entity classes and confirmed field naming conventions\n"
            + "No new decisions this turn.\n";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);
        ResponseDirective directive = parsed.getDirective();

        assertTrue(directive.isValid());
        assertTrue(directive.isNonFinishUserMessage());
        assertNotNull(directive.getPlanningContent());
        assertEquals("Implement the service layer for the task management system",
            directive.getPlanningContent().getGoalSummary());
        assertEquals("Inspect existing entities to align service signatures with the data model",
            directive.getPlanningContent().getStrategySummary());
        assertEquals("Identified entity classes and confirmed field naming conventions",
            directive.getPlanningContent().getProgressSummary());
        }

        @Test
        void detectsNoNewDecisionMarker() {
        String raw = "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: message-to-user\n\n"
            + "GOAL: Some goal\n"
            + "STRATEGY: Some strategy\n"
            + "PROGRESS: Some progress\n"
            + "No new decisions this turn.\n";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);
        ResponseDirective.DecisionState ds = parsed.getDirective().getDecisionState();

        assertNotNull(ds);
        assertEquals("No new decisions this turn.", ds.getNoNewDecisionMarker());
        assertFalse(ds.isHasNewDecision());
        assertTrue(ds.getReferences().isEmpty());
        }

        @Test
        void parsesDecisionCitationTriple() {
        String raw = "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: message-to-user\n\n"
            + "GOAL: Compile entities\n"
            + "STRATEGY: Read spec first\n"
            + "PROGRESS: Reviewed requirements\n"
            + "DECISIONS:\n"
            + "- ref: specs/017-gemini-incremental-plan-messages/spec.md | section: ## Requirements | quote: \"acknowledgement-only plugin replies\"\n";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);
        ResponseDirective.DecisionState ds = parsed.getDirective().getDecisionState();

        assertNotNull(ds);
        assertNull(ds.getNoNewDecisionMarker());
        assertTrue(ds.isHasNewDecision());
        assertEquals(1, ds.getReferences().size());

        ResponseDirective.DecisionReference ref = ds.getReferences().get(0);
        assertEquals("specs/017-gemini-incremental-plan-messages/spec.md", ref.getRelativeMarkdownPath());
        assertEquals("## Requirements", ref.getSectionHeading());
        assertEquals("acknowledgement-only plugin replies", ref.getQuotedSubjectPhrase());
        }

        @Test
        void doesNotExtractPlanningFieldsFromToolRequest() {
        String raw = "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: tool-request\n\n"
            + "GOAL: some goal\n"
            + "operation: read_file\nbase: main\npath: entities.md\n";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);
        ResponseDirective directive = parsed.getDirective();

        assertTrue(directive.isValid());
        assertFalse(directive.isNonFinishUserMessage());
        assertNull(directive.getPlanningContent());
    }

    @Test
    void rejectsMultipleDirectiveHeaderBlocksInSingleResponse() {
        String raw = "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: message-to-user\n\n"
            + "GOAL: Inspect project structure\n"
            + "STRATEGY: Start with listings\n"
            + "PROGRESS: Initial planning complete\n\n"
            + "INTENT: waiting-for-next-message\n"
            + "CONTENT_TYPE: tool-request\n\n"
            + "operation: list_files\nbase: target\npath: .\nrecursive: true\n";

        ResponseDirectiveParser.ParseResult parsed = parser.parse(raw);

        assertFalse(parsed.getDirective().isValid());
        assertEquals("Response contains multiple directive header blocks; exactly one message is allowed per turn.",
            parsed.getDirective().getFailureReason());
    }

    @Test
    void canonicalParserExposesRoleHeader() {
        String raw = MessageBuilder.spoofedAssistant("tool-request", "operation: list_files\nbase: main\npath: src");

        var message = parser.parseCanonicalMessage(raw);

        assertTrue(message.isValid());
        assertTrue(message.hasRoleHeader());
        assertTrue(message.isSpoofedAssistantRole());
        assertEquals("assistant", message.getRole());
    }

    @Test
    void canonicalParserHandlesScenarioFixtures() {
        var scenarios = ParsingScenarioFixtures.allScenarios();

        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_assistant_in_process")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_assistant_external")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_user")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("role_unknown")).isValid());
        assertTrue(parser.parseCanonicalMessage(scenarios.get("no_role")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_intent")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_content_type")).isValid());
        assertFalse(parser.parseCanonicalMessage(scenarios.get("missing_blank_line")).isValid());
    }
}
