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

import java.util.List;

 
/**
 * ScriptChainEvaluationResult is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public record ScriptChainEvaluationResult(
        ScriptTerminalReason terminalReason,
        String renderedOutput,
        ScriptDescriptor selectedDescriptor,
        List<ScriptDescriptor> attemptedDescriptors,
        ScriptChainDiagnostics diagnostics,
        ScriptEvaluationException fatalException
) {
    /**
     * Selected.
     *
     * @param renderedOutput the rendered output
     * @param selectedDescriptor the selected descriptor
     * @param attemptedDescriptors the attempted descriptors
     * @param diagnostics the diagnostics
     * @return the resulting result
     */
    public static ScriptChainEvaluationResult selected(String renderedOutput,
                                                       ScriptDescriptor selectedDescriptor,
                                                       List<ScriptDescriptor> attemptedDescriptors,
                                                       ScriptChainDiagnostics diagnostics) {
        return new ScriptChainEvaluationResult(
                ScriptTerminalReason.NON_BLANK_SELECTED,
                renderedOutput,
                selectedDescriptor,
                List.copyOf(attemptedDescriptors),
                diagnostics,
                null);
    }

    /**
     * Fatal.
     *
     * @param terminalReason the terminal reason
     * @param attemptedDescriptors the attempted descriptors
     * @param diagnostics the diagnostics
     * @param fatalException the fatal exception
     * @return the resulting result
     */
    public static ScriptChainEvaluationResult fatal(ScriptTerminalReason terminalReason,
                                                    List<ScriptDescriptor> attemptedDescriptors,
                                                    ScriptChainDiagnostics diagnostics,
                                                    ScriptEvaluationException fatalException) {
        return new ScriptChainEvaluationResult(
                terminalReason,
                null,
                null,
                List.copyOf(attemptedDescriptors),
                diagnostics,
                fatalException);
    }
}
