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

import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionType;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.ToolOperationsReference;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.ReasoningRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * ReasoningScriptContextFactory is part of the dynamic script evaluation using
 * Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing inference script context factory.
 */
public class ReasoningScriptContextFactory {

    /**
     * Builds the configured target base.
     *
     * @param request             the request containing path and scope metadata
     * @param config              the Reins configuration settings
     * @param cycle               the cycle
     * @param policy              the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree       the reference tree
     * @param inspectedPaths      the inspected paths
     * @param compiledPaths       the compiled paths
     * @param attachments         the list of attachments
     * @param isProjectCycle      the is project cycle
     * @return the resulting context
     */
    public ReasoningScriptContext buildBase(
            ReasoningRequest request,
            ReinsConfig config,
            ReasoningCycle cycle,
            FilePolicy policy,
            boolean scriptRunnerEnabled,
            String referenceTree,
            List<String> inspectedPaths,
            List<String> compiledPaths,
            List<ReasoningScriptViews.AttachmentView> attachments,
            boolean isProjectCycle) {

        ReasoningScriptViews.SourceView sourceView = buildSourceView(request);
        ReasoningScriptViews.FileBasesView fileBasesView = buildFileBasesView(request, config);
        ReasoningScriptViews.InferenceStateView inferenceStateView = buildInferenceStateView(
                request, null, Collections.emptyList(), inspectedPaths, compiledPaths, Collections.emptyList());
        ReasoningScriptViews.ConfigView configView = buildConfigView(config, cycle);
        ReasoningScriptViews.CycleView cycleView = buildCycleView(cycle);
        ReasoningScriptViews.PolicyView policyView = buildPolicyView(policy, config, scriptRunnerEnabled);
        ReasoningScriptViews.TrackingView trackingView = buildTrackingView(request);

        return ReasoningScriptContext.builder()
                .source(sourceView)
                .fileBases(fileBasesView)
                .inference(inferenceStateView)
                .config(configView)
                .cycle(cycleView)
                .policy(policyView)
                .tracking(trackingView)
                .pipeline(new ReasoningScriptViews.PipelineView(
                        null,
                        -1,
                        0,
                        List.of(),
                        request != null && request.getProcessingStatus() != null
                                ? request.getProcessingStatus().name()
                                : null,
                        null,
                        null,
                        null,
                        null,
                        false))
                .referenceTree(referenceTree != null ? referenceTree : "")
                .attachments(attachments != null ? attachments : Collections.emptyList())
                .build();
    }

    /**
     * With Tool Result.
     *
     * @param base       the base
     * @param toolResult the tool result
     * @return the resulting context
     */
    public ReasoningScriptContext withToolResult(
            ReasoningScriptContext base,
            ToolExecutionResult toolResult) {
        ReasoningScriptViews.ToolResultView resultView = buildToolResultView(toolResult);
        return ReasoningScriptContext.builder()
                .source(base.getSource())
                .fileBases(base.getFileBases())
                .tracking(base.getTracking())
                .referenceTree(base.getReferenceTree())
                .inference(base.getInference())
                .attachments(base.getAttachments())
                .config(base.getConfig())
                .cycle(base.getCycle())
                .policy(base.getPolicy())
                .pipeline(base.getPipeline())
                .currentToolResult(resultView)
                .build();
    }

    /**
     * With Pipeline State.
     *
     * @param base                       the base
     * @param conversationHistory        the conversation history
     * @param messageContext             the message context
     * @param currentPhase               the current phase
     * @param currentPhaseOrdinal        the current phase ordinal
     * @param phaseNames                 the phase names
     * @param lastDirectiveIntent        the last directive intent
     * @param lastDirectiveContentType   the last directive content type
     * @param lastDirectiveBody          the last directive body
     * @param lastFailureClass           the last failure class
     * @param currentToolResultAvailable the current tool result available
     * @return the resulting context
     */
    public ReasoningScriptContext withPipelineState(ReasoningScriptContext base,
            List<ConversationMessage> conversationHistory,
            String messageContext,
            String currentPhase,
            int currentPhaseOrdinal,
            List<String> phaseNames,
            String lastDirectiveIntent,
            String lastDirectiveContentType,
            String lastDirectiveBody,
            String lastFailureClass,
            boolean currentToolResultAvailable) {
        List<ReasoningScriptViews.TurnView> turns = toTurnViews(conversationHistory);
        ReasoningScriptViews.InferenceStateView inference = new ReasoningScriptViews.InferenceStateView(
                messageContext == null
                        ? (base.getInference() == null ? "" : base.getInference().getContext())
                        : messageContext,
                turns,
                base.getInference() == null ? List.of() : base.getInference().getInspectedFiles(),
                base.getInference() == null ? List.of() : base.getInference().getCompiledFiles(),
                base.getInference() == null ? List.of() : base.getInference().getToolOperations());
        ReasoningScriptViews.PipelineView pipeline = new ReasoningScriptViews.PipelineView(
                currentPhase,
                currentPhaseOrdinal,
                phaseNames == null ? 0 : phaseNames.size(),
                phaseNames == null ? List.of() : phaseNames,
                base.getPipeline() == null ? null : base.getPipeline().getProcessingStatus(),
                lastDirectiveIntent,
                lastDirectiveContentType,
                lastDirectiveBody,
                lastFailureClass,
                currentToolResultAvailable);
        return ReasoningScriptContext.builder()
                .source(base.getSource())
                .fileBases(base.getFileBases())
                .tracking(base.getTracking())
                .referenceTree(base.getReferenceTree())
                .inference(inference)
                .attachments(base.getAttachments())
                .config(base.getConfig())
                .cycle(base.getCycle())
                .policy(base.getPolicy())
                .pipeline(pipeline)
                .currentToolResult(base.getCurrentToolResult())
                .build();
    }

    private List<ReasoningScriptViews.TurnView> toTurnViews(List<ConversationMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return List.of();
        }
        List<ReasoningScriptViews.TurnView> turns = new ArrayList<>(conversationHistory.size());
        int index = 1;
        for (ConversationMessage message : conversationHistory) {
            List<String> attachments = message.getAttachments() == null
                    ? List.of()
                    : message.getAttachments().stream()
                            .map(att -> att.getQualifiedPath() == null ? att.getRelativePath() : att.getQualifiedPath())
                            .filter(path -> path != null && !path.isBlank())
                            .collect(Collectors.toList());
            turns.add(new ReasoningScriptViews.TurnView(
                    index++,
                    message.getRole() == null ? ConversationMessage.Role.USER.name() : message.getRole().name(),
                    message.getText(),
                    attachments));
        }
        return turns;
    }

    private ReasoningScriptViews.SourceView buildSourceView(ReasoningRequest request) {
        if (request == null) {
            return new ReasoningScriptViews.SourceView("", "", "", "", "", "source", Collections.emptyList());
        }
        String qualifiedPath = request.getMainSourceQualifiedPath();
        String scope = request.getSourceScope() != null ? request.getSourceScope() : "source";
        String rawPath = request.getSourcePath() != null ? request.getSourcePath() : "";
        String path = rawPath;

        if (qualifiedPath != null && qualifiedPath.contains(":")) {
            int colon = qualifiedPath.indexOf(':');
            path = qualifiedPath.substring(colon + 1);
        } else if (request.getProjectRoot() != null && request.getBaseMappings() != null) {
            java.nio.file.Path baseRoot = request.getBaseMappings().getSourceRoot(scope);
            if (baseRoot != null && !rawPath.isBlank()) {
                java.nio.file.Path abs = request.getProjectRoot().resolve(rawPath).toAbsolutePath().normalize();
                if (abs.startsWith(baseRoot)) {
                    path = br.com.dizeno.reins.util.PathNormalizer.toForwardSlashes(baseRoot.relativize(abs).toString());
                }
            }
        }

        if (qualifiedPath == null || qualifiedPath.isBlank()) {
            qualifiedPath = scope + ":" + path;
        }

        String absolutePath = "";
        if (request.getProjectRoot() != null && !rawPath.isEmpty()) {
            absolutePath = request.getProjectRoot().resolve(rawPath).toAbsolutePath().normalize().toString();
        }
        String hash = request.getSourceHash() != null ? request.getSourceHash() : "";
        return new ReasoningScriptViews.SourceView(path, absolutePath, qualifiedPath, "", hash, scope, Collections.emptyList());
    }

    private ReasoningScriptViews.FileBasesView buildFileBasesView(
            ReasoningRequest request, ReinsConfig config) {
        String main = "", test = "", target = "", projectRoot = "";
        if (config != null) {
            if (config.getSourceBase("main") != null)
                main = config.getSourceBase("main").getAbsolutePath();
            if (config.getSourceBase("test") != null)
                test = config.getSourceBase("test").getAbsolutePath();
        }
        if (request != null && request.getProjectRoot() != null) {
            projectRoot = request.getProjectRoot().toAbsolutePath().normalize().toString();
            if (config != null && config.getTarget() != null) {
                try {
                    target = config.getTarget()
                            .resolveTargetOutput("main", request.getProjectRoot()).toString();
                } catch (Exception ignored) {

                }
            }
        }
        return new ReasoningScriptViews.FileBasesView(main, test, target, projectRoot);
    }

    private ReasoningScriptViews.InferenceStateView buildInferenceStateView(
            ReasoningRequest request,
            String context,
            List<ReasoningScriptViews.TurnView> conversationHistory,
            List<String> inspectedFiles,
            List<String> compiledFiles,
            List<ReasoningScriptViews.ToolOpView> toolOperations) {
        List<ReasoningScriptViews.CompiledSourceGroupView> compiledSourceGroups = buildCompiledSourceGroupViews(request);
        return new ReasoningScriptViews.InferenceStateView(
                context != null ? context : "",
                conversationHistory != null ? new ArrayList<>(conversationHistory) : Collections.emptyList(),
                inspectedFiles != null ? new ArrayList<>(inspectedFiles) : Collections.emptyList(),
                compiledFiles != null ? new ArrayList<>(compiledFiles) : Collections.emptyList(),
                compiledSourceGroups,
                toolOperations != null ? new ArrayList<>(toolOperations) : Collections.emptyList());
    }

    private List<ReasoningScriptViews.CompiledSourceGroupView> buildCompiledSourceGroupViews(ReasoningRequest request) {
        if (request == null || request.getEagerlyProvide() == null || request.getEagerlyProvide().getCompiledSourceGroups() == null) {
            return Collections.emptyList();
        }
        List<br.com.dizeno.reins.reasoning.CompiledSourceGroup> groups = request.getEagerlyProvide().getCompiledSourceGroups();
        List<ReasoningScriptViews.CompiledSourceGroupView> views = new ArrayList<>(groups.size());
        for (br.com.dizeno.reins.reasoning.CompiledSourceGroup g : groups) {
            List<String> paths = g.getCompiledAttachments().stream()
                    .map(AttachedFilePayload::getQualifiedPath)
                    .filter(p -> p != null && !p.isBlank())
                    .collect(Collectors.toList());
            views.add(new ReasoningScriptViews.CompiledSourceGroupView(
                    g.getSourceCanonicalPath(),
                    g.getSourceSimpleName(),
                    paths));
        }
        return views;
    }

    private ReasoningScriptViews.ConfigView buildConfigView(
            ReinsConfig config, ReasoningCycle cycle) {
        return new ReasoningScriptViews.ConfigView(config);
    }

    private ReasoningScriptViews.CycleView buildCycleView(ReasoningCycle cycle) {
        if (cycle == null) {
            return new ReasoningScriptViews.CycleView("", 0, 0, "IN_PROGRESS", null);
        }
        String status = switch (cycle.getStatus()) {
            case ACTIVE -> "IN_PROGRESS";
            case FINISHED_SUCCESS -> "FINISHED_SUCCESS";
            case FINISHED_ERROR -> "FINISHED_ERROR";
        };
        String completedAt = cycle.getCompletedAt() != null ? cycle.getCompletedAt().toString() : null;
        return new ReasoningScriptViews.CycleView(
                cycle.getCycleId(), cycle.getTurns() != null ? cycle.getTurns().size() : 0,
                cycle.getMaxTurns(), status, completedAt);
    }

    private ReasoningScriptViews.PolicyView buildPolicyView(
            FilePolicy policy,
            ReinsConfig config,
            boolean scriptRunnerEnabled) {
        if (policy == null) {
            policy = FilePolicy.allPermissive();
        }
        List<String> listBases = basesAsList(policy, ToolExecutionType.LIST_FILES);
        List<String> readBases = basesAsList(policy, ToolExecutionType.READ_FILE);
        List<String> writeBases = basesAsList(policy, ToolExecutionType.WRITE_FILE);
        List<String> patchBases = basesAsList(policy, ToolExecutionType.PATCH_FILE);
        List<String> deleteBases = basesAsList(policy, ToolExecutionType.DELETE_FILE);
        List<String> appendBases = basesAsList(policy, ToolExecutionType.APPEND_FILE);
        List<String> prependBases = basesAsList(policy, ToolExecutionType.PREPEND_FILE);
        List<String> moveBases = basesAsList(policy, ToolExecutionType.MOVE_FILE);
        List<String> copyBases = basesAsList(policy, ToolExecutionType.COPY_FILE);
        List<String> listGenBases = basesAsList(policy, ToolExecutionType.LIST_COMPILED_FILES);

        boolean fileListingAndReadingEnabled = !listBases.isEmpty()
                || !readBases.isEmpty()
                || !listGenBases.isEmpty();
        boolean fileMutatingEnabled = !writeBases.isEmpty()
                || !patchBases.isEmpty()
                || !deleteBases.isEmpty()
                || !appendBases.isEmpty()
                || !prependBases.isEmpty()
                || !moveBases.isEmpty()
                || !copyBases.isEmpty();
        boolean scriptRunEnabled = scriptRunnerEnabled;

        boolean addReasoningNotesEnabled = config != null && config.getTooling() != null
                && config.getTooling().isAddReasoningNotes();

        String toolOpsReference = ToolOperationsReference.build(
                policy,
                scriptRunnerEnabled,
                fileListingAndReadingEnabled,
                fileMutatingEnabled,
                scriptRunEnabled,
                addReasoningNotesEnabled);

        return new ReasoningScriptViews.PolicyView(
                listBases, readBases, writeBases, patchBases, deleteBases,
                appendBases, prependBases, moveBases, copyBases, listGenBases,
                scriptRunnerEnabled, toolOpsReference);
    }

    private List<String> basesAsList(FilePolicy policy, ToolExecutionType opType) {
        if (policy == null) {
            return List.of("main", "test", "target");
        }
        Set<FilePolicy.Base> bases = policy.getEnabledBasesForOperation(opType);
        return new TreeSet<>(bases).stream()
                .map(b -> b.name().toLowerCase())
                .collect(Collectors.toList());
    }

    private ReasoningScriptViews.ToolResultView buildToolResultView(ToolExecutionResult result) {
        if (result == null) {
            return null;
        }
        String status = result.getStatus() != null ? result.getStatus().name() : "ERROR";
        String operation = result.getOperation() != null ? result.getOperation().name() : null;
        boolean readFileSuccess = result
                .getOperation() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE
                && result.getStatus() == ToolExecutionResult.Status.SUCCESS;

        List<ReasoningScriptViews.CompiledFileStatusView> genStatuses = new ArrayList<>();
        if (result.getCompiledFileStatuses() != null) {
            for (ToolExecutionResult.CompiledFileStatus s : result.getCompiledFileStatuses()) {
                genStatuses.add(new ReasoningScriptViews.CompiledFileStatusView(
                        s.getQualifiedPath(), s.getAttachStatus(), s.getReason()));
            }
        }
        List<ReasoningScriptViews.ReadFileStatusView> readStatuses = new ArrayList<>();
        if (result.getReadFileStatuses() != null) {
            for (ToolExecutionResult.ReadFileStatus s : result.getReadFileStatuses()) {
                readStatuses.add(new ReasoningScriptViews.ReadFileStatusView(
                        s.getQualifiedPath(), s.getAttachStatus(), s.getReason()));
            }
        }

        return new ReasoningScriptViews.ToolResultView(
                status, operation, result.getQualifiedPath(),
                result.getResolvedBase(), result.getFailureReason(), result.getPolicyCode(),
                result.getResolvedPath(), result.getExitCode(), result.isStarted(), result.isTruncated(),
                result.getStdout(), result.getStderr(), result.getContent(), readFileSuccess,
                result.getListedPaths() != null ? new ArrayList<>(result.getListedPaths()) : null,
                genStatuses, readStatuses,
                result.getExcludedPaths() != null ? new ArrayList<>(result.getExcludedPaths()) : null,
                result.getExclusionReason());
    }

    private ReasoningScriptViews.TrackingView buildTrackingView(ReasoningRequest request) {
        if (request == null || request.getPriorRecord() == null) {
            return null;
        }
        SourceTrackingRecord record = request.getPriorRecord();
        List<String> compiledPaths = record.getCompiledFiles() != null
                ? new ArrayList<>(record.getCompiledFiles().keySet())
                : Collections.emptyList();
        List<String> noteTexts = record.getNotes() != null
                ? record.getNotes().stream()
                        .filter(n -> n.getText() != null)
                        .map(ReasoningNote::getText)
                        .collect(Collectors.toList())
                : Collections.emptyList();
        return new ReasoningScriptViews.TrackingView(
                record.getSourceHash(),
                compiledPaths,
                record.getLastStatus(),
                record.getLastCompiledAt(),
                noteTexts);
    }
}
