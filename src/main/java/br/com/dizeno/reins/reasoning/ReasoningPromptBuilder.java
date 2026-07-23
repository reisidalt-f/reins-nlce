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

import java.util.List;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

/**
 * ReasoningPromptBuilder is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a helper utility for building its prefix objects.
 */
public class ReasoningPromptBuilder {

    private final ReferenceTreeRenderer treeRenderer;

    /**
     * Constructs a new instance of {@link ReasoningPromptBuilder}.
     */
    public ReasoningPromptBuilder() {
        this(new ReferenceTreeRenderer());
    }

    /**
     * Constructs a new instance of {@link ReasoningPromptBuilder}.
     *
     * @param treeRenderer the tree renderer
     */
    public ReasoningPromptBuilder(ReferenceTreeRenderer treeRenderer) {
        this.treeRenderer = treeRenderer;
    }

    /**
     * Builds the configured target reference tree.
     *
     * @param context the context
     * @return the string result
     */
    public String buildReferenceTree(ReferenceTreeContextService.ReferenceTreeContext context) {
        return treeRenderer.render(context);
    }

    /**
     * Builds the configured target prompt.
     *
     * @param cycle the cycle
     * @param nextMessage the next message
     * @param attachments the list of attachments
     * @return the string result
     */
    public String buildPrompt(ReasoningCycle cycle,
                              String nextMessage,
                              List<AttachedFilePayload> attachments) {
        return buildPrompt(cycle, nextMessage, attachments, true);
    }

    /**
     * Builds the configured target prompt.
     *
     * @param cycle the cycle
     * @param nextMessage the next message
     * @param attachments the list of attachments
     * @param includeSystemContext the include system context
     * @return the string result
     */
    public String buildPrompt(ReasoningCycle cycle,
                              String nextMessage,
                              List<AttachedFilePayload> attachments,
                              boolean includeSystemContext) {
        StringBuilder sb = new StringBuilder();

        sb.append(nextMessage == null ? "" : nextMessage).append("\n");

        

        return sb.toString();
    }

}
