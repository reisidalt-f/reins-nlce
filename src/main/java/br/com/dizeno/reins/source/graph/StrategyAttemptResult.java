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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * StrategyAttemptResult is part of the general application functions in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class StrategyAttemptResult {
    private final List<Path> candidates;
    private final String diagnostic;

    private StrategyAttemptResult(List<Path> candidates, String diagnostic) {
        this.candidates = List.copyOf(candidates);
        this.diagnostic = diagnostic;
    }

    /**
     * No Match.
     *
     * @param diagnostic the diagnostic
     * @return the resulting result
     */
    public static StrategyAttemptResult noMatch(String diagnostic) {
        return new StrategyAttemptResult(List.of(), diagnostic);
    }

    /**
     * From Candidates.
     *
     * @param candidates the candidates
     * @param diagnostic the diagnostic
     * @return the resulting result
     */
    public static StrategyAttemptResult fromCandidates(List<Path> candidates, String diagnostic) {
        return new StrategyAttemptResult(new ArrayList<>(candidates), diagnostic);
    }

    /**
     * Gets the candidates.
     *
     * @return the collection of elements
     */
    public List<Path> getCandidates() {
        return candidates;
    }

    /**
     * Gets the diagnostic.
     *
     * @return the string result
     */
    public String getDiagnostic() {
        return diagnostic;
    }
}
