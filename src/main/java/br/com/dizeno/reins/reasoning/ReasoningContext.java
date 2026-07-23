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
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.ReasoningPipelinePlan;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.MessageTypePlan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * ReasoningContext is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reasoning context.
 */
public class ReasoningContext {
    private final ReasoningRequest request;
    private final ReinsConfig config;
    private final ReasoningCycle cycle;
    private final Path projectRoot;

    private ReasoningCycleLog cycleLog;
    private String nextMessage;
    private boolean nextMessageFromScript;

    private final List<String> userFacingMessages = new ArrayList<>();
    private final List<String> toolInfoPhrases = new ArrayList<>();
    private final LinkedHashSet<String> writtenPaths = new LinkedHashSet<>();
    private final LinkedHashSet<String> inspectedPaths = new LinkedHashSet<>();
    private final LinkedHashMap<String, Long> writtenMtimes = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> inspectedMtimes = new LinkedHashMap<>();
    private final LinkedHashSet<String> readMarkdownPaths = new LinkedHashSet<>();

    private List<AttachedFilePayload> promptAttachments;
    private final List<ConversationMessage> conversationHistory = new ArrayList<>();

    private FilePolicy toolPermission;
    private ScriptRunnerConfig scriptRunnerConfig;

    private String firstTurnReferenceTree;
    private ReasoningPipelinePlan pipelinePlan;
    private int pipelinePhaseIndex;
    private String currentPipelinePhase;
    private List<String> activePerSourcePhases;
    private String systemContext;
    private boolean cachedContentEnabled;
    private String cycleCachedContentId;

    private BasePathResolver resolver;
    private AttachedFilePayload sourceAttachment;
    private ReferenceTreeContextService.ReferenceTreeContext firstTurnReferenceTreeContext;
    private List<AttachedFilePayload> firstTurnContextAttachments;

    private ReasoningResult result;

    private List<ConversationMessage> prependMessages;
    private List<MessageTypePlan> contextPlan;
    private String prependMessagesSummary;
    private String prependMessagesSafeSummary;
    private boolean logSystemContextEnabled;

    
    private int logicalTurn = 1;
    private int attemptIndex = 1;
    private int closedTurns = 0;
    private boolean sourceAttachmentSent = false;
    private String turnMessageContent;
    private List<AttachedFilePayload> turnOutboundAttachments;
    private MarkdownInferenceResponse turnResponse;
    private ResponseDirectiveParser.ParseResult turnParseResult;
    private ResponseDirective turnDirective;
    private ReasoningTurn turnInbound;
    private ReasoningTurn turnOutbound;
    private List<ToolExecutionRequest> turnOperationRequests;

    /**
     * Constructs a new instance of {@link ReasoningContext}.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param projectRoot the root path of the project
     */
    public ReasoningContext(ReasoningRequest request, ReinsConfig config, ReasoningCycle cycle, Path projectRoot) {
        this.request = request;
        this.config = config;
        this.cycle = cycle;
        this.projectRoot = projectRoot;
        this.nextMessage = request.getMessage();
        this.promptAttachments = request.getAttachments() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getAttachments());
    }

    /**
     * Gets the request.
     *
     * @return the resolved or constructed object
     */
    public ReasoningRequest getRequest() {
        return request;
    }

    /**
     * Gets the config.
     *
     * @return the resulting config
     */
    public ReinsConfig getConfig() {
        return config;
    }

    /**
     * Gets the cycle.
     *
     * @return the resolved or constructed object
     */
    public ReasoningCycle getCycle() {
        return cycle;
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Gets the cycle log.
     *
     * @return the resolved or constructed object
     */
    public ReasoningCycleLog getCycleLog() {
        return cycleLog;
    }

    /**
     * Sets the cycle log.
     *
     * @param cycleLog the reasoning cycle log instance
     */
    public void setCycleLog(ReasoningCycleLog cycleLog) {
        this.cycleLog = cycleLog;
    }

    /**
     * Gets the next message.
     *
     * @return the string result
     */
    public String getNextMessage() {
        return nextMessage;
    }

    /**
     * Sets the next message.
     *
     * @param nextMessage the next message
     */
    public void setNextMessage(String nextMessage) {
        this.nextMessage = nextMessage;
    }

    /**
     * Checks if the component is next message from script.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isNextMessageFromScript() {
        return nextMessageFromScript;
    }

    /**
     * Sets the next message from script.
     *
     * @param nextMessageFromScript the next message from script
     */
    public void setNextMessageFromScript(boolean nextMessageFromScript) {
        this.nextMessageFromScript = nextMessageFromScript;
    }

    /**
     * Gets the user facing messages.
     *
     * @return the string result
     */
    public List<String> getUserFacingMessages() {
        return userFacingMessages;
    }

    /**
     * Gets the tool info phrases.
     *
     * @return the string result
     */
    public List<String> getToolInfoPhrases() {
        return toolInfoPhrases;
    }

    /**
     * Gets the written paths.
     *
     * @return the string result
     */
    public LinkedHashSet<String> getWrittenPaths() {
        return writtenPaths;
    }

    /**
     * Gets the inspected paths.
     *
     * @return the string result
     */
    public LinkedHashSet<String> getInspectedPaths() {
        return inspectedPaths;
    }

    /**
     * Gets the written mtimes.
     *
     * @return the string result
     */
    public LinkedHashMap<String, Long> getWrittenMtimes() {
        return writtenMtimes;
    }

    /**
     * Gets the inspected mtimes.
     *
     * @return the string result
     */
    public LinkedHashMap<String, Long> getInspectedMtimes() {
        return inspectedMtimes;
    }

    /**
     * Gets the read markdown paths.
     *
     * @return the string result
     */
    public LinkedHashSet<String> getReadMarkdownPaths() {
        return readMarkdownPaths;
    }

    /**
     * Gets the prompt attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getPromptAttachments() {
        return promptAttachments;
    }

    /**
     * Sets the prompt attachments.
     *
     * @param promptAttachments the prompt attachments
     */
    public void setPromptAttachments(List<AttachedFilePayload> promptAttachments) {
        this.promptAttachments = promptAttachments;
    }

    /**
     * Gets the conversation history.
     *
     * @return the collection of elements
     */
    public List<ConversationMessage> getConversationHistory() {
        return conversationHistory;
    }

    /**
     * Gets the tool permission.
     *
     * @return the resolved or constructed object
     */
    public FilePolicy getToolPermission() {
        return toolPermission;
    }

    /**
     * Sets the tool permission.
     *
     * @param toolPermission the tool permission
     */
    public void setToolPermission(FilePolicy toolPermission) {
        this.toolPermission = toolPermission;
    }

    /**
     * Gets the script runner config.
     *
     * @return the resulting config
     */
    public ScriptRunnerConfig getScriptRunnerConfig() {
        return scriptRunnerConfig;
    }

    /**
     * Sets the script runner config.
     *
     * @param scriptRunnerConfig the script runner config
     */
    public void setScriptRunnerConfig(ScriptRunnerConfig scriptRunnerConfig) {
        this.scriptRunnerConfig = scriptRunnerConfig;
    }

    /**
     * Gets the first turn reference tree.
     *
     * @return the string result
     */
    public String getFirstTurnReferenceTree() {
        return firstTurnReferenceTree;
    }

    /**
     * Sets the first turn reference tree.
     *
     * @param firstTurnReferenceTree the first turn reference tree
     */
    public void setFirstTurnReferenceTree(String firstTurnReferenceTree) {
        this.firstTurnReferenceTree = firstTurnReferenceTree;
    }

    /**
     * Gets the pipeline plan.
     *
     * @return the resolved or constructed object
     */
    public ReasoningPipelinePlan getPipelinePlan() {
        return pipelinePlan;
    }

    /**
     * Sets the pipeline plan.
     *
     * @param pipelinePlan the pipeline plan
     */
    public void setPipelinePlan(ReasoningPipelinePlan pipelinePlan) {
        this.pipelinePlan = pipelinePlan;
    }

    /**
     * Gets the pipeline phase index.
     *
     * @return the numeric value
     */
    public int getPipelinePhaseIndex() {
        return pipelinePhaseIndex;
    }

    /**
     * Sets the pipeline phase index.
     *
     * @param pipelinePhaseIndex the pipeline phase index
     */
    public void setPipelinePhaseIndex(int pipelinePhaseIndex) {
        this.pipelinePhaseIndex = pipelinePhaseIndex;
    }

    /**
     * Gets the current pipeline phase.
     *
     * @return the string result
     */
    public String getCurrentPipelinePhase() {
        return currentPipelinePhase;
    }

    /**
     * Sets the current pipeline phase.
     *
     * @param currentPipelinePhase the current pipeline phase
     */
    public void setCurrentPipelinePhase(String currentPipelinePhase) {
        this.currentPipelinePhase = currentPipelinePhase;
    }

    /**
     * Gets the active per source phases.
     *
     * @return the string result
     */
    public List<String> getActivePerSourcePhases() {
        return activePerSourcePhases;
    }

    /**
     * Sets the active per source phases.
     *
     * @param activePerSourcePhases the active per source phases
     */
    public void setActivePerSourcePhases(List<String> activePerSourcePhases) {
        this.activePerSourcePhases = activePerSourcePhases;
    }

    /**
     * Gets the system context.
     *
     * @return the string result
     */
    public String getSystemContext() {
        return systemContext;
    }

    /**
     * Sets the system context.
     *
     * @param systemContext the system context
     */
    public void setSystemContext(String systemContext) {
        this.systemContext = systemContext;
    }

    /**
     * Checks if the component is cached content enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isCachedContentEnabled() {
        return cachedContentEnabled;
    }

    /**
     * Sets the cached content enabled.
     *
     * @param cachedContentEnabled the cached content enabled
     */
    public void setCachedContentEnabled(boolean cachedContentEnabled) {
        this.cachedContentEnabled = cachedContentEnabled;
    }

    /**
     * Gets the cycle cached content id.
     *
     * @return the string result
     */
    public String getCycleCachedContentId() {
        return cycleCachedContentId;
    }

    /**
     * Sets the cycle cached content id.
     *
     * @param cycleCachedContentId the cycle cached content id
     */
    public void setCycleCachedContentId(String cycleCachedContentId) {
        this.cycleCachedContentId = cycleCachedContentId;
    }

    /**
     * Gets the resolver.
     *
     * @return the resolved or constructed object
     */
    public BasePathResolver getResolver() {
        return resolver;
    }

    /**
     * Sets the resolver.
     *
     * @param resolver the resolver
     */
    public void setResolver(BasePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Gets the source attachment.
     *
     * @return the resulting payload
     */
    public AttachedFilePayload getSourceAttachment() {
        return sourceAttachment;
    }

    /**
     * Sets the source attachment.
     *
     * @param sourceAttachment the source attachment
     */
    public void setSourceAttachment(AttachedFilePayload sourceAttachment) {
        this.sourceAttachment = sourceAttachment;
    }

    public ReferenceTreeContextService.ReferenceTreeContext getFirstTurnReferenceTreeContext() {
        return firstTurnReferenceTreeContext;
    }

    /**
     * Sets the first turn reference tree context.
     *
     * @param firstTurnReferenceTreeContext the first turn reference tree context
     */
    public void setFirstTurnReferenceTreeContext(ReferenceTreeContextService.ReferenceTreeContext firstTurnReferenceTreeContext) {
        this.firstTurnReferenceTreeContext = firstTurnReferenceTreeContext;
    }

    /**
     * Gets the first turn context attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getFirstTurnContextAttachments() {
        return firstTurnContextAttachments;
    }

    /**
     * Sets the first turn context attachments.
     *
     * @param firstTurnContextAttachments the first turn context attachments
     */
    public void setFirstTurnContextAttachments(List<AttachedFilePayload> firstTurnContextAttachments) {
        this.firstTurnContextAttachments = firstTurnContextAttachments;
    }

    /**
     * Gets the result.
     *
     * @return the resulting result
     */
    public ReasoningResult getResult() {
        return result;
    }

    /**
     * Sets the result.
     *
     * @param result the result
     */
    public void setResult(ReasoningResult result) {
        this.result = result;
    }

    /**
     * Gets the prepend messages.
     *
     * @return the collection of elements
     */
    public List<ConversationMessage> getPrependMessages() {
        return prependMessages;
    }

    /**
     * Sets the prepend messages.
     *
     * @param prependMessages the prepend messages
     */
    public void setPrependMessages(List<ConversationMessage> prependMessages) {
        this.prependMessages = prependMessages;
    }

    /**
     * Gets the context plan.
     *
     * @return the collection of elements
     */
    public List<MessageTypePlan> getContextPlan() {
        return contextPlan;
    }

    /**
     * Sets the context plan.
     *
     * @param contextPlan the context plan
     */
    public void setContextPlan(List<MessageTypePlan> contextPlan) {
        this.contextPlan = contextPlan;
    }

    /**
     * Gets the prepend messages summary.
     *
     * @return the string result
     */
    public String getPrependMessagesSummary() {
        return prependMessagesSummary;
    }

    /**
     * Sets the prepend messages summary.
     *
     * @param prependMessagesSummary the prepend messages summary
     */
    public void setPrependMessagesSummary(String prependMessagesSummary) {
        this.prependMessagesSummary = prependMessagesSummary;
    }

    /**
     * Gets the prepend messages safe summary.
     *
     * @return the string result
     */
    public String getPrependMessagesSafeSummary() {
        return prependMessagesSafeSummary;
    }

    /**
     * Sets the prepend messages safe summary.
     *
     * @param prependMessagesSafeSummary the prepend messages safe summary
     */
    public void setPrependMessagesSafeSummary(String prependMessagesSafeSummary) {
        this.prependMessagesSafeSummary = prependMessagesSafeSummary;
    }

    /**
     * Checks if the component is log system context enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isLogSystemContextEnabled() {
        return logSystemContextEnabled;
    }

    /**
     * Sets the log system context enabled.
     *
     * @param logSystemContextEnabled the log system context enabled
     */
    public void setLogSystemContextEnabled(boolean logSystemContextEnabled) {
        this.logSystemContextEnabled = logSystemContextEnabled;
    }

    
    /**
     * Gets the logical turn.
     *
     * @return the numeric value
     */
    public int getLogicalTurn() {
        return logicalTurn;
    }

    /**
     * Sets the logical turn.
     *
     * @param logicalTurn the logical turn
     */
    public void setLogicalTurn(int logicalTurn) {
        this.logicalTurn = logicalTurn;
    }

    /**
     * Gets the attempt index.
     *
     * @return the numeric value
     */
    public int getAttemptIndex() {
        return attemptIndex;
    }

    /**
     * Sets the attempt index.
     *
     * @param attemptIndex the attempt index
     */
    public void setAttemptIndex(int attemptIndex) {
        this.attemptIndex = attemptIndex;
    }

    /**
     * Gets the closed turns.
     *
     * @return the numeric value
     */
    public int getClosedTurns() {
        return closedTurns;
    }

    /**
     * Sets the closed turns.
     *
     * @param closedTurns the closed turns
     */
    public void setClosedTurns(int closedTurns) {
        this.closedTurns = closedTurns;
    }

    /**
     * Checks if the component is source attachment sent.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSourceAttachmentSent() {
        return sourceAttachmentSent;
    }

    /**
     * Sets the source attachment sent.
     *
     * @param sourceAttachmentSent the source attachment sent
     */
    public void setSourceAttachmentSent(boolean sourceAttachmentSent) {
        this.sourceAttachmentSent = sourceAttachmentSent;
    }

    /**
     * Gets the turn message content.
     *
     * @return the string result
     */
    public String getTurnMessageContent() {
        return turnMessageContent;
    }

    /**
     * Sets the turn message content.
     *
     * @param turnMessageContent the turn message content
     */
    public void setTurnMessageContent(String turnMessageContent) {
        this.turnMessageContent = turnMessageContent;
    }

    /**
     * Gets the turn outbound attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getTurnOutboundAttachments() {
        return turnOutboundAttachments;
    }

    /**
     * Sets the turn outbound attachments.
     *
     * @param turnOutboundAttachments the turn outbound attachments
     */
    public void setTurnOutboundAttachments(List<AttachedFilePayload> turnOutboundAttachments) {
        this.turnOutboundAttachments = turnOutboundAttachments;
    }

    /**
     * Gets the turn response.
     *
     * @return the resolved or constructed object
     */
    public MarkdownInferenceResponse getTurnResponse() {
        return turnResponse;
    }

    /**
     * Sets the turn response.
     *
     * @param turnResponse the turn response
     */
    public void setTurnResponse(MarkdownInferenceResponse turnResponse) {
        this.turnResponse = turnResponse;
    }

    public ResponseDirectiveParser.ParseResult getTurnParseResult() {
        return turnParseResult;
    }

    /**
     * Sets the turn parse result.
     *
     * @param turnParseResult the turn parse result
     */
    public void setTurnParseResult(ResponseDirectiveParser.ParseResult turnParseResult) {
        this.turnParseResult = turnParseResult;
    }

    /**
     * Gets the turn directive.
     *
     * @return the resolved or constructed object
     */
    public ResponseDirective getTurnDirective() {
        return turnDirective;
    }

    /**
     * Sets the turn directive.
     *
     * @param turnDirective the turn directive
     */
    public void setTurnDirective(ResponseDirective turnDirective) {
        this.turnDirective = turnDirective;
    }

    /**
     * Gets the turn inbound.
     *
     * @return the resolved or constructed object
     */
    public ReasoningTurn getTurnInbound() {
        return turnInbound;
    }

    /**
     * Sets the turn inbound.
     *
     * @param turnInbound the turn inbound
     */
    public void setTurnInbound(ReasoningTurn turnInbound) {
        this.turnInbound = turnInbound;
    }

    /**
     * Gets the turn outbound.
     *
     * @return the resolved or constructed object
     */
    public ReasoningTurn getTurnOutbound() {
        return turnOutbound;
    }

    /**
     * Sets the turn outbound.
     *
     * @param turnOutbound the turn outbound
     */
    public void setTurnOutbound(ReasoningTurn turnOutbound) {
        this.turnOutbound = turnOutbound;
    }

    /**
     * Gets the turn operation requests.
     *
     * @return the collection of elements
     */
    public List<ToolExecutionRequest> getTurnOperationRequests() {
        return turnOperationRequests;
    }

    /**
     * Sets the turn operation requests.
     *
     * @param turnOperationRequests the turn operation requests
     */
    public void setTurnOperationRequests(List<ToolExecutionRequest> turnOperationRequests) {
        this.turnOperationRequests = turnOperationRequests;
    }
}
