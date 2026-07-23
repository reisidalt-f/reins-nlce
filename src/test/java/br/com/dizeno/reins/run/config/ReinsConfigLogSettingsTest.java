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

package br.com.dizeno.reins.run.config;
import br.com.dizeno.reins.run.config.settings.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinsConfigLogSettingsTest {

    @Test
    void defaultsAllLoggingTogglesToFalse() {
        ReinsConfig config = new ReinsConfig();

        assertNotNull(config.getLogging());
        assertFalse(config.getLogging().isScriptsEvents());
        assertFalse(config.getLogging().isEagerlyProvided());
        assertFalse(config.getLogging().isFileListingAndReading());
        assertFalse(config.getLogging().isFileMutating());
        assertFalse(config.getLogging().isScriptRun());
        assertFalse(config.getLogging().isSelectionReason());
        assertFalse(config.getLogging().isResult());
    }

    @Test
    void appliesExplicitLoggingToggleValues() {
        ReinsConfig config = new ReinsConfig();
        LoggingSettings settings = new LoggingSettings();
        settings.setScriptsEvents(true);
        settings.setEagerlyProvided(true);
        settings.setFileListingAndReading(true);
        settings.setFileMutating(true);
        settings.setScriptRun(true);
        settings.setSelectionReason(true);
        settings.setResult(true);

        config.setLogging(settings);

        assertTrue(config.getLogging().isScriptsEvents());
        assertTrue(config.getLogging().isEagerlyProvided());
        assertTrue(config.getLogging().isFileListingAndReading());
        assertTrue(config.getLogging().isFileMutating());
        assertTrue(config.getLogging().isScriptRun());
        assertTrue(config.getLogging().isSelectionReason());
        assertTrue(config.getLogging().isResult());
    }

    @Test
    void setLoggingNullFallsBackToDefaultObject() {
        ReinsConfig config = new ReinsConfig();

        config.setLogging(null);

        assertNotNull(config.getLogging());
        assertFalse(config.getLogging().isScriptsEvents());
        assertFalse(config.getLogging().isEagerlyProvided());
        assertFalse(config.getLogging().isFileListingAndReading());
        assertFalse(config.getLogging().isFileMutating());
        assertFalse(config.getLogging().isScriptRun());
        assertFalse(config.getLogging().isSelectionReason());
        assertFalse(config.getLogging().isResult());
    }

    @Test
    void loggingSettingsSelectionReasonDefaultsFalse() {
        LoggingSettings loggingSettings = new LoggingSettings();

        assertFalse(loggingSettings.isSelectionReason());
    }

    @Test
    void loggingSettingsSelectionReasonCanBeEnabledExplicitly() {
        LoggingSettings loggingSettings = new LoggingSettings();

        loggingSettings.setSelectionReason(true);

        assertTrue(loggingSettings.isSelectionReason());
    }

    @Test
    void defaultsLlmProviderToFalse() {
        ReinsConfig config = new ReinsConfig();

        assertNotNull(config.getLogging());
        assertFalse(config.getLogging().isLlmProvider());
    }

    @Test
    void appliesExplicitLlmProviderToggleValue() {
        ReinsConfig config = new ReinsConfig();
        LoggingSettings settings = new LoggingSettings();
        settings.setLlmProvider(true);

        config.setLogging(settings);

        assertTrue(config.getLogging().isLlmProvider());
    }
}
