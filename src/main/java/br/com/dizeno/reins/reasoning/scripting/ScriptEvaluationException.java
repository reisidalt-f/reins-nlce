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

package br.com.dizeno.reins.reasoning.scripting;

 
/**
 * ScriptEvaluationException is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class ScriptEvaluationException extends Exception {

    private final String scriptName;
    private final PhaseType phaseType;
    private final String resolvedLocation;
    private final ScriptFailureCategory failureCategory;
    private final String sourceIdentifier;
    private final Integer sourceIndex;
    private final ScriptTerminalReason terminalReason;

    /**
     * Constructs a new instance of {@link ScriptEvaluationException}.
     *
     * @param scriptName the script name
     * @param phaseType the phase type
     * @param resolvedLocation the resolved location
     * @param message the message content
     * @param cause the cause
     */
    public ScriptEvaluationException(String scriptName,
                                     PhaseType phaseType,
                                     String resolvedLocation,
                                     String message,
                                     Throwable cause) {
        this(scriptName, phaseType, resolvedLocation, message, cause,
            ScriptFailureCategory.EVALUATION_FAILURE, null, null, ScriptTerminalReason.FATAL_ERROR);
        }

        /**
         * Constructs a new instance of {@link ScriptEvaluationException}.
         *
         * @param scriptName the script name
         * @param phaseType the phase type
         * @param resolvedLocation the resolved location
         * @param message the message content
         * @param cause the cause
         * @param failureCategory the failure category
         * @param sourceIdentifier the source identifier
         * @param sourceIndex the source index
         * @param terminalReason the terminal reason
         */
        public ScriptEvaluationException(String scriptName,
                         PhaseType phaseType,
                         String resolvedLocation,
                         String message,
                         Throwable cause,
                         ScriptFailureCategory failureCategory,
                         String sourceIdentifier,
                         Integer sourceIndex,
                         ScriptTerminalReason terminalReason) {
        super(message, cause);
        this.scriptName = scriptName;
        this.phaseType = phaseType;
        this.resolvedLocation = resolvedLocation;
        this.failureCategory = failureCategory;
        this.sourceIdentifier = sourceIdentifier;
        this.sourceIndex = sourceIndex;
        this.terminalReason = terminalReason;
    }

     
    /**
     * Gets the script name.
     *
     * @return the string result
     */
    public String getScriptName() {
        return scriptName;
    }

     
    /**
     * Gets the phase type.
     *
     * @return the collection of elements
     */
    public PhaseType getPhaseType() {
        return phaseType;
    }

    /**
     * Gets the resolved location.
     *
     * @return the string result
     */
    public String getResolvedLocation() {
        return resolvedLocation;
    }

    /**
     * Gets the failure category.
     *
     * @return the resolved or constructed object
     */
    public ScriptFailureCategory getFailureCategory() {
        return failureCategory;
    }

    /**
     * Gets the source identifier.
     *
     * @return the string result
     */
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Gets the source index.
     *
     * @return the numeric value
     */
    public Integer getSourceIndex() {
        return sourceIndex;
    }

    /**
     * Gets the terminal reason.
     *
     * @return the resolved or constructed object
     */
    public ScriptTerminalReason getTerminalReason() {
        return terminalReason;
    }
}
