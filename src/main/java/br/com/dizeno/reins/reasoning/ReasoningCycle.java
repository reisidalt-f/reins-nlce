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

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ReasoningCycle is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reasoning cycle.
 */
public class ReasoningCycle {
    /**
     * Status is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing status.
     */
    public enum Status {
        ACTIVE,
        FINISHED_SUCCESS,
        FINISHED_ERROR
    }

    private String cycleId;
    private Status status = Status.ACTIVE;
    private List<ReasoningTurn> turns = new ArrayList<>();
    private int maxTurns;
    private BasePathMappingSet baseMappings;
    private Instant createdAt = Instant.now();
    private Instant completedAt;
    private boolean graceTurnUsed;
    private int maxTurnAt;
    private int closedTurnCount;
    private int currentTurnIndex = 1;
    private int retryStreakCount;
    private String fixedProviderId;

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
     * Gets the status.
     *
     * @return the resulting status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the turns.
     *
     * @return the collection of elements
     */
    public List<ReasoningTurn> getTurns() {
        return turns;
    }

    /**
     * Sets the turns.
     *
     * @param turns the turns
     */
    public void setTurns(List<ReasoningTurn> turns) {
        this.turns = turns;
    }

    /**
     * Gets the max turns.
     *
     * @return the numeric value
     */
    public int getMaxTurns() {
        return maxTurns;
    }

    /**
     * Sets the max turns.
     *
     * @param maxTurns the maximum turn limit for the reasoning cycle
     */
    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    /**
     * Gets the base mappings.
     *
     * @return the collection of elements
     */
    public BasePathMappingSet getBaseMappings() {
        return baseMappings;
    }

    /**
     * Sets the base mappings.
     *
     * @param baseMappings the base mappings
     */
    public void setBaseMappings(BasePathMappingSet baseMappings) {
        this.baseMappings = baseMappings;
    }

    /**
     * Gets the created at.
     *
     * @return the resolved or constructed object
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the created at.
     *
     * @param createdAt the created at
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the completed at.
     *
     * @return the resolved or constructed object
     */
    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Sets the completed at.
     *
     * @param completedAt the completed at
     */
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
     * Gets the max turn at.
     *
     * @return the numeric value
     */
    public int getMaxTurnAt() {
        return maxTurnAt;
    }

    /**
     * Sets the max turn at.
     *
     * @param maxTurnAt the max turn at
     */
    public void setMaxTurnAt(int maxTurnAt) {
        this.maxTurnAt = maxTurnAt;
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
     * Gets the current turn index.
     *
     * @return the numeric value
     */
    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    /**
     * Sets the current turn index.
     *
     * @param currentTurnIndex the current turn index
     */
    public void setCurrentTurnIndex(int currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
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
     * Increment Retry Streak Count.
     *
     */
    public void incrementRetryStreakCount() {
        this.retryStreakCount++;
    }

    /**
     * Reset Retry Streak Count.
     *
     */
    public void resetRetryStreakCount() {
        this.retryStreakCount = 0;
    }

    /**
     * Gets the fixed provider id.
     *
     * @return the string result
     */
    public String getFixedProviderId() {
        return fixedProviderId;
    }

    /**
     * Sets the fixed provider id.
     *
     * @param fixedProviderId the fixed provider id
     */
    public void setFixedProviderId(String fixedProviderId) {
        this.fixedProviderId = fixedProviderId;
    }
}
