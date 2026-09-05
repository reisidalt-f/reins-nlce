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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ToolRequestParser is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing tool request parser.
 */
public class ToolRequestParser {

    private static final Pattern FENCED_BLOCK =
            Pattern.compile("(?m)^(?:```|\"\"\")(?:tool_request|text|reins-boundary)?\\s*\\n(.*?)^(?:```|\"\"\")\\s*$", Pattern.DOTALL);

    /**
     * Parse body text into tool execution requests.
     *
     * @param body the raw model response body
     * @return list of parsed tool execution requests
     */
    public List<ToolExecutionRequest> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        List<ToolExecutionRequest> requests = fromFencedBlocks(body);
        if (!requests.isEmpty()) {
            return requests;
        }

        return ToolExecutionRequest.fromTextAny(body);
    }

    private List<ToolExecutionRequest> fromFencedBlocks(String body) {
        List<ToolExecutionRequest> requests = new ArrayList<>();
        Matcher matcher = FENCED_BLOCK.matcher(body);
        while (matcher.find()) {
            String block = matcher.group(1).trim();
            if (!block.isBlank()) {
                requests.addAll(ToolExecutionRequest.fromTextAny(block));
            }
        }
        return requests;
    }
}
