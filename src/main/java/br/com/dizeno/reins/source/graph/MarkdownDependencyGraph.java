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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MarkdownDependencyGraph is part of the general application functions in the reins architecture.
 * Acts as a model representation of the dependency graph structure.
 */
public class MarkdownDependencyGraph {
    private final Map<String, MarkdownSourceNode> nodes;
    private final Map<String, List<String>> edges;
    private final Map<String, List<String>> reverseEdges;
    private final List<String> roots;
    private final Map<String, List<String>> resolutionDiagnostics;
    private final Map<String, List<String>> winningStrategies;

    /**
     * Constructs a new instance of {@link MarkdownDependencyGraph}.
     *
     * @param nodes the nodes
     * @param edges the edges
     * @param reverseEdges the reverse edges
     * @param roots the roots
     */
    public MarkdownDependencyGraph(Map<String, MarkdownSourceNode> nodes,
                                   Map<String, List<String>> edges,
                                   Map<String, List<String>> reverseEdges,
                                   List<String> roots) {
        this(nodes, edges, reverseEdges, roots, Map.of(), Map.of());
    }

    /**
     * Constructs a new instance of {@link MarkdownDependencyGraph}.
     *
     * @param nodes the nodes
     * @param edges the edges
     * @param reverseEdges the reverse edges
     * @param roots the roots
     * @param resolutionDiagnostics the resolution diagnostics
     * @param winningStrategies the winning strategies
     */
    public MarkdownDependencyGraph(Map<String, MarkdownSourceNode> nodes,
                                   Map<String, List<String>> edges,
                                   Map<String, List<String>> reverseEdges,
                                   List<String> roots,
                                   Map<String, List<String>> resolutionDiagnostics,
                                   Map<String, List<String>> winningStrategies) {
        this.nodes = Map.copyOf(new LinkedHashMap<>(nodes));
        this.edges = copyListMap(edges);
        this.reverseEdges = copyListMap(reverseEdges);
        this.roots = List.copyOf(roots);
        this.resolutionDiagnostics = copyListMap(resolutionDiagnostics);
        this.winningStrategies = copyListMap(winningStrategies);
    }

    /**
     * Gets the nodes.
     *
     * @return the string result
     */
    public Map<String, MarkdownSourceNode> getNodes() {
        return nodes;
    }

    /**
     * Gets the edges.
     *
     * @return the string result
     */
    public Map<String, List<String>> getEdges() {
        return edges;
    }

    /**
     * Gets the reverse edges.
     *
     * @return the string result
     */
    public Map<String, List<String>> getReverseEdges() {
        return reverseEdges;
    }

    /**
     * Gets the roots.
     *
     * @return the string result
     */
    public List<String> getRoots() {
        return roots;
    }

    /**
     * Gets the children.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public List<String> getChildren(String sourcePath) {
        return edges.getOrDefault(sourcePath, List.of());
    }

    /**
     * Gets the parents.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public List<String> getParents(String sourcePath) {
        return reverseEdges.getOrDefault(sourcePath, List.of());
    }

    /**
     * Gets the direct referencing sources.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public List<String> getDirectReferencingSources(String sourcePath) {
        return getParents(sourcePath);
    }

    /**
     * Gets the resolution diagnostics.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public List<String> getResolutionDiagnostics(String sourcePath) {
        return resolutionDiagnostics.getOrDefault(sourcePath, List.of());
    }

    /**
     * Gets the winning strategies.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public List<String> getWinningStrategies(String sourcePath) {
        return winningStrategies.getOrDefault(sourcePath, List.of());
    }

    /**
     * Size.
     *
     * @return the numeric value
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Gets the dependency link count.
     *
     * @return the numeric value
     */
    public int getDependencyLinkCount() {
        return edges.values().stream().mapToInt(List::size).sum();
    }

    private Map<String, List<String>> copyListMap(Map<String, ? extends List<String>> input) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends List<String>> entry : input.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}