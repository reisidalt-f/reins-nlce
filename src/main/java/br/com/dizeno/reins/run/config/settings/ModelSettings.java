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
 * ModelSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ModelSettings {
    private boolean requestResponseLog = false;

    /**
     * Checks if requestResponseLog is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isRequestResponseLog() {
        return requestResponseLog;
    }

    /**
     * Sets the requestResponseLog.
     *
     * @param requestResponseLog the requestResponseLog flag
     */
    public void setRequestResponseLog(boolean requestResponseLog) {
        this.requestResponseLog = requestResponseLog;
    }
}
