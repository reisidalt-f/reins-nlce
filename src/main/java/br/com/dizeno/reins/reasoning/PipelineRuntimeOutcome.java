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
import java.util.Collections;
import java.util.List;

/**
 * PipelineRuntimeOutcome is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing pipeline runtime outcome.
 */
public class PipelineRuntimeOutcome {
    /**
     * Status is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
     * Acts as a component managing status.
     */
    public enum Status {
        SUCCESS,
        ERROR,
        SKIPPED
    }

    private final Status status;
    private final String terminalPhase;
    private final String reason;
    private final List<String> warnings;

    /**
     * Constructs a new instance of {@link PipelineRuntimeOutcome}.
     *
     * @param status the status
     * @param terminalPhase the terminal phase
     * @param reason the reason
     * @param warnings the warnings
     */
    public PipelineRuntimeOutcome(Status status, String terminalPhase, String reason, List<String> warnings) {
        this.status = status;
        this.terminalPhase = terminalPhase;
        this.reason = reason;
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * Success.
     *
     * @param terminalPhase the terminal phase
     * @param reason the reason
     * @return the resolved or constructed object
     */
    public static PipelineRuntimeOutcome success(String terminalPhase, String reason) {
        return new PipelineRuntimeOutcome(Status.SUCCESS, terminalPhase, reason, List.of());
    }

    /**
     * Error.
     *
     * @param terminalPhase the terminal phase
     * @param reason the reason
     * @return the resolved or constructed object
     */
    public static PipelineRuntimeOutcome error(String terminalPhase, String reason) {
        return new PipelineRuntimeOutcome(Status.ERROR, terminalPhase, reason, List.of());
    }

    /**
     * Skipped.
     *
     * @param reason the reason
     * @param warnings the warnings
     * @return the resolved or constructed object
     */
    public static PipelineRuntimeOutcome skipped(String reason, List<String> warnings) {
        return new PipelineRuntimeOutcome(Status.SKIPPED, null, reason, warnings);
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
     * Gets the terminal phase.
     *
     * @return the string result
     */
    public String getTerminalPhase() {
        return terminalPhase;
    }

    /**
     * Gets the reason.
     *
     * @return the string result
     */
    public String getReason() {
        return reason;
    }

    /**
     * Gets the warnings.
     *
     * @return the string result
     */
    public List<String> getWarnings() {
        return warnings;
    }
}