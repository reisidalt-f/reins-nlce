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

package br.com.dizeno.reins.reasoning.service;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.ReasoningRequest;

import java.nio.file.Path;

/**
 * ReasoningConfigResolver is part of the extracted reasoning helper services
 * for scripts, attachments, MCP tools, configuration, and state in the reins
 * architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class ReasoningConfigResolver {

    /**
     * Resolves the configured value or path max turns.
     *
     * @param config the Reins configuration settings
     * @return the numeric value
     */
    public int resolveMaxTurns(ReinsConfig config) {
        String provider = config == null ? null : config.getProvider();
        boolean isGemini = provider == null || "gemini".equalsIgnoreCase(provider.trim());
        if (isGemini && config != null && config.getGemini() != null && config.getGemini().getMaximumTurns() != null) {
            return config.getGemini().resolveMaximumTurns();
        }
        ReasoningSettings settings = config == null ? null : config.getReasoning();
        if (settings == null || settings.getMaxTurns() <= 0) {
            return 1;
        }
        return settings.getMaxTurns();
    }

    /**
     * Checks if the component is non fulfillment attempt budget exceeded.
     *
     * @param config           the Reins configuration settings
     * @param nextAttemptIndex the next attempt index
     * @return true if successful or matching, false otherwise
     */
    public boolean isNonFulfillmentAttemptBudgetExceeded(ReinsConfig config, int nextAttemptIndex) {
        int configuredRetries = 0;
        if (config != null && config.getGemini() != null) {
            configuredRetries = Math.max(0, config.getGemini().getRetryAttempts());
        }
        int maxAttemptsPerTurn = configuredRetries + 1;
        return nextAttemptIndex > maxAttemptsPerTurn;
    }

    /**
     * Checks if the component is reasoning log enabled.
     *
     * @param config the Reins configuration settings
     * @return true if successful or matching, false otherwise
     */
    public boolean isReasoningLogEnabled(ReinsConfig config) {
        return config != null
                && config.getReasoning() != null
                && config.getReasoning().isEnableReasoningLog();
    }

    /**
     * Resolves the configured value or path project root.
     *
     * @param request the request containing path and scope metadata
     * @return the resolved or constructed object
     */
    public Path resolveProjectRoot(ReasoningRequest request) {
        if (request != null && request.getProjectRoot() != null) {
            return request.getProjectRoot();
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    /**
     * Checks if the component is cached content enabled.
     *
     * @param config the Reins configuration settings
     * @return true if successful or matching, false otherwise
     */
    public boolean isCachedContentEnabled(ReinsConfig config) {
        if (config == null || config.getContext() == null) {
            return true;
        }
        return config.getContext().isCachedContent();
    }

    /**
     * Checks if the component is turn count note enabled.
     *
     * @param config the Reins configuration settings
     * @return true if successful or matching, false otherwise
     */
    public boolean isTurnCountNoteEnabled(ReinsConfig config) {
        if (config == null || config.getReasoning() == null) {
            return true;
        }
        return config.getReasoning().isTurnCountNote();
    }

    /**
     * Resolves the configured value or path source base.
     *
     * @param sourceScope the source scope
     * @return the string result
     */
    public String resolveSourceBase(String sourceScope) {
        if (sourceScope == null || sourceScope.isBlank()) {
            return "main";
        }
        if ("test".equals(sourceScope)) {
            return "test";
        }
        return "main";
    }

    /**
     * Resolves the configured value or path scripts path.
     *
     * @param config the Reins configuration settings
     * @return the string result
     */
    public String resolveScriptsPath(ReinsConfig config) {
        if (config == null || config.getReasoning() == null) {
            return null;
        }
        String configured = config.getReasoning().getScriptsPath();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return configured.trim();
    }

    /**
     * Builds the configured target config fingerprint.
     *
     * @param config the Reins configuration settings
     * @return the string result
     */
    public String buildConfigFingerprint(ReinsConfig config) {
        if (config == null) {
            return "";
        }
        String value = (config.resolveModel() == null ? "" : config.resolveModel())
                + "|" + (config.getProvider() == null ? "" : config.getProvider())
                + "|" + (config.getReasoning() == null ? "" : String.valueOf(config.getReasoning().getMaxTurns()))
                + "|" + (config.getContext() == null ? "" : String.valueOf(config.getContext().isCachedContent()))
                + "|" + resolveScriptsPath(config);
        return Integer.toHexString(value.hashCode());
    }

    /**
     * Builds the configured target policy fingerprint.
     *
     * @param policy the policy
     * @return the string result
     */
    public String buildPolicyFingerprint(FilePolicy policy) {
        if (policy == null) {
            return "";
        }
        String value = policy.getPermissionsForBase(FilePolicy.Base.MAIN).toString()
                + "|" + policy.getPermissionsForBase(FilePolicy.Base.TEST)
                + "|" + policy.getPermissionsForBase(FilePolicy.Base.TARGET);
        return Integer.toHexString(value.hashCode());
    }

    /**
     * Resolves the configured value or path main source qualified path.
     *
     * @param request the request containing path and scope metadata
     * @return the string result
     */
    public String resolveMainSourceQualifiedPath(ReasoningRequest request) {
        if (request != null && request.getMainSourceQualifiedPath() != null
                && !request.getMainSourceQualifiedPath().isBlank()) {
            return request.getMainSourceQualifiedPath();
        }
        String scope = request == null ? null : request.getSourceScope();
        String sourcePath = request == null ? "" : request.getSourcePath();
        String base = "test".equalsIgnoreCase(scope) ? "test" : "main";
        String relativePath = sourcePath == null ? "" : sourcePath;

        if ("main".equals(base) && relativePath.startsWith("src/main/nl/")) {
            relativePath = relativePath.substring("src/main/nl/".length());
        } else if ("test".equals(base) && relativePath.startsWith("src/test/nl/")) {
            relativePath = relativePath.substring("src/test/nl/".length());
        }
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return base + ":" + relativePath;
    }

    /**
     * Builds the configured target tool policy.
     *
     * @param config the Reins configuration settings
     * @return the resolved or constructed object
     */
    public FilePolicy buildToolPolicy(ReinsConfig config) {
        ToolingSettings settings = config.getTooling();
        if (settings == null) {
            settings = new ToolingSettings();
        }
        return new FilePolicy(settings);
    }
}
