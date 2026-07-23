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
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.ReasoningPhase;
import java.util.UUID;

/**
 * InitializeCyclePhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class InitializeCyclePhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link InitializeCyclePhase}.
     *
     * @param coordinator the coordinator
     */
    public InitializeCyclePhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        ReasoningCycle cycle = context.getCycle();
        cycle.setCycleId(UUID.randomUUID().toString());
        cycle.setBaseMappings(context.getRequest().getBaseMappings());
        cycle.setMaxTurns(coordinator.configResolver.resolveMaxTurns(context.getConfig()));
        
        coordinator.cycleStateManager.ensureFixedProvider(cycle,
            (context.getConfig().getProvider() == null || context.getConfig().getProvider().isBlank())
                ? "gemini"
                : context.getConfig().getProvider().trim().toLowerCase());

        if (coordinator.configResolver.isReasoningLogEnabled(context.getConfig())) {
            try {
                var cycleLog = coordinator.inferenceLogService.initializeCycleLog(
                        cycle.getCycleId(),
                        context.getProjectRoot()
                );
                context.setCycleLog(cycleLog);
                coordinator.cycleStateManager.emitLifecycleMessage(
                        cycleLog,
                        "llm-provider-fixed: " + cycle.getFixedProviderId()
                );
            } catch (Exception e) {
                context.setCycleLog(null);
            }
        }

        return new PipelinePlanningPhase(coordinator);
    }
}
