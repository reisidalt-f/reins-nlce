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

import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SafeTemplateExecutor is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing safe template executor.
 */
public class SafeTemplateExecutor {
    private static final Logger LOGGER = Logger.getLogger(SafeTemplateExecutor.class.getName());

    private final ScriptEvaluator scriptEvaluator;
    private final MessageFormattingService messageFormattingService;

    /**
     * Constructs a new instance of {@link SafeTemplateExecutor}.
     *
     * @param scriptEvaluator the script evaluator instance
     * @param messageFormattingService the message formatting service
     */
    public SafeTemplateExecutor(ScriptEvaluator scriptEvaluator,
                                MessageFormattingService messageFormattingService) {
        this.scriptEvaluator = scriptEvaluator;
        this.messageFormattingService = messageFormattingService;
    }

    /**
     * Evaluate Script Safely.
     *
     * @param scriptName the script name
     * @param context the context
     * @param config the Reins configuration settings
     * @param scriptContext the script context
     * @return the string result
     */
    public String evaluateScriptSafely(String scriptName,
                                       ReasoningScriptContext context,
                                       ReinsConfig config,
                                       String scriptContext) {
        if (scriptEvaluator == null || context == null) {
            throw new IllegalStateException("Script evaluation context is not available for required script: " + scriptName);
        }
        try {
            logScriptDataModelDebug(scriptName, context, config, scriptContext);
            ScriptChainEvaluationResult result = scriptEvaluator.evaluateWithOutcome(scriptName, context);
            if (result.terminalReason() == ScriptTerminalReason.FATAL_ERROR && result.fatalException() != null) {
                throw result.fatalException();
            }
            if (result.diagnostics() != null && result.diagnostics().terminalReason() == ScriptTerminalReason.NON_BLANK_SELECTED) {
                LOGGER.fine("[reins-script] terminal"
                        + " context=" + (scriptContext == null ? "unknown" : scriptContext)
                        + " script=" + scriptName
                        + " sourceIdentifier=" + result.diagnostics().sourceIdentifier()
                        + " sourceIndex=" + result.diagnostics().sourceIndex()
                        + " terminalReason=" + result.diagnostics().terminalReason().name());
            }
            return result.renderedOutput();
        } catch (ScriptEvaluationException e) {
            String msg = "[reins-script] evaluation failed"
                    + " context=" + (scriptContext == null ? "unknown" : scriptContext)
                    + " script=" + scriptName
                    + " scriptPath=" + (e.getResolvedLocation() == null ? "unknown" : e.getResolvedLocation())
                    + " phaseType=" + (e.getPhaseType() == null ? "unknown" : e.getPhaseType().name())
                    + " sourceIdentifier=" + (e.getSourceIdentifier() == null ? "unknown" : e.getSourceIdentifier())
                    + " sourceIndex=" + (e.getSourceIndex() == null ? "unknown" : e.getSourceIndex())
                    + " terminalReason=" + (e.getTerminalReason() == null ? "unknown" : e.getTerminalReason().name())
                    + " failureCategory=" + (e.getFailureCategory() == null ? "unknown" : e.getFailureCategory().name())
                    + " reason=" + (e.getMessage() == null ? "n/a" : e.getMessage());
            LOGGER.warning(msg);
            throw new IllegalStateException(msg, e);
        } catch (RuntimeException e) {
            String msg = "[reins-script] runtime failure"
                    + " context=" + (scriptContext == null ? "unknown" : scriptContext)
                    + " script=" + scriptName
                    + " reason=" + (e.getMessage() == null ? "n/a" : e.getMessage());
            LOGGER.warning(msg);
            throw new IllegalStateException(msg, e);
        }
    }

    private void logScriptDataModelDebug(String scriptName,
                                         ReasoningScriptContext context,
                                         ReinsConfig config,
                                         String scriptContext) {
        if (config == null || !config.isVerbose() || !LOGGER.isLoggable(Level.FINE)) {
            return;
        }
        String sourcePath = context.getSource() == null ? "" : messageFormattingService.safe(context.getSource().getPath());
        String sourceScope = context.getSource() == null ? "" : messageFormattingService.safe(context.getSource().getScope());
        String cycleId = context.getCycle() == null ? "" : messageFormattingService.safe(context.getCycle().getCycleId());
        int maxTurns = context.getCycle() == null ? 0 : context.getCycle().getMaxTurns();
        int attachmentCount = context.getAttachments() == null ? 0 : context.getAttachments().size();
        int inspectedCount = context.getInference() == null || context.getInference().getInspectedFiles() == null
                ? 0 : context.getInference().getInspectedFiles().size();
        int compiledCount = context.getInference() == null || context.getInference().getCompiledFiles() == null
                ? 0 : context.getInference().getCompiledFiles().size();
        int referenceTreeLength = context.getReferenceTree() == null ? 0 : context.getReferenceTree().length();

        LOGGER.fine("[reins-script] model"
                + " context=" + (scriptContext == null ? "unknown" : scriptContext)
                + " script=" + scriptName
                + " sourcePath=" + sourcePath
                + " sourceScope=" + sourceScope
                + " cycleId=" + cycleId
                + " maxTurns=" + maxTurns
                + " attachments=" + attachmentCount
                + " inspectedFiles=" + inspectedCount
                + " compiledFiles=" + compiledCount
                + " referenceTreeLength=" + referenceTreeLength);
    }
}
