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
import br.com.dizeno.reins.reasoning.ReasoningTurn;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.AttachmentNormalizer;
import br.com.dizeno.reins.reasoning.ReferenceMutationDecision;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ToolExecutionPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class ToolExecutionPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link ToolExecutionPhase}.
     *
     * @param coordinator the coordinator
     */
    public ToolExecutionPhase(DefaultReasoningExecutionCoordinator coordinator) {
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
        ReasoningTurn inbound = context.getTurnInbound();
        List<ToolExecutionRequest> operationRequests;

        try {
            operationRequests = coordinator.toolRequestParser.parse(context.getTurnParseResult().getBody());
        } catch (IllegalArgumentException parseError) {
            ToolExecutionResult malformedRequestResult = ToolExecutionResult.error(
                    null,
                    null,
                    parseError.getMessage()
            );
            inbound.setToolResult(malformedRequestResult);
            inbound.setFulfilled(false);
            inbound.setFailureClass("parse-error");
            context.setPromptAttachments(new ArrayList<>());

            String nextMessage = coordinator.renderPipelineMessageToModelWithFailureLogging(
                    context.getCycleLog(),
                    inbound.getSequence(),
                    context.getCurrentPipelinePhase(),
                    context.getPipelinePlan(),
                    context.getPipelinePhaseIndex(),
                    context.getNextMessage(),
                    context.getRequest(),
                    context.getConfig(),
                    context.getCycle(),
                    context.getToolPermission(),
                    context.getScriptRunnerConfig().isEnabled(),
                    context.getFirstTurnReferenceTree(),
                    new ArrayList<>(context.getInspectedPaths()),
                    new ArrayList<>(context.getWrittenPaths()),
                    context.getPromptAttachments(),
                    context.getConversationHistory(),
                    null,
                    null,
                    null,
                    "parse-error",
                    true,
                    malformedRequestResult);
            context.setNextMessageFromScript(false);
            if (nextMessage == null || nextMessage.isBlank()) {
                nextMessage = coordinator.buildParseErrorRetryMessage(parseError.getMessage(), context.getConfig());
                context.setNextMessageFromScript(false);
            }
            nextMessage = coordinator.appendParseErrorDetails(nextMessage, parseError.getMessage());
            context.setNextMessage(nextMessage);
            context.getCycle().incrementRetryStreakCount();

            coordinator.cycleStateManager.logAttemptEvent(context.getCycleLog(), inbound, "parse-error", context.getClosedTurns(), context.getCycle().getMaxTurns(),
                    context.getCycle().getRetryStreakCount(), parseError.getMessage());

            context.setAttemptIndex(context.getAttemptIndex() + 1);
            if (coordinator.configResolver.isNonFulfillmentAttemptBudgetExceeded(context.getConfig(), context.getAttemptIndex())) {
                ReasoningResult termResult = coordinator.terminateCycleOnNonFulfillmentAttemptExhaustion(
                        context.getCycle(),
                        context.getCycleLog(),
                        context.getRequest(),
                        context.getUserFacingMessages(),
                        context.getToolInfoPhrases(),
                        context.getWrittenPaths(),
                        context.getInspectedPaths(),
                        context.getWrittenMtimes(),
                        context.getInspectedMtimes(),
                        context.getReadMarkdownPaths(),
                        context.getFirstTurnReferenceTree(),
                        context.getLogicalTurn(),
                        "parse-error",
                        context.getCycleCachedContentId(),
                        context.getConfig());
                context.setResult(termResult);
                return null;
            }
            context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
            return new TurnOutboundPrepPhase(coordinator); 
        }

        List<ToolExecutionResult> operationResults = new ArrayList<>();
        Map<String, ToolExecutionResult> dedupe = new LinkedHashMap<>();
        List<AttachedFilePayload> aggregatedAttachments = new ArrayList<>();

        String batchMutationViolation = coordinator.toolOperationHandler.validateBatchMutationScope(operationRequests, context.getResolver());
        if (batchMutationViolation != null) {
            for (ToolExecutionRequest operationRequest : operationRequests) {
                String phrase = coordinator.toolInfoPhraseFormatter.format(operationRequest, context.getResolver());
                context.getToolInfoPhrases().add(phrase);
                coordinator.cycleStateManager.logOperationPhrase(phrase, context.getRequest());
                operationResults.add(ToolExecutionResult.scopeViolation(
                        operationRequest.getOperation(),
                        context.getResolver().qualify(operationRequest.getBase(), operationRequest.getPath()),
                        batchMutationViolation
                ));
            }
            inbound.setToolResult(operationResults.isEmpty() ? null : operationResults.get(operationResults.size() - 1));
            inbound.setFulfilled(false);
            inbound.setFailureClass("tool-failure");
            context.setPromptAttachments(aggregatedAttachments);

            String nextMessage = operationResults.size() == 1
                    ? coordinator.renderPipelineMessageToModelWithFailureLogging(
                        context.getCycleLog(),
                        inbound.getSequence(),
                        context.getCurrentPipelinePhase(),
                        context.getPipelinePlan(),
                        context.getPipelinePhaseIndex(),
                        context.getNextMessage(),
                        context.getRequest(),
                        context.getConfig(),
                        context.getCycle(),
                        context.getToolPermission(),
                        context.getScriptRunnerConfig().isEnabled(),
                        context.getFirstTurnReferenceTree(),
                        new ArrayList<>(context.getInspectedPaths()),
                        new ArrayList<>(context.getWrittenPaths()),
                        context.getPromptAttachments(),
                        context.getConversationHistory(),
                        null,
                        null,
                        null,
                        "tool-failure",
                        true,
                        operationResults.get(0))
                    : coordinator.toolResultFormatter.formatBatch(operationResults);
            if (operationResults.size() == 1 && (nextMessage == null || nextMessage.isBlank())) {
                nextMessage = coordinator.scriptEvaluationService.formatToolResultWithScript(
                        operationResults.get(0),
                        context.getRequest(),
                        context.getConfig(),
                        context.getCycle(),
                        context.getToolPermission(),
                        context.getScriptRunnerConfig().isEnabled(),
                        context.getFirstTurnReferenceTree(),
                        new ArrayList<>(context.getInspectedPaths()),
                        new ArrayList<>(context.getWrittenPaths()),
                        context.getPromptAttachments());
            }
            context.setNextMessage(nextMessage);
            context.setNextMessageFromScript(false);
            context.getCycle().incrementRetryStreakCount();

            coordinator.cycleStateManager.logAttemptEvent(context.getCycleLog(), inbound, "tool-failure", context.getClosedTurns(), context.getCycle().getMaxTurns(),
                    context.getCycle().getRetryStreakCount(), batchMutationViolation);

            context.setAttemptIndex(context.getAttemptIndex() + 1);
            if (coordinator.configResolver.isNonFulfillmentAttemptBudgetExceeded(context.getConfig(), context.getAttemptIndex())) {
                ReasoningResult termResult = coordinator.terminateCycleOnNonFulfillmentAttemptExhaustion(
                        context.getCycle(),
                        context.getCycleLog(),
                        context.getRequest(),
                        context.getUserFacingMessages(),
                        context.getToolInfoPhrases(),
                        context.getWrittenPaths(),
                        context.getInspectedPaths(),
                        context.getWrittenMtimes(),
                        context.getInspectedMtimes(),
                        context.getReadMarkdownPaths(),
                        context.getFirstTurnReferenceTree(),
                        context.getLogicalTurn(),
                        "tool-failure",
                        context.getCycleCachedContentId(),
                        context.getConfig());
                context.setResult(termResult);
                return null;
            }
            context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
            return new TurnOutboundPrepPhase(coordinator); 
        }

        for (ToolExecutionRequest operationRequest : operationRequests) {
            String phrase = coordinator.toolInfoPhraseFormatter.format(operationRequest, context.getResolver());
            context.getToolInfoPhrases().add(phrase);
            coordinator.cycleStateManager.logOperationPhrase(phrase, context.getRequest());
            String dedupeKey = coordinator.toolOperationHandler.buildDedupeKey(operationRequest);
            ToolExecutionResult operationResult = dedupe.get(dedupeKey);

            if (operationResult == null) {
                ReferenceMutationDecision mutationDecision =
                        coordinator.toolOperationHandler.evaluateReferenceMutationDecision(context.getRequest(), operationRequest, context.getResolver(), context.getConfig());
                if (mutationDecision.isBlocked()) {
                    String reasonText = coordinator.toolOperationHandler.formatDecisionReason(mutationDecision);
                    if (context.getRequest().getOperationLogger() != null) {
                        context.getRequest().getOperationLogger().accept(DefaultReasoningExecutionCoordinator.formatReferenceMutationLogFields(mutationDecision));
                    }
                    boolean addReasoningNotesEnabled = context.getConfig() != null
                            && context.getConfig().getTooling() != null
                            && context.getConfig().getTooling().isAddReasoningNotes();
                    operationResult = ToolExecutionResult.error(
                            operationRequest.getOperation(),
                            context.getResolver().qualify(operationRequest.getBase(), operationRequest.getPath()),
                            DefaultReasoningExecutionCoordinator.formatReferenceMutationBlockedMessage(reasonText, addReasoningNotesEnabled)
                    );
                } else {
                    operationResult = coordinator.toolOperationHandler.executeWithSerialization(
                            operationRequest,
                            context.getResolver(),
                            context.getRequest().getSourceScope(),
                            context.getToolPermission(),
                            context.getScriptRunnerConfig());
                }

                if (operationRequest.getOperation() == ToolExecutionRequest.Operation.READ_FILE
                        && context.getSourceAttachment() != null
                        && coordinator.attachmentManagementService.sameSourceRequest(context.getSourceAttachment(), operationRequest, context.getResolver())) {
                    operationResult = ToolExecutionResult.success(
                            operationRequest.getOperation(),
                            context.getResolver().qualify(operationRequest.getBase(), operationRequest.getPath()),
                            "read"
                    );
                    operationResult.setContent(context.getSourceAttachment().getContent());
                    operationResult.setResolvedBase(context.getResolver().normalizeBase(operationRequest.getBase()));
                }

                dedupe.put(dedupeKey, operationResult);

                if (operationResult.getStatus() == ToolExecutionResult.Status.SUCCESS) {
                    ToolExecutionRequest.Operation op = operationRequest.getOperation();
                    if (op == ToolExecutionRequest.Operation.WRITE_FILE
                            || op == ToolExecutionRequest.Operation.PATCH_FILE) {
                        try {
                            Path writtenAbsolute = context.getResolver().resolve(operationRequest.getBase(), operationRequest.getPath());
                            String relPath = context.getResolver().toProjectRelativePath(writtenAbsolute);
                            context.getWrittenPaths().add(relPath);
                            try {
                                context.getWrittenMtimes().put(relPath, Files.getLastModifiedTime(writtenAbsolute).toMillis());
                            } catch (Exception ignored) {
                            }
                        } catch (Exception ignored) {
                        }
                    } else if (op == ToolExecutionRequest.Operation.DELETE_FILE) {
                        try {
                            Path deletedAbsolute = context.getResolver().resolve(operationRequest.getBase(), operationRequest.getPath());
                            String relPath = context.getResolver().toProjectRelativePath(deletedAbsolute);
                            context.getWrittenPaths().remove(relPath);
                            context.getWrittenMtimes().remove(relPath);
                            context.getInspectedPaths().remove(relPath);
                            context.getInspectedMtimes().remove(relPath);
                        } catch (Exception ignored) {
                        }
                    } else if (op == ToolExecutionRequest.Operation.READ_FILE) {
                        try {
                            String normalizedBase = context.getResolver().normalizeBase(operationRequest.getBase());
                            String mdPath = operationRequest.getPath();
                            if (!"target".equals(normalizedBase) && mdPath != null && mdPath.endsWith(".md")) {
                                Path readAbsolute = context.getResolver().resolve(normalizedBase, mdPath);
                                String relPath = context.getResolver().toProjectRelativePath(readAbsolute);
                                String canonicalReadPath = coordinator.trackingStore.canonicalizePath(relPath);
                                String canonicalSourcePath = context.getRequest().getSourcePath() == null
                                        ? null
                                        : coordinator.trackingStore.canonicalizePath(context.getRequest().getSourcePath());
                                if (canonicalSourcePath == null || !canonicalReadPath.equals(canonicalSourcePath)) {
                                    context.getReadMarkdownPaths().add(relPath);
                                }
                            } else {
                                Path readAbsolute = context.getResolver().resolve(normalizedBase, mdPath);
                                String relPath = context.getResolver().toProjectRelativePath(readAbsolute);
                                context.getInspectedPaths().add(relPath);
                                try {
                                    context.getInspectedMtimes().put(relPath, Files.getLastModifiedTime(readAbsolute).toMillis());
                                } catch (Exception ignored) {
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            if (operationRequest.getOperation() == ToolExecutionRequest.Operation.LIST_COMPILED_FILES
                    && operationResult.getStatus() == ToolExecutionResult.Status.SUCCESS) {
                AttachmentNormalizer.NormalizationOutcome outcome =
                        coordinator.attachmentNormalizer.normalizeQualifiedPaths(operationResult.getListedPaths(), context.getRequest().getBaseMappings());
                operationResult.setCompiledFileStatuses(outcome.getCompiledFileStatuses());
                aggregatedAttachments.addAll(outcome.getAttachments());
            }

            if (operationRequest.getOperation() == ToolExecutionRequest.Operation.READ_FILE) {
                AttachmentNormalizer.ReadFileNormalizationOutcome readOutcome =
                        coordinator.attachmentNormalizer.normalizeReadFileResult(operationRequest, operationResult, context.getResolver());
                operationResult.setReadFileStatuses(readOutcome.getReadFileStatuses());
                aggregatedAttachments.addAll(readOutcome.getAttachments());
            }

            operationResults.add(operationResult);
        }

        inbound.setToolResult(operationResults.isEmpty() ? null : operationResults.get(operationResults.size() - 1));
        List<AttachedFilePayload> promptAttachments = coordinator.scriptSelectionService.applyAttachmentListScript(aggregatedAttachments, context.getConfig());
        context.setPromptAttachments(promptAttachments);

        String nextMessage = operationResults.size() == 1
                ? coordinator.renderPipelineMessageToModelWithFailureLogging(
                    context.getCycleLog(),
                    inbound.getSequence(),
                    context.getCurrentPipelinePhase(),
                    context.getPipelinePlan(),
                    context.getPipelinePhaseIndex(),
                    context.getNextMessage(),
                    context.getRequest(),
                    context.getConfig(),
                    context.getCycle(),
                    context.getToolPermission(),
                    context.getScriptRunnerConfig().isEnabled(),
                    context.getFirstTurnReferenceTree(),
                    new ArrayList<>(context.getInspectedPaths()),
                    new ArrayList<>(context.getWrittenPaths()),
                    promptAttachments,
                    context.getConversationHistory(),
                    context.getTurnDirective().getIntent().name().toLowerCase(),
                    context.getTurnDirective().getContentType() == null ? null : context.getTurnDirective().getContentType().name().toLowerCase(),
                    context.getTurnParseResult().getBody(),
                    null,
                    true,
                    operationResults.get(operationResults.size() - 1))
                : coordinator.toolResultFormatter.formatBatch(operationResults);
        context.setNextMessageFromScript(operationResults.size() == 1);

        if (operationResults.size() == 1 && (nextMessage == null || nextMessage.isBlank())) {
            nextMessage = coordinator.scriptEvaluationService.formatToolResultWithScript(
                    operationResults.get(0),
                    context.getRequest(),
                    context.getConfig(),
                    context.getCycle(),
                    context.getToolPermission(),
                    context.getScriptRunnerConfig().isEnabled(),
                    context.getFirstTurnReferenceTree(),
                    new ArrayList<>(context.getInspectedPaths()),
                    new ArrayList<>(context.getWrittenPaths()),
                    promptAttachments);
            context.setNextMessageFromScript(false);
        }
        context.setNextMessage(nextMessage);

        boolean hasToolFailure = operationResults.stream()
                .anyMatch(res -> res != null && res.getStatus() == ToolExecutionResult.Status.ERROR);
        if (hasToolFailure) {
            inbound.setFulfilled(false);
            inbound.setFailureClass("tool-failure");
            context.getCycle().incrementRetryStreakCount();
            String failureReason = operationResults.stream()
                    .filter(res -> res != null && res.getStatus() == ToolExecutionResult.Status.ERROR)
                    .map(ToolExecutionResult::getFailureReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .findFirst()
                    .orElse("required tool operation failed");

            coordinator.cycleStateManager.logAttemptEvent(context.getCycleLog(), inbound, "tool-failure", context.getClosedTurns(), context.getCycle().getMaxTurns(),
                    context.getCycle().getRetryStreakCount(), failureReason);

            context.setAttemptIndex(context.getAttemptIndex() + 1);
            if (coordinator.configResolver.isNonFulfillmentAttemptBudgetExceeded(context.getConfig(), context.getAttemptIndex())) {
                ReasoningResult termResult = coordinator.terminateCycleOnNonFulfillmentAttemptExhaustion(
                        context.getCycle(),
                        context.getCycleLog(),
                        context.getRequest(),
                        context.getUserFacingMessages(),
                        context.getToolInfoPhrases(),
                        context.getWrittenPaths(),
                        context.getInspectedPaths(),
                        context.getWrittenMtimes(),
                        context.getInspectedMtimes(),
                        context.getReadMarkdownPaths(),
                        context.getFirstTurnReferenceTree(),
                        context.getLogicalTurn(),
                        "tool-failure",
                        context.getCycleCachedContentId(),
                        context.getConfig());
                context.setResult(termResult);
                return null;
            }
            context.getCycle().setCurrentTurnIndex(context.getLogicalTurn());
            return new TurnOutboundPrepPhase(coordinator); 
        }

        return new TurnCompletionPhase(coordinator);
    }
}
