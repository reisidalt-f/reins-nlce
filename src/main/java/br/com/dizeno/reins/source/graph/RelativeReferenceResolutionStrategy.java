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
import java.nio.file.Files;
import java.util.List;

/**
 * RelativeReferenceResolutionStrategy is part of the general application functions in the reins architecture.
 * Acts as a component managing relative reference resolution strategy.
 */
public class RelativeReferenceResolutionStrategy implements ResolutionStrategy {
    /**
     * Strategy Id.
     *
     * @return the string result
     */
    @Override
    public String strategyId() {
        return "relative";
    }

    /**
     * Applies.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean applies(ReferenceRequest request, ResolverContext context) {
        return true;
    }

    /**
     * Attempt.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return the resulting result
     */
    @Override
    public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) throws Exception {
        Path resolvedPath = context.getPathValidator().resolveInProject(request.refererDir(), request.rawReference());
        if (!Files.exists(resolvedPath)) {
            return StrategyAttemptResult.noMatch("relative candidate missing");
        }
        return StrategyAttemptResult.fromCandidates(List.of(resolvedPath), "resolved relative candidate");
    }
}
