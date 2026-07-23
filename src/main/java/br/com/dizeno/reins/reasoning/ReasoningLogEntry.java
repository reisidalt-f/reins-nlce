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

 
/**
 * ReasoningLogEntry is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ReasoningLogEntry {
    /**
     * Direction is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing direction.
     */
    public enum Direction {
        OUTBOUND,
        INBOUND
    }

    private Instant timestamp;
    private Direction direction;
    private String role;
    private int sequence;
    private String body;
    private String pipelinePhase;
    private String pipelineOutcome;

    /**
     * Constructs a new instance of {@link ReasoningLogEntry}.
     */
    public ReasoningLogEntry() {
    }

    /**
     * Constructs a new instance of {@link ReasoningLogEntry}.
     *
     * @param timestamp the timestamp
     * @param direction the direction
     * @param role the role
     * @param sequence the sequence
     * @param body the body
     */
    public ReasoningLogEntry(Instant timestamp, Direction direction, String role, int sequence, String body) {
        this.timestamp = timestamp;
        this.direction = direction;
        this.role = role;
        this.sequence = sequence;
        this.body = body;
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
     * Gets the role.
     *
     * @return the string result
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role the role
     */
    public void setRole(String role) {
        this.role = role;
    }

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
     * Gets the body.
     *
     * @return the string result
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body.
     *
     * @param body the body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Gets the pipeline phase.
     *
     * @return the string result
     */
    public String getPipelinePhase() {
        return pipelinePhase;
    }

    /**
     * Sets the pipeline phase.
     *
     * @param pipelinePhase the pipeline phase
     */
    public void setPipelinePhase(String pipelinePhase) {
        this.pipelinePhase = pipelinePhase;
    }

    /**
     * Gets the pipeline outcome.
     *
     * @return the string result
     */
    public String getPipelineOutcome() {
        return pipelineOutcome;
    }

    /**
     * Sets the pipeline outcome.
     *
     * @param pipelineOutcome the pipeline outcome
     */
    public void setPipelineOutcome(String pipelineOutcome) {
        this.pipelineOutcome = pipelineOutcome;
    }

    /**
     * Ordering Key.
     *
     * @return the string result
     */
    public String orderingKey() {
        return direction + ":" + sequence;
    }
}
