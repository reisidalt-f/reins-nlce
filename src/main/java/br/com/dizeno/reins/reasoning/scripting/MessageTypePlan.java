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
 * MessageTypePlan is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing message type plan.
 */
public class MessageTypePlan {
    private final String stepName;
    private final int ordinal;

    /**
     * Constructs a new instance of {@link MessageTypePlan}.
     *
     * @param stepName the step name
     * @param ordinal the ordinal
     */
    public MessageTypePlan(String stepName, int ordinal) {
        this.stepName = stepName;
        this.ordinal = ordinal;
    }

    /**
     * Gets the step name.
     *
     * @return the string result
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * Gets the ordinal.
     *
     * @return the numeric value
     */
    public int getOrdinal() {
        return ordinal;
    }
}
