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

import br.com.dizeno.reins.reasoning.scripting.*;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTreeRendererTest {

    @Test
    void rendersPlainUnicodeTree() {
        ReferenceTreeContextService.ReferenceTreeContext context = contextWithRootAndChild();

        String rendered = new ReferenceTreeRenderer().render(context);

        assertEquals("main:root.md\n└── child.md", rendered);
    }

    @Test
    void rendersUnicodePrefixesForSiblings() {
        ReferenceTreeContextService.ReferenceNode root = new ReferenceTreeContextService.ReferenceNode("src/main/nl/root.md");
        root.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("child-a.md", "src/main/nl/child-a.md"));
        root.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("child-b.md", "src/main/nl/child-b.md"));

        Map<String, ReferenceTreeContextService.ReferenceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/root.md", root);
        nodes.put("src/main/nl/child-a.md", new ReferenceTreeContextService.ReferenceNode("src/main/nl/child-a.md"));
        nodes.put("src/main/nl/child-b.md", new ReferenceTreeContextService.ReferenceNode("src/main/nl/child-b.md"));

        ReferenceTreeContextService.ReferenceTreeContext context = new ReferenceTreeContextService.ReferenceTreeContext(
                "src/main/nl/root.md",
                "main:root.md",
                new ReferenceTreeRenderPolicy(8),
                nodes
        );

        String rendered = new ReferenceTreeRenderer().render(context);

        assertTrue(rendered.contains("├── child-a.md"));
        assertTrue(rendered.contains("└── child-b.md"));
    }

    @Test
    void preservesRepeatAndDepthMarkersWithUnicodeBranches() {
        ReferenceTreeContextService.ReferenceNode root = new ReferenceTreeContextService.ReferenceNode("src/main/nl/root.md");
        root.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("child-a.md", "src/main/nl/child-a.md"));
        root.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("child-b.md", "src/main/nl/child-b.md"));

        ReferenceTreeContextService.ReferenceNode childA = new ReferenceTreeContextService.ReferenceNode("src/main/nl/child-a.md");
        childA.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("shared.md", "src/main/nl/shared.md"));

        ReferenceTreeContextService.ReferenceNode childB = new ReferenceTreeContextService.ReferenceNode("src/main/nl/child-b.md");
        childB.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("shared.md", "src/main/nl/shared.md"));

        ReferenceTreeContextService.ReferenceNode shared = new ReferenceTreeContextService.ReferenceNode("src/main/nl/shared.md");
        shared.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("deep.md", "src/main/nl/deep.md"));

        Map<String, ReferenceTreeContextService.ReferenceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/root.md", root);
        nodes.put("src/main/nl/child-a.md", childA);
        nodes.put("src/main/nl/child-b.md", childB);
        nodes.put("src/main/nl/shared.md", shared);
        nodes.put("src/main/nl/deep.md", new ReferenceTreeContextService.ReferenceNode("src/main/nl/deep.md"));

        ReferenceTreeContextService.ReferenceTreeContext context = new ReferenceTreeContextService.ReferenceTreeContext(
                "src/main/nl/root.md",
                "main:root.md",
                new ReferenceTreeRenderPolicy(2),
                nodes
        );

        String rendered = new ReferenceTreeRenderer().render(context);

        assertTrue(rendered.contains("│   └── shared.md (depth-truncated)"));
        assertTrue(rendered.contains("    └── shared.md (repeat)"));
    }

    private ReferenceTreeContextService.ReferenceTreeContext contextWithRootAndChild() {
        ReferenceTreeContextService.ReferenceNode root = new ReferenceTreeContextService.ReferenceNode("src/main/nl/root.md");
        root.getEdges().add(new ReferenceTreeContextService.ReferenceEdge("child.md", "src/main/nl/child.md"));

        ReferenceTreeContextService.ReferenceNode child = new ReferenceTreeContextService.ReferenceNode("src/main/nl/child.md");

        Map<String, ReferenceTreeContextService.ReferenceNode> nodes = new LinkedHashMap<>();
        nodes.put("src/main/nl/root.md", root);
        nodes.put("src/main/nl/child.md", child);

        return new ReferenceTreeContextService.ReferenceTreeContext(
                "src/main/nl/root.md",
                "main:root.md",
                                new ReferenceTreeRenderPolicy(8),
                nodes
        );
    }
}
