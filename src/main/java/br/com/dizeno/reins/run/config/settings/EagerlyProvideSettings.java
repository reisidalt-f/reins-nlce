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

/**
 * EagerlyProvideSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class EagerlyProvideSettings {
    private long maxAttachmentSizeBytes = 0L;

    /**
     * Gets the max attachment size bytes.
     *
     * @return the numeric value
     */
    public long getMaxAttachmentSizeBytes() {
        return maxAttachmentSizeBytes;
    }

    /**
     * Sets the max attachment size bytes.
     *
     * @param maxAttachmentSizeBytes the max attachment size bytes
     */
    public void setMaxAttachmentSizeBytes(long maxAttachmentSizeBytes) {
        this.maxAttachmentSizeBytes = maxAttachmentSizeBytes;
    }
}
