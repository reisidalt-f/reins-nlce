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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ReasoningTurn is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reasoning turn.
 */
public class ReasoningTurn {
    /**
     * Direction is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing direction.
     */
    public enum Direction {
        OUTBOUND,
        INBOUND
    }

    private int sequence;
    private Direction direction;
    private String payload;
    private ResponseDirective directive;
    private List<AttachedFilePayload> attachedFiles = new ArrayList<>();
    private ToolExecutionResult toolResult;
    private Instant timestamp = Instant.now();
    private int turnIndex;
    private int attemptIndex;
    private boolean fulfilled;
    private String failureClass = "none";

    /**
     * Gets the sequence.
     *
     * @return the numeric value
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Sets the sequence.
     *
     * @param sequence the sequence
     */
    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    /**
     * Gets the direction.
     *
     * @return the resolved or constructed object
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the direction.
     *
     * @param direction the direction
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Gets the payload.
     *
     * @return the string result
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the payload.
     *
     * @param payload the message payload text
     */
    public void setPayload(String payload) {
        this.payload = payload;
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
     * Sets the directive.
     *
     * @param directive the directive
     */
    public void setDirective(ResponseDirective directive) {
        this.directive = directive;
    }

    /**
     * Gets the attached files.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getAttachedFiles() {
        return attachedFiles;
    }

    /**
     * Sets the attached files.
     *
     * @param attachedFiles the attached files
     */
    public void setAttachedFiles(List<AttachedFilePayload> attachedFiles) {
        this.attachedFiles = attachedFiles;
    }

    /**
     * Gets the tool result.
     *
     * @return the resulting result
     */
    public ToolExecutionResult getToolResult() {
        return toolResult;
    }

    /**
     * Sets the tool result.
     *
     * @param toolResult the tool result
     */
    public void setToolResult(ToolExecutionResult toolResult) {
        this.toolResult = toolResult;
    }

    /**
     * Gets the timestamp.
     *
     * @return the resolved or constructed object
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the turn index.
     *
     * @return the numeric value
     */
    public int getTurnIndex() {
        return turnIndex;
    }

    /**
     * Sets the turn index.
     *
     * @param turnIndex the turn index
     */
    public void setTurnIndex(int turnIndex) {
        this.turnIndex = turnIndex;
    }

    /**
     * Gets the attempt index.
     *
     * @return the numeric value
     */
    public int getAttemptIndex() {
        return attemptIndex;
    }

    /**
     * Sets the attempt index.
     *
     * @param attemptIndex the attempt index
     */
    public void setAttemptIndex(int attemptIndex) {
        this.attemptIndex = attemptIndex;
    }

    /**
     * Checks if the component is fulfilled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFulfilled() {
        return fulfilled;
    }

    /**
     * Sets the fulfilled.
     *
     * @param fulfilled the fulfilled
     */
    public void setFulfilled(boolean fulfilled) {
        this.fulfilled = fulfilled;
    }

    /**
     * Gets the failure class.
     *
     * @return the string result
     */
    public String getFailureClass() {
        return failureClass;
    }

    /**
     * Sets the failure class.
     *
     * @param failureClass the failure class
     */
    public void setFailureClass(String failureClass) {
        this.failureClass = failureClass;
    }
}
