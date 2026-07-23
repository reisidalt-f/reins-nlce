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
    private final Path mainNlRoot;
    private final List<String> strategyOrder;
    private final PathValidator pathValidator;
    private final Map<Path, String> knownSourcesByAbsolute;

    /**
     * Constructs a new instance of {@link ResolverContext}.
     *
     * @param projectRoot the root path of the project
     * @param mainNlRoot the main nl root
     * @param strategyOrder the strategy order
     * @param pathValidator the path validator
     * @param knownSourcesByAbsolute the known sources by absolute
     */
    public ResolverContext(Path projectRoot,
                           Path mainNlRoot,
                           List<String> strategyOrder,
                           PathValidator pathValidator,
                           Map<Path, String> knownSourcesByAbsolute) {
        this.projectRoot = projectRoot;
        this.mainNlRoot = mainNlRoot;
        this.strategyOrder = List.copyOf(strategyOrder);
        this.pathValidator = pathValidator;
        this.knownSourcesByAbsolute = new LinkedHashMap<>(knownSourcesByAbsolute);
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Gets the main nl root.
     *
     * @return the resolved or constructed object
     */
    public Path getMainNlRoot() {
        return mainNlRoot;
    }

    /**
     * Gets the strategy order.
     *
     * @return the string result
     */
    public List<String> getStrategyOrder() {
        return strategyOrder;
    }

    /**
     * Gets the path validator.
     *
     * @return the resolved or constructed object
     */
    public PathValidator getPathValidator() {
        return pathValidator;
    }

    /**
     * Gets the known sources by absolute.
     *
     * @return the string result
     */
    public Map<Path, String> getKnownSourcesByAbsolute() {
        return knownSourcesByAbsolute;
    }
}
