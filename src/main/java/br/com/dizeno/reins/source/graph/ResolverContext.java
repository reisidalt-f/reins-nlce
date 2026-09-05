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

import br.com.dizeno.reins.security.PathValidator;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ResolverContext is part of the general application functions in the reins architecture.
 * Acts as a component managing resolver context.
 */
public class ResolverContext {
    private final Path projectRoot;
    private final Map<String, Path> sourceBases;
    private final List<String> strategyOrder;
    private final PathValidator pathValidator;
    private final Map<Path, String> knownSourcesByAbsolute;

    public ResolverContext(Path projectRoot,
                           Map<String, Path> sourceBases,
                           List<String> strategyOrder,
                           PathValidator pathValidator,
                           Map<Path, String> knownSourcesByAbsolute) {
        this.projectRoot = projectRoot;
        this.sourceBases = new LinkedHashMap<>();
        if (sourceBases != null) {
            for (Map.Entry<String, Path> e : sourceBases.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    this.sourceBases.put(e.getKey().toLowerCase(java.util.Locale.ROOT), e.getValue().toAbsolutePath().normalize());
                }
            }
        }
        this.strategyOrder = strategyOrder != null ? List.copyOf(strategyOrder) : List.of();
        this.pathValidator = pathValidator;
        this.knownSourcesByAbsolute = knownSourcesByAbsolute != null ? new LinkedHashMap<>(knownSourcesByAbsolute) : Map.of();
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Map<String, Path> getSourceBases() {
        return sourceBases;
    }

    public Path getSourceBase(String name) {
        return name == null ? null : sourceBases.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public List<String> getStrategyOrder() {
        return strategyOrder;
    }

    public PathValidator getPathValidator() {
        return pathValidator;
    }

    public Map<Path, String> getKnownSourcesByAbsolute() {
        return knownSourcesByAbsolute;
    }
}
