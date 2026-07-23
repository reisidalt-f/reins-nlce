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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ReasoningCycleStateManager is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reasoning cycle state manager.
 */
public class ReasoningCycleStateManager {
    private final ReasoningLogService inferenceLogService;

    /**
     * Constructs a new instance of {@link ReasoningCycleStateManager}.
     *
     * @param inferenceLogService the inference log service
     */
    public ReasoningCycleStateManager(ReasoningLogService inferenceLogService) {
        this.inferenceLogService = inferenceLogService;
    }

    /**
     * Builds the configured target result.
     *
     * @param cycle the cycle
     * @param finalIntent the final intent
     * @param terminalReasonCode the terminal reason code
     * @param terminalReasonMessage the terminal reason message
     * @param userFacingMessages the user facing messages
     * @param toolInfoPhrases the tool info phrases
     * @param writtenPaths the written paths
     * @param inspectedPaths the inspected paths
     * @param writtenMtimes the written mtimes
     * @param inspectedMtimes the inspected mtimes
     * @param readMarkdownPaths the read markdown paths
     * @param firstTurnReferenceTree the first turn reference tree
     * @return the resulting result
     */
    public ReasoningResult buildResult(ReasoningCycle cycle,
                                       ResponseDirective.Intent finalIntent,
                                       String terminalReasonCode,
                                       String terminalReasonMessage,
                                       List<String> userFacingMessages,
                                       List<String> toolInfoPhrases,
                                       List<String> writtenPaths,
                                       List<String> inspectedPaths,
                                       Map<String, Long> writtenMtimes,
                                       Map<String, Long> inspectedMtimes,
                                       List<String> readMarkdownPaths,
                                       String firstTurnReferenceTree) {
        ReasoningResult result = new ReasoningResult();
        result.setCycleId(cycle.getCycleId());
        result.setTurnCount(cycle.getClosedTurnCount());
        result.setClosedTurnCount(cycle.getClosedTurnCount());
        result.setRetryStreakCount(cycle.getRetryStreakCount());
        result.setFinalIntent(finalIntent == null ? null : finalIntent.name().toLowerCase());
        result.setTerminalReasonCode(terminalReasonCode);
        result.setTerminalReasonMessage(terminalReasonMessage);
        result.setGraceTurnUsed(cycle.isGraceTurnUsed());
        result.setUserFacingMessages(userFacingMessages == null ? List.of() : new ArrayList<>(userFacingMessages));
        result.setToolInfoPhrases(toolInfoPhrases == null ? List.of() : new ArrayList<>(toolInfoPhrases));
        result.setWrittenPaths(writtenPaths == null ? List.of() : writtenPaths);
        result.setInspectedPaths(inspectedPaths);
        result.setWrittenMtimes(writtenMtimes);
        result.setInspectedMtimes(inspectedMtimes);
        result.setReadMarkdownPaths(readMarkdownPaths);
        result.setFirstTurnReferenceTree(firstTurnReferenceTree);
        return result;
    }

    /**
     * Builds the configured target skipped result.
     *
     * @param cycle the cycle
     * @param reasonCode the reason code
     * @param firstTurnReferenceTree the first turn reference tree
     * @param reason the reason
     * @return the resulting result
     */
    public ReasoningResult buildSkippedResult(ReasoningCycle cycle,
                                              String reasonCode,
                                              String firstTurnReferenceTree,
                                              String reason) {
        cycle.setStatus(ReasoningCycle.Status.FINISHED_SUCCESS);
        cycle.setCompletedAt(Instant.now());
        ReasoningResult result = new ReasoningResult();
        result.setCycleId(cycle.getCycleId());
        result.setTurnCount(0);
        result.setClosedTurnCount(0);
        result.setRetryStreakCount(0);
        result.setFinalIntent("skipped");
        result.setTerminalReasonCode(reasonCode);
        result.setTerminalReasonMessage(reason);
        result.setWrittenPaths(List.of());
        result.setFirstTurnReferenceTree(firstTurnReferenceTree);
        return result;
    }

    /**
     * Emit Lifecycle Message.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param message the message content
     */
    public void emitLifecycleMessage(ReasoningCycleLog cycleLog, String message) {
        writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "lifecycle", 0,
                "lifecycle: " + (message == null ? "" : message));
    }

    /**
     * Write Log.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param dir the dir
     * @param role the role
     * @param seq the seq
     * @param body the body
     */
    public void writeLog(ReasoningCycleLog cycleLog,
                         ReasoningLogEntry.Direction dir,
                         String role,
                         int seq,
                         String body) {
        if (cycleLog == null) {
            return;
        }
        ReasoningLogEntry entry = new ReasoningLogEntry();
        entry.setTimestamp(Instant.now());
        entry.setDirection(dir);
        entry.setRole(role);
        entry.setSequence(seq);
        entry.setBody(body == null ? "" : body);
        inferenceLogService.writeEntry(cycleLog, entry);
    }

    /**
     * Log Attempt Event.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param turn the current turn index
     * @param eventType the event type
     * @param closedTurns the closed turns
     * @param maximumTurns the maximum turns
     * @param retryStreakCount the retry streak count
     * @param message the message content
     */
    public void logAttemptEvent(ReasoningCycleLog cycleLog,
                                ReasoningTurn turn,
                                String eventType,
                                int closedTurns,
                                int maximumTurns,
                                int retryStreakCount,
                                String message) {
        if (cycleLog == null || turn == null) {
            return;
        }
        String body = "eventType=" + eventType + "\n"
                + "turnIndex=" + turn.getTurnIndex() + "\n"
                + "attemptIndex=" + turn.getAttemptIndex() + "\n"
                + "closedTurns=" + closedTurns + "\n"
                + "maxTurns=" + maximumTurns + "\n"
                + "retryStreakCount=" + retryStreakCount + "\n"
                + "message=" + (message == null ? "" : message);
        writeLog(cycleLog, ReasoningLogEntry.Direction.INBOUND, "turn-attempt", 0, body);
    }

    /**
     * Log Operation Phrase.
     *
     * @param phrase the phrase
     * @param request the request containing path and scope metadata
     */
    public void logOperationPhrase(String phrase, ReasoningRequest request) {
        if (request == null || phrase == null || phrase.isBlank()) {
            return;
        }
        Consumer<String> logger = request.getOperationLogger();
        if (logger != null) {
            try {
                logger.accept(phrase);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Close Cycle Log.
     *
     * @param cycleLog the reasoning cycle log instance
     */
    public void closeCycleLog(ReasoningCycleLog cycleLog) {
        if (cycleLog != null) {
            inferenceLogService.closeCycleLog(cycleLog);
        }
    }

    /**
     * Ensure Fixed Provider.
     *
     * @param cycle the cycle
     * @param providerId the provider id
     */
    public void ensureFixedProvider(ReasoningCycle cycle, String providerId) {
        if (cycle == null || providerId == null || providerId.isBlank()) {
            return;
        }
        if (cycle.getFixedProviderId() == null || cycle.getFixedProviderId().isBlank()) {
            cycle.setFixedProviderId(providerId);
            return;
        }
        if (!cycle.getFixedProviderId().equals(providerId)) {
            throw new IllegalStateException("Provider changed during reasoning cycle: "
                    + cycle.getFixedProviderId() + " -> " + providerId);
        }
    }
}
