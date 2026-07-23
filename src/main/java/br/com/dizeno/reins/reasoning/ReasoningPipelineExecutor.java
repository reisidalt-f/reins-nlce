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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.ResponseDirective;
import br.com.dizeno.reins.reasoning.ResponseDirectiveParser;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;
import br.com.dizeno.reins.reasoning.scripting.PhaseListValidator;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluationException;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReasoningPipelineExecutor is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing inference pipeline executor.
 */
public class ReasoningPipelineExecutor {
    public static final String SCRIPT_NAME = "reasoning-pipeline.ftl";
    public static final String LIST_PHASES = "list-phases";
    public static final String DEFAULT_PHASE = "default-cycle";

    private final ScriptEvaluator scriptEvaluator;
    private final ResponseDirectiveParser directiveParser;
    private final ResponseDirectiveParser fallbackDirectiveParser;

    /**
     * Constructs a new instance of {@link ReasoningPipelineExecutor}.
     *
     * @param scriptEvaluator the script evaluator instance
     * @param directiveParser the directive parser
     */
    public ReasoningPipelineExecutor(ScriptEvaluator scriptEvaluator,
                                     ResponseDirectiveParser directiveParser) {
        this.scriptEvaluator = scriptEvaluator;
        this.directiveParser = directiveParser;
        this.fallbackDirectiveParser = new ResponseDirectiveParser();
    }

    /**
     * Resolves the configured value or path plan.
     *
     * @param sourcePath the path of the source file
     * @param context the context
     * @param phaseOverride the phase override
     * @return the resolved or constructed object
     */
    public ReasoningPipelinePlan resolvePlan(String sourcePath,
                                             ReasoningScriptContext context,
                                             List<String> phaseOverride) throws ScriptEvaluationException {
        List<String> phases;
        if (phaseOverride != null && !phaseOverride.isEmpty()) {
            phases = new ArrayList<>(phaseOverride);
        } else {
            String rendered = scriptEvaluator.evaluatePhase(SCRIPT_NAME, LIST_PHASES, context, Map.of());
            phases = parsePhaseList(rendered);
        }
        PhaseListValidator.validate(phases, scriptEvaluator, false);
        if (phases.isEmpty()) {
            return new ReasoningPipelinePlan(sourcePath, List.of(), ReasoningPipelinePlan.PlanStatus.EMPTY);
        }
        List<ReasoningPhaseDescriptor> descriptors = new ArrayList<>(phases.size());
        for (int index = 0; index < phases.size(); index++) {
            descriptors.add(new ReasoningPhaseDescriptor(phases.get(index), index, ReasoningPhaseDescriptor.State.PENDING));
        }
        return new ReasoningPipelinePlan(sourcePath, descriptors, ReasoningPipelinePlan.PlanStatus.VALID);
    }

    /**
     * Render Phase Message.
     *
     * @param phase the phase
     * @param context the context
     * @param variables the variables
     * @return the resolved or constructed object
     */
    public PipelineExchangeMessage renderPhaseMessage(String phase,
                                                      ReasoningScriptContext context,
                                                      Map<String, Object> variables) throws ScriptEvaluationException {
        String rendered = renderPhaseRawMessage(phase, context, variables);
        return parsePhaseMessage(rendered);
    }

    /**
     * Render Phase Raw Message.
     *
     * @param phase the phase
     * @param context the context
     * @param variables the variables
     * @return the string result
     */
    public String renderPhaseRawMessage(String phase,
                                        ReasoningScriptContext context,
                                        Map<String, Object> variables) throws ScriptEvaluationException {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (variables != null) {
            merged.putAll(variables);
        }
        return scriptEvaluator.evaluatePhase(SCRIPT_NAME, phase, context, merged);
    }

    /**
     * Parse Phase Message.
     *
     * @param rendered the rendered
     * @return the resolved or constructed object
     */
    public PipelineExchangeMessage parsePhaseMessage(String rendered) {
        PipelineExchangeMessage parsed = directiveParser == null
                ? null
                : directiveParser.parseCanonicalMessage(rendered);
        if (parsed != null) {
            return parsed;
        }
        return fallbackDirectiveParser.parseCanonicalMessage(rendered);
    }

    /**
     * Advance Phase Index.
     *
     * @param plan the plan
     * @param currentIndex the current index
     * @return the numeric value
     */
    public int advancePhaseIndex(ReasoningPipelinePlan plan, int currentIndex) {
        if (plan == null || plan.getPhases().isEmpty()) {
            return -1;
        }
        int nextIndex = currentIndex + 1;
        return nextIndex >= plan.getPhases().size() ? -1 : nextIndex;
    }

    /**
     * Outcome For Directive.
     *
     * @param plan the plan
     * @param currentIndex the current index
     * @param directive the directive
     * @return the resolved or constructed object
     */
    public PipelineRuntimeOutcome outcomeForDirective(ReasoningPipelinePlan plan,
                                                      int currentIndex,
                                                      ResponseDirective directive) {
        if (directive == null || directive.getIntent() == null) {
            return PipelineRuntimeOutcome.error(currentPhaseName(plan, currentIndex), "Missing terminal intent.");
        }
        if (directive.getIntent() == ResponseDirective.Intent.FINISH_ERROR) {
            return PipelineRuntimeOutcome.error(currentPhaseName(plan, currentIndex), "Pipeline phase failed with finish-error.");
        }
        if (directive.getIntent() == ResponseDirective.Intent.FINISH_SUCCESS
                && advancePhaseIndex(plan, currentIndex) < 0) {
            return PipelineRuntimeOutcome.success(currentPhaseName(plan, currentIndex), "Pipeline completed successfully.");
        }
        return null;
    }

    /**
     * Current Phase Name.
     *
     * @param plan the plan
     * @param currentIndex the current index
     * @return the string result
     */
    public String currentPhaseName(ReasoningPipelinePlan plan, int currentIndex) {
        if (plan == null || currentIndex < 0 || currentIndex >= plan.getPhases().size()) {
            return null;
        }
        return plan.getPhases().get(currentIndex).getName();
    }

    /**
     * Parse Phase List.
     *
     * @param rendered the rendered
     * @return the string result
     */
    public List<String> parsePhaseList(String rendered) {
        List<String> phases = new ArrayList<>();
        if (rendered == null || rendered.isBlank()) {
            return phases;
        }
        for (String line : rendered.split("\\R", -1)) {
            String phase = line == null ? "" : line.trim();
            if (phase.isEmpty()) {
                continue;
            }
            phases.add(phase);
        }
        return phases;
    }

}