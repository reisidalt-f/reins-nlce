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
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.ReferenceTreeContextService;
import br.com.dizeno.reins.reasoning.ResponseDirective;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import java.time.Instant;

/**
 * ReferenceTreePhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class ReferenceTreePhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link ReferenceTreePhase}.
     *
     * @param coordinator the coordinator
     */
    public ReferenceTreePhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        BasePathResolver resolver = new BasePathResolver(
                context.getRequest().getBaseMappings(),
                new PathValidator(context.getProjectRoot())
        );
        context.setResolver(resolver);

        AttachedFilePayload sourceAttachment = coordinator.toolOperationHandler.buildSourceAttachment(
                context.getRequest(),
                resolver,
                coordinator.configResolver.resolveSourceBase(context.getRequest().getSourceScope())
        );
        context.setSourceAttachment(sourceAttachment);

        String firstTurnReferenceTree = null;
        ReferenceTreeContextService.ReferenceTreeContext firstTurnReferenceTreeContext = null;

        if (sourceAttachment != null) {
            try {
                ReferenceTreeContextService.ReferenceTreeContext treeContext =
                        coordinator.referenceTreeContextService.build(
                                context.getRequest(),
                                context.getConfig(),
                                resolver
                        );
                firstTurnReferenceTreeContext = treeContext;
                firstTurnReferenceTree = coordinator.promptBuilder.buildReferenceTree(treeContext);
            } catch (GraphProcessingException violation) {
                String violationMessage = violation.getMessage() == null
                        ? "Reference graph validation failed."
                        : violation.getMessage();

                if (context.getCycleLog() != null) {
                    coordinator.cycleStateManager.writeLog(
                            context.getCycleLog(),
                            ReasoningLogEntry.Direction.OUTBOUND,
                            "system",
                            1,
                            "lifecycle: reference-tree-build-failed\n"
                                    + "violation=" + violation.getViolationType() + "\n"
                                    + "message=" + violationMessage + "\n"
                                    + "paths=" + String.join(" -> ", violation.getInvolvedPaths())
                    );
                }

                context.getCycle().setStatus(ReasoningCycle.Status.FINISHED_ERROR);
                context.getCycle().setCompletedAt(Instant.now());

                ReasoningResult result = coordinator.cycleStateManager.buildResult(
                        context.getCycle(),
                        ResponseDirective.Intent.FINISH_ERROR,
                        "finish_error",
                        violationMessage,
                        context.getUserFacingMessages(),
                        context.getToolInfoPhrases(),
                        coordinator.scriptSelectionService.applyFileListScript(new java.util.ArrayList<>(context.getWrittenPaths()), context.getConfig()),
                        new java.util.ArrayList<>(context.getInspectedPaths()),
                        new java.util.LinkedHashMap<>(context.getWrittenMtimes()),
                        new java.util.LinkedHashMap<>(context.getInspectedMtimes()),
                        new java.util.ArrayList<>(context.getReadMarkdownPaths()),
                        firstTurnReferenceTree
                );
                context.setResult(result);
                return null; 
            }
        }

        context.setFirstTurnReferenceTreeContext(firstTurnReferenceTreeContext);
        context.setFirstTurnReferenceTree(firstTurnReferenceTree);

        return new ContextMessagePrepPhase(coordinator);
    }
}
