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

package br.com.dizeno.reins.reasoning.inference.llm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AdapterValidationResult is part of the general application functions in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class AdapterValidationResult {
    private String providerId;
    private boolean valid;
    private List<String> violations = new ArrayList<>();

    /**
     * Gets the provider id.
     *
     * @return the string result
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the provider id.
     *
     * @param providerId the provider id
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * Checks if the component is valid.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the valid.
     *
     * @param valid the valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the violations.
     *
     * @return the string result
     */
    public List<String> getViolations() {
        return violations;
    }

    /**
     * Sets the violations.
     *
     * @param violations the violations
     */
    public void setViolations(List<String> violations) {
        this.violations = violations == null ? new ArrayList<>() : violations;
    }

    /**
     * Success.
     *
     * @param providerId the provider id
     * @return the resulting result
     */
    public static AdapterValidationResult success(String providerId) {
        AdapterValidationResult result = new AdapterValidationResult();
        result.setProviderId(providerId);
        result.setValid(true);
        return result;
    }

    /**
     * Failure.
     *
     * @param providerId the provider id
     * @param violation the violation
     * @return the resulting result
     */
    public static AdapterValidationResult failure(String providerId, String violation) {
        AdapterValidationResult result = new AdapterValidationResult();
        result.setProviderId(providerId);
        result.setValid(false);
        result.getViolations().add(violation);
        return result;
    }
}
