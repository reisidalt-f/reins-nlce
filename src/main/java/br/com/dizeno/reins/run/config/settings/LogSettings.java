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
 * LogSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class LogSettings {
    private boolean skipped = false;
    private boolean processingOrder = false;
    private boolean eagerlyProvided = false;

    /**
     * Checks if the component is skipped.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Sets the skipped.
     *
     * @param skipped the skipped
     */
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    /**
     * Checks if the component is processing order.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isProcessingOrder() {
        return processingOrder;
    }

    /**
     * Sets the processing order.
     *
     * @param processingOrder the processing order
     */
    public void setProcessingOrder(boolean processingOrder) {
        this.processingOrder = processingOrder;
    }

    /**
     * Checks if the component is eagerly provided.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEagerlyProvided() {
        return eagerlyProvided;
    }

    /**
     * Sets the eagerly provided.
     *
     * @param eagerlyProvided the eagerly provided
     */
    public void setEagerlyProvided(boolean eagerlyProvided) {
        this.eagerlyProvided = eagerlyProvided;
    }
}
