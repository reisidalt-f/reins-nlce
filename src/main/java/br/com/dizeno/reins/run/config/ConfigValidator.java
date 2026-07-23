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

import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * ConfigValidator is part of the general application functions in the reins architecture.
 * Acts as a helper utility for validating its prefix constraints.
 */
public class ConfigValidator {
    /**
     * Validates the inputs or files.
     *
     * @param config the Reins configuration settings
     * @param projectBaseDir the project base dir
     */
    public void validate(ReinsConfig config, File projectBaseDir) {
        validate(config, projectBaseDir, msg -> {});
    }

    /**
     * Validates the inputs or files.
     *
     * @param config the Reins configuration settings
     * @param projectBaseDir the project base dir
     * @param warnConsumer the warn consumer
     */
    public void validate(ReinsConfig config, File projectBaseDir, Consumer<String> warnConsumer) {
        if (config == null) {
            throw new ConfigValidationException("Plugin configuration is missing.");
        }
        if (isBlank(config.getProvider())) {
            config.setProvider("gemini");
        }
        String provider = config.getProvider().trim().toLowerCase();
        if (!("gemini".equals(provider) || "ollama".equals(provider) || "stub".equals(provider))) {
            throw new ConfigValidationException("provider must be one of: gemini, ollama, stub.");
        }

        
        if ("gemini".equals(provider)) {
            validateGeminiConfig(config, warnConsumer);
        } else if ("ollama".equals(provider)) {
            validateOllamaConfig(config, warnConsumer);
        }

        List<File> roots = config.getScanRoots();
        if (roots == null || roots.isEmpty()) {
            throw new ConfigValidationException("At least one scan root is required.");
        }
        for (File root : roots) {
            if (root == null) {
                throw new ConfigValidationException("Scan root cannot be null.");
            }
            if (!root.toPath().toAbsolutePath().normalize().startsWith(projectBaseDir.toPath().toAbsolutePath().normalize())) {
                throw new ConfigValidationException("Scan root must be inside project base directory: " + root);
            }
            if (!root.exists()) {
                if (config.isDefaultScanRoots()) {
                    continue;
                }
                throw new ConfigValidationException("Scan root must exist and be a directory: " + root);
            }
            if (!root.isDirectory()) {
                throw new ConfigValidationException("Scan root must exist and be a directory: " + root);
            }
        }
        validateConfiguredBaseRoot(config.getMainNlRoot(), projectBaseDir, "mainNlRoot");
        validateConfiguredBaseRoot(config.getTestNlRoot(), projectBaseDir, "testNlRoot");
        ToolingSettings tooling = config.getTooling();
        ReasoningSettings reasoning = config.getReasoning();
        validateScriptsPath(reasoning == null ? null : reasoning.getScriptsPath(), projectBaseDir);
        TargetSettings targetSettings = config.getTarget();
        validateTargetSettings(targetSettings, projectBaseDir, warnConsumer);
        if (reasoning != null && reasoning.getEnabled() != null) {
            throw new ConfigValidationException("reasoning.enable is no longer supported; inference is always multi-turn.");
        }
        if (reasoning != null && reasoning.getMaxTurns() <= 0) {
            throw new ConfigValidationException("reasoning.maxTurns must be > 0.");
        }
        if (reasoning != null && reasoning.getMaxReferenceDepth() <= 0) {
            throw new ConfigValidationException("reasoning.maxReferenceDepth must be > 0.");
        }
        validateReferenceTreeDepth(config);
        validateToolingOperationConfig(tooling, projectBaseDir);
        validateLoggingSettings(config);
        
        
        
        if (config.isEnableProjectInference() && config.getProjectContextFile() != null
                && !config.getProjectContextFile().exists()) {
            throw new ConfigValidationException(
                    "enableProjectInference is true but project file not found: "
                            + config.getProjectContextFile().getAbsolutePath());
        }
    }

    private void validateTargetSettings(TargetSettings targetSettings,
                                        File projectBaseDir,
                                        Consumer<String> warnConsumer) {
        if (targetSettings == null) {
            return;
        }

        if (targetSettings.getProject() != null && targetSettings.getLegacyRootAlias() != null) {
            warnConsumer.accept("Both target.project and deprecated target.root are configured; target.project takes precedence and target.root is ignored.");
        } else if (targetSettings.getLegacyRootAlias() != null) {
            warnConsumer.accept("target.root is deprecated; use target.project instead.");
        }

        if (targetSettings.getProject() != null) {
            validateConfiguredBaseRoot(targetSettings.getProject(), projectBaseDir, "target.project");
        } else {
            validateConfiguredBaseRoot(targetSettings.getLegacyRootAlias(), projectBaseDir, "target.root");
        }

        validateIndependentTargetPath(targetSettings.getMain(), projectBaseDir, "target.main");
        validateIndependentTargetPath(targetSettings.getTest(), projectBaseDir, "target.test");
    }

    private void validateReferenceTreeDepth(ReinsConfig config) {
        ContextSettings context = config.getContext();
        if (context == null) {
            return;
        }
        try {
            context.resolveReferenceDepthPolicy();
        } catch (IllegalArgumentException ex) {
            throw new ConfigValidationException(ReferenceDepthPolicy.validValuesMessage());
        }
    }

    private void validateToolingOperationConfig(ToolingSettings tooling, File projectBaseDir) {
        if (tooling == null) {
            return;
        }
        validateToolingAddReasoningNotes(tooling);
        
        if (!isBlank(tooling.getMain())) {
            validateMcpTokens(tooling.getMain(), "tooling.main");
        }
        
        if (!isBlank(tooling.getTest())) {
            validateMcpTokens(tooling.getTest(), "tooling.test");
        }
        
        if (!isBlank(tooling.getTarget())) {
            validateMcpTokens(tooling.getTarget(), "tooling.target");
        }
        validateToolingScriptPath(tooling.getScriptPath(), projectBaseDir);
    }

    private void validateToolingAddReasoningNotes(ToolingSettings tooling) {
        
        
        boolean addReasoningNotes = tooling.isAddReasoningNotes();
        if (addReasoningNotes && isBlank(tooling.getMain()) && isBlank(tooling.getTest()) && isBlank(tooling.getTarget())) {
            
        }
    }

    private void validateScriptsPath(String scriptsPath, File projectBaseDir) {
        if (isBlank(scriptsPath)) {
            return;
        }

        Path projectRoot = projectBaseDir.toPath().toAbsolutePath().normalize();
        Path configured = Path.of(scriptsPath.trim());
        Path resolved = configured.isAbsolute()
                ? configured.normalize()
                : projectRoot.resolve(configured).normalize();

        if (!resolved.startsWith(projectRoot)) {
            throw new ConfigValidationException("reasoning.scriptsPath must resolve within project base directory: " + scriptsPath);
        }

        if (!Files.exists(resolved) || !Files.isDirectory(resolved)) {
            throw new ConfigValidationException("reasoning.scriptsPath must resolve to an existing directory: " + scriptsPath);
        }
        if (!Files.isReadable(resolved)) {
            throw new ConfigValidationException("reasoning.scriptsPath must be readable: " + scriptsPath);
        }
    }

    private void validateToolingScriptPath(String scriptPath, File projectBaseDir) {
        if (isBlank(scriptPath)) {
            return;
        }

        Path projectRoot = projectBaseDir.toPath().toAbsolutePath().normalize();
        Path configured = Path.of(scriptPath.trim());
        Path resolved = configured.isAbsolute()
                ? configured.normalize()
                : projectRoot.resolve(configured).normalize();

        if (!resolved.startsWith(projectRoot)) {
            throw new ConfigValidationException("tooling.scriptPath must resolve within project base directory: " + scriptPath);
        }
        if (!Files.exists(resolved)) {
            throw new ConfigValidationException("tooling.scriptPath must resolve to an existing directory: " + scriptPath);
        }
        if (!Files.isDirectory(resolved)) {
            throw new ConfigValidationException("tooling.scriptPath must resolve to a directory: " + scriptPath);
        }
        if (!Files.isReadable(resolved)) {
            throw new ConfigValidationException("tooling.scriptPath must be readable: " + scriptPath);
        }
    }

    private void validateMcpTokens(String tokenString, String fieldName) {
        String[] parts = tokenString.split("\\s*,\\s*");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            
            String normalizedToken = part.trim().toLowerCase();
            boolean valid = normalizedToken.equals("list") || normalizedToken.equals("list_compiled") || normalizedToken.equals("read")
                    || normalizedToken.equals("write") || normalizedToken.equals("patch")
                    || normalizedToken.equals("delete");
            if (!valid) {
                throw new ConfigValidationException(
                    "Unrecognized MCP operation token '" + part + "' in " + fieldName + ". " +
                    "Recognized tokens: list, list_compiled, read, write, patch, delete"
                );
            }
        }
    }

    private void validateIndependentTargetPath(String subPath, File projectBaseDir, String fieldName) {
        if (subPath == null || subPath.isBlank()) {
            return;
        }
        for (String segment : subPath.replace("\\", "/").split("/")) {
            if ("..".equals(segment)) {
                throw new ConfigValidationException(fieldName + " must not contain '..' segments: " + subPath);
            }
        }
        java.nio.file.Path projectRootPath = projectBaseDir.toPath().toAbsolutePath().normalize();
        java.nio.file.Path configuredPath = java.nio.file.Path.of(subPath);
        java.nio.file.Path resolved = configuredPath.isAbsolute()
                ? configuredPath.normalize()
                : projectRootPath.resolve(configuredPath).normalize();
        if (!resolved.startsWith(projectRootPath)) {
            throw new ConfigValidationException(fieldName + " must resolve within project base directory: " + subPath);
        }
        if (resolved.toFile().exists() && !resolved.toFile().isDirectory()) {
            throw new ConfigValidationException(fieldName + " must be a directory when it exists: " + subPath);
        }
    }

    private void validateConfiguredBaseRoot(File root, File projectBaseDir, String fieldName) {
        if (root == null) {
            return;
        }
        File normalizedRoot = root.isAbsolute() ? root : new File(projectBaseDir, root.getPath());
        validateDirectoryPath(normalizedRoot, projectBaseDir, fieldName, root);
    }

    private void validateDirectoryPath(File resolvedRoot, File projectBaseDir, String fieldName, File displayRoot) {
        if (!resolvedRoot.toPath().toAbsolutePath().normalize()
                .startsWith(projectBaseDir.toPath().toAbsolutePath().normalize())) {
            throw new ConfigValidationException(fieldName + " must be inside project base directory: " + displayRoot);
        }
        if (resolvedRoot.exists() && !resolvedRoot.isDirectory()) {
            throw new ConfigValidationException(fieldName + " must be a directory when it exists: " + displayRoot);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateGeneration(GenerationSettings gc) {
        if (gc == null) {
            return;
        }
        if (gc.getTemperature() != null) {
            float t = gc.getTemperature();
            if (t < 0.0f || t > 2.0f) {
                throw new ConfigValidationException(
                        "gemini.generation.temperature must be between 0.0 and 2.0, got: " + t);
            }
        }
        if (gc.getTopP() != null) {
            float p = gc.getTopP();
            if (p < 0.0f || p > 1.0f) {
                throw new ConfigValidationException(
                        "gemini.generation.topP must be between 0.0 and 1.0, got: " + p);
            }
        }
        if (gc.getTopK() != null) {
            int k = gc.getTopK();
            if (k < 1) {
                throw new ConfigValidationException(
                        "gemini.generation.topK must be >= 1, got: " + k);
            }
        }
        if (gc.getPresencePenalty() != null) {
            float pp = gc.getPresencePenalty();
            if (pp < -2.0f || pp > 2.0f) {
                throw new ConfigValidationException(
                        "gemini.generation.presencePenalty must be between -2.0 and 2.0, got: " + pp);
            }
        }
        if (gc.getFrequencyPenalty() != null) {
            float fp = gc.getFrequencyPenalty();
            if (fp < -2.0f || fp > 2.0f) {
                throw new ConfigValidationException(
                        "gemini.generation.frequencyPenalty must be between -2.0 and 2.0, got: " + fp);
            }
        }
    }

    private void validateEndpoint(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ConfigValidationException("gemini.endpoint must be an absolute http or https URL.");
            }
        } catch (URISyntaxException ex) {
            throw new ConfigValidationException("gemini.endpoint must be an absolute http or https URL.");
        }
    }

    private void validateLoggingSettings(ReinsConfig config) {
        if (config.getLogging() == null) {
            config.setLogging(new LoggingSettings());
        }
    }

    private void validateGeminiConfig(ReinsConfig config, Consumer<String> warnConsumer) {
        GeminiSettings gemini = config.getGemini();
        if (gemini == null) {
            throw new ConfigValidationException("Gemini configuration section is missing.");
        }
        if (isBlank(gemini.getApiKey())) {
            throw new ConfigValidationException("Gemini apiKey is required.");
        }
        if (isBlank(gemini.getModel())) {
            throw new ConfigValidationException("Gemini model is required.");
        }
        if (isBlank(gemini.getEndpoint())) {
            throw new ConfigValidationException("Gemini endpoint is required.");
        }
        validateEndpoint(gemini.getEndpoint());
        if (gemini.getTimeoutSeconds() <= 0) {
            throw new ConfigValidationException("gemini.timeoutSeconds must be > 0.");
        }
        if (gemini.getMaximumTurns() != null && gemini.getMaximumTurns() < 1) {
            throw new ConfigValidationException("gemini.maximumTurns must be >= 1.");
        }
        if (gemini.getRetryAttempts() < 0) {
            gemini.setRetryAttempts(0);
            warnConsumer.accept("gemini.retryAttempts was negative and has been clamped to 0.");
        }
        if (gemini.getEmptyResponseRetryDelayMs() < 0) {
            gemini.setEmptyResponseRetryDelayMs(0);
            warnConsumer.accept("gemini.emptyResponseRetryDelayMs was negative and has been clamped to 0.");
        }
        validateGeneration(gemini.getGeneration());
    }

    private void validateOllamaConfig(ReinsConfig config, Consumer<String> warnConsumer) {
        OllamaSettings ollama = config.getOllama();
        if (ollama == null) {
            throw new ConfigValidationException("Ollama configuration section is missing.");
        }
        if (isBlank(ollama.getModel())) {
            throw new ConfigValidationException("Ollama model is required.");
        }
        if (isBlank(ollama.getEndpoint())) {
            ollama.setEndpoint("http://localhost:11434");
        }
        validateEndpoint(ollama.getEndpoint());
        if (ollama.getTimeoutSeconds() <= 0) {
            throw new ConfigValidationException("ollama.timeoutSeconds must be > 0.");
        }
        if (ollama.getRetryAttempts() < 0) {
            ollama.setRetryAttempts(0);
            warnConsumer.accept("ollama.retryAttempts was negative and has been clamped to 0.");
        }
        if (ollama.getApiKey() != null) {
            String trimmed = ollama.getApiKey().trim();
            ollama.setApiKey(trimmed.isEmpty() ? null : trimmed);
        }
    }
}

