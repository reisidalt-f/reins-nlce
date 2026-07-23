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
 * SkipReinsMessages is part of the general application functions in the reins architecture.
 * Acts as a component managing skip reins messages.
 */
public final class SkipReinsMessages {
    private SkipReinsMessages() {
    }

    /**
     * Skip Message.
     *
     * @param goalName the goal name
     * @param decision the decision
     * @return the string result
     */
    public static String skipMessage(String goalName, SkipDecision decision) {
        String normalized = decision.getNormalizedValue() == null ? "true" : decision.getNormalizedValue();
        return "[skipReins] " + goalName + " skipped (reasonCode=" + decision.getReasonCode()
                + ", value=" + normalized + ").";
    }

    /**
     * Invalid Value Message.
     *
     * @param goalName the goal name
     * @param decision the decision
     * @return the string result
     */
    public static String invalidValueMessage(String goalName, SkipDecision decision) {
        String raw = decision.getRawValue() == null ? "" : decision.getRawValue();
        return "Invalid value for skipReins in " + goalName + ": '" + raw
                + "'. Expected true, false, or empty value via -DskipReins.";
    }
}