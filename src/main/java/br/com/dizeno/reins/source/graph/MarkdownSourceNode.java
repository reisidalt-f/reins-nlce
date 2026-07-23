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
import java.util.List;

/**
 * MarkdownSourceNode is part of the general application functions in the reins architecture.
 * Acts as a model representation of a dependency graph node.
 */
public record MarkdownSourceNode(String sourcePath,
                                 Path absolutePath,
                                 long lastModifiedMillis,
                                 List<String> outboundRefs) {
    public MarkdownSourceNode {
        outboundRefs = List.copyOf(outboundRefs);
    }
}