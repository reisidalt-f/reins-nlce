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

import br.com.dizeno.reins.run.config.settings.GeminiSettings;
import br.com.dizeno.reins.run.config.settings.OllamaSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReinsConfigTest {

    @Test
    void resolveModel_nullProvider_returnsGeminiModel() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider(null);

        GeminiSettings gemini = new GeminiSettings();
        gemini.setModel("gemini-1.5-flash");
        config.setGemini(gemini);

        assertEquals("gemini-1.5-flash", config.resolveModel());
    }

    @Test
    void resolveModel_geminiProvider_returnsGeminiModel() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");

        GeminiSettings gemini = new GeminiSettings();
        gemini.setModel("gemini-1.5-pro");
        config.setGemini(gemini);

        assertEquals("gemini-1.5-pro", config.resolveModel());
    }

    @Test
    void resolveModel_ollamaProvider_returnsOllamaModel() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("ollama");

        OllamaSettings ollama = new OllamaSettings();
        ollama.setModel("llama3");
        config.setOllama(ollama);

        assertEquals("llama3", config.resolveModel());
    }

    @Test
    void resolveModel_stubProvider_returnsStubString() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("stub");

        assertEquals("stub", config.resolveModel());
    }

    @Test
    void resolveModel_unknownProviderCaseInsensitive_returnsGeminiModel() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider(" GeMiNi ");

        GeminiSettings gemini = new GeminiSettings();
        gemini.setModel("gemini-custom");
        config.setGemini(gemini);

        assertEquals("gemini-custom", config.resolveModel());
    }
}
