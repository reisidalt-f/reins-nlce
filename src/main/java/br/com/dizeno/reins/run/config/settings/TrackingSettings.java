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
 * TrackingSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class TrackingSettings {
    private boolean freezeState = false;

    /**
     * Checks if the component is freeze state.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFreezeState() {
        return freezeState;
    }

    /**
     * Sets the freeze state.
     *
     * @param freezeState the freeze state
     */
    public void setFreezeState(boolean freezeState) {
        this.freezeState = freezeState;
    }
}
