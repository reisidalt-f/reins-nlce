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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProcessingOrderResolver is part of the general application functions in the reins architecture.
 * Performs topological sorting on the dependency graph to determine the correct compile sequence.
 */
public class ProcessingOrderResolver {
    /**
     * VisitState is part of the general application functions in the reins architecture.
     * Acts as a component managing visit state.
     */
    private enum VisitState {
        WHITE,
        GRAY,
        BLACK
    }

    /**
     * Frame is part of the general application functions in the reins architecture.
     * Acts as a component managing frame.
     */
    private static final class Frame {
        private final String node;
        private final List<String> children;
        private int childIndex;
        private boolean entered;

        private Frame(String node, List<String> children) {
            this.node = node;
            this.children = children;
        }
    }

    /**
     * Resolves the configured value or path.
     *
     * @param graph the markdown dependency graph
     * @return the string result
     */
    public List<String> resolve(MarkdownDependencyGraph graph) {
        return resolveFrom(graph, graph.getNodes().keySet());
    }

     
    /**
     * Resolves the configured value or path ordered parents.
     *
     * @param sourcePath the path of the source file
     * @param graph the markdown dependency graph
     * @param orderIndex the order index
     * @return the string result
     */
    public List<String> resolveOrderedParents(String sourcePath,
                                              MarkdownDependencyGraph graph,
                                              Map<String, Integer> orderIndex) {
        List<String> parents = new ArrayList<>(graph.getDirectReferencingSources(sourcePath));
        parents.sort((a, b) -> {
            int ia = orderIndex.getOrDefault(a, Integer.MAX_VALUE);
            int ib = orderIndex.getOrDefault(b, Integer.MAX_VALUE);
            return Integer.compare(ia, ib);
        });
        return parents;
    }

     
    /**
     * Order Index Map.
     *
     * @param order the order
     * @return the string result
     */
    public Map<String, Integer> orderIndexMap(List<String> order) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < order.size(); i++) {
            index.put(order.get(i), i);
        }
        return index;
    }

    private List<String> resolveFrom(MarkdownDependencyGraph graph, Set<String> allNodes) {
        Map<String, VisitState> states = new LinkedHashMap<>();
        for (String node : allNodes) {
            states.put(node, VisitState.WHITE);
        }

        List<String> order = new ArrayList<>();
        for (String root : graph.getRoots()) {
            traverse(root, graph, states, order);
        }
        for (String node : allNodes) {
            if (states.get(node) == VisitState.WHITE) {
                traverse(node, graph, states, order);
            }
        }
        return order;
    }

    private void traverse(String start,
                          MarkdownDependencyGraph graph,
                          Map<String, VisitState> states,
                          List<String> order) {
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(start, graph.getChildren(start)));

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (!frame.entered) {
                states.put(frame.node, VisitState.GRAY);
                frame.entered = true;
            }

            if (frame.childIndex < frame.children.size()) {
                String child = frame.children.get(frame.childIndex++);
                if (states.getOrDefault(child, VisitState.WHITE) == VisitState.WHITE) {
                    stack.push(new Frame(child, graph.getChildren(child)));
                }
                continue;
            }

            stack.pop();
            if (states.get(frame.node) != VisitState.BLACK) {
                states.put(frame.node, VisitState.BLACK);
                order.add(frame.node);
            }
        }
    }
}