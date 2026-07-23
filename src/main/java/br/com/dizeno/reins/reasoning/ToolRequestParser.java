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
            Pattern.compile("(?m)^(?:```|\"\"\")(?:tool_request|mcp_request|yaml)?\\s*\\n(.*?)^(?:```|\"\"\")\\s*$", Pattern.DOTALL);
    private static final Pattern OPERATION_START =
            Pattern.compile("(?m)^operation:\\s*");

    /**
     * Parse.
     *
     * @param body the body
     * @return the collection of elements
     */
    public List<ToolExecutionRequest> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        List<ToolExecutionRequest> requests = fromFencedBlocks(body);
        if (!requests.isEmpty()) {
            return requests;
        }

        requests = fromConsecutiveOperationBlocks(body);
        if (!requests.isEmpty()) {
            return requests;
        }

        
        List<ToolExecutionRequest> separated = new ArrayList<>();
        String[] chunks = body.split("\\n---\\n");
        for (String chunk : chunks) {
            String candidate = chunk.trim();
            if (!candidate.isBlank() && candidate.contains("operation:")) {
                separated.addAll(ToolExecutionRequest.fromYamlAny(candidate));
            }
        }
        if (!separated.isEmpty()) {
            return separated;
        }

        return new ArrayList<>(ToolExecutionRequest.fromYamlAny(body));
    }

    private List<ToolExecutionRequest> fromFencedBlocks(String body) {
        List<ToolExecutionRequest> requests = new ArrayList<>();
        Matcher matcher = FENCED_BLOCK.matcher(body);
        while (matcher.find()) {
            String block = matcher.group(1).trim();
            if (!block.isBlank()) {
                requests.addAll(ToolExecutionRequest.fromYamlAny(block));
            }
        }
        return requests;
    }

     
    private List<ToolExecutionRequest> fromConsecutiveOperationBlocks(String body) {
        Matcher matcher = OPERATION_START.matcher(body);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.size() <= 1) {
            return List.of();
        }

        List<ToolExecutionRequest> requests = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : body.length();
            String chunk = body.substring(start, end).trim();
            if (!chunk.isBlank()) {
                requests.addAll(ToolExecutionRequest.fromYamlAny(chunk));
            }
        }
        return requests;
    }
}
