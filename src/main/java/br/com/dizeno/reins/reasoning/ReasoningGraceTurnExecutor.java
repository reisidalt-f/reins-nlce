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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluationService;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.service.ReasoningConfigResolver;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReasoningGraceTurnExecutor is part of the orchestration of conversational
 * reasoning loops, prompt construction, and tool instruction mapping in the
 * reins architecture.
 * Acts as a component managing reasoning grace turn executor.
 */
public class ReasoningGraceTurnExecutor {

    static final String REASON_FINISH_ERROR = "finish_error";
    static final String REASON_MAX_TURN_EXHAUSTED = "max_turn_exhausted";

    private final InferenceService inferenceService;
    private final ResponseDirectiveParser directiveParser;
    private final ReasoningConfigResolver configResolver;
    private final ReasoningCycleStateManager cycleStateManager;
    private final ScriptEvaluationService scriptEvaluationService;
    private final MessageFormattingService messageFormattingService;
    private final ReasoningScriptSelectionService scriptSelectionService;

    /**
     * Constructs a new instance of {@link ReasoningGraceTurnExecutor}.
     *
     * @param inferenceService         the service invoking LLM endpoints
     * @param directiveParser          the directive parser
     * @param configResolver           the config resolver
     * @param cycleStateManager        the cycle state manager
     * @param scriptEvaluationService  the script evaluation service
     * @param messageFormattingService the message formatting service
     * @param scriptSelectionService   the script selection service
     */
    public ReasoningGraceTurnExecutor(InferenceService inferenceService,
            ResponseDirectiveParser directiveParser,
            ReasoningConfigResolver configResolver,
            ReasoningCycleStateManager cycleStateManager,
            ScriptEvaluationService scriptEvaluationService,
            MessageFormattingService messageFormattingService,
            ReasoningScriptSelectionService scriptSelectionService) {
        this.inferenceService = inferenceService;
        this.directiveParser = directiveParser;
        this.configResolver = configResolver;
        this.cycleStateManager = cycleStateManager;
        this.scriptEvaluationService = scriptEvaluationService;
        this.messageFormattingService = messageFormattingService;
        this.scriptSelectionService = scriptSelectionService;
    }

    /**
     * Runs the execution cycle.
     *
     * @param cycle                  the cycle
     * @param cycleLog               the reasoning cycle log instance
     * @param request                the request containing path and scope metadata
     * @param conversationHistory    the conversation history
     * @param config                 the Reins configuration settings
     * @param userFacingMessages     the user facing messages
     * @param toolInfoPhrases        the tool info phrases
     * @param writtenPaths           the written paths
     * @param inspectedPaths         the inspected paths
     * @param writtenMtimes          the written mtimes
     * @param inspectedMtimes        the inspected mtimes
     * @param readMarkdownPaths      the read markdown paths
     * @param activePerSourcePhases  the active per source phases
     * @param firstTurnReferenceTree the first turn reference tree
     * @return the resulting result
     */
    public ReasoningResult run(ReasoningCycle cycle,
            ReasoningCycleLog cycleLog,
            ReasoningRequest request,
            List<ConversationMessage> conversationHistory,
            ReinsConfig config,
            List<String> userFacingMessages,
            List<String> toolInfoPhrases,
            List<String> writtenPaths,
            List<String> inspectedPaths,
            Map<String, Long> writtenMtimes,
            Map<String, Long> inspectedMtimes,
            List<String> readMarkdownPaths,
            List<String> activePerSourcePhases,
            String firstTurnReferenceTree) throws Exception {
        cycle.setGraceTurnUsed(true);
        cycle.setMaxTurnAt(cycle.getMaxTurns());
        FilePolicy toolPermission = configResolver.buildToolPolicy(config);
        ScriptRunnerConfig scriptRunnerConfig = ScriptRunnerConfig.fromSettings(config,
                configResolver.resolveProjectRoot(request));
        String gracePromptText = scriptEvaluationService.evaluateGracePromptScript(
                request,
                config,
                cycle,
                toolPermission,
                scriptRunnerConfig.isEnabled(),
                firstTurnReferenceTree,
                new ArrayList<>(inspectedPaths),
                new ArrayList<>(writtenPaths),
                List.of());
        String gracePrompt = messageFormattingService.withTurnCountNote(
                gracePromptText,
                cycle.getClosedTurnCount() + 1,
                cycle.getMaxTurns(),
                configResolver.isTurnCountNoteEnabled(config));
        conversationHistory.add(new ConversationMessage(ConversationMessage.Role.USER, gracePrompt, List.of()));

        cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "system",
                cycle.getTurns().size() + 1,
                "lifecycle: grace-turn-enter\nmaxTurns=" + cycle.getMaxTurns());

        ReasoningTurn graceOutbound = new ReasoningTurn();
        graceOutbound.setSequence(cycle.getTurns().size() + 1);
        graceOutbound.setDirection(ReasoningTurn.Direction.OUTBOUND);
        graceOutbound.setPayload(gracePrompt);
        graceOutbound.setAttachedFiles(new ArrayList<>());
        cycle.getTurns().add(graceOutbound);

        cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "user", graceOutbound.getSequence(),
                gracePrompt);

        MarkdownInferenceRequest graceRequest = buildInferenceRequestWithHistory(request, conversationHistory, config);
        MarkdownInferenceResponse graceResponse = inferenceService.infer(graceRequest, config);
        conversationHistory
                .add(new ConversationMessage(ConversationMessage.Role.MODEL, graceResponse.getRawResponseText()));

        ReasoningTurn graceInbound = new ReasoningTurn();
        graceInbound.setSequence(cycle.getTurns().size() + 1);
        graceInbound.setDirection(ReasoningTurn.Direction.INBOUND);
        graceInbound.setPayload(graceResponse.getRawResponseText());
        cycle.getTurns().add(graceInbound);

        cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.INBOUND, "assistant",
                graceInbound.getSequence(),
                graceResponse.getRawResponseText());

        ResponseDirectiveParser.ParseResult graceParsed = directiveParser.parse(graceResponse.getRawResponseText());
        ResponseDirective graceDirective = graceParsed.getDirective();
        graceInbound.setDirective(graceDirective);

        List<ResponseDirectiveParser.ParseResult> graceBlocks = graceParsed.getBlocks();
        if (graceBlocks != null) {
            for (ResponseDirectiveParser.ParseResult block : graceBlocks) {
                ResponseDirective bd = block.getDirective();
                if (bd.isValid()
                        && bd.getContentType() == ResponseDirective.ContentType.MESSAGE_TO_USER
                        && block.getBody() != null
                        && !block.getBody().isBlank()) {
                    userFacingMessages.add(block.getBody().trim());
                    if (request.getUserMessageListener() != null) {
                        try {
                            request.getUserMessageListener().accept(block.getBody().trim());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        if (cycleLog != null) {
            cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "system",
                    cycle.getTurns().size() + 1,
                    "lifecycle: grace-turn-exit");
            cycleStateManager.closeCycleLog(cycleLog);
        }

        cycle.setStatus(ReasoningCycle.Status.FINISHED_ERROR);
        cycle.setCompletedAt(Instant.now());

        if (graceDirective.isValid() && graceDirective.getIntent() == ResponseDirective.Intent.FINISH_ERROR) {
            return cycleStateManager.buildResult(cycle, ResponseDirective.Intent.FINISH_ERROR,
                    REASON_FINISH_ERROR,
                    "Grace turn finished with explicit finish-error intent.",
                    userFacingMessages,
                    toolInfoPhrases,
                    scriptSelectionService.applyFileListScript(writtenPaths, config),
                    inspectedPaths,
                    writtenMtimes,
                    inspectedMtimes,
                    readMarkdownPaths,
                    firstTurnReferenceTree);
        }

        return cycleStateManager.buildResult(cycle, ResponseDirective.Intent.FINISH_ERROR,
                REASON_MAX_TURN_EXHAUSTED,
                "Reasoning could not conclude because max turns were reached without a valid finish intent.",
                userFacingMessages,
                toolInfoPhrases,
                scriptSelectionService.applyFileListScript(writtenPaths, config),
                inspectedPaths,
                writtenMtimes,
                inspectedMtimes,
                readMarkdownPaths,
                firstTurnReferenceTree);
    }

    /**
     * Builds the configured target inference request with history.
     *
     * @param request             the request containing path and scope metadata
     * @param conversationHistory the conversation history
     * @param config              the Reins configuration settings
     * @return the resolved or constructed object
     */
    public MarkdownInferenceRequest buildInferenceRequestWithHistory(ReasoningRequest request,
            List<ConversationMessage> conversationHistory,
            ReinsConfig config) {
        MarkdownInferenceRequest inferenceRequest = new MarkdownInferenceRequest();
        inferenceRequest
                .setSourcePath(request.getSourcePath() == null ? "reasoning-cycle.md" : request.getSourcePath());
        inferenceRequest.setSourceScope(request.getSourceScope() == null ? "main" : request.getSourceScope());
        inferenceRequest.setSourceBase(configResolver.resolveSourceBase(request.getSourceScope()));
        String sourcePath = request.getSourcePath();
        if (sourcePath != null) {
            inferenceRequest.setSourceRelativePath(sourcePath);
        }
        inferenceRequest.setSourceHash(request.getSourceHash());
        inferenceRequest.setConversationHistory(conversationHistory);
        inferenceRequest.setCachedContentId(request.getCachedContentId());
        inferenceRequest.setUseCachedContent(request.isUseCachedContent());
        inferenceRequest.setPromptTemplateVersion("reasoning-v1");
        inferenceRequest.setModelConfigSnapshot(
                request.getModelConfigSnapshot() == null ? config.resolveActiveModelSettings() : request.getModelConfigSnapshot());
        return inferenceRequest;
    }
}
