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

package br.com.dizeno.reins.reasoning.phase;

import br.com.dizeno.reins.reasoning.DefaultReasoningExecutionCoordinator;
import br.com.dizeno.reins.reasoning.ReasoningContext;
import br.com.dizeno.reins.reasoning.ReasoningPhase;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningLogEntry;
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import java.util.List;

/**
 * PipelinePlanningPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class PipelinePlanningPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link PipelinePlanningPhase}.
     *
     * @param coordinator the coordinator
     */
    public PipelinePlanningPhase(DefaultReasoningExecutionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Executes the operation.
     *
     * @param context the context
     * @return the resolved or constructed object
     */
    @Override
    public ReasoningPhase execute(ReasoningContext context) throws Exception {
        FilePolicy toolPermission = coordinator.configResolver.buildToolPolicy(context.getConfig());
        ScriptRunnerConfig scriptRunnerConfig = ScriptRunnerConfig.fromSettings(context.getConfig(), context.getProjectRoot());
        context.setToolPermission(toolPermission);
        context.setScriptRunnerConfig(scriptRunnerConfig);

        ReasoningPipelinePlan pipelinePlan = coordinator.inferencePipelineOrchestrator.resolveInferencePipelinePlan(
                context.getRequest(),
                context.getConfig(),
                context.getCycle(),
                toolPermission,
                scriptRunnerConfig.isEnabled(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                false,
                null);
        context.setPipelinePlan(pipelinePlan);

        if (pipelinePlan.isEmpty()) {
            coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "pipeline-skipped-empty-phase-list");
            ReasoningResult skipped = coordinator.cycleStateManager.buildSkippedResult(
                    context.getCycle(),
                    "pipeline_skipped",
                    context.getFirstTurnReferenceTree(),
                    "Inference pipeline phase list returned no phases."
            );
            context.setResult(skipped);
            return null; 
        }

        int pipelinePhaseIndex = 0;
        context.setPipelinePhaseIndex(pipelinePhaseIndex);
        String currentPipelinePhase = coordinator.inferencePipelineExecutor.currentPhaseName(pipelinePlan, pipelinePhaseIndex);
        context.setCurrentPipelinePhase(currentPipelinePhase);
        coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "pipeline-phase-start: " + currentPipelinePhase);

        List<String> activePerSourcePhases = coordinator.inferencePipelineOrchestrator.resolvePerSourcePhaseNames(
                context.getRequest(),
                context.getConfig(),
                context.getCycle(),
                toolPermission,
                scriptRunnerConfig.isEnabled());
        context.setActivePerSourcePhases(activePerSourcePhases);

        String systemContext = activePerSourcePhases.contains("system-context")
                ? coordinator.scriptEvaluationService.evaluateSystemContextScript(
                        context.getRequest(),
                        context.getConfig(),
                        context.getCycle(),
                        toolPermission,
                        scriptRunnerConfig.isEnabled(),
                        null,
                        List.of(),
                        List.of(),
                        List.of())
                : null;
        if (systemContext == null || systemContext.isBlank()) {
            throw new IllegalStateException("Required script 'system-context.ftl' produced empty content.");
        }
        context.setSystemContext(systemContext);

        boolean logSystemContextEnabled = context.getConfig() != null
                && context.getConfig().getReasoning() != null
                && context.getConfig().getReasoning().isLogSystemContext();
        if (context.getCycleLog() != null && logSystemContextEnabled) {
            coordinator.cycleStateManager.writeLog(
                    context.getCycleLog(),
                    ReasoningLogEntry.Direction.OUTBOUND,
                    "system-context",
                    0,
                    systemContext
            );
        }

        boolean cachedContentEnabled = coordinator.configResolver.isCachedContentEnabled(context.getConfig());
        context.setCachedContentEnabled(cachedContentEnabled);

        return new ReferenceTreePhase(coordinator);
    }
}
