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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingOrderResolverTest {
    @Test
    void resolvesLeafFirstOrderForChain() {
        MarkdownDependencyGraph graph = graph(
                Map.of(
                        "a", List.of("b"),
                        "b", List.of("c"),
                        "c", List.of()
                ),
                Map.of(
                        "a", List.of(),
                        "b", List.of("a"),
                        "c", List.of("b")
                ),
                List.of("a")
        );

        assertEquals(List.of("c", "b", "a"), new ProcessingOrderResolver().resolve(graph));
    }

    @Test
    void resolvesIndependentRootsAndSharedChildrenOnce() {
        MarkdownDependencyGraph graph = graph(
                Map.of(
                        "root-a", List.of("shared"),
                        "root-b", List.of("shared"),
                        "shared", List.of()
                ),
                Map.of(
                        "root-a", List.of(),
                        "root-b", List.of(),
                        "shared", List.of("root-a", "root-b")
                ),
                List.of("root-a", "root-b")
        );

        List<String> order = new ProcessingOrderResolver().resolve(graph);
        assertEquals(3, order.size());
        assertEquals("shared", order.get(0));
    }

    @Test
    void keepsParentsAfterDependenciesForMultiLevelGraph() {
        MarkdownDependencyGraph graph = graph(
                Map.of(
                        "ui", List.of("app"),
                        "app", List.of("shared"),
                        "shared", List.of("core"),
                        "core", List.of()
                ),
                Map.of(
                        "ui", List.of(),
                        "app", List.of("ui"),
                        "shared", List.of("app"),
                        "core", List.of("shared")
                ),
                List.of("ui")
        );

        ProcessingOrderResolver resolver = new ProcessingOrderResolver();
        List<String> order = resolver.resolve(graph);
        Map<String, Integer> index = resolver.orderIndexMap(order);

        for (Map.Entry<String, List<String>> edge : graph.getEdges().entrySet()) {
            String parent = edge.getKey();
            for (String child : edge.getValue()) {
                assertTrue(index.get(child) < index.get(parent),
                        "Expected dependency '" + child + "' before parent '" + parent + "' in order: " + order);
            }
        }
    }

    @Test
    void resolvesOrderedParentsByProcessingPosition() {
        MarkdownDependencyGraph graph = graph(
                Map.of(
                        "ui", List.of("app"),
                        "report", List.of("app"),
                        "app", List.of("shared"),
                        "shared", List.of()
                ),
                Map.of(
                        "ui", List.of(),
                        "report", List.of(),
                        "app", List.of("ui", "report"),
                        "shared", List.of("app")
                ),
                List.of("ui", "report")
        );

        ProcessingOrderResolver resolver = new ProcessingOrderResolver();
        List<String> order = resolver.resolve(graph);
        Map<String, Integer> index = resolver.orderIndexMap(order);

        List<String> orderedParents = resolver.resolveOrderedParents("app", graph, index);
        List<String> expected = new ArrayList<>(List.of("ui", "report"));
        expected.sort((a, b) -> Integer.compare(index.get(a), index.get(b)));

        assertEquals(expected, orderedParents);
    }

    private MarkdownDependencyGraph graph(Map<String, List<String>> edges,
                                          Map<String, List<String>> reverseEdges,
                                          List<String> roots) {
        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        for (String key : edges.keySet()) {
            nodes.put(key, new MarkdownSourceNode(key, Path.of(key), 0L, List.of()));
        }
        return new MarkdownDependencyGraph(nodes, edges, reverseEdges, roots);
    }
}