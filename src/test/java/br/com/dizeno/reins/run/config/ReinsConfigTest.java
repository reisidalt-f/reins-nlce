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

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReinsConfigTest {

    @Test
    void resolveModel_nullProvider_returnsNull() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider(null);

        GeminiSettings gemini = new GeminiSettings();
        gemini.setModel("gemini-1.5-flash");
        config.setGemini(gemini);

        assertNull(config.resolveModel());
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

    // ── getScanRoots ────────────────────────────────────────────────────────

    @Test
    void getScanRoots_onlyMainBase_returnsSingleRoot() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", new File("src/main/nl"));

        List<File> roots = config.getScanRoots();

        assertEquals(1, roots.size());
        assertEquals(new File("src/main/nl"), roots.get(0));
    }

    @Test
    void getScanRoots_mainAndTestBases_returnsBoth() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", new File("src/main/nl"));
        config.setSourceBase("test", new File("src/test/nl"));

        List<File> roots = config.getScanRoots();

        assertEquals(2, roots.size());
        assertTrue(roots.contains(new File("src/main/nl")));
        assertTrue(roots.contains(new File("src/test/nl")));
    }

    @Test
    void getScanRoots_skipTest_excludesTestBase() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", new File("src/main/nl"));
        config.setSourceBase("test", new File("src/test/nl"));
        config.setSkipTest(true);

        List<File> roots = config.getScanRoots();

        assertEquals(1, roots.size());
        assertEquals(new File("src/main/nl"), roots.get(0));
    }

    @Test
    void getScanRoots_customBaseOnly_returnsEmpty() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("doc", new File("docs"));

        List<File> roots = config.getScanRoots();

        assertTrue(roots.isEmpty(), "Custom bases like 'doc' must not be included in implicit scan roots");
    }

    @Test
    void getScanRoots_mainAndCustomBases_returnsOnlyMain() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("main", new File("src/main/nl"));
        config.setSourceBase("doc", new File("docs"));
        config.setSourceBase("api", new File("api-specs"));

        List<File> roots = config.getScanRoots();

        assertEquals(1, roots.size());
        assertEquals(new File("src/main/nl"), roots.get(0));
    }

    @Test
    void getScanRoots_nullSourceBases_returnsEmpty() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBases(null);

        List<File> roots = config.getScanRoots();

        assertTrue(roots.isEmpty());
    }

    @Test
    void getScanRoots_baseNameCaseInsensitive_includesMainAndTest() {
        ReinsConfig config = new ReinsConfig();
        config.setSourceBase("MAIN", new File("src/main/nl"));
        config.setSourceBase("TEST", new File("src/test/nl"));
        config.setSourceBase("DOC", new File("docs"));

        List<File> roots = config.getScanRoots();

        assertEquals(2, roots.size());
        assertTrue(roots.contains(new File("src/main/nl")));
        assertTrue(roots.contains(new File("src/test/nl")));
    }

    @Test
    void noteProperty_getsAndSetsValue() {
        ReinsConfig config = new ReinsConfig();
        assertNull(config.getNote());

        config.setNote("Fix API signature");
        assertEquals("Fix API signature", config.getNote());
    }
}
