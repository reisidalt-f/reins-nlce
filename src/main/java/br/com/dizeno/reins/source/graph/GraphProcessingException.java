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

import java.util.List;

/**
 * GraphProcessingException is part of the general application functions in the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class GraphProcessingException extends RuntimeException {
    /**
     * ViolationType is part of the general application functions in the reins architecture.
     * Acts as a component managing violation type.
     */
    public enum ViolationType {
        CYCLE,
        UNRESOLVED_REFERENCE,
        AMBIGUOUS_REFERENCE
    }

    private final ViolationType violationType;
    private final List<String> involvedPaths;

    /**
     * Constructs a new instance of {@link GraphProcessingException}.
     *
     * @param violationType the violation type
     * @param involvedPaths the involved paths
     */
    public GraphProcessingException(ViolationType violationType, List<String> involvedPaths) {
        super(buildMessage(violationType, involvedPaths));
        this.violationType = violationType;
        this.involvedPaths = List.copyOf(involvedPaths);
    }

    /**
     * Constructs a new instance of {@link GraphProcessingException}.
     *
     * @param violationType the violation type
     * @param involvedPaths the involved paths
     * @param message the message content
     */
    public GraphProcessingException(ViolationType violationType, List<String> involvedPaths, String message) {
        super(message);
        this.violationType = violationType;
        this.involvedPaths = List.copyOf(involvedPaths);
    }

    /**
     * Gets the violation type.
     *
     * @return the resolved or constructed object
     */
    public ViolationType getViolationType() {
        return violationType;
    }

    /**
     * Gets the involved paths.
     *
     * @return the string result
     */
    public List<String> getInvolvedPaths() {
        return involvedPaths;
    }

    /**
     * To Structured Diagnostic.
     *
     * @return the string result
     */
    public String toStructuredDiagnostic() {
        if (violationType != ViolationType.CYCLE) {
            return getMessage();
        }
        return "[graph] failure=reference-cycle path=\""
                + String.join(" -> ", involvedPaths)
                + "\" msg=\"Reference graph contains a cycle; processing aborted\"";
    }

    private static String buildMessage(ViolationType violationType, List<String> involvedPaths) {
        if (violationType == ViolationType.CYCLE) {
            return "Reference graph contains a cycle: " + String.join(" -> ", involvedPaths);
        }
        if (violationType == ViolationType.AMBIGUOUS_REFERENCE) {
            if (involvedPaths.size() >= 2) {
                return "Ambiguous markdown reference in '" + involvedPaths.get(0)
                        + "': target '" + involvedPaths.get(1)
                        + "' matched multiple candidates";
            }
            return "Ambiguous markdown reference detected";
        }
        if (involvedPaths.size() >= 2) {
            return "Unresolved markdown reference in '" + involvedPaths.get(0)
                    + "': target '" + involvedPaths.get(1) + "' not found in scan roots";
        }
        return "Graph processing failed: " + violationType;
    }
}