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
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValidatorTest {
    private final ConfigValidator validator = new ConfigValidator();

    @TempDir
    Path projectDir;

    @Test
    void acceptsValidConfiguration() throws IOException {
        ReinsConfig config = validConfig();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsMissingSourceMain() {
        ReinsConfig config = baseConfig();

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Invalid configuration: source.main is required and must be explicitly configured.", error.getMessage());
    }

    @Test
    void rejectsMissingExplicitSourceBaseDir() {
        ReinsConfig config = baseConfig();
        config.setSourceBase("main", projectDir.resolve("missing-root").toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Source root must exist and be a directory: " + projectDir.resolve("missing-root"), error.getMessage());
    }

    @Test
    void rejectsSourceBaseWithoutMatchingTargetBase() throws IOException {
        ReinsConfig config = validConfig();
        Path libNl = projectDir.resolve("src/lib/nl");
        Files.createDirectories(libNl);
        config.setSourceBase("lib", libNl.toFile());

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Source base 'lib' is defined but has no corresponding target path defined in target.lib", error.getMessage());
    }

    @Test
    void rejectsMissingApiKey() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setApiKey("   ");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Gemini apiKey is required.", error.getMessage());
    }

    @Test
    void rejectsInvalidEndpoint() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setEndpoint("not-a-url");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("gemini.endpoint must be an absolute http or https URL.", error.getMessage());
    }

    @Test
    void rejectsNonPositiveMaxReferenceDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getReasoning().setMaxReferenceDepth(0);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("reasoning.maxReferenceDepth must be > 0.", error.getMessage());
    }

    @Test
    void rejectsLegacyReasoningEnableWhenPresent() throws IOException {
        ReinsConfig config = validConfig();
        config.getReasoning().setEnabled(Boolean.TRUE);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("reasoning.enable is no longer supported; inference is always multi-turn.", error.getMessage());
    }

    @Test
    void rejectsMaximumTurnsBelowOne() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setMaximumTurns(0);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("gemini.maximumTurns must be >= 1.", error.getMessage());
    }

    @Test
    void acceptsMissingMaximumTurnsAndUsesFeatureDefault() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setMaximumTurns(null);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertEquals(1, config.getGemini().resolveMaximumTurns());
    }

    @Test
    void acceptsDefaultContextReferencesTreeDepthWhenOmitted() throws IOException {
        ReinsConfig config = validConfig();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertEquals("bounded (3)", config.getContext().getReferencesTree().resolveReferenceDepthPolicy().describeForLog());
    }

    @Test
    void acceptsDisabledContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().getReferencesTree().setDepth("0");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsUnlimitedContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().getReferencesTree().setDepth("*");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsBlankContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().getReferencesTree().setDepth("   ");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTree.depth must be 0, a non-negative integer, or *.", error.getMessage());
    }

    @Test
    void rejectsNegativeContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().getReferencesTree().setDepth("-1");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTree.depth must be 0, a non-negative integer, or *.", error.getMessage());
    }

    @Test
    void rejectsNonNumericContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().getReferencesTree().setDepth("many");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTree.depth must be 0, a non-negative integer, or *.", error.getMessage());
    }





    @Test
    void acceptsInferenceNotesToggleInFileToolsSettings() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setAddReasoningNotes(true);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertTrue(config.getTooling().isAddReasoningNotes());
    }

    @Test
    void acceptsIndependentTargetMainAndTestPaths() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "build/compiled-main");
        target.setTargetBase("test", "build/compiled-test");
        config.setTarget(target);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsTargetMainThatEscapesProjectBaseDirectory() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "../outside");
        config.setTarget(target);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("target.main must not contain '..' segments: ../outside", error.getMessage());
    }

    @Test
    void acceptsAbsentCompilationConfig() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setGeneration(null);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsEmptyCompilationConfig() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setGeneration(new GenerationSettings());

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsValidCompilationConfig() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc = new GenerationSettings();
        gc.setTemperature(1.0f);
        gc.setTopP(0.5f);
        gc.setTopK(10);
        gc.setPresencePenalty(0.0f);
        gc.setFrequencyPenalty(-1.0f);
        config.getGemini().setGeneration(gc);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsTemperatureAboveRange() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc = new GenerationSettings();
        gc.setTemperature(2.5f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("temperature"));
    }

    @Test
    void acceptsValidOllamaConfiguration() throws IOException {
        ReinsConfig config = validOllamaConfig();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsMissingProvider() throws IOException {
        ReinsConfig config = validConfig();
        config.setProvider(null);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("provider is required and must be explicitly configured.", error.getMessage());
    }

    private ReinsConfig validConfig() throws IOException {
        ReinsConfig config = baseConfig();
        Path mainNl = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        config.setSourceBase("main", mainNl.toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        return config;
    }

    private ReinsConfig baseConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        gemini.setTimeoutSeconds(30);
        gemini.setRetryAttempts(1);

        config.setGemini(gemini);
        config.setIncludePattern("**/*.md");

        return config;
    }

    private ReinsConfig validOllamaConfig() throws IOException {
        ReinsConfig config = new ReinsConfig();
        OllamaSettings ollama = new OllamaSettings();
        ollama.setModel("llama3");
        ollama.setEndpoint("http://localhost:11434");
        ollama.setTimeoutSeconds(30);
        ollama.setRetryAttempts(1);

        config.setProvider("ollama");
        config.setOllama(ollama);
        config.setIncludePattern("**/*.md");

        Path mainNl = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        config.setSourceBase("main", mainNl.toFile());
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        return config;
    }
}
