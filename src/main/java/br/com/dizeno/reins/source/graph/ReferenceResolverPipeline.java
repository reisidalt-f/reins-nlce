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

import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReferenceResolverPipeline is part of the general application functions in the reins architecture.
 * Acts as a component managing reference resolver pipeline.
 */
public class ReferenceResolverPipeline {
    private final List<ResolutionStrategy> strategies;

    /**
     * Constructs a new instance of {@link ReferenceResolverPipeline}.
     *
     * @param strategies the strategies
     */
    public ReferenceResolverPipeline(List<ResolutionStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /**
     * Resolves the configured value or path.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return the resulting result
     */
    public ResolutionResult resolve(ReferenceRequest request, ResolverContext context) throws Exception {
        List<String> attempted = new ArrayList<>();
        String lastDiagnostic = "";
        for (ResolutionStrategy strategy : strategies) {
            if (!strategy.applies(request, context)) {
                continue;
            }
            attempted.add(strategy.strategyId());
            StrategyAttemptResult attempt = strategy.attempt(request, context);
            lastDiagnostic = attempt.getDiagnostic();
            if (attempt.getCandidates().isEmpty()) {
                continue;
            }

            if (attempt.getCandidates().size() == 1) {
                Path candidate = context.getPathValidator().validateInProject(attempt.getCandidates().get(0));
                String resolved = normalizeRelativePath(context.getProjectRoot().relativize(candidate));
                return ResolutionResult.resolved(
                        resolved,
                        strategy.strategyId(),
                        attempted,
                        attempt.getDiagnostic()
                );
            }

            List<String> candidatePaths = new ArrayList<>();
            for (Path candidate : attempt.getCandidates()) {
                Path validated = context.getPathValidator().validateInProject(candidate);
                candidatePaths.add(normalizeRelativePath(context.getProjectRoot().relativize(validated)));
            }
            candidatePaths.sort(String::compareTo);
            return ResolutionResult.ambiguous(attempted, attempt.getDiagnostic(), candidatePaths);
        }
        if (attempted.isEmpty()) {
            return ResolutionResult.unresolved(List.of(), "no applicable strategies");
        }
        return ResolutionResult.unresolved(attempted, lastDiagnostic.isBlank() ? "no strategy resolved reference" : lastDiagnostic);
    }

    /**
     * Strategies By Id.
     *
     * @return the string result
     */
    public Map<String, ResolutionStrategy> strategiesById() {
        Map<String, ResolutionStrategy> byId = new LinkedHashMap<>();
        for (ResolutionStrategy strategy : strategies) {
            byId.put(strategy.strategyId(), strategy);
        }
        return byId;
    }

    private String normalizeRelativePath(Path path) {
        return PathNormalizer.toForwardSlashes(path);
    }
}
