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

/**
 * ContextStepScriptResolver is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class ContextStepScriptResolver {
    private final String scriptName;

    /**
     * Constructs a new instance of {@link ContextStepScriptResolver}.
     */
    public ContextStepScriptResolver() {
        this("context-build.ftl");
    }

    /**
     * Constructs a new instance of {@link ContextStepScriptResolver}.
     *
     * @param scriptName the script name
     */
    public ContextStepScriptResolver(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Resolves the configured value or path script name.
     *
     * @param stepName the step name
     * @return the string result
     */
    public String resolveScriptName(String stepName) {
        return scriptName;
    }
}
