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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReasoningResult is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ReasoningResult {
    private String cycleId;
    private int turnCount;
    private String finalIntent;
    private String terminalReasonCode;
    private String terminalReasonMessage;
    private boolean graceTurnUsed;
    private String firstTurnReferenceTree;
    private int closedTurnCount;
    private int retryStreakCount;
    private List<String> userFacingMessages = new ArrayList<>();
    private List<String> toolInfoPhrases = new ArrayList<>();

    /**
     * Gets the cycle id.
     *
     * @return the string result
     */
    public String getCycleId() {
        return cycleId;
    }

    /**
     * Sets the cycle id.
     *
     * @param cycleId the cycle id
     */
    public void setCycleId(String cycleId) {
        this.cycleId = cycleId;
    }

    /**
     * Gets the turn count.
     *
     * @return the numeric value
     */
    public int getTurnCount() {
        return turnCount;
    }

    /**
     * Sets the turn count.
     *
     * @param turnCount the turn count
     */
    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    /**
     * Gets the final intent.
     *
     * @return the string result
     */
    public String getFinalIntent() {
        return finalIntent;
    }

    /**
     * Sets the final intent.
     *
     * @param finalIntent the final intent
     */
    public void setFinalIntent(String finalIntent) {
        this.finalIntent = finalIntent;
    }

    /**
     * Gets the terminal reason code.
     *
     * @return the string result
     */
    public String getTerminalReasonCode() {
        return terminalReasonCode;
    }

    /**
     * Sets the terminal reason code.
     *
     * @param terminalReasonCode the terminal reason code
     */
    public void setTerminalReasonCode(String terminalReasonCode) {
        this.terminalReasonCode = terminalReasonCode;
    }

    /**
     * Gets the terminal reason message.
     *
     * @return the string result
     */
    public String getTerminalReasonMessage() {
        return terminalReasonMessage;
    }

    /**
     * Sets the terminal reason message.
     *
     * @param terminalReasonMessage the terminal reason message
     */
    public void setTerminalReasonMessage(String terminalReasonMessage) {
        this.terminalReasonMessage = terminalReasonMessage;
    }

    /**
     * Checks if the component is grace turn used.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isGraceTurnUsed() {
        return graceTurnUsed;
    }

    /**
     * Sets the grace turn used.
     *
     * @param graceTurnUsed the grace turn used
     */
    public void setGraceTurnUsed(boolean graceTurnUsed) {
        this.graceTurnUsed = graceTurnUsed;
    }

    /**
     * Gets the first turn reference tree.
     *
     * @return the string result
     */
    public String getFirstTurnReferenceTree() {
        return firstTurnReferenceTree;
    }

    /**
     * Sets the first turn reference tree.
     *
     * @param firstTurnReferenceTree the first turn reference tree
     */
    public void setFirstTurnReferenceTree(String firstTurnReferenceTree) {
        this.firstTurnReferenceTree = firstTurnReferenceTree;
    }

    /**
     * Gets the closed turn count.
     *
     * @return the numeric value
     */
    public int getClosedTurnCount() {
        return closedTurnCount;
    }

    /**
     * Sets the closed turn count.
     *
     * @param closedTurnCount the closed turn count
     */
    public void setClosedTurnCount(int closedTurnCount) {
        this.closedTurnCount = closedTurnCount;
    }

    /**
     * Gets the retry streak count.
     *
     * @return the numeric value
     */
    public int getRetryStreakCount() {
        return retryStreakCount;
    }

    /**
     * Sets the retry streak count.
     *
     * @param retryStreakCount the retry streak count
     */
    public void setRetryStreakCount(int retryStreakCount) {
        this.retryStreakCount = Math.max(0, retryStreakCount);
    }

    /**
     * Gets the user facing messages.
     *
     * @return the string result
     */
    public List<String> getUserFacingMessages() {
        return userFacingMessages;
    }

    /**
     * Sets the user facing messages.
     *
     * @param userFacingMessages the user facing messages
     */
    public void setUserFacingMessages(List<String> userFacingMessages) {
        this.userFacingMessages = userFacingMessages;
    }

    /**
     * Gets the tool info phrases.
     *
     * @return the string result
     */
    public List<String> getToolInfoPhrases() {
        return toolInfoPhrases;
    }

    /**
     * Sets the tool info phrases.
     *
     * @param toolInfoPhrases the tool info phrases
     */
    public void setToolInfoPhrases(List<String> toolInfoPhrases) {
        this.toolInfoPhrases = toolInfoPhrases;
    }

    private List<String> writtenPaths = new ArrayList<>();

    /**
     * Gets the written paths.
     *
     * @return the string result
     */
    public List<String> getWrittenPaths() {
        return writtenPaths;
    }

    /**
     * Sets the written paths.
     *
     * @param writtenPaths the written paths
     */
    public void setWrittenPaths(List<String> writtenPaths) {
        this.writtenPaths = writtenPaths == null ? new ArrayList<>() : new ArrayList<>(writtenPaths);
    }

    private List<String> inspectedPaths = new ArrayList<>();

    /**
     * Gets the inspected paths.
     *
     * @return the string result
     */
    public List<String> getInspectedPaths() {
        return inspectedPaths;
    }

    /**
     * Sets the inspected paths.
     *
     * @param readPaths the read paths
     */
    public void setInspectedPaths(List<String> readPaths) {
        this.inspectedPaths = readPaths == null ? new ArrayList<>() : new ArrayList<>(readPaths);
    }

    private Map<String, Long> writtenMtimes = new LinkedHashMap<>();

    /**
     * Gets the written mtimes.
     *
     * @return the string result
     */
    public Map<String, Long> getWrittenMtimes() {
        return writtenMtimes;
    }

    /**
     * Sets the written mtimes.
     *
     * @param writtenMtimes the written mtimes
     */
    public void setWrittenMtimes(Map<String, Long> writtenMtimes) {
        this.writtenMtimes = writtenMtimes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(writtenMtimes);
    }

    private List<String> readMarkdownPaths = new ArrayList<>();

    /**
     * Gets the read markdown paths.
     *
     * @return the string result
     */
    public List<String> getReadMarkdownPaths() {
        return readMarkdownPaths;
    }

    /**
     * Sets the read markdown paths.
     *
     * @param readMarkdownPaths the read markdown paths
     */
    public void setReadMarkdownPaths(List<String> readMarkdownPaths) {
        this.readMarkdownPaths = readMarkdownPaths == null ? new ArrayList<>() : new ArrayList<>(readMarkdownPaths);
    }

    private Map<String, Long> inspectedMtimes = new LinkedHashMap<>();

    /**
     * Gets the inspected mtimes.
     *
     * @return the string result
     */
    public Map<String, Long> getInspectedMtimes() {
        return inspectedMtimes;
    }

    /**
     * Sets the inspected mtimes.
     *
     * @param inspectedMtimes the inspected mtimes
     */
    public void setInspectedMtimes(Map<String, Long> inspectedMtimes) {
        this.inspectedMtimes = inspectedMtimes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inspectedMtimes);
    }
}
