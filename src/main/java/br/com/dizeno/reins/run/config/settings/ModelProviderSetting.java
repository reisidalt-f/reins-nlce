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
 * ModelProviderSetting represents common configuration options for LLM providers in reins.
 */
public interface ModelProviderSetting {
    String getModel();
    String getEndpoint();
    String getApiKey();
    int getTimeoutSeconds();
    int getRetryAttempts();
    
    default Float getTemperature() {
        return null;
    }
    
    default Integer getMaximumTurns() {
        return null;
    }

    default int resolveMaximumTurns() {
        Integer turns = getMaximumTurns();
        return turns == null ? 1 : turns;
    }
}
