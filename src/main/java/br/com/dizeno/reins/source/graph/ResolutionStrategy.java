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

/**
 * ResolutionStrategy is part of the general application functions in the reins architecture.
 * Acts as a component managing resolution strategy.
 */
public interface ResolutionStrategy {
    /**
     * Strategy Id.
     *
     * @return the string result
     */
    String strategyId();

    /**
     * Applies.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return true if successful or matching, false otherwise
     */
    boolean applies(ReferenceRequest request, ResolverContext context);

    /**
     * Attempt.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return the resulting result
     */
    StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) throws Exception;
}
