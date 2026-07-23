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

import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;

/**
 * ReferenceTreeRenderPolicy is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reference tree render policy.
 */
public class ReferenceTreeRenderPolicy {
    private final int maxDepth;
    private final String repeatMarker;
    private final String depthTruncatedMarker;

    /**
     * Constructs a new instance of {@link ReferenceTreeRenderPolicy}.
     *
     * @param maxDepth the max depth
     */
    public ReferenceTreeRenderPolicy(int maxDepth) {
        this(maxDepth, "(repeat)", "(depth-truncated)");
    }

    /**
     * Constructs a new instance of {@link ReferenceTreeRenderPolicy}.
     *
     * @param maxDepth the max depth
     * @param repeatMarker the repeat marker
     * @param depthTruncatedMarker the depth truncated marker
     */
    public ReferenceTreeRenderPolicy(int maxDepth, String repeatMarker, String depthTruncatedMarker) {
        this.maxDepth = maxDepth;
        this.repeatMarker = repeatMarker;
        this.depthTruncatedMarker = depthTruncatedMarker;
    }

    /**
     * For Policy.
     *
     * @param policy the policy
     * @return the resolved or constructed object
     */
    public static ReferenceTreeRenderPolicy forPolicy(ReferenceDepthPolicy policy) {
        ReferenceDepthPolicy effectivePolicy = policy == null ? ReferenceDepthPolicy.defaultPolicy() : policy;
        return new ReferenceTreeRenderPolicy(effectivePolicy.toRenderMaxDepth());
    }

    /**
     * Gets the max depth.
     *
     * @return the numeric value
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Gets the repeat marker.
     *
     * @return the string result
     */
    public String getRepeatMarker() {
        return repeatMarker;
    }

    /**
     * Gets the depth truncated marker.
     *
     * @return the string result
     */
    public String getDepthTruncatedMarker() {
        return depthTruncatedMarker;
    }
}
