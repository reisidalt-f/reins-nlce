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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingOrderResolverMdWriterTest {
    @Test
    void resolvesBlockNodeBeforeHeadingForMdWriterStructure() {
        
        
        
        
        

        MarkdownDependencyGraph graph = graph(
                Map.of(
                        "heading", List.of("block-node"),
                        "block-node", List.of("node"),
                        "document", List.of("block-node"),
                        "node", List.of()
                ),
                Map.of(
                        "heading", List.of(),
                        "block-node", List.of("heading", "document"),
                        "document", List.of(),
                        "node", List.of("block-node")
                ),
                List.of("heading", "document")
        );

        List<String> order = new ProcessingOrderResolver().resolve(graph);
        System.out.println("Processing order: " + order);

        int nodeIndex = order.indexOf("node");
        int blockNodeIndex = order.indexOf("block-node");
        int headingIndex = order.indexOf("heading");
        int documentIndex = order.indexOf("document");

        assertTrue(nodeIndex < blockNodeIndex, "node should come before block-node");
        assertTrue(blockNodeIndex < headingIndex, "block-node should come before heading");
        assertTrue(blockNodeIndex < documentIndex, "block-node should come before document");
        assertTrue(order.get(0).equals("node"), "First should be node");
        assertTrue(order.get(1).equals("block-node"), "Second should be block-node");
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
