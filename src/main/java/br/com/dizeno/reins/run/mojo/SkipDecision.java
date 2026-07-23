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

package br.com.dizeno.reins.run.mojo;

/**
 * SkipDecision is part of the general application functions in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public final class SkipDecision {
    /**
     * Decision is part of the general application functions in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public enum Decision {
        EXECUTE,
        SKIP,
        FAIL
    }

    private final Decision decision;
    private final SkipReasonCode reasonCode;
    private final String rawValue;
    private final String normalizedValue;

    private SkipDecision(Decision decision, SkipReasonCode reasonCode, String rawValue, String normalizedValue) {
        this.decision = decision;
        this.reasonCode = reasonCode;
        this.rawValue = rawValue;
        this.normalizedValue = normalizedValue;
    }

    /**
     * Executes the operation.
     *
     * @param rawValue the raw value
     * @param normalizedValue the normalized value
     * @return the resolved or constructed object
     */
    public static SkipDecision execute(String rawValue, String normalizedValue) {
        return new SkipDecision(Decision.EXECUTE, SkipReasonCode.SKIP_DISABLED, rawValue, normalizedValue);
    }

    /**
     * Skip.
     *
     * @param rawValue the raw value
     * @param normalizedValue the normalized value
     * @return the resolved or constructed object
     */
    public static SkipDecision skip(String rawValue, String normalizedValue) {
        return new SkipDecision(Decision.SKIP, SkipReasonCode.SKIP_ENABLED, rawValue, normalizedValue);
    }

    /**
     * Fail.
     *
     * @param rawValue the raw value
     * @return the resolved or constructed object
     */
    public static SkipDecision fail(String rawValue) {
        return new SkipDecision(Decision.FAIL, SkipReasonCode.SKIP_INVALID_VALUE, rawValue, null);
    }

    /**
     * Gets the decision.
     *
     * @return the resolved or constructed object
     */
    public Decision getDecision() {
        return decision;
    }

    /**
     * Gets the reason code.
     *
     * @return the resolved or constructed object
     */
    public SkipReasonCode getReasonCode() {
        return reasonCode;
    }

    /**
     * Gets the raw value.
     *
     * @return the string result
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * Gets the normalized value.
     *
     * @return the string result
     */
    public String getNormalizedValue() {
        return normalizedValue;
    }

    /**
     * Should Skip.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean shouldSkip() {
        return decision == Decision.SKIP;
    }

    /**
     * Should Fail.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean shouldFail() {
        return decision == Decision.FAIL;
    }
}