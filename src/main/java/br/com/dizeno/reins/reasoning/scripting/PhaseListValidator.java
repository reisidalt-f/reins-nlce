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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

 
/**
 * PhaseListValidator is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a helper utility for validating its prefix constraints.
 */
public final class PhaseListValidator {

    private static final Pattern VALID_PHASE_TOKEN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");
    private static final Set<String> KNOWN_PHASE_SCRIPTS = ScriptRegistry.requiredScriptsForCycle(false)
            .keySet()
            .stream()
            .collect(Collectors.toSet());

    private PhaseListValidator() {
    }

    /**
     * Validates the inputs or files.
     *
     * @param phaseNames the phase names
     * @param evaluator the script evaluator instance
     */
    public static void validate(List<String> phaseNames, ScriptEvaluator evaluator) {
        validate(phaseNames, evaluator, true);
    }

    /**
     * Validates the inputs or files.
     *
     * @param phaseNames the phase names
     * @param evaluator the script evaluator instance
     * @param requireRegisteredScripts the require registered scripts
     */
    public static void validate(List<String> phaseNames,
                                ScriptEvaluator evaluator,
                                boolean requireRegisteredScripts) {
        if (phaseNames == null || phaseNames.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (String phaseName : phaseNames) {
            String normalized = phaseName == null ? "" : phaseName.trim();
            if (normalized.isEmpty()) {
                throw new IllegalStateException("Phase list contains a blank phase name.");
            }
            if (!VALID_PHASE_TOKEN.matcher(normalized).matches()) {
                throw new IllegalStateException("Malformed phase name in phase list: " + normalized);
            }
            if (!seen.add(normalized)) {
                throw new IllegalStateException("Duplicate phase name in phase list: " + normalized);
            }
            if (requireRegisteredScripts) {
                String scriptName = normalized.endsWith(".ftl") ? normalized : normalized + ".ftl";
                boolean existsInEvaluator = evaluator != null && evaluator.hasScript(scriptName);
                boolean isKnownBuiltIn = KNOWN_PHASE_SCRIPTS.contains(scriptName);
                if (!existsInEvaluator && !isKnownBuiltIn) {
                    throw new IllegalStateException("Unknown phase name in custom phase list: " + normalized);
                }
            }
        }
    }
}
