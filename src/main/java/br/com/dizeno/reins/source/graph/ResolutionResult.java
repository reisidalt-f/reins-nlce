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

package br.com.dizeno.reins.source.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * ResolutionResult is part of the general application functions in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ResolutionResult {
    /**
     * ResolutionStatus is part of the general application functions in the reins architecture.
     * Acts as a component managing resolution status.
     */
    public enum ResolutionStatus {
        RESOLVED,
        UNRESOLVED,
        AMBIGUOUS
    }

    private final ResolutionStatus status;
    private final String resolvedPath;
    private final String winningStrategyId;
    private final List<String> attemptedStrategies;
    private final String diagnostic;
    private final List<String> candidatePaths;

    private ResolutionResult(ResolutionStatus status,
                             String resolvedPath,
                             String winningStrategyId,
                             List<String> attemptedStrategies,
                             String diagnostic,
                             List<String> candidatePaths) {
        this.status = status;
        this.resolvedPath = resolvedPath;
        this.winningStrategyId = winningStrategyId;
        this.attemptedStrategies = List.copyOf(attemptedStrategies);
        this.diagnostic = diagnostic;
        this.candidatePaths = List.copyOf(candidatePaths);
    }

    /**
     * Resolves the configured value or path d.
     *
     * @param resolvedPath the resolved path
     * @param winningStrategyId the winning strategy id
     * @param attemptedStrategies the attempted strategies
     * @param diagnostic the diagnostic
     * @return the resulting result
     */
    public static ResolutionResult resolved(String resolvedPath,
                                            String winningStrategyId,
                                            List<String> attemptedStrategies,
                                            String diagnostic) {
        return new ResolutionResult(ResolutionStatus.RESOLVED, resolvedPath, winningStrategyId,
                new ArrayList<>(attemptedStrategies), diagnostic, List.of());
    }

    /**
     * Unresolved.
     *
     * @param attemptedStrategies the attempted strategies
     * @param diagnostic the diagnostic
     * @return the resulting result
     */
    public static ResolutionResult unresolved(List<String> attemptedStrategies, String diagnostic) {
        return new ResolutionResult(ResolutionStatus.UNRESOLVED, null, null,
                new ArrayList<>(attemptedStrategies), diagnostic, List.of());
    }

    /**
     * Ambiguous.
     *
     * @param attemptedStrategies the attempted strategies
     * @param diagnostic the diagnostic
     * @param candidatePaths the candidate paths
     * @return the resulting result
     */
    public static ResolutionResult ambiguous(List<String> attemptedStrategies,
                                             String diagnostic,
                                             List<String> candidatePaths) {
        return new ResolutionResult(ResolutionStatus.AMBIGUOUS, null, null,
                new ArrayList<>(attemptedStrategies), diagnostic, new ArrayList<>(candidatePaths));
    }

    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public ResolutionStatus getStatus() {
        return status;
    }

    /**
     * Gets the resolved path.
     *
     * @return the string result
     */
    public String getResolvedPath() {
        return resolvedPath;
    }

    /**
     * Gets the winning strategy id.
     *
     * @return the string result
     */
    public String getWinningStrategyId() {
        return winningStrategyId;
    }

    /**
     * Gets the attempted strategies.
     *
     * @return the string result
     */
    public List<String> getAttemptedStrategies() {
        return attemptedStrategies;
    }

    /**
     * Gets the diagnostic.
     *
     * @return the string result
     */
    public String getDiagnostic() {
        return diagnostic;
    }

    /**
     * Gets the candidate paths.
     *
     * @return the string result
     */
    public List<String> getCandidatePaths() {
        return candidatePaths;
    }
}
