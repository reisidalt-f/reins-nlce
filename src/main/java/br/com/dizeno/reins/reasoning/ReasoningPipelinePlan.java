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
 * ReasoningPipelinePlan is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing inference pipeline plan.
 */
public class ReasoningPipelinePlan {
    /**
     * PlanStatus is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
     * Acts as a component managing plan status.
     */
    public enum PlanStatus {
        VALID,
        INVALID,
        EMPTY
    }

    private final String sourcePath;
    private final List<ReasoningPhaseDescriptor> phases;
    private final PlanStatus planStatus;

    /**
     * Constructs a new instance of {@link ReasoningPipelinePlan}.
     *
     * @param sourcePath the path of the source file
     * @param phases the phases
     * @param planStatus the plan status
     */
    public ReasoningPipelinePlan(String sourcePath,
                                 List<ReasoningPhaseDescriptor> phases,
                                 PlanStatus planStatus) {
        this.sourcePath = sourcePath;
        this.phases = phases == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(phases));
        this.planStatus = planStatus == null ? PlanStatus.INVALID : planStatus;
    }

    /**
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Gets the phases.
     *
     * @return the collection of elements
     */
    public List<ReasoningPhaseDescriptor> getPhases() {
        return phases;
    }

    /**
     * Gets the plan status.
     *
     * @return the resulting status
     */
    public PlanStatus getPlanStatus() {
        return planStatus;
    }

    /**
     * Checks if the component is empty.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEmpty() {
        return planStatus == PlanStatus.EMPTY || phases.isEmpty();
    }

    /**
     * Checks if the component is valid.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValid() {
        return planStatus == PlanStatus.VALID;
    }
}