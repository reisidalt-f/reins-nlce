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
        private final List<ParseResult> blocks;

        /**
         * Constructs a new instance of {@link ParseResult}.
         *
         * @param directive the directive
         * @param body      the body
         */
        public ParseResult(ResponseDirective directive, String body) {
            this(directive, body, null);
        }

        /**
         * Constructs a new instance of {@link ParseResult} with block list.
         *
         * @param directive the directive
         * @param body      the body
         * @param blocks    the list of parsed blocks
         */
        public ParseResult(ResponseDirective directive, String body, List<ParseResult> blocks) {
            this.directive = directive;
            this.body = body;
            this.blocks = blocks != null ? blocks : List.of(this);
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

        /**
         * Gets all parsed blocks from the raw response.
         *
         * @return list of parsed blocks
         */
        public List<ParseResult> getBlocks() {
            return blocks;
        }
    }

    /**
     * Splits a raw response string into individual block strings based on block header boundaries.
     *
     * @param rawResponse the raw model response string
     * @return list of raw block strings
     */
    public List<String> splitBlocks(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return List.of();
        }
        String[] parts = rawResponse.split("(?=(?:^|\\n\\s*\\n)\\s*(?:ROLE|INTENT|CONTENT_TYPE|GOTO_PHASE|GOTO-PHASE)\\s*:)");
        List<String> blocks = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                splitEmbeddedBlocks(trimmed, blocks);
            }
        }
        while (blocks.size() > 1 && !startsWithBlockHeaders(blocks.get(0))) {
            blocks.remove(0);
        }
        if (blocks.isEmpty()) {
            blocks.add(rawResponse);
        }
        return blocks;
    }

    private void splitEmbeddedBlocks(String blockStr, List<String> outBlocks) {
        String[] sections = blockStr.split("\\r?\\n\\r?\\n", 2);
        if (sections.length < 2) {
            outBlocks.add(blockStr);
            return;
        }

        String headerBlock = sections[0];
        String bodyBlock = sections[1];

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "(?:^|\\n|\\s{2,})\\s*(?:ROLE|INTENT|GOTO_PHASE|GOTO-PHASE)\\s*:"
        ).matcher(bodyBlock);

        if (matcher.find()) {
            int matchStart = matcher.start();
            String firstBody = bodyBlock.substring(0, matchStart).trim();
            String firstBlockStr = headerBlock + "\n\n" + firstBody;
            outBlocks.add(firstBlockStr.trim());

            String remainingStr = bodyBlock.substring(matchStart).trim();
            splitEmbeddedBlocks(remainingStr, outBlocks);
        } else {
            outBlocks.add(blockStr);
        }
    }

    private boolean startsWithBlockHeaders(String blockStr) {
        if (blockStr == null || blockStr.isBlank()) {
            return false;
        }
        String firstLine = blockStr.lines().findFirst().orElse("").trim();
        return firstLine.startsWith("ROLE:")
                || firstLine.startsWith("INTENT:")
                || firstLine.startsWith("CONTENT_TYPE:")
                || firstLine.startsWith("GOTO_PHASE:")
                || firstLine.startsWith("GOTO-PHASE:")
                || containsHeaderKey(firstLine);
    }

    private boolean containsHeaderKey(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("intent:")
                || lower.contains("content_type:")
                || lower.contains("role:")
                || lower.contains("goto_phase:")
                || lower.contains("goto-phase:");
    }

    /**
     * Parse single block.
     *
     * @param blockStr the single block raw string
     * @return the resulting ParseResult for this block
     */
    public ParseResult parseSingleBlock(String blockStr) {
        ResponseDirective directive = new ResponseDirective();
        if (blockStr == null || blockStr.isBlank()) {
            directive.setValid(false);
            directive.setFailureReason("Empty response.");
            return new ParseResult(directive, "");
        }

        String[] sections = blockStr.split("\\r?\\n\\r?\\n", 2);
        if (sections.length < 2) {
            directive.setValid(false);
            directive.setFailureReason("Missing required blank line between headers and body.");
            return new ParseResult(directive, blockStr);
        }

        String headerBlock = sections[0];
        String body = sections[1];
        Map<String, String> headers = parseHeaders(headerBlock);
        directive.setRawHeaders(headers);

        String gotoPhaseHeader = headers.get("goto_phase");
        if (gotoPhaseHeader == null) {
            gotoPhaseHeader = headers.get("goto-phase");
        }
        String targetPhaseHeader = headers.get("target_phase");
        if (targetPhaseHeader == null) {
            targetPhaseHeader = headers.get("target-phase");
        }
        String targetPhaseValue = gotoPhaseHeader != null ? gotoPhaseHeader : targetPhaseHeader;

        String intentRaw = headers.get("intent");
        if (intentRaw == null && gotoPhaseHeader != null) {
            intentRaw = "goto-phase";
        }
        String contentTypeRaw = headers.get("content_type");
        if (contentTypeRaw == null && "goto-phase".equalsIgnoreCase(intentRaw)) {
            contentTypeRaw = "user-progress";
        }

        if (intentRaw == null || contentTypeRaw == null) {
            directive.setValid(false);
            directive.setFailureReason("Missing INTENT or CONTENT_TYPE header.");
            return new ParseResult(directive, body);
        }

        try {
            directive.setIntent(parseIntent(intentRaw));
            directive.setContentType(parseContentType(contentTypeRaw));
            if (targetPhaseValue != null && !targetPhaseValue.isBlank()) {
                directive.setTargetPhase(targetPhaseValue.trim());
            }
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
     * Parses all blocks in rawResponse.
     *
     * @param rawResponse the raw response string
     * @return list of parsed block results
     */
    public List<ParseResult> parseAll(String rawResponse) {
        List<String> rawBlocks = splitBlocks(rawResponse);
        if (rawBlocks.isEmpty()) {
            ResponseDirective directive = new ResponseDirective();
            directive.setValid(false);
            directive.setFailureReason("Empty response.");
            return List.of(new ParseResult(directive, ""));
        }
        List<ParseResult> results = new ArrayList<>();
        for (String rawBlock : rawBlocks) {
            results.add(parseSingleBlock(rawBlock));
        }
        return results;
    }

    /**
     * Parse.
     *
     * @param rawResponse the raw response
     * @return the resulting result
     */
    public ParseResult parse(String rawResponse) {
        List<ParseResult> blocks = parseAll(rawResponse);
        if (blocks.isEmpty()) {
            ResponseDirective directive = new ResponseDirective();
            directive.setValid(false);
            directive.setFailureReason("Empty response.");
            return new ParseResult(directive, "", List.of());
        }

        for (ParseResult b : blocks) {
            if (!b.getDirective().isValid()) {
                return new ParseResult(b.getDirective(), b.getBody(), blocks);
            }
        }

        ParseResult primary = selectPrimaryBlock(blocks);
        return new ParseResult(primary.getDirective(), primary.getBody(), blocks);
    }

    private ParseResult selectPrimaryBlock(List<ParseResult> blocks) {
        if (blocks.size() == 1) {
            return blocks.get(0);
        }
        for (ParseResult b : blocks) {
            if (b.getDirective().getContentType() == ResponseDirective.ContentType.TOOL_REQUEST) {
                return b;
            }
        }
        for (ParseResult b : blocks) {
            if (b.getDirective().isFinishIntent()) {
                return b;
            }
        }
        return blocks.get(blocks.size() - 1);
    }

    /**
     * Parse Canonical Message.
     *
     * @param rawMessage the raw message
     * @return the resolved or constructed object
     */
    public PipelineExchangeMessage parseCanonicalMessage(String rawMessage) {
        List<ParseResult> blocks = parseAll(rawMessage);
        if (blocks.isEmpty()) {
            return PipelineExchangeMessage.invalid("", "Empty response.");
        }
        ParseResult primary = selectPrimaryBlock(blocks);
        ResponseDirective directive = primary.getDirective();
        if (!directive.isValid()) {
            return PipelineExchangeMessage.invalid(primary.getBody(), directive.getFailureReason());
        }

        String intent = directive.getRawHeaders().getOrDefault("intent", "");
        String contentType = directive.getRawHeaders().getOrDefault("content_type", "");
        String body = primary.getBody();

        return new PipelineExchangeMessage(
                intent.trim(),
                contentType.trim(),
                directive.getRawHeaders(),
                body,
                isMessageToModel(contentType) ? body : null,
                isUserProgress(contentType) || isMessageToUser(contentType) || isConversationSummary(contentType) ? body : null,
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
            int spaceIdx = key.lastIndexOf(' ');
            if (spaceIdx >= 0) {
                String subKey = key.substring(spaceIdx + 1);
                if (isKnownHeaderKey(subKey)) {
                    key = subKey;
                }
            }
            String value = line.substring(separator + 1).trim();
            headers.put(key, value);
        }
        return headers;
    }

    private boolean isKnownHeaderKey(String key) {
        return "intent".equals(key) || "content_type".equals(key) || "role".equals(key)
                || "goto_phase".equals(key) || "goto-phase".equals(key)
                || "target_phase".equals(key) || "target-phase".equals(key);
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
            case "goto-phase", "goto_phase" -> ResponseDirective.Intent.GOTO_PHASE;
            default -> throw new IllegalArgumentException("Unsupported INTENT value: " + value);
        };
    }

    private ResponseDirective.ContentType parseContentType(String value) {
        return switch (unquote(value).toLowerCase(Locale.ROOT)) {
            case "message-to-user", "message-to-model", "user-progress" -> ResponseDirective.ContentType.MESSAGE_TO_USER;
            case "conversation-summary", "summary" -> ResponseDirective.ContentType.CONVERSATION_SUMMARY;
            case "tool-request" -> ResponseDirective.ContentType.TOOL_REQUEST;
            default -> throw new IllegalArgumentException("Unsupported CONTENT_TYPE value: " + value);
        };
    }

    private boolean isMessageToModel(String contentType) {
        return "message-to-model".equalsIgnoreCase(contentType);
    }

    private boolean isUserProgress(String contentType) {
        return "user-progress".equalsIgnoreCase(contentType);
    }

    private boolean isMessageToUser(String contentType) {
        return "message-to-user".equalsIgnoreCase(contentType);
    }

    private boolean isConversationSummary(String contentType) {
        return "conversation-summary".equalsIgnoreCase(contentType) || "summary".equalsIgnoreCase(contentType);
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
