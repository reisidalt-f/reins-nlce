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

import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;

 
/**
 * ContextMessageBuildRequest is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing context message build request.
 */
public class ContextMessageBuildRequest {
    private final String sourcePath;
    private final String sourceHash;
    private final String configFingerprint;
    private final String policyFingerprint;
    private final boolean cachedContentEnabled;
    private final ReasoningScriptContext scriptContext;

    /**
     * Constructs a new instance of {@link ContextMessageBuildRequest}.
     *
     * @param sourcePath the path of the source file
     * @param sourceHash the source hash
     * @param configFingerprint the config fingerprint
     * @param policyFingerprint the policy fingerprint
     * @param cachedContentEnabled the cached content enabled
     * @param scriptContext the script context
     */
    public ContextMessageBuildRequest(String sourcePath,
                                      String sourceHash,
                                      String configFingerprint,
                                      String policyFingerprint,
                                      boolean cachedContentEnabled,
                                      ReasoningScriptContext scriptContext) {
        this.sourcePath = sourcePath;
        this.sourceHash = sourceHash;
        this.configFingerprint = configFingerprint;
        this.policyFingerprint = policyFingerprint;
        this.cachedContentEnabled = cachedContentEnabled;
        this.scriptContext = scriptContext;
    }

    /**
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Gets the source hash.
     *
     * @return the string result
     */
    public String getSourceHash() {
        return sourceHash;
    }

    /**
     * Gets the config fingerprint.
     *
     * @return the string result
     */
    public String getConfigFingerprint() {
        return configFingerprint;
    }

    /**
     * Gets the policy fingerprint.
     *
     * @return the string result
     */
    public String getPolicyFingerprint() {
        return policyFingerprint;
    }

    /**
     * Checks if the component is cached content enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isCachedContentEnabled() {
        return cachedContentEnabled;
    }

    /**
     * Gets the script context.
     *
     * @return the resulting context
     */
    public ReasoningScriptContext getScriptContext() {
        return scriptContext;
    }
}
