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

package br.com.dizeno.reins.reasoning;

import java.util.Map;

 
/**
 * ReferenceTreeRenderer is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reference tree renderer.
 */
public class ReferenceTreeRenderer {

    /**
     * Render.
     *
     * @param context the context
     * @return the string result
     */
    public String render(ReferenceTreeContextService.ReferenceTreeContext context) {
        if (context == null) {
            return "<empty>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(context.getRootDisplayLabel()).append("\n");

        ReferenceTraversalState traversalState = new ReferenceTraversalState();
        traversalState.increment(context.getRootCanonicalPath());
        renderChildren(
                sb,
                context.getNodesByCanonical(),
                context.getRootCanonicalPath(),
                "",
                0,
                traversalState,
                context.getRenderPolicy()
        );

        return sb.toString().trim();
    }

    private void renderChildren(StringBuilder sb,
                                Map<String, ReferenceTreeContextService.ReferenceNode> nodes,
                                String parentCanonical,
                                String prefix,
                                int parentDepth,
                                ReferenceTraversalState traversalState,
                                ReferenceTreeRenderPolicy policy) {
        ReferenceTreeContextService.ReferenceNode parentNode = nodes.get(parentCanonical);
        if (parentNode == null) {
            return;
        }

        var edges = parentNode.getEdges();
        for (int i = 0; i < edges.size(); i++) {
            var edge = edges.get(i);
            boolean last = i == edges.size() - 1;
            String branch = last ? "└── " : "├── ";
            String nextPrefix = prefix + (last ? "    " : "│   ");
            int childDepth = parentDepth + 1;

            int occurrence = traversalState.increment(edge.targetCanonicalPath());
            boolean repeated = occurrence > 1;
            boolean truncated = childDepth >= policy.getMaxDepth();

            StringBuilder nodeLine = new StringBuilder();
            nodeLine.append(prefix).append(branch).append(edge.literalPath());

            if (repeated) {
                nodeLine.append(" ").append(policy.getRepeatMarker());
                sb.append(nodeLine).append("\n");
                continue;
            }
            if (truncated) {
                nodeLine.append(" ").append(policy.getDepthTruncatedMarker());
                sb.append(nodeLine).append("\n");
                continue;
            }

            sb.append(nodeLine).append("\n");
            renderChildren(sb, nodes, edge.targetCanonicalPath(), nextPrefix, childDepth, traversalState, policy);
        }
    }
}
