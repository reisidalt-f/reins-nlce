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

package br.com.dizeno.reins.reasoning.scripting;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ScriptRunnerConfig is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ScriptRunnerConfig {
    public static final int DEFAULT_OUTPUT_TRUNCATION_LIMIT = 32_768;

    private final Path projectRoot;
    private final Path scriptRoot;
    private final boolean enabled;
    private final int outputTruncationLimit;

    private ScriptRunnerConfig(Path projectRoot, Path scriptRoot, boolean enabled, int outputTruncationLimit) {
        this.projectRoot = projectRoot;
        this.scriptRoot = scriptRoot;
        this.enabled = enabled;
        this.outputTruncationLimit = outputTruncationLimit;
    }

    /**
     * Disabled.
     *
     * @return the resulting config
     */
    public static ScriptRunnerConfig disabled() {
        return new ScriptRunnerConfig(null, null, false, DEFAULT_OUTPUT_TRUNCATION_LIMIT);
    }

    /**
     * From Settings.
     *
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @return the resulting config
     */
    public static ScriptRunnerConfig fromSettings(ReinsConfig config, Path projectRoot) {
        if (config == null) {
            return disabled();
        }
        String path = null;
        if (config.getTooling() != null && config.getTooling().getScriptPath() != null && !config.getTooling().getScriptPath().isBlank()) {
            path = config.getTooling().getScriptPath().trim();
        } else if (config.getReasoning() != null && config.getReasoning().getScriptsPath() != null && !config.getReasoning().getScriptsPath().isBlank()) {
            path = config.getReasoning().getScriptsPath().trim();
        }

        if (path == null) {
            return disabled();
        }
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        Path configured = Path.of(path);
        Path resolved = configured.isAbsolute()
                ? configured.normalize()
                : normalizedProjectRoot.resolve(configured).normalize();
        return new ScriptRunnerConfig(normalizedProjectRoot, resolved, true, DEFAULT_OUTPUT_TRUNCATION_LIMIT);
    }

    /**
     * From Settings.
     *
     * @param settings the settings
     * @param projectRoot the root path of the project
     * @return the resulting config
     */
    @Deprecated
    public static ScriptRunnerConfig fromSettings(ToolingSettings settings, Path projectRoot) {
        return disabled();
    }

    /**
     * Resolves the configured value or path script path.
     *
     * @param rawPath the raw path
     * @return the resolved or constructed object
     */
    public Path resolveScriptPath(String rawPath) throws Exception {
        if (!enabled || scriptRoot == null) {
            throw new IllegalStateException("Operation run_script is not available.");
        }
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("Script identifier is required.");
        }

        String normalized = normalizeScriptRequestPath(rawPath);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Script identifier is required.");
        }

        Path requested = Path.of(normalized);
        Path candidate = requested.isAbsolute()
                ? requested.toAbsolutePath().normalize()
                : scriptRoot.resolve(requested).toAbsolutePath().normalize();
        if (!candidate.startsWith(scriptRoot.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Script path escapes configured script root: " + rawPath);
        }

        if (Files.exists(candidate)) {
            Path realRoot = scriptRoot.toRealPath();
            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realRoot)) {
                throw new IllegalArgumentException("Script path escapes configured script root via symlink: " + rawPath);
            }
        }
        return candidate;
    }

    /**
     * To Project Relative Display Path.
     *
     * @param path the file or directory path
     * @return the string result
     */
    public String toProjectRelativeDisplayPath(Path path) {
        if (path == null) {
            return null;
        }
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (projectRoot == null) {
            return PathNormalizer.toForwardSlashes(normalizedPath.toString());
        }
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(normalizedProjectRoot)) {
            return PathNormalizer.toForwardSlashes(normalizedPath.toString());
        }
        return PathNormalizer.toForwardSlashes(normalizedProjectRoot.relativize(normalizedPath).toString());
    }

    private String normalizeScriptRequestPath(String rawPath) {
        String normalized = PathNormalizer.toForwardSlashes(rawPath.trim());
        if (normalized.regionMatches(true, 0, "script:", 0, "script:".length())) {
            normalized = normalized.substring("script:".length());
        }
        return normalized;
    }

    /**
     * Gets the script root.
     *
     * @return the resolved or constructed object
     */
    public Path getScriptRoot() {
        return scriptRoot;
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Checks if the component is enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the output truncation limit.
     *
     * @return the numeric value
     */
    public int getOutputTruncationLimit() {
        return outputTruncationLimit;
    }
}
