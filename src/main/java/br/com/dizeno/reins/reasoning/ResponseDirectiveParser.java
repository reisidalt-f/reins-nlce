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

import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * ResponseDirectiveParser is part of the orchestration of conversational
 * reasoning loops, prompt construction, and tool instruction mapping in the
 * reins architecture.
 * Acts as a component managing response directive parser.
 */
public class ResponseDirectiveParser {
    /**
     * ParseResult is part of the orchestration of conversational reasoning loops,
     * prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public static class ParseResult {
        private final ResponseDirective directive;
        private final String body;

        /**
         * Constructs a new instance of {@link ParseResult}.
         *
         * @param directive the directive
         * @param body      the body
         */
        public ParseResult(ResponseDirective directive, String body) {
            this.directive = directive;
            this.body = body;
        }

        /**
         * Gets the directive.
         *
         * @return the resolved or constructed object
         */
        public ResponseDirective getDirective() {
            return directive;
        }

        /**
         * Gets the body.
         *
         * @return the string result
         */
        public String getBody() {
            return body;
        }
    }

    /**
     * Parse.
     *
     * @param rawResponse the raw response
     * @return the resulting result
     */
    public ParseResult parse(String rawResponse) {
        ResponseDirective directive = new ResponseDirective();
        if (rawResponse == null || rawResponse.isBlank()) {
            directive.setValid(false);
            directive.setFailureReason("Empty response.");
            return new ParseResult(directive, "");
        }

        String[] sections = rawResponse.split("\\r?\\n\\r?\\n", 2);
        if (sections.length < 2) {
            directive.setValid(false);
            directive.setFailureReason("Missing required blank line between headers and body.");
            return new ParseResult(directive, rawResponse);
        }

        String headerBlock = sections[0];
        String body = sections[1];
        if (containsEmbeddedDirectiveHeaders(body)) {
            directive.setValid(false);
            directive.setFailureReason(
                    "Response contains multiple directive header blocks; exactly one message is allowed per turn.");
            return new ParseResult(directive, body);
        }
        Map<String, String> headers = parseHeaders(headerBlock);
        directive.setRawHeaders(headers);

        String intentRaw = headers.get("intent");
        String contentTypeRaw = headers.get("content_type");
        if (intentRaw == null || contentTypeRaw == null) {
            directive.setValid(false);
            directive.setFailureReason("Missing INTENT or CONTENT_TYPE header.");
            return new ParseResult(directive, body);
        }

        try {
            directive.setIntent(parseIntent(intentRaw));
            directive.setContentType(parseContentType(contentTypeRaw));
            directive.setValid(true);
        } catch (IllegalArgumentException ex) {
            directive.setValid(false);
            directive.setFailureReason(ex.getMessage());
        }

        if (directive.isValid() && directive.isNonFinishUserMessage()) {
            extractPlanningContent(directive, body);
        }

        return new ParseResult(directive, body);
    }

    /**
     * Parse Canonical Message.
     *
     * @param rawMessage the raw message
     * @return the resolved or constructed object
     */
    public PipelineExchangeMessage parseCanonicalMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return PipelineExchangeMessage.invalid("", "Empty response.");
        }

        String[] sections = rawMessage.split("\\r?\\n\\r?\\n", 2);
        if (sections.length < 2) {
            return PipelineExchangeMessage.invalid(rawMessage, "Missing required blank line between headers and body.");
        }

        Map<String, String> headers = parseHeaders(sections[0]);
        String intent = headers.get("intent");
        String contentType = headers.get("content_type");
        String body = sections[1];
        if (containsEmbeddedDirectiveHeaders(body)) {
            return PipelineExchangeMessage.invalid(body,
                    "Response contains multiple directive header blocks; exactly one message is allowed per turn.");
        }
        if (intent == null || contentType == null) {
            return PipelineExchangeMessage.invalid(body, "Missing INTENT or CONTENT_TYPE header.");
        }
        try {
            parseIntent(intent);
        } catch (IllegalArgumentException ex) {
            return PipelineExchangeMessage.invalid(body, ex.getMessage());
        }
        return new PipelineExchangeMessage(
                intent.trim(),
                contentType.trim(),
                headers,
                body,
                isGeminiMessage(contentType) ? body : null,
                isUserProgress(contentType) || isMessageToUser(contentType) ? body : null,
                true,
                null);
    }

    private Map<String, String> parseHeaders(String headerBlock) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = headerBlock.split("\\r?\\n");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            headers.put(key, value);
        }
        return headers;
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ResponseDirective.Intent parseIntent(String value) {
        return switch (unquote(value).toLowerCase(Locale.ROOT)) {
            case "finish-success" -> ResponseDirective.Intent.FINISH_SUCCESS;
            case "finish-error" -> ResponseDirective.Intent.FINISH_ERROR;
            case "waiting-for-next-message" -> ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE;
            default -> throw new IllegalArgumentException("Unsupported INTENT value: " + value);
        };
    }

    private ResponseDirective.ContentType parseContentType(String value) {
        return switch (unquote(value).toLowerCase(Locale.ROOT)) {
            case "message-to-user" -> ResponseDirective.ContentType.MESSAGE_TO_USER;
            case "tool-request", "mcp-request" -> ResponseDirective.ContentType.TOOL_REQUEST;
            default -> throw new IllegalArgumentException("Unsupported CONTENT_TYPE value: " + value);
        };
    }

    private boolean isGeminiMessage(String contentType) {
        return "gemini-message".equalsIgnoreCase(contentType);
    }

    private boolean isUserProgress(String contentType) {
        return "user-progress".equalsIgnoreCase(contentType);
    }

    private boolean isMessageToUser(String contentType) {
        return "message-to-user".equalsIgnoreCase(contentType);
    }

    private boolean containsEmbeddedDirectiveHeaders(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.replace("\r\n", "\n");
        return normalized.matches("(?s).*(^|\\n)INTENT\\s*:.*\\nCONTENT_TYPE\\s*:.*");
    }

    private void extractPlanningContent(ResponseDirective directive, String body) {
        if (body == null || body.isBlank()) {
            return;
        }

        ResponseDirective.PlanningContent planning = new ResponseDirective.PlanningContent();
        ResponseDirective.DecisionState decisionState = new ResponseDirective.DecisionState();
        List<ResponseDirective.IntermediateGoal> goals = new ArrayList<>();

        String[] lines = body.split("\\r?\\n");
        String currentLabel = null;
        StringBuilder currentValue = new StringBuilder();
        List<String> decisionsSection = new ArrayList<>();
        List<String> goalsSection = new ArrayList<>();
        boolean inDecisions = false;
        boolean inGoals = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.toUpperCase(Locale.ROOT).startsWith("GOAL:")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = "GOAL";
                currentValue = new StringBuilder(trimmed.substring(5).trim());
                inDecisions = false;
                inGoals = false;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("STRATEGY:")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = "STRATEGY";
                currentValue = new StringBuilder(trimmed.substring(9).trim());
                inDecisions = false;
                inGoals = false;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("PROGRESS:")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = "PROGRESS";
                currentValue = new StringBuilder(trimmed.substring(9).trim());
                inDecisions = false;
                inGoals = false;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("DECISIONS:")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = null;
                currentValue = new StringBuilder();
                inDecisions = true;
                inGoals = false;
            } else if (trimmed.toUpperCase(Locale.ROOT).startsWith("INTERMEDIATE_GOALS:")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = null;
                currentValue = new StringBuilder();
                inDecisions = false;
                inGoals = true;
            } else if (trimmed.equals("No new decisions this turn.")) {
                flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);
                currentLabel = null;
                currentValue = new StringBuilder();
                inDecisions = false;
                inGoals = false;
                decisionState.setNoNewDecisionMarker("No new decisions this turn.");
            } else if (inDecisions) {
                if (!trimmed.isEmpty()) {
                    decisionsSection.add(trimmed);
                }
            } else if (inGoals) {
                if (!trimmed.isEmpty()) {
                    goalsSection.add(trimmed);
                }
            } else if (currentLabel != null) {
                if (currentValue.length() > 0) {
                    currentValue.append(" ");
                }
                currentValue.append(trimmed);
            }
        }
        flushLabel(currentLabel, currentValue.toString().trim(), planning, decisionState);

        List<ResponseDirective.DecisionReference> refs = new ArrayList<>();
        for (String decLine : decisionsSection) {
            String dl = decLine.startsWith("- ") ? decLine.substring(2).trim() : decLine;
            ResponseDirective.DecisionReference ref = parseCitationTriple(dl);
            if (ref != null) {
                refs.add(ref);
            }
        }
        decisionState.setReferences(refs);
        decisionState.setHasNewDecision(!refs.isEmpty()
                || !decisionState.getDecisions().isEmpty()
                || !decisionState.getQuestions().isEmpty());

        ResponseDirective.IntermediateGoal currentGoal = null;
        for (String goalLine : goalsSection) {
            if (goalLine.startsWith("- ") || goalLine.startsWith("GOAL_ITEM:")) {
                currentGoal = parseGoalItem(goalLine);
                if (currentGoal != null) {
                    goals.add(currentGoal);
                }
            } else if ((goalLine.startsWith("TASK ") || goalLine.startsWith("  TASK "))
                    && currentGoal != null) {
                ResponseDirective.GoalTask task = parseGoalTask(goalLine.trim());
                if (task != null) {
                    currentGoal.getTasks().add(task);
                }
            }
        }

        directive.setPlanningContent(planning);
        directive.setDecisionState(decisionState);
        directive.setIntermediateGoals(goals);
    }

    private void flushLabel(String label, String value,
            ResponseDirective.PlanningContent planning,
            ResponseDirective.DecisionState decisionState) {
        if (label == null || value.isBlank()) {
            return;
        }
        switch (label) {
            case "GOAL" -> planning.setGoalSummary(value);
            case "STRATEGY" -> planning.setStrategySummary(value);
            case "PROGRESS" -> planning.setProgressSummary(value);
        }
    }

    private ResponseDirective.DecisionReference parseCitationTriple(String line) {
        if (line == null || !line.toLowerCase(Locale.ROOT).startsWith("ref:")) {
            return null;
        }
        String[] parts = line.split("\\|");
        if (parts.length < 3) {
            return null;
        }

        String pathPart = stripPrefix(parts[0].trim(), "ref:").trim();
        String sectionPart = stripPrefix(parts[1].trim(), "section:").trim();
        String quotePart = stripPrefix(parts[2].trim(), "quote:").trim();
        if (quotePart.startsWith("\"") && quotePart.endsWith("\"") && quotePart.length() > 1) {
            quotePart = quotePart.substring(1, quotePart.length() - 1);
        }
        if (pathPart.isBlank() || sectionPart.isBlank() || quotePart.isBlank()) {
            return null;
        }
        ResponseDirective.DecisionReference ref = new ResponseDirective.DecisionReference();
        ref.setRelativeMarkdownPath(pathPart);
        ref.setSectionHeading(sectionPart);
        ref.setQuotedSubjectPhrase(quotePart);
        return ref;
    }

    private ResponseDirective.IntermediateGoal parseGoalItem(String line) {
        String l = line.startsWith("- ") ? line.substring(2).trim() : line;
        l = stripPrefix(l, "GOAL_ITEM:").trim();
        String[] parts = l.split("\\|");
        if (parts.length < 2) {
            return null;
        }
        ResponseDirective.IntermediateGoal goal = new ResponseDirective.IntermediateGoal();
        goal.setGoalId(parts[0].trim());
        goal.setTitle(parts[1].trim());
        if (parts.length >= 3) {
            try {
                goal.setStatus(ResponseDirective.GoalStatus.valueOf(parts[2].trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                goal.setStatus(ResponseDirective.GoalStatus.PLANNED);
            }
        }
        return goal;
    }

    private ResponseDirective.GoalTask parseGoalTask(String line) {
        String l = stripPrefix(line, "TASK").trim();
        String[] parts = l.split("\\|");
        if (parts.length < 2) {
            return null;
        }
        ResponseDirective.GoalTask task = new ResponseDirective.GoalTask();
        task.setTaskId(parts[0].trim());
        if (parts.length >= 2) {
            try {
                String typeStr = parts[1].trim().toUpperCase(Locale.ROOT);
                if ("MCP_TOOL_REQUEST".equals(typeStr)) {
                    typeStr = "TOOL_REQUEST";
                }
                task.setTaskType(ResponseDirective.TaskType.valueOf(typeStr));
            } catch (IllegalArgumentException ignored) {
                task.setTaskType(ResponseDirective.TaskType.ANALYSIS);
            }
        }
        if (parts.length >= 3) {
            String tool = parts[2].trim();
            task.setReferencedTool(tool.isEmpty() ? null : tool);
        }
        if (parts.length >= 4) {
            task.setDescription(parts[3].trim());
        }
        return task;
    }

    private String stripPrefix(String s, String prefix) {
        String lower = s.toLowerCase(Locale.ROOT);
        String prefixLower = prefix.toLowerCase(Locale.ROOT);
        if (lower.startsWith(prefixLower)) {
            return s.substring(prefix.length());
        }
        return s;
    }
}
