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

import java.util.ArrayList;
import java.util.List;

/**
 * ContextStepPlanner is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing context step planner.
 */
public class ContextStepPlanner {
    /**
     * Parse Message Type Plan.
     *
     * @param output the output
     * @return the collection of elements
     */
    public List<MessageTypePlan> parseMessageTypePlan(String output) {
        if (output == null || output.isBlank()) {
            throw new IllegalStateException("Step 'list-message-type' produced empty output.");
        }
        String[] lines = output.split("\\R");
        List<MessageTypePlan> plan = new ArrayList<>();
        int ordinal = 1;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String stepName = line.trim();
            if (stepName.isBlank()) {
                continue;
            }
            plan.add(new MessageTypePlan(stepName, ordinal++));
        }
        if (plan.isEmpty()) {
            throw new IllegalStateException("Step 'list-message-type' produced no message types.");
        }
        return plan;
    }
}
