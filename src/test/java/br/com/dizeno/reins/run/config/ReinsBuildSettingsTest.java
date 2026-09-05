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

import br.com.dizeno.reins.run.config.settings.TargetSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ReinsBuildSettingsTest {

    @TempDir
    Path tempDir;

    private ReinsConfig createValidConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.setTarget(new TargetSettings());
        config.setSourceBase("main", tempDir.toFile());
        config.getTarget().setTargetBase("main", "target/gen");
        config.getGemini().setApiKey("dummy-key");
        config.getGemini().setModel("gemini-2.0-flash");
        config.getGemini().setEndpoint("https://generativelanguage.googleapis.com");
        return config;
    }

    @Test
    public void testDefaultCompilationThreadsIsOne() {
        ReinsConfig config = new ReinsConfig();
        assertEquals(1, config.getCompilationThreads());
        assertEquals(1, config.getBuild().getCompilationThreads());
    }

    @Test
    public void testPropertiesLoadingCompilationThreads() {
        Properties props = new Properties();
        props.setProperty("reins.compilationThreads", "4");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertEquals(4, config.getCompilationThreads());
    }

    @Test
    public void testCliArgsLoadingCompilationThreads() {
        String[] args = {
                "--compilationThreads", "8"
        };

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(args, null, null, baseDir);

        assertEquals(8, config.getCompilationThreads());
    }

    @Test
    public void testYamlLoadingCompilationThreads() throws IOException {
        File baseDir = tempDir.toFile();
        File yamlFile = new File(baseDir, "reins.yaml");
        String yamlContent = "compilationThreads: 16\n";
        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write(yamlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals(16, config.getCompilationThreads());
    }

    @Test
    public void testValidationCompilationThreadsValid() {
        ReinsConfig config = createValidConfig();
        config.setCompilationThreads(4);

        ConfigValidator validator = new ConfigValidator();
        assertDoesNotThrow(() -> validator.validate(config, tempDir.toFile()));
    }

    @Test
    public void testValidationCompilationThreadsInvalidZero() {
        ReinsConfig config = createValidConfig();
        config.setCompilationThreads(0);

        ConfigValidator validator = new ConfigValidator();
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, tempDir.toFile()));
        assertTrue(ex.getMessage().contains("compilationThreads must be >= 1"));
    }

    @Test
    public void testValidationCompilationThreadsInvalidNegative() {
        ReinsConfig config = createValidConfig();
        config.setCompilationThreads(-2);

        ConfigValidator validator = new ConfigValidator();
        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, tempDir.toFile()));
        assertTrue(ex.getMessage().contains("compilationThreads must be >= 1"));
    }
}
