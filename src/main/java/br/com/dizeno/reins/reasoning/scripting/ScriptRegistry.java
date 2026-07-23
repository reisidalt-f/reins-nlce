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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

 
/**
 * ScriptRegistry is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing script registry.
 */
public class ScriptRegistry {

     
    static final Map<String, PhaseType> REQUIRED_SCRIPTS;
    static final Map<String, PhaseType> REQUIRED_PER_SOURCE_SCRIPTS;
    static final Map<String, PhaseType> REQUIRED_SOURCE_BASE_SCRIPTS;

    static {
        Map<String, PhaseType> m = new LinkedHashMap<>();
        m.put(PhaseType.SCRIPT_REASONING_PIPELINE,     PhaseType.PROMPT_ASSEMBLY);
        m.put(PhaseType.SCRIPT_PHASE_LIST,             PhaseType.PHASE_LIST);
        m.put(PhaseType.SCRIPT_SOURCE_BASE_PHASE_LIST, PhaseType.PHASE_LIST);
        m.put(PhaseType.SCRIPT_SYSTEM_CONTEXT,         PhaseType.PROMPT_ASSEMBLY);
        m.put(PhaseType.SCRIPT_REFERENCE_TREE,         PhaseType.PROMPT_ASSEMBLY);
        m.put(PhaseType.SCRIPT_PROJECT_CONTEXT,        PhaseType.PROMPT_ASSEMBLY);
        m.put(PhaseType.SCRIPT_ATTACHMENT_LIST,        PhaseType.ATTACHMENT_LIST);
        m.put(PhaseType.SCRIPT_FILE_LIST,              PhaseType.FILE_LIST);
        m.put(PhaseType.SCRIPT_TOOL_RESULT,             PhaseType.TOOL_RESPONSE);
        m.put(PhaseType.SCRIPT_MAX_TURN_GRACE_PROMPT,  PhaseType.PROMPT_ASSEMBLY);
        m.put(PhaseType.SCRIPT_RETRY_MESSAGE,          PhaseType.PROMPT_ASSEMBLY);
        REQUIRED_SCRIPTS = Collections.unmodifiableMap(m);

        Map<String, PhaseType> perSource = new LinkedHashMap<>(m);
        perSource.remove(PhaseType.SCRIPT_SOURCE_BASE_PHASE_LIST);
        REQUIRED_PER_SOURCE_SCRIPTS = Collections.unmodifiableMap(perSource);

        Map<String, PhaseType> sourceBase = new LinkedHashMap<>();
        sourceBase.put(PhaseType.SCRIPT_SOURCE_BASE_PHASE_LIST, PhaseType.PHASE_LIST);
        sourceBase.put(PhaseType.SCRIPT_SYSTEM_CONTEXT,         PhaseType.PROMPT_ASSEMBLY);
        sourceBase.put(PhaseType.SCRIPT_TOOL_RESULT,             PhaseType.TOOL_RESPONSE);
        sourceBase.put(PhaseType.SCRIPT_MAX_TURN_GRACE_PROMPT,  PhaseType.PROMPT_ASSEMBLY);
        REQUIRED_SOURCE_BASE_SCRIPTS = Collections.unmodifiableMap(sourceBase);
    }

    private final Map<String, ScriptDescriptor> scripts;
    private final Map<String, List<ScriptDescriptor>> sourceChainCache;
    private final List<String> validationErrors;
    private final boolean ready;
    private final ScriptResolver resolver;

    private ScriptRegistry(Map<String, ScriptDescriptor> scripts,
                           List<String> validationErrors,
                           ScriptResolver resolver) {
        this.scripts = new LinkedHashMap<>(scripts);
        this.sourceChainCache = new LinkedHashMap<>();
        this.validationErrors = Collections.unmodifiableList(validationErrors);
        this.ready = validationErrors.isEmpty();
        this.resolver = resolver;
    }

     
    /**
     * Builds the configured target.
     *
     * @param resolver the resolver
     * @return the resolved or constructed object
     */
    public static ScriptRegistry build(ScriptResolver resolver) {
        return build(resolver, null);
    }

     
    /**
     * Builds the configured target.
     *
     * @param resolver the resolver
     * @param customScriptDir the custom script dir
     * @return the resolved or constructed object
     */
    public static ScriptRegistry build(ScriptResolver resolver, java.io.File customScriptDir) {
        Map<String, ScriptDescriptor> scripts = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        if (customScriptDir != null) {
            if (!customScriptDir.exists() || !customScriptDir.isDirectory()) {
                errors.add("Cannot resolve custom script directory: " + customScriptDir.getPath());
            }
        }

        for (Map.Entry<String, PhaseType> entry : REQUIRED_SCRIPTS.entrySet()) {
            String name = entry.getKey();
            PhaseType phaseType = entry.getValue();
            try {
                ScriptDescriptor descriptor = resolver.resolve(name, phaseType);
                scripts.put(name, descriptor);
            } catch (IOException e) {
                errors.add("Failed to load script '" + name + "': " + e.getMessage());
            }
        }

        return new ScriptRegistry(scripts, errors, resolver);
    }

     
    /**
     * Checks if the component is ready.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isReady() {
        return ready;
    }

     
    /**
     * Gets the validation errors.
     *
     * @return the string result
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

     
    /**
     * Validates the inputs or files all.
     *
     */
    public void validateAll() {
        if (validationErrors.isEmpty()) {
            return;
        }
        throw new IllegalStateException(String.join("\n", validationErrors));
    }

     
    /**
     * Gets the script.
     *
     * @param name the name
     * @return the resolved or constructed object
     */
    public synchronized ScriptDescriptor getScript(String name) {
        return scripts.get(name);
    }

     
    /**
     * Gets the or resolve script.
     *
     * @param name the name
     * @return the resolved or constructed object
     */
    public synchronized ScriptDescriptor getOrResolveScript(String name) {
        ScriptDescriptor existing = scripts.get(name);
        if (existing != null || resolver == null || name == null || name.isBlank()) {
            return existing;
        }

        try {
            ScriptDescriptor resolved = resolver.resolve(name, inferPhaseType(name));
            scripts.put(name, resolved);
            return resolved;
        } catch (IOException ignored) {
            return null;
        }
    }

     
    /**
     * Resolves the configured value or path source chain.
     *
     * @param scriptName the script name
     * @return the collection of elements
     */
    public synchronized List<ScriptDescriptor> resolveSourceChain(String scriptName) throws IOException {
        if (scriptName == null || scriptName.isBlank() || resolver == null) {
            return List.of();
        }

        List<ScriptDescriptor> cached = sourceChainCache.get(scriptName);
        if (cached != null) {
            return cached;
        }

        PhaseType phaseType = inferPhaseType(scriptName);
        List<ScriptDescriptor> resolved = new ArrayList<>();
        for (ScriptSource source : resolver.resolveSourceOrder(scriptName)) {
            resolved.add(resolver.resolveFromSource(scriptName, phaseType, source));
        }

        List<ScriptDescriptor> immutable = Collections.unmodifiableList(new ArrayList<>(resolved));
        sourceChainCache.put(scriptName, immutable);
        return immutable;
    }

    private PhaseType inferPhaseType(String scriptName) {
        if (PhaseType.SCRIPT_PHASE_LIST.equals(scriptName)
                || PhaseType.SCRIPT_SOURCE_BASE_PHASE_LIST.equals(scriptName)) {
            return PhaseType.PHASE_LIST;
        }
        if (PhaseType.SCRIPT_ATTACHMENT_LIST.equals(scriptName)) {
            return PhaseType.ATTACHMENT_LIST;
        }
        if (PhaseType.SCRIPT_FILE_LIST.equals(scriptName)) {
            return PhaseType.FILE_LIST;
        }
        if (PhaseType.SCRIPT_TOOL_RESULT.equals(scriptName) || "mcp-result.ftl".equals(scriptName)) {
            return PhaseType.TOOL_RESPONSE;
        }
        return PhaseType.PROMPT_ASSEMBLY;
    }

     
    /**
     * Gets the all scripts.
     *
     * @return the string result
     */
    public synchronized Map<String, ScriptDescriptor> getAllScripts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(scripts));
    }

    /**
     * Required Scripts For Cycle.
     *
     * @param projectInferenceCycle the project inference cycle
     * @return the string result
     */
    public static Map<String, PhaseType> requiredScriptsForCycle(boolean projectInferenceCycle) {
        return projectInferenceCycle ? REQUIRED_SOURCE_BASE_SCRIPTS : REQUIRED_PER_SOURCE_SCRIPTS;
    }

    /**
     * Required Script Count For Cycle.
     *
     * @param projectInferenceCycle the project inference cycle
     * @return the numeric value
     */
    public static int requiredScriptCountForCycle(boolean projectInferenceCycle) {
        return requiredScriptsForCycle(projectInferenceCycle).size();
    }
}
