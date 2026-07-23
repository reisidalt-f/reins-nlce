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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContext;
import br.com.dizeno.reins.reasoning.scripting.ReasoningScriptContextFactory;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluationService;
import br.com.dizeno.reins.reasoning.scripting.ContextMessageBuilderService;
import br.com.dizeno.reins.reasoning.scripting.DefaultContextMessageBuilderService;
import br.com.dizeno.reins.reasoning.scripting.ContextStepScriptResolver;
import br.com.dizeno.reins.reasoning.scripting.ReasoningPipelineOrchestrator;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.MessageTypePlan;
import br.com.dizeno.reins.reasoning.scripting.ScriptStepResult;
import br.com.dizeno.reins.reasoning.scripting.ContextMessageBundle;
import br.com.dizeno.reins.reasoning.scripting.ContextMessageBuildRequest;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.reasoning.ReasoningPipelineExecutor;
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.PipelineRuntimeOutcome;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.model.RoutingFlag;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.service.ReasoningConfigResolver;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.reasoning.phase.InitializeCyclePhase;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DefaultReasoningExecutionCoordinator is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing default reasoning execution coordinator.
 */
public class DefaultReasoningExecutionCoordinator implements ReasoningService {
    public static final String REASON_FINISH_SUCCESS = "finish_success";
    public static final String REASON_FINISH_ERROR = "finish_error";
    public static final String REASON_PIPELINE_SKIPPED = "pipeline_skipped";
    
    
    public final ConcurrentHashMap<String, ReentrantLock> sourceLocks = new ConcurrentHashMap<>();

    public final InferenceService inferenceService;
    public final ResponseDirectiveParser directiveParser;
    public final ReasoningPromptBuilder promptBuilder;
    public final ToolingService toolingService;
    public final ToolResultFormatter toolResultFormatter;
    public final ToolInfoPhraseFormatter toolInfoPhraseFormatter;
    public final ReasoningLogService inferenceLogService;
    public final CompilationTrackingStore trackingStore;
    public final AttachmentNormalizer attachmentNormalizer;
    public final ReferenceTreeContextService referenceTreeContextService;
    public final ReferencedAttachmentBuilder referencedAttachmentBuilder;
    public final ToolRequestParser toolRequestParser;
    public final ScriptEvaluator scriptEvaluator;
    public final ReasoningScriptContextFactory scriptContextFactory;
    public final ContextMessageBuilderService contextMessageBuilderService;
    public final ContextStepScriptResolver contextStepScriptResolver;
    public final ReasoningPipelineExecutor inferencePipelineExecutor;
    public final ReasoningConfigResolver configResolver;
    public final MessageFormattingService messageFormattingService;
    public final ReasoningToolOperationHandler toolOperationHandler;
    public final ReasoningCycleStateManager cycleStateManager;
    public final ScriptEvaluationService scriptEvaluationService;
    public final ReasoningPipelineOrchestrator inferencePipelineOrchestrator;
    public final ReasoningAttachmentManagementService attachmentManagementService;
    public final ReasoningScriptSelectionService scriptSelectionService;
    public final ReasoningGraceTurnExecutor graceTurnExecutor;

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     */
    public DefaultReasoningExecutionCoordinator() {
        this(new InferenceService(),
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                new ToolingService(),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     */
    public DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter) {
        this(inferenceService, directiveParser, promptBuilder, toolingService, toolResultFormatter,
                new FileReasoningLogService(),
                new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     */
    public DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService) {
        this(inferenceService, directiveParser, promptBuilder, toolingService, toolResultFormatter, inferenceLogService,
                        new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     */
    public DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore) {
        this(inferenceService, directiveParser, promptBuilder, toolingService, toolResultFormatter,
                inferenceLogService, trackingStore,
                new ToolInfoPhraseFormatter(), new AttachmentNormalizer(),
            new ReferenceTreeContextService(), new ReferencedAttachmentBuilder(), new ToolRequestParser());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     * @param toolInfoPhraseFormatter the tool info phrase formatter
     * @param attachmentNormalizer the attachment normalizer
     * @param referenceTreeContextService the reference tree context service
     * @param toolRequestParser the tool request parser
     */
    public DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore,
                                   ToolInfoPhraseFormatter toolInfoPhraseFormatter,
                                   AttachmentNormalizer attachmentNormalizer,
                                   ReferenceTreeContextService referenceTreeContextService,
                                   ToolRequestParser toolRequestParser) {
        this(inferenceService, directiveParser, promptBuilder, toolingService, toolResultFormatter,
                inferenceLogService, trackingStore, toolInfoPhraseFormatter, attachmentNormalizer,
                        referenceTreeContextService, new ReferencedAttachmentBuilder(), toolRequestParser);
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningExecutionCoordinator}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     * @param toolInfoPhraseFormatter the tool info phrase formatter
     * @param attachmentNormalizer the attachment normalizer
     * @param referenceTreeContextService the reference tree context service
     * @param referencedAttachmentBuilder the referenced attachment builder
     * @param toolRequestParser the tool request parser
     */
    public DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore,
                                   ToolInfoPhraseFormatter toolInfoPhraseFormatter,
                                   AttachmentNormalizer attachmentNormalizer,
                                   ReferenceTreeContextService referenceTreeContextService,
                                   ReferencedAttachmentBuilder referencedAttachmentBuilder,
                                   ToolRequestParser toolRequestParser) {
        this.inferenceService = inferenceService;
        this.directiveParser = directiveParser;
        this.promptBuilder = promptBuilder;
        this.toolingService = toolingService;
        this.toolResultFormatter = toolResultFormatter;
        this.toolInfoPhraseFormatter = toolInfoPhraseFormatter;
        this.inferenceLogService = inferenceLogService;
        this.trackingStore = trackingStore;
        this.attachmentNormalizer = attachmentNormalizer;
        this.referenceTreeContextService = referenceTreeContextService;
        this.referencedAttachmentBuilder = referencedAttachmentBuilder;
        this.toolRequestParser = toolRequestParser;
        this.scriptEvaluator = buildDefaultScriptEvaluator();
        this.scriptContextFactory = new ReasoningScriptContextFactory();
        this.contextMessageBuilderService = new DefaultContextMessageBuilderService();
        this.contextStepScriptResolver = new ContextStepScriptResolver();
        this.inferencePipelineExecutor = new ReasoningPipelineExecutor(this.scriptEvaluator, this.directiveParser);
        this.configResolver = new ReasoningConfigResolver();
        this.messageFormattingService = new MessageFormattingService();
        this.toolOperationHandler = new ReasoningToolOperationHandler(
            this.toolingService,
            this.trackingStore,
            this.sourceLocks);
        this.cycleStateManager = new ReasoningCycleStateManager(this.inferenceLogService);
        this.scriptEvaluationService = new ScriptEvaluationService(
            this.scriptEvaluator,
            this.scriptContextFactory,
            this.contextStepScriptResolver,
            this.inferencePipelineExecutor,
            this.messageFormattingService);
        this.inferencePipelineOrchestrator = new ReasoningPipelineOrchestrator(this.scriptEvaluationService);
        this.attachmentManagementService = new ReasoningAttachmentManagementService(
            this.referencedAttachmentBuilder,
            this.messageFormattingService,
            this.cycleStateManager);
        this.scriptSelectionService = new ReasoningScriptSelectionService(
            this.scriptEvaluationService,
            this.messageFormattingService);
        this.graceTurnExecutor = new ReasoningGraceTurnExecutor(
            this.inferenceService,
            this.directiveParser,
            this.configResolver,
            this.cycleStateManager,
            this.scriptEvaluationService,
            this.messageFormattingService,
            this.scriptSelectionService);
    }

    /**
     * With Script Evaluator.
     *
     * @param evaluator the script evaluator instance
     * @return the resolved or constructed object
     */
    public DefaultReasoningExecutionCoordinator withScriptEvaluator(ScriptEvaluator evaluator) {
        return new DefaultReasoningExecutionCoordinator(
                this.inferenceService,
                this.directiveParser,
                this.promptBuilder,
                this.toolingService,
                this.toolResultFormatter,
                this.inferenceLogService,
                this.trackingStore,
                this.toolInfoPhraseFormatter,
                this.attachmentNormalizer,
                this.referenceTreeContextService,
                this.referencedAttachmentBuilder,
                this.toolRequestParser,
                evaluator,
                new ReasoningScriptContextFactory());
    }

    private DefaultReasoningExecutionCoordinator(InferenceService inferenceService,
                                    ResponseDirectiveParser directiveParser,
                                    ReasoningPromptBuilder promptBuilder,
                                    ToolingService toolingService,
                                    ToolResultFormatter toolResultFormatter,
                                    ReasoningLogService inferenceLogService,
                                    CompilationTrackingStore trackingStore,
                                    ToolInfoPhraseFormatter toolInfoPhraseFormatter,
                                    AttachmentNormalizer attachmentNormalizer,
                                    ReferenceTreeContextService referenceTreeContextService,
                                    ReferencedAttachmentBuilder referencedAttachmentBuilder,
                                    ToolRequestParser toolRequestParser,
                                    ScriptEvaluator scriptEvaluator,
                                    ReasoningScriptContextFactory scriptContextFactory) {
        this.inferenceService = inferenceService;
        this.directiveParser = directiveParser;
        this.promptBuilder = promptBuilder;
        this.toolingService = toolingService;
        this.toolResultFormatter = toolResultFormatter;
        this.toolInfoPhraseFormatter = toolInfoPhraseFormatter;
        this.inferenceLogService = inferenceLogService;
        this.trackingStore = trackingStore;
        this.attachmentNormalizer = attachmentNormalizer;
        this.referenceTreeContextService = referenceTreeContextService;
        this.referencedAttachmentBuilder = referencedAttachmentBuilder;
        this.toolRequestParser = toolRequestParser;
        this.scriptEvaluator = scriptEvaluator;
        this.scriptContextFactory = scriptContextFactory;
        this.contextMessageBuilderService = new DefaultContextMessageBuilderService();
        this.contextStepScriptResolver = new ContextStepScriptResolver();
        this.inferencePipelineExecutor = new ReasoningPipelineExecutor(this.scriptEvaluator, this.directiveParser);
        this.configResolver = new ReasoningConfigResolver();
        this.messageFormattingService = new MessageFormattingService();
        this.toolOperationHandler = new ReasoningToolOperationHandler(
            this.toolingService,
            this.trackingStore,
            this.sourceLocks);
        this.cycleStateManager = new ReasoningCycleStateManager(this.inferenceLogService);
        this.scriptEvaluationService = new ScriptEvaluationService(
            this.scriptEvaluator,
            this.scriptContextFactory,
            this.contextStepScriptResolver,
            this.inferencePipelineExecutor,
            this.messageFormattingService);
        this.inferencePipelineOrchestrator = new ReasoningPipelineOrchestrator(this.scriptEvaluationService);
        this.attachmentManagementService = new ReasoningAttachmentManagementService(
            this.referencedAttachmentBuilder,
            this.messageFormattingService,
            this.cycleStateManager);
        this.scriptSelectionService = new ReasoningScriptSelectionService(
            this.scriptEvaluationService,
            this.messageFormattingService);
        this.graceTurnExecutor = new ReasoningGraceTurnExecutor(
            this.inferenceService,
            this.directiveParser,
            this.configResolver,
            this.cycleStateManager,
            this.scriptEvaluationService,
            this.messageFormattingService,
            this.scriptSelectionService);
    }

    private ScriptEvaluator buildDefaultScriptEvaluator() {
        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver());
        registry.validateAll();
        return new ScriptEvaluator(registry, null);
    }

    /**
     * Gets the tooling service.
     *
     * @return the resolved or constructed object
     */
    public ToolingService getToolingService() {
        return toolingService;
    }

    /**
     * Runs the execution cycle cycle.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public ReasoningResult runCycle(ReasoningRequest request, ReinsConfig config) throws Exception {
        ReasoningCycle cycle = new ReasoningCycle();
        Path projectRoot = configResolver.resolveProjectRoot(request);
        ReasoningContext context = new ReasoningContext(request, config, cycle, projectRoot);

        try {
            ReasoningPhase phase = new InitializeCyclePhase(this);
            while (phase != null) {
                phase = phase.execute(context);
            }
        } finally {
            if (context.getCycleLog() != null) {
                cycleStateManager.closeCycleLog(context.getCycleLog());
            }
            cleanupCachedContentIfNeeded(config, request, context.getCycleLog(), context.getCycleCachedContentId());
        }

        return context.getResult();
    }

    /**
     * Terminate Cycle On Non Fulfillment Attempt Exhaustion.
     *
     * @param cycle the cycle
     * @param cycleLog the reasoning cycle log instance
     * @param request the request containing path and scope metadata
     * @param userFacingMessages the user facing messages
     * @param toolInfoPhrases the tool info phrases
     * @param writtenPaths the written paths
     * @param inspectedPaths the inspected paths
     * @param writtenMtimes the written mtimes
     * @param inspectedMtimes the inspected mtimes
     * @param readMarkdownPaths the read markdown paths
     * @param firstTurnReferenceTree the first turn reference tree
     * @param logicalTurn the logical turn
     * @param failureClass the failure class
     * @param cycleCachedContentId the cycle cached content id
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    public ReasoningResult terminateCycleOnNonFulfillmentAttemptExhaustion(
            ReasoningCycle cycle,
            ReasoningCycleLog cycleLog,
            ReasoningRequest request,
            List<String> userFacingMessages,
            List<String> toolInfoPhrases,
            java.util.LinkedHashSet<String> writtenPaths,
            java.util.LinkedHashSet<String> inspectedPaths,
            java.util.LinkedHashMap<String, Long> writtenMtimes,
            java.util.LinkedHashMap<String, Long> inspectedMtimes,
            java.util.LinkedHashSet<String> readMarkdownPaths,
            String firstTurnReferenceTree,
            int logicalTurn,
            String failureClass,
            String cycleCachedContentId,
            ReinsConfig config) {
        cycle.setStatus(ReasoningCycle.Status.FINISHED_ERROR);
        cycle.setCompletedAt(Instant.now());
        String terminalMessage = "Reasoning failed after exhausting non-fulfillment attempts on turn "
                + logicalTurn + " (" + failureClass + ").";

        if (cycleLog != null) {
            cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "system", 0,
                    "lifecycle: non-fulfillment-attempts-exhausted\n"
                            + "turn=" + logicalTurn + "\n"
                            + "failureClass=" + failureClass);
            cycleStateManager.closeCycleLog(cycleLog);
        }

        ReasoningResult result = cycleStateManager.buildResult(
                cycle,
                ResponseDirective.Intent.FINISH_ERROR,
                REASON_FINISH_ERROR,
                terminalMessage,
                userFacingMessages,
                toolInfoPhrases,
                scriptSelectionService.applyFileListScript(new java.util.ArrayList<>(writtenPaths), config),
                new java.util.ArrayList<>(inspectedPaths),
                new java.util.LinkedHashMap<>(writtenMtimes),
                new java.util.LinkedHashMap<>(inspectedMtimes),
                new java.util.ArrayList<>(readMarkdownPaths),
                firstTurnReferenceTree);

        cleanupCachedContentIfNeeded(config, request, cycleLog, cycleCachedContentId);
        return result;
    }

    /**
     * Render Pipeline Gemini Message With Failure Logging.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param sequence the sequence
     * @param currentPhase the current phase
     * @param pipelinePlan the pipeline plan
     * @param pipelinePhaseIndex the pipeline phase index
     * @param messageContext the message context
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @param conversationHistory the conversation history
     * @param lastDirectiveIntent the last directive intent
     * @param lastDirectiveContentType the last directive content type
     * @param lastDirectiveBody the last directive body
     * @param lastFailureClass the last failure class
     * @param currentToolResultAvailable the current tool result available
     * @param currentToolResult the current tool result
     * @return the string result
     */
    public String renderPipelineGeminiMessageWithFailureLogging(ReasoningCycleLog cycleLog,
                                                                 int sequence,
                                                                 String currentPhase,
                                                                 ReasoningPipelinePlan pipelinePlan,
                                                                 int pipelinePhaseIndex,
                                                                 String messageContext,
                                                                 ReasoningRequest request,
                                                                 ReinsConfig config,
                                                                 ReasoningCycle cycle,
                                                                 FilePolicy policy,
                                                                 boolean scriptRunnerEnabled,
                                                                 String referenceTree,
                                                                 List<String> inspectedPaths,
                                                                 List<String> compiledPaths,
                                                                 List<AttachedFilePayload> attachments,
                                                                 List<ConversationMessage> conversationHistory,
                                                                 String lastDirectiveIntent,
                                                                 String lastDirectiveContentType,
                                                                 String lastDirectiveBody,
                                                                 String lastFailureClass,
                                                                 boolean currentToolResultAvailable,
                                                                 ToolExecutionResult currentToolResult) {
        try {
            return inferencePipelineOrchestrator.renderPipelineGeminiMessage(
                    currentPhase,
                    pipelinePlan,
                    pipelinePhaseIndex,
                    messageContext,
                    request,
                    config,
                    cycle,
                    policy,
                    scriptRunnerEnabled,
                    referenceTree,
                    inspectedPaths,
                    compiledPaths,
                    attachments,
                    conversationHistory,
                    lastDirectiveIntent,
                    lastDirectiveContentType,
                    lastDirectiveBody,
                    lastFailureClass,
                    currentToolResultAvailable,
                    currentToolResult);
        } catch (ScriptEvaluationService.PipelineMessageParseException parseEx) {
            if (cycleLog != null) {
                String logBody = "lifecycle: pipeline-script-parse-failed\n"
                        + "phase=" + (parseEx.getPhase() == null ? "unknown" : parseEx.getPhase()) + "\n"
                        + "failure-reason=" + (parseEx.getFailureReason() == null ? "unknown" : parseEx.getFailureReason()) + "\n"
                        + "script-output:\n"
                        + (parseEx.getRenderedMessage() == null ? "" : parseEx.getRenderedMessage());
                cycleStateManager.writeLog(cycleLog,
                        ReasoningLogEntry.Direction.OUTBOUND,
                        "pipeline-script-parse-failed",
                        sequence,
                        logBody);
            }
            throw parseEx;
        }
    }

    /**
     * Format Next Message.
     *
     * @param message the message content
     * @param turnIndex the turn index
     * @param maxTurns the maximum turn limit for the reasoning cycle
     * @param turnCountNoteEnabled the turn count note enabled
     * @param nextMessageFromScript the next message from script
     * @return the string result
     */
    public String formatNextMessage(String message,
                                     int turnIndex,
                                     int maxTurns,
                                     boolean turnCountNoteEnabled,
                                     boolean nextMessageFromScript) {
        if (isTrustedSpoofedAssistantMessage(message, nextMessageFromScript)) {
            return message == null ? "" : message;
        }
        return messageFormattingService.withTurnCountNote(message, turnIndex, maxTurns, turnCountNoteEnabled);
    }

    /**
     * Checks if the component is trusted spoofed assistant message.
     *
     * @param message the message content
     * @param nextMessageFromScript the next message from script
     * @return true if successful or matching, false otherwise
     */
    public boolean isTrustedSpoofedAssistantMessage(String message, boolean nextMessageFromScript) {
        if (!nextMessageFromScript || message == null || message.isBlank()) {
            return false;
        }
        PipelineExchangeMessage candidate = directiveParser.parseCanonicalMessage(message);
        return candidate != null
            && candidate.isValid()
            && candidate.isSpoofedAssistantRole()
            && SpoofingMessageValidator.validateHeaders(candidate.getHeaders(), true) == null;
    }

    /**
     * Cleanup Cached Content If Needed.
     *
     * @param config the Reins configuration settings
     * @param request the request containing path and scope metadata
     * @param cycleLog the reasoning cycle log instance
     * @param cachedContentId the cached content id
     */
    public void cleanupCachedContentIfNeeded(ReinsConfig config,
                                              ReasoningRequest request,
                                              ReasoningCycleLog cycleLog,
                                              String cachedContentId) {
        if (cachedContentId == null || cachedContentId.isBlank()) {
            return;
        }
        try {
            cycleStateManager.emitLifecycleMessage(cycleLog, "cache-delete-attempt: " + cachedContentId);
            inferenceService.deleteCachedContent(config, cachedContentId);
            cycleStateManager.emitLifecycleMessage(cycleLog, "cache-deleted: " + cachedContentId);
        } catch (Exception ex) {
            cycleStateManager.emitLifecycleMessage(cycleLog, "cache-delete-failed: " + cachedContentId + " cause=" + ex.getMessage());
        }
    }

    /**
     * Builds the configured target parse error retry message.
     *
     * @param failureReason the failure reason
     * @param config the Reins configuration settings
     * @return the string result
     */
    public String buildParseErrorRetryMessage(String failureReason, ReinsConfig config) {
        String scriptedRetry = scriptEvaluationService.evaluateScriptSafely(
            "retry-message.ftl",
            scriptEvaluationService.buildScriptBaseContext(
                null,
                config,
                null,
                null,
                false,
                null,
                List.of(),
                List.of(),
                List.of(),
                false),
            config,
            "retry-message");
        if (scriptedRetry == null || scriptedRetry.isBlank()) {
            throw new IllegalStateException("Required script 'retry-message.ftl' produced empty content.");
        }
        return appendParseErrorDetails(scriptedRetry, failureReason);
    }

    /**
     * Append Parse Error Details.
     *
     * @param retryMessage the retry message
     * @param failureReason the failure reason
     * @return the string result
     */
    public String appendParseErrorDetails(String retryMessage, String failureReason) {
        if (retryMessage == null || retryMessage.isBlank()) {
            return retryMessage;
        }
        if (failureReason == null || failureReason.isBlank()) {
            return retryMessage;
        }
        if (retryMessage.contains(failureReason)) {
            return retryMessage;
        }
        return retryMessage.trim()
                + System.lineSeparator()
                + System.lineSeparator()
                + "Previous error:"
                + System.lineSeparator()
                + failureReason.trim()
                + System.lineSeparator()
                + System.lineSeparator()
                + "Avoid repeating this exact formatting mistake. Correct the response so it satisfies the error above.";
    }

    /**
     * Format Reference Mutation Blocked Message.
     *
     * @param reasonText the reason text
     * @return the string result
     */
    public static String formatReferenceMutationBlockedMessage(String reasonText) {
        return "Mutation of artifacts compiled by referenced markdown is not allowed. "
                + "Use add_reasoning_note on the artifact you attempted to mutate to trigger reprocessing of the owning source, describe the reason for the block, and describe what needs to change. "
                + reasonText;
    }

    /**
     * Format Reference Mutation Log Fields.
     *
     * @param decision the decision
     * @return the string result
     */
    public static String formatReferenceMutationLogFields(ReferenceMutationDecision decision) {
        return "referenceMutationBlocked=true reason=" + decision.getReasonCode()
                + " ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
    }
}
