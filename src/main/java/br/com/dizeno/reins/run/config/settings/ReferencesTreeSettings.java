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

package br.com.dizeno.reins.run.config.settings;

import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;

/**
 * ReferencesTreeSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ReferencesTreeSettings {
    private boolean attachFiles = false;
    private String depth = "3";
    private int maxDepth = 8;

    /**
     * Checks if attaching referenced files is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isAttachFiles() {
        return attachFiles;
    }

    /**
     * Sets whether attaching referenced files is enabled.
     *
     * @param attachFiles the attachFiles flag
     */
    public void setAttachFiles(boolean attachFiles) {
        this.attachFiles = attachFiles;
    }

    /**
     * Gets the reference depth setting value.
     *
     * @return the string result
     */
    public String getDepth() {
        return depth;
    }

    /**
     * Sets the reference depth setting value.
     *
     * @param depth the depth string ("0", integer, "*")
     */
    public void setDepth(String depth) {
        this.depth = depth;
    }

    /**
     * Gets the max reference depth limit.
     *
     * @return the numeric value
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Sets the max reference depth limit.
     *
     * @param maxDepth the max depth
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * Resolves the configured value or path reference depth policy.
     *
     * @return the resolved or constructed object
     */
    public ReferenceDepthPolicy resolveReferenceDepthPolicy() {
        return ReferenceDepthPolicy.parse(depth);
    }
}
