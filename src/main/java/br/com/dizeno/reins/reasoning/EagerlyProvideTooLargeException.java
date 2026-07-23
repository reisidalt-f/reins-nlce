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

/**
 * EagerlyProvideTooLargeException is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class EagerlyProvideTooLargeException extends RuntimeException {
    private final String filePath;
    private final long actualSizeBytes;
    private final long limitBytes;

    /**
     * Constructs a new instance of {@link EagerlyProvideTooLargeException}.
     *
     * @param filePath the file path
     * @param actualSizeBytes the actual size bytes
     * @param limitBytes the limit bytes
     */
    public EagerlyProvideTooLargeException(String filePath, long actualSizeBytes, long limitBytes) {
        super("Eagerly provided file exceeds maxAttachmentSizeBytes limit: " + filePath
                + " (" + actualSizeBytes + " bytes > " + limitBytes + " bytes)");
        this.filePath = filePath;
        this.actualSizeBytes = actualSizeBytes;
        this.limitBytes = limitBytes;
    }

    /**
     * Gets the file path.
     *
     * @return the string result
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Gets the actual size bytes.
     *
     * @return the numeric value
     */
    public long getActualSizeBytes() {
        return actualSizeBytes;
    }

    /**
     * Gets the limit bytes.
     *
     * @return the numeric value
     */
    public long getLimitBytes() {
        return limitBytes;
    }
}
