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
    void acceptsMissingDefaultScanRoots() {
        ReinsConfig config = baseConfig();
        config.setDefaultScanRoots(true);
        config.setScanRoots(List.of(
                projectDir.resolve("src/main/nl").toFile(),
                projectDir.resolve("src/test/nl").toFile()
        ));

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsMissingExplicitScanRoot() {
        ReinsConfig config = baseConfig();
        config.setScanRoots(List.of(projectDir.resolve("missing-root").toFile()));

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Scan root must exist and be a directory: " + projectDir.resolve("missing-root"), error.getMessage());
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
        assertEquals("default (1)", config.getContext().resolveReferenceDepthPolicy().describeForLog());
    }

    @Test
    void acceptsDisabledContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().setReferencesTreeDepth("0");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsUnlimitedContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().setReferencesTreeDepth("*");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsBlankContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().setReferencesTreeDepth("   ");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTreeDepth must be 0, a non-negative integer, or *.", error.getMessage());
    }

    @Test
    void rejectsNegativeContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().setReferencesTreeDepth("-1");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTreeDepth must be 0, a non-negative integer, or *.", error.getMessage());
    }

    @Test
    void rejectsNonNumericContextReferencesTreeDepth() throws IOException {
        ReinsConfig config = validConfig();
        config.getContext().setReferencesTreeDepth("many");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("context.referencesTreeDepth must be 0, a non-negative integer, or *.", error.getMessage());
    }

    @Test
    void rejectsEnableProjectInferenceWithMissingProjectFile() throws IOException {
        ReinsConfig config = validConfig();
        File missingFile = projectDir.resolve("project.md").toFile();
        config.setEnableProjectInference(true);
        config.setProjectContextFile(missingFile);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertTrue(error.getMessage().contains(missingFile.getAbsolutePath()),
                "Expected error message to contain the missing file path");
    }

    @Test
    void acceptsEnableProjectInferenceWithPresentProjectFile() throws IOException {
        ReinsConfig config = validConfig();
        File presentFile = projectDir.resolve("project.md").toFile();
        presentFile.createNewFile();
        config.setEnableProjectInference(true);
        config.setProjectContextFile(presentFile);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsCanonicalTargetProject_whenConfiguredRelativeToProjectRoot() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setProject(new File("compiled/project"));
        config.setTarget(target);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsInferenceNotesToggleInMcpSettings() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setAddReasoningNotes(true);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertTrue(config.getTooling().isAddReasoningNotes());
    }

    @Test
    void acceptsDeprecatedTargetRoot_andEmitsWarning() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setLegacyRootAlias(projectDir.resolve("compiled/project").toFile());
        config.setTarget(target);
        List<String> warnings = new ArrayList<>();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile(), warnings::add));
        assertTrue(warnings.contains("target.root is deprecated; use target.project instead."));
    }

    @Test
    void targetProjectTakesPrecedenceOverDeprecatedRoot() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setProject(projectDir.resolve("compiled/project").toFile());
        target.setLegacyRootAlias(projectDir.resolve("legacy/project").toFile());
        config.setTarget(target);
        List<String> warnings = new ArrayList<>();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile(), warnings::add));
        assertTrue(warnings.stream().anyMatch(msg -> msg.contains("target.project takes precedence")));
    }

    @Test
    void acceptsIndependentTargetMainAndTestPaths() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setProject(projectDir.resolve("compiled/project").toFile());
        target.setMain("build/compiled-main");
        target.setTest("build/compiled-test");
        config.setTarget(target);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsIndependentTargetMainAndTest_whenTheyAreEqual() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setMain("build/compiled-shared");
        target.setTest("build/compiled-shared");
        config.setTarget(target);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsTargetMainThatEscapesProjectBaseDirectory() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setMain("../outside");
        config.setTarget(target);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("target.main must not contain '..' segments: ../outside", error.getMessage());
    }

    @Test
    void acceptsAbsoluteTargetProjectWithinProjectBaseDirectory() throws IOException {
        ReinsConfig config = validConfig();
        TargetSettings target = new TargetSettings();
        target.setProject(projectDir.resolve("compiled/absolute-project").toFile());
        config.setTarget(target);

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
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
        GenerationSettings gc =
                new GenerationSettings();
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
        GenerationSettings gc =
                new GenerationSettings();
        gc.setTemperature(2.5f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("temperature"));
    }

    @Test
    void rejectsTemperatureBelowRange() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc =
                new GenerationSettings();
        gc.setTemperature(-0.1f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("temperature"));
    }

    @Test
    void rejectsTopPAboveRange() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc =
                new GenerationSettings();
        gc.setTopP(1.5f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("topP"));
    }

    @Test
    void rejectsTopKBelowOne() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc =
                new GenerationSettings();
        gc.setTopK(0);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("topK"));
    }

    @Test
    void rejectsPresencePenaltyAboveRange() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc =
                new GenerationSettings();
        gc.setPresencePenalty(3.0f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("presencePenalty"));
    }

    @Test
    void rejectsFrequencyPenaltyBelowRange() throws IOException {
        ReinsConfig config = validConfig();
        GenerationSettings gc =
                new GenerationSettings();
        gc.setFrequencyPenalty(-3.0f);
        config.getGemini().setGeneration(gc);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile()));
        assertTrue(ex.getMessage().contains("frequencyPenalty"));
    }

    @Test
    void acceptsListCompiledTokenInMcpConfig() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setMain("list_compiled");
        config.getTooling().setTest("read,list_compiled");
        config.getTooling().setTarget("list,list_compiled,patch");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void listOnlyConfigurationRemainsValidForMigration() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setMain("list");
        config.getTooling().setTest("list,read");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsValidScriptPathDirectory() throws IOException {
        ReinsConfig config = validConfig();
        Files.createDirectories(projectDir.resolve("scripts"));
        config.getTooling().setScriptPath("scripts");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsMissingScriptPathDirectory() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setScriptPath("missing-scripts");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertTrue(error.getMessage().contains("tooling.scriptPath"));
    }

    @Test
    void rejectsScriptPathOutsideProjectRoot() throws IOException {
        ReinsConfig config = validConfig();
        Path outside = projectDir.resolveSibling("external-scripts");
        Files.createDirectories(outside);
        config.getTooling().setScriptPath(outside.toString());

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertTrue(error.getMessage().contains("project base directory"));
    }

    @Test
    void acceptsRelativeScriptDirResolvedAgainstProjectBaseDir() throws IOException {
        ReinsConfig config = validConfig();
        Files.createDirectories(projectDir.resolve("custom-scripts"));
        config.getReasoning().setScriptsPath("custom-scripts");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void acceptsAbsoluteScriptDir() throws IOException {
        ReinsConfig config = validConfig();
        Path absoluteScripts = projectDir.resolve("absolute-scripts");
        Files.createDirectories(absoluteScripts);
        config.getReasoning().setScriptsPath(absoluteScripts.toString());

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsMissingScriptDir() throws IOException {
        ReinsConfig config = validConfig();
        config.getReasoning().setScriptsPath("missing-custom-scripts");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertTrue(error.getMessage().contains("reasoning.scriptsPath must resolve to an existing directory"));
    }

    @Test
    void invalidMcpTokenErrorListsListCompiledAsSupportedToken() throws IOException {
        ReinsConfig config = validConfig();
        config.getTooling().setMain("list,foo");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertTrue(error.getMessage().contains("Recognized tokens: list, list_compiled, read, write, patch, delete"));
    }

    @Test
    void emptyResponseRetryDelayDefaultsToThousandMs() {
        ReinsConfig config = new ReinsConfig();

        assertEquals(1000, config.getGemini().getEmptyResponseRetryDelayMs());
    }

    @Test
    void negativeEmptyResponseRetryDelayIsClampedToZeroWithWarning() throws IOException {
        ReinsConfig config = validConfig();
        config.getGemini().setEmptyResponseRetryDelayMs(-15);
        List<String> warnings = new ArrayList<>();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile(), warnings::add));
        assertEquals(0, config.getGemini().getEmptyResponseRetryDelayMs());
        assertTrue(warnings.stream().anyMatch(msg -> msg.contains("gemini.emptyResponseRetryDelayMs")));
    }

    @Test
    void acceptsValidOllamaConfiguration() throws IOException {
        ReinsConfig config = validOllamaConfig();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
    }

    @Test
    void rejectsOllamaConfigurationMissingModel() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setModel("   ");

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("Ollama model is required.", error.getMessage());
    }

    @Test
    void rejectsOllamaTimeoutSecondsEqualToZero() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setTimeoutSeconds(0);

        ConfigValidationException error = assertThrows(
                ConfigValidationException.class,
                () -> validator.validate(config, projectDir.toFile())
        );

        assertEquals("ollama.timeoutSeconds must be > 0.", error.getMessage());
    }

    @Test
    void clampsNegativeOllamaRetryAttemptsToZeroWithWarning() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setRetryAttempts(-1);
        List<String> warnings = new ArrayList<>();

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile(), warnings::add));
        assertEquals(0, config.getOllama().getRetryAttempts());
        assertTrue(warnings.stream().anyMatch(msg -> msg.contains("ollama.retryAttempts")));
    }

    @Test
    void defaultsBlankOllamaEndpointToLocalhost() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setEndpoint("   ");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertEquals("http://localhost:11434", config.getOllama().getEndpoint());
    }

    @Test
    void trimsOllamaApiKeyWhitespaceBeforeUse() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setApiKey("  secret-key  ");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertEquals("secret-key", config.getOllama().getApiKey());
    }

    @Test
    void treatsEmptyOllamaApiKeyAsBlank() throws IOException {
        ReinsConfig config = validOllamaConfig();
        config.getOllama().setApiKey("   ");

        assertDoesNotThrow(() -> validator.validate(config, projectDir.toFile()));
        assertEquals(null, config.getOllama().getApiKey());
    }

    private ReinsConfig validConfig() throws IOException {
        ReinsConfig config = baseConfig();
        Path mainNl = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainNl);
        config.setScanRoots(List.of(mainNl.toFile()));
        return config;
    }

    private ReinsConfig baseConfig() {
        ReinsConfig config = new ReinsConfig();
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
        config.setScanRoots(List.of(mainNl.toFile()));
        return config;
    }
}
