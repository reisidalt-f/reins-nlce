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

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;

/**
 * ScriptEvaluator is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Evaluates dynamic scripting logic using Freemarker templates and injects system/user variables.
 */
public class ScriptEvaluator {

    private final ScriptRegistry registry;
    private final Log log;
    private final boolean scriptsEventsEnabled;
    private final BasePathResolver basePathResolver;

    /**
     * Constructs a new instance of {@link ScriptEvaluator}.
     *
     * @param registry the registry
     * @param log the logger instance
     */
    public ScriptEvaluator(ScriptRegistry registry, Log log) {
        this(registry, log, false, null);
    }

    /**
     * Constructs a new instance of {@link ScriptEvaluator}.
     *
     * @param registry the registry
     * @param log the logger instance
     * @param scriptsEventsEnabled the inference scripts events enabled
     */
    public ScriptEvaluator(ScriptRegistry registry, Log log, boolean scriptsEventsEnabled) {
        this(registry, log, scriptsEventsEnabled, null);
    }

    /**
     * Constructs a new instance of {@link ScriptEvaluator}.
     *
     * @param registry the registry
     * @param log the logger instance
     * @param scriptsEventsEnabled the inference scripts events enabled
     * @param basePathResolver the base path resolver instance
     */
    public ScriptEvaluator(ScriptRegistry registry, Log log, boolean scriptsEventsEnabled, BasePathResolver basePathResolver) {
        this.registry = registry;
        this.log = log;
        this.scriptsEventsEnabled = scriptsEventsEnabled;
        this.basePathResolver = basePathResolver;
    }

    /**
     * With Base Path Resolver.
     *
     * @param resolver the base path resolver instance
     * @return a new ScriptEvaluator instance with the specified BasePathResolver
     */
    public ScriptEvaluator withBasePathResolver(BasePathResolver resolver) {
        return new ScriptEvaluator(registry, log, scriptsEventsEnabled, resolver);
    }

     
    /**
     * Evaluate.
     *
     * @param scriptName the script name
     * @param context the context
     * @return the string result
     */
    public String evaluate(String scriptName, ReasoningScriptContext context)
            throws ScriptEvaluationException {
        ScriptChainEvaluationResult outcome = evaluateWithOutcome(scriptName, context, scriptsEventsEnabled);
        if (outcome.terminalReason() == ScriptTerminalReason.NON_BLANK_SELECTED) {
            return outcome.renderedOutput();
        }
        if (outcome.fatalException() != null) {
            throw outcome.fatalException();
        }
        throw new ScriptEvaluationException(
                scriptName,
                null,
                null,
                "Script evaluation ended without a selected output",
                null,
                ScriptFailureCategory.EVALUATION_FAILURE,
                null,
                null,
                ScriptTerminalReason.FATAL_ERROR);
    }

    /**
     * Evaluate.
     *
     * @param scriptName the script name
     * @param context the context
     * @param scriptsEventsEnabled the inference scripts events enabled
     * @return the string result
     */
    public String evaluate(String scriptName,
                           ReasoningScriptContext context,
                           boolean scriptsEventsEnabled)
            throws ScriptEvaluationException {
        ScriptChainEvaluationResult outcome = evaluateWithOutcome(scriptName, context, scriptsEventsEnabled);
        if (outcome.terminalReason() == ScriptTerminalReason.NON_BLANK_SELECTED) {
            return outcome.renderedOutput();
        }
        if (outcome.fatalException() != null) {
            throw outcome.fatalException();
        }
        throw new ScriptEvaluationException(
                scriptName,
                null,
                null,
                "Script evaluation ended without a selected output",
                null,
                ScriptFailureCategory.EVALUATION_FAILURE,
                null,
                null,
                ScriptTerminalReason.FATAL_ERROR);
    }

     
    /**
     * Evaluate.
     *
     * @param scriptName the script name
     * @param context the context
     * @param variables the variables
     * @return the string result
     */
    public String evaluate(String scriptName,
                           ReasoningScriptContext context,
                           Map<String, Object> variables) throws ScriptEvaluationException {
        ScriptChainEvaluationResult outcome = evaluateWithOutcome(scriptName, context, variables, scriptsEventsEnabled);
        if (outcome.terminalReason() == ScriptTerminalReason.NON_BLANK_SELECTED) {
            return outcome.renderedOutput();
        }
        if (outcome.fatalException() != null) {
            throw outcome.fatalException();
        }
        throw new ScriptEvaluationException(
                scriptName,
                null,
                null,
                "Script evaluation ended without a selected output",
                null,
                ScriptFailureCategory.EVALUATION_FAILURE,
                null,
                null,
                ScriptTerminalReason.FATAL_ERROR);
    }

    /**
     * Evaluate.
     *
     * @param scriptName the script name
     * @param context the context
     * @param variables the variables
     * @param scriptsEventsEnabled the inference scripts events enabled
     * @return the string result
     */
    public String evaluate(String scriptName,
                           ReasoningScriptContext context,
                           Map<String, Object> variables,
                           boolean scriptsEventsEnabled) throws ScriptEvaluationException {
        ScriptChainEvaluationResult outcome = evaluateWithOutcome(scriptName, context, variables, scriptsEventsEnabled);
        if (outcome.terminalReason() == ScriptTerminalReason.NON_BLANK_SELECTED) {
            return outcome.renderedOutput();
        }
        if (outcome.fatalException() != null) {
            throw outcome.fatalException();
        }
        throw new ScriptEvaluationException(
                scriptName,
                null,
                null,
                "Script evaluation ended without a selected output",
                null,
                ScriptFailureCategory.EVALUATION_FAILURE,
                null,
                null,
                ScriptTerminalReason.FATAL_ERROR);
    }

    /**
     * Evaluate Phase.
     *
     * @param scriptName the script name
     * @param phase the phase
     * @param context the context
     * @param variables the variables
     * @return the string result
     */
    public String evaluatePhase(String scriptName,
                                String phase,
                                ReasoningScriptContext context,
                                Map<String, Object> variables) throws ScriptEvaluationException {
        Map<String, Object> merged = new HashMap<>();
        if (variables != null && !variables.isEmpty()) {
            merged.putAll(variables);
        }
        merged.put("phase", phase);
        return evaluate(scriptName, context, merged);
    }

     
    /**
     * Evaluate With Outcome.
     *
     * @param scriptName the script name
     * @param context the context
     * @return the resulting result
     */
    public ScriptChainEvaluationResult evaluateWithOutcome(String scriptName,
                                                           ReasoningScriptContext context)
            throws ScriptEvaluationException {
        return evaluateWithOutcome(scriptName, context, Collections.emptyMap(), scriptsEventsEnabled);
    }

    /**
     * Evaluate With Outcome.
     *
     * @param scriptName the script name
     * @param context the context
     * @param scriptsEventsEnabled the inference scripts events enabled
     * @return the resulting result
     */
    public ScriptChainEvaluationResult evaluateWithOutcome(String scriptName,
                                                           ReasoningScriptContext context,
                                                           boolean scriptsEventsEnabled)
            throws ScriptEvaluationException {
        return evaluateWithOutcome(scriptName, context, Collections.emptyMap(), scriptsEventsEnabled);
    }

     
    /**
     * Evaluate With Outcome.
     *
     * @param scriptName the script name
     * @param context the context
     * @param variables the variables
     * @return the resulting result
     */
    public ScriptChainEvaluationResult evaluateWithOutcome(String scriptName,
                                                           ReasoningScriptContext context,
                                                           Map<String, Object> variables)
            throws ScriptEvaluationException {
        return evaluateWithOutcome(scriptName, context, variables, scriptsEventsEnabled);
    }

    /**
     * Evaluate With Outcome.
     *
     * @param scriptName the script name
     * @param context the context
     * @param variables the variables
     * @param scriptsEventsEnabled the inference scripts events enabled
     * @return the resulting result
     */
    public ScriptChainEvaluationResult evaluateWithOutcome(String scriptName,
                                                           ReasoningScriptContext context,
                                                           Map<String, Object> variables,
                                                           boolean scriptsEventsEnabled)
            throws ScriptEvaluationException {
        List<ScriptDescriptor> descriptors;
        try {
            descriptors = registry.resolveSourceChain(scriptName);
        } catch (IOException e) {
            ScriptFailureCategory category = categorizeFailure(e);
            throw new ScriptEvaluationException(
                    scriptName,
                    null,
                    null,
                    "Failed to resolve script source chain for '" + scriptName + "'",
                    e,
                    category,
                    null,
                    null,
                    ScriptTerminalReason.FATAL_ERROR);
        }

        if (descriptors.isEmpty()) {
            throw new ScriptEvaluationException(
                    scriptName,
                    null,
                    null,
                    "Script not found in registry: " + scriptName,
                    new IllegalStateException("Script not found in registry: " + scriptName),
                    ScriptFailureCategory.EVALUATION_FAILURE,
                    null,
                    null,
                    ScriptTerminalReason.FATAL_ERROR);
        }

        List<ScriptDescriptor> attempted = new ArrayList<>();
        ScriptDescriptor lastAttempted = null;
        int sourceIndex = 0;
        for (ScriptDescriptor descriptor : descriptors) {
            attempted.add(descriptor);
            lastAttempted = descriptor;

            if (scriptsEventsEnabled && log != null && log.isInfoEnabled()) {
                log.info("[reins-script] Executing script '" + scriptName
                        + "' (phase: " + descriptor.getPhaseType()
                        + ", sourceIndex=" + sourceIndex
                        + ", from: " + descriptor.getResolvedLocation() + ")");
            }

            Template template = descriptor.getTemplate();
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("project", context);
            if (context != null) {
                if (context.getSource() != null) dataModel.put("source", context.getSource());
                if (context.getFileBases() != null) dataModel.put("fileBases", context.getFileBases());
                if (context.getPipeline() != null) dataModel.put("pipeline", context.getPipeline());
                if (context.getPolicy() != null) dataModel.put("policy", context.getPolicy());
                if (context.getConfig() != null) dataModel.put("config", context.getConfig());
                if (context.getTracking() != null) dataModel.put("tracking", context.getTracking());
                if (context.getCycle() != null) dataModel.put("cycle", context.getCycle());
                if (context.getInference() != null) dataModel.put("inference", context.getInference());
                if (context.getCurrentToolResult() != null) dataModel.put("currentToolResult", context.getCurrentToolResult());
                if (context.getAttachments() != null) dataModel.put("attachments", context.getAttachments());
            }
            dataModel.put("readFile", new ReadFileMethod(basePathResolver, context));
            dataModel.put("hasFile", new HasFileMethod(basePathResolver, context));
            dataModel.put("readFileOrDefault", new ReadFileOrDefaultMethod(basePathResolver, context));
            if (variables != null && !variables.isEmpty()) {
                dataModel.putAll(variables);
            }

            StringWriter writer = new StringWriter();
            try {
                template.process(dataModel, writer);
                String rendered = writer.toString();
                if (isBlank(rendered)) {
                    sourceIndex++;
                    continue;
                }

                ScriptChainDiagnostics diagnostics = new ScriptChainDiagnostics(
                        descriptor.getResolvedFrom().name(),
                        sourceIndex,
                        ScriptTerminalReason.NON_BLANK_SELECTED,
                        scriptName,
                        null,
                        "selected non-blank output");

                return ScriptChainEvaluationResult.selected(rendered, descriptor, attempted, diagnostics);
            } catch (TemplateException | IOException e) {
                ScriptFailureCategory category = categorizeFailure(e);
                ScriptEvaluationException fatal = new ScriptEvaluationException(
                        scriptName,
                        descriptor.getPhaseType(),
                        descriptor.getResolvedLocation(),
                        "Failed to evaluate script '" + scriptName + "'",
                        e,
                        category,
                        descriptor.getResolvedFrom().name(),
                        sourceIndex,
                        ScriptTerminalReason.FATAL_ERROR);
                ScriptChainDiagnostics diagnostics = new ScriptChainDiagnostics(
                        descriptor.getResolvedFrom().name(),
                        sourceIndex,
                        ScriptTerminalReason.FATAL_ERROR,
                        scriptName,
                        category,
                        fatal.getMessage());
                return ScriptChainEvaluationResult.fatal(
                        ScriptTerminalReason.FATAL_ERROR,
                        attempted,
                        diagnostics,
                        fatal);
            }
        }

        ScriptEvaluationException noUsableOutput = new ScriptEvaluationException(
                scriptName,
                lastAttempted == null ? null : lastAttempted.getPhaseType(),
                lastAttempted == null ? null : lastAttempted.getResolvedLocation(),
                "All sources rendered blank output for script '" + scriptName + "'",
                null,
                ScriptFailureCategory.NO_USABLE_OUTPUT,
                lastAttempted == null ? null : lastAttempted.getResolvedFrom().name(),
                descriptors.size() - 1,
                ScriptTerminalReason.NO_USABLE_OUTPUT);
        ScriptChainDiagnostics diagnostics = new ScriptChainDiagnostics(
                lastAttempted == null ? "unknown" : lastAttempted.getResolvedFrom().name(),
                descriptors.isEmpty() ? -1 : descriptors.size() - 1,
                ScriptTerminalReason.NO_USABLE_OUTPUT,
                scriptName,
                ScriptFailureCategory.NO_USABLE_OUTPUT,
                noUsableOutput.getMessage());
        return ScriptChainEvaluationResult.fatal(
                ScriptTerminalReason.NO_USABLE_OUTPUT,
                attempted,
                diagnostics,
                noUsableOutput);
    }

     
    /**
     * Checks if the component has script.
     *
     * @param scriptName the script name
     * @return true if successful or matching, false otherwise
     */
    public boolean hasScript(String scriptName) {
        return registry.getOrResolveScript(scriptName) != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.strip().isEmpty();
    }

    private ScriptFailureCategory categorizeFailure(Throwable throwable) {
        String message = throwable == null || throwable.getMessage() == null
                ? ""
                : throwable.getMessage().toLowerCase();

        if (message.contains("forbidden built-in") || message.contains("security")) {
            return ScriptFailureCategory.SECURITY_FAILURE;
        }
        if (message.contains("include") || message.contains("could not include") || message.contains("template not found")) {
            return ScriptFailureCategory.INCLUDE_FAILURE;
        }

        Throwable root = rootCause(throwable);
        if (root instanceof ParseException || message.contains("syntax")) {
            return ScriptFailureCategory.SYNTAX_FAILURE;
        }

        return ScriptFailureCategory.EVALUATION_FAILURE;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }
}
