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

import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.*;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

 
/**
 * ReasoningScriptViews is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing inference script views.
 */
public final class ReasoningScriptViews {

    private ReasoningScriptViews() {
    }

    /**
     * SourceView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing source view.
     */
    public static final class SourceView {
        private final String path;
        private final String absolutePath;
        private final String qualifiedPath;
        private final String content;
        private final String hash;
        private final String scope;
        private final List<CodegenBlockView> codegenBlocks;

        /**
         * Constructs a new instance of {@link SourceView}.
         *
         * @param path the file or directory path
         * @param absolutePath the absolute path
         * @param content the content
         * @param hash the hash
         * @param scope the scope
         * @param codegenBlocks the codegen blocks
         */
        public SourceView(String path, String absolutePath, String content,
                          String hash, String scope, List<CodegenBlockView> codegenBlocks) {
            this(path, absolutePath, scope + ":" + path, content, hash, scope, codegenBlocks);
        }

        /**
         * Constructs a new instance of {@link SourceView} with explicit qualifiedPath.
         *
         * @param path the file or directory path
         * @param absolutePath the absolute path
         * @param qualifiedPath the base-qualified path
         * @param content the content
         * @param hash the hash
         * @param scope the scope
         * @param codegenBlocks the codegen blocks
         */
        public SourceView(String path, String absolutePath, String qualifiedPath, String content,
                          String hash, String scope, List<CodegenBlockView> codegenBlocks) {
            this.path = path;
            this.absolutePath = absolutePath;
            this.qualifiedPath = qualifiedPath != null ? qualifiedPath : (scope + ":" + path);
            this.content = content;
            this.hash = hash;
            this.scope = scope;
            this.codegenBlocks = codegenBlocks != null
                    ? Collections.unmodifiableList(codegenBlocks)
                    : Collections.emptyList();
        }

        /**
         * Gets the path.
         *
         * @return the string result
         */
        public String getPath() { return path; }
        /**
         * Gets the absolute path.
         *
         * @return the string result
         */
        public String getAbsolutePath() { return absolutePath; }
        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() { return qualifiedPath; }
        /**
         * Gets the content.
         *
         * @return the string result
         */
        public String getContent() { return content; }
        /**
         * Gets the hash.
         *
         * @return the string result
         */
        public String getHash() { return hash; }
        /**
         * Gets the scope.
         *
         * @return the string result
         */
        public String getScope() { return scope; }
        /**
         * Gets the codegen blocks.
         *
         * @return the collection of elements
         */
        public List<CodegenBlockView> getCodegenBlocks() { return codegenBlocks; }
    }

    /**
     * CodegenBlockView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing codegen block view.
     */
    public static final class CodegenBlockView {
        private final int index;
        private final String language;
        private final List<String> instructions;
        private final String outputPath;
        private final String fingerprint;

        /**
         * Constructs a new instance of {@link CodegenBlockView}.
         *
         * @param index the index
         * @param language the language
         * @param instructions the instructions
         * @param outputPath the output path
         * @param fingerprint the fingerprint
         */
        public CodegenBlockView(int index, String language, List<String> instructions,
                                String outputPath, String fingerprint) {
            this.index = index;
            this.language = language;
            this.instructions = instructions != null
                    ? Collections.unmodifiableList(instructions)
                    : Collections.emptyList();
            this.outputPath = outputPath;
            this.fingerprint = fingerprint;
        }

        /**
         * Gets the index.
         *
         * @return the numeric value
         */
        public int getIndex() { return index; }
        /**
         * Gets the language.
         *
         * @return the string result
         */
        public String getLanguage() { return language; }
        /**
         * Gets the instructions.
         *
         * @return the string result
         */
        public List<String> getInstructions() { return instructions; }
        /**
         * Gets the output path.
         *
         * @return the string result
         */
        public String getOutputPath() { return outputPath; }
        /**
         * Gets the fingerprint.
         *
         * @return the string result
         */
        public String getFingerprint() { return fingerprint; }
    }

    /**
     * FileBasesView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing file bases view.
     */
    public static final class FileBasesView {
        private final String main;
        private final String test;
        private final String target;
        private final String projectRoot;

        /**
         * Constructs a new instance of {@link FileBasesView}.
         *
         * @param main the main
         * @param test the test
         * @param target the target
         * @param projectRoot the root path of the project
         */
        public FileBasesView(String main, String test, String target, String projectRoot) {
            this.main = main;
            this.test = test;
            this.target = target;
            this.projectRoot = projectRoot;
        }

        /**
         * Gets the main.
         *
         * @return the string result
         */
        public String getMain() { return main; }
        /**
         * Gets the test.
         *
         * @return the string result
         */
        public String getTest() { return test; }
        /**
         * Gets the target.
         *
         * @return the string result
         */
        public String getTarget() { return target; }
        /**
         * Gets the project root.
         *
         * @return the string result
         */
        public String getProjectRoot() { return projectRoot; }
    }

    /**
     * TrackingView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing tracking view.
     */
    public static final class TrackingView {
        private final String sourceHash;
        private final List<String> compiledPaths;
        private final List<String> compiledRelativePaths;
        private final String lastStatus;
        private final String lastCompiledAt;
        private final List<String> notes;

        /**
         * Constructs a new instance of {@link TrackingView}.
         *
         * @param sourceHash the source hash
         * @param compiledPaths the compiled paths
         * @param lastStatus the last status
         * @param lastCompiledAt the last compiled at
         * @param notes the notes
         */
        public TrackingView(String sourceHash, List<String> compiledPaths,
                            String lastStatus, String lastCompiledAt,
                            List<String> notes) {
            this.sourceHash = sourceHash;
            this.compiledPaths = compiledPaths != null
                    ? Collections.unmodifiableList(compiledPaths)
                    : Collections.emptyList();
            this.compiledRelativePaths = this.compiledPaths.stream()
                    .map(TrackingView::stripKnownBasePrefix)
                    .collect(Collectors.toUnmodifiableList());
            this.lastStatus = lastStatus;
            this.lastCompiledAt = lastCompiledAt;
            this.notes = notes != null
                    ? Collections.unmodifiableList(notes)
                    : Collections.emptyList();
        }

        /**
         * Gets the source hash.
         *
         * @return the string result
         */
        public String getSourceHash() { return sourceHash; }
        /**
         * Gets the compiled paths.
         *
         * @return the string result
         */
        public List<String> getCompiledPaths() { return compiledPaths; }
        /**
         * Gets the compiled relative paths.
         *
         * @return the string result
         */
        public List<String> getCompiledRelativePaths() { return compiledRelativePaths; }
        /**
         * Gets the last status.
         *
         * @return the string result
         */
        public String getLastStatus() { return lastStatus; }
        /**
         * Gets the last compiled at.
         *
         * @return the string result
         */
        public String getLastCompiledAt() { return lastCompiledAt; }
        /**
         * Gets the notes.
         *
         * @return the string result
         */
        public List<String> getNotes() { return notes; }

        /**
         * Formats notes list as markdown bullet points or returns "- none".
         *
         * @return formatted string
         */
        public String getNotesOrNone() {
            if (notes == null || notes.isEmpty()) {
                return "- none";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < notes.size(); i++) {
                sb.append("- ").append(notes.get(i));
                if (i < notes.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        /**
         * Formats compiled paths list as markdown bullet points or returns "- none".
         *
         * @return formatted string
         */
        public String getCompiledPathsOrNone() {
            if (compiledPaths == null || compiledPaths.isEmpty()) {
                return "- none";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < compiledPaths.size(); i++) {
                sb.append("- ").append(compiledPaths.get(i));
                if (i < compiledPaths.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        private static String stripKnownBasePrefix(String path) {
            if (path == null || path.isBlank()) {
                return path;
            }
            int colon = path.indexOf(':');
            if (colon <= 0) {
                return path;
            }

            String prefix = path.substring(0, colon).toLowerCase(Locale.ROOT);
            if (isKnownBasePrefix(prefix)) {
                return path.substring(colon + 1);
            }
            return path;
        }

        private static boolean isKnownBasePrefix(String prefix) {
            return "main".equals(prefix)
                    || "test".equals(prefix)
                    || "target".equals(prefix)
                    || "script".equals(prefix)
                    || "main-source".equals(prefix)
                    || "test-source".equals(prefix)
                    || "main-target".equals(prefix)
                    || "test-target".equals(prefix);
        }
    }

    public static final class CompiledSourceGroupView {
        private final String sourceCanonicalPath;
        private final String sourceSimpleName;
        private final List<String> compiledPaths;

        public CompiledSourceGroupView(String sourceCanonicalPath, String sourceSimpleName, List<String> compiledPaths) {
            this.sourceCanonicalPath = sourceCanonicalPath;
            this.sourceSimpleName = sourceSimpleName;
            this.compiledPaths = compiledPaths != null ? Collections.unmodifiableList(compiledPaths) : Collections.emptyList();
        }

        public String getSourceCanonicalPath() { return sourceCanonicalPath; }
        public String getSourceSimpleName() { return sourceSimpleName; }
        public List<String> getCompiledPaths() { return compiledPaths; }

        public String getCompiledPathsOrNone() {
            if (compiledPaths == null || compiledPaths.isEmpty()) {
                return "- none";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < compiledPaths.size(); i++) {
                sb.append("- ").append(compiledPaths.get(i));
                if (i < compiledPaths.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    /**
     * InferenceStateView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing inference state view.
     */
    public static final class InferenceStateView {
        private final String context;
        private final List<TurnView> conversationHistory;
        private final List<String> inspectedFiles;
        private final List<String> compiledFiles;
        private final List<CompiledSourceGroupView> compiledSourceGroups;
        private final List<ToolOpView> toolOperations;

        /**
         * Constructs a new instance of {@link InferenceStateView}.
         *
         * @param context the context
         * @param conversationHistory the conversation history
         * @param inspectedFiles the inspected files
         * @param compiledFiles the compiled files
         * @param toolOperations the tool operations
         */
        public InferenceStateView(String context,
                                  List<TurnView> conversationHistory,
                                  List<String> inspectedFiles,
                                  List<String> compiledFiles,
                                  List<ToolOpView> toolOperations) {
            this(context, conversationHistory, inspectedFiles, compiledFiles, Collections.emptyList(), toolOperations);
        }

        /**
         * Constructs a new instance of {@link InferenceStateView} with compiledSourceGroups.
         *
         * @param context the context
         * @param conversationHistory the conversation history
         * @param inspectedFiles the inspected files
         * @param compiledFiles the compiled files
         * @param compiledSourceGroups the compiled source groups
         * @param toolOperations the tool operations
         */
        public InferenceStateView(String context,
                                  List<TurnView> conversationHistory,
                                  List<String> inspectedFiles,
                                  List<String> compiledFiles,
                                  List<CompiledSourceGroupView> compiledSourceGroups,
                                  List<ToolOpView> toolOperations) {
            this.context = context;
            this.conversationHistory = conversationHistory != null
                    ? Collections.unmodifiableList(conversationHistory)
                    : Collections.emptyList();
            this.inspectedFiles = inspectedFiles != null
                    ? Collections.unmodifiableList(inspectedFiles)
                    : Collections.emptyList();
            this.compiledFiles = compiledFiles != null
                    ? Collections.unmodifiableList(compiledFiles)
                    : Collections.emptyList();
            this.compiledSourceGroups = compiledSourceGroups != null
                    ? Collections.unmodifiableList(compiledSourceGroups)
                    : Collections.emptyList();
            this.toolOperations = toolOperations != null
                    ? Collections.unmodifiableList(toolOperations)
                    : Collections.emptyList();
        }

        /**
         * Gets the context.
         *
         * @return the string result
         */
        public String getContext() { return context; }
        /**
         * Gets the conversation history.
         *
         * @return the collection of elements
         */
        public List<TurnView> getConversationHistory() { return conversationHistory; }
        /**
         * Gets the inspected files.
         *
         * @return the string result
         */
        public List<String> getInspectedFiles() { return inspectedFiles; }
        /**
         * Gets the compiled files.
         *
         * @return the string result
         */
        public List<String> getCompiledFiles() { return compiledFiles; }
        /**
         * Gets the compiled source groups.
         *
         * @return the collection of elements
         */
        public List<CompiledSourceGroupView> getCompiledSourceGroups() { return compiledSourceGroups; }
        /**
         * Gets the tool operations.
         *
         * @return the collection of elements
         */
        public List<ToolOpView> getToolOperations() { return toolOperations; }

        /**
         * Formats inspected files list as markdown bullet points or returns "- none".
         *
         * @return formatted string
         */
        public String getInspectedFilesOrNone() {
            if (inspectedFiles == null || inspectedFiles.isEmpty()) {
                return "- none";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inspectedFiles.size(); i++) {
                sb.append("- ").append(inspectedFiles.get(i));
                if (i < inspectedFiles.size() - 1) {
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        /**
         * Checks if conversation history contains turns.
         *
         * @return true if history is non-empty
         */
        public boolean isHasHistory() {
            return conversationHistory != null && !conversationHistory.isEmpty();
        }

        /**
         * Checks if this is the first turn in the conversation.
         *
         * @return true if no conversation history exists
         */
        public boolean isFirstTurn() {
            return !isHasHistory();
        }
    }

    /**
     * TurnView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing turn view.
     */
    public static final class TurnView {
        private final int sequence;
        private final String role;
        private final String content;
        private final List<String> attachedFilePaths;

        /**
         * Constructs a new instance of {@link TurnView}.
         *
         * @param sequence the sequence
         * @param role the role
         * @param content the content
         * @param attachedFilePaths the attached file paths
         */
        public TurnView(int sequence, String role, String content, List<String> attachedFilePaths) {
            this.sequence = sequence;
            this.role = role;
            this.content = content;
            this.attachedFilePaths = attachedFilePaths != null
                    ? Collections.unmodifiableList(attachedFilePaths)
                    : Collections.emptyList();
        }

        /**
         * Gets the sequence.
         *
         * @return the numeric value
         */
        public int getSequence() { return sequence; }
        /**
         * Gets the role.
         *
         * @return the string result
         */
        public String getRole() { return role; }
        /**
         * Gets the content.
         *
         * @return the string result
         */
        public String getContent() { return content; }
        /**
         * Gets the attached file paths.
         *
         * @return the string result
         */
        public List<String> getAttachedFilePaths() { return attachedFilePaths; }
    }

    /**
     * ToolOpView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing tool op view.
     */
    public static final class ToolOpView {
        private final String operation;
        private final String base;
        private final String path;
        private final String status;
        private final String failureReason;
        private final boolean permittedToLlm;
        private final long durationMs;

        /**
         * Constructs a new instance of {@link ToolOpView}.
         *
         * @param operation the operation
         * @param base the base
         * @param path the file or directory path
         * @param status the status
         * @param failureReason the failure reason
         * @param permittedToLlm the permitted to llm
         * @param durationMs the duration ms
         */
        public ToolOpView(String operation, String base, String path, String status,
                         String failureReason, boolean permittedToLlm, long durationMs) {
            this.operation = operation;
            this.base = base;
            this.path = path;
            this.status = status;
            this.failureReason = failureReason;
            this.permittedToLlm = permittedToLlm;
            this.durationMs = durationMs;
        }

        /**
         * Gets the operation.
         *
         * @return the string result
         */
        public String getOperation() { return operation; }
        /**
         * Gets the base.
         *
         * @return the string result
         */
        public String getBase() { return base; }
        /**
         * Gets the path.
         *
         * @return the string result
         */
        public String getPath() { return path; }
        /**
         * Gets the status.
         *
         * @return the string result
         */
        public String getStatus() { return status; }
        /**
         * Gets the failure reason.
         *
         * @return the string result
         */
        public String getFailureReason() { return failureReason; }
        /**
         * Checks if the component is permitted to llm.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isPermittedToLlm() { return permittedToLlm; }
        /**
         * Gets the duration ms.
         *
         * @return the numeric value
         */
        public long getDurationMs() { return durationMs; }
    }

    /**
     * AttachmentView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing attachment view.
     */
    public static final class AttachmentView {
        private final String path;
        private final String content;
        private final String mimeType;

        /**
         * Constructs a new instance of {@link AttachmentView}.
         *
         * @param path the file or directory path
         * @param content the content
         * @param mimeType the mime type
         */
        public AttachmentView(String path, String content, String mimeType) {
            this.path = path;
            this.content = content;
            this.mimeType = mimeType;
        }

        /**
         * Gets the path.
         *
         * @return the string result
         */
        public String getPath() { return path; }
        /**
         * Gets the content.
         *
         * @return the string result
         */
        public String getContent() { return content; }
        /**
         * Gets the mime type.
         *
         * @return the string result
         */
        public String getMimeType() { return mimeType; }
    }

    /**
     * CycleView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing cycle view.
     */
    public static final class CycleView {
        private final String cycleId;
        private final int turnCount;
        private final int maxTurns;
        private final String status;
        private final String completedAt;

        /**
         * Constructs a new instance of {@link CycleView}.
         *
         * @param cycleId the cycle id
         * @param turnCount the turn count
         * @param maxTurns the maximum turn limit for the reasoning cycle
         * @param status the status
         * @param completedAt the completed at
         */
        public CycleView(String cycleId, int turnCount, int maxTurns, String status, String completedAt) {
            this.cycleId = cycleId;
            this.turnCount = turnCount;
            this.maxTurns = maxTurns;
            this.status = status;
            this.completedAt = completedAt;
        }

        /**
         * Gets the cycle id.
         *
         * @return the string result
         */
        public String getCycleId() { return cycleId; }
        /**
         * Gets the turn count.
         *
         * @return the numeric value
         */
        public int getTurnCount() { return turnCount; }
        /**
         * Gets the max turns.
         *
         * @return the numeric value
         */
        public int getMaxTurns() { return maxTurns; }
        /**
         * Gets the status.
         *
         * @return the string result
         */
        public String getStatus() { return status; }
        /**
         * Gets the completed at.
         *
         * @return the string result
         */
        public String getCompletedAt() { return completedAt; }
    }

    /**
     * PipelineView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing pipeline view.
     */
    public static final class PipelineView {
        private final String currentPhase;
        private final int currentPhaseOrdinal;
        private final int totalPhases;
        private final List<String> phaseNames;
        private final String processingStatus;
        private final String lastDirectiveIntent;
        private final String lastDirectiveContentType;
        private final String lastDirectiveBody;
        private final String lastFailureClass;
        private final boolean currentToolResultAvailable;

        /**
         * Constructs a new instance of {@link PipelineView}.
         *
         * @param currentPhase the current phase
         * @param currentPhaseOrdinal the current phase ordinal
         * @param totalPhases the total phases
         * @param phaseNames the phase names
         * @param processingStatus the processing status
         * @param lastDirectiveIntent the last directive intent
         * @param lastDirectiveContentType the last directive content type
         * @param lastDirectiveBody the last directive body
         * @param lastFailureClass the last failure class
         * @param currentToolResultAvailable the current tool result available
         */
        public PipelineView(String currentPhase,
                            int currentPhaseOrdinal,
                            int totalPhases,
                            List<String> phaseNames,
                            String processingStatus,
                            String lastDirectiveIntent,
                            String lastDirectiveContentType,
                            String lastDirectiveBody,
                            String lastFailureClass,
                            boolean currentToolResultAvailable) {
            this.currentPhase = currentPhase;
            this.currentPhaseOrdinal = currentPhaseOrdinal;
            this.totalPhases = totalPhases;
            this.phaseNames = phaseNames == null ? Collections.emptyList() : Collections.unmodifiableList(phaseNames);
            this.processingStatus = processingStatus;
            this.lastDirectiveIntent = lastDirectiveIntent;
            this.lastDirectiveContentType = lastDirectiveContentType;
            this.lastDirectiveBody = lastDirectiveBody;
            this.lastFailureClass = lastFailureClass;
            this.currentToolResultAvailable = currentToolResultAvailable;
        }

        /**
         * Gets the current phase.
         *
         * @return the string result
         */
        public String getCurrentPhase() { return currentPhase; }
        /**
         * Gets the current phase ordinal.
         *
         * @return the numeric value
         */
        public int getCurrentPhaseOrdinal() { return currentPhaseOrdinal; }
        /**
         * Gets the total phases.
         *
         * @return the numeric value
         */
        public int getTotalPhases() { return totalPhases; }
        /**
         * Gets the phase names.
         *
         * @return the string result
         */
        public List<String> getPhaseNames() { return phaseNames; }
        /**
         * Gets the processing status.
         *
         * @return the string result
         */
        public String getProcessingStatus() { return processingStatus; }
        /**
         * Gets the last directive intent.
         *
         * @return the string result
         */
        public String getLastDirectiveIntent() { return lastDirectiveIntent; }
        /**
         * Gets the last directive content type.
         *
         * @return the string result
         */
        public String getLastDirectiveContentType() { return lastDirectiveContentType; }
        /**
         * Gets the last directive body.
         *
         * @return the string result
         */
        public String getLastDirectiveBody() { return lastDirectiveBody; }
        /**
         * Gets the last failure class.
         *
         * @return the string result
         */
        public String getLastFailureClass() { return lastFailureClass; }
        /**
         * Checks if the component is current tool result available.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isCurrentToolResultAvailable() { return currentToolResultAvailable; }

        /**
         * Checks if this is the first message in the pipeline phase before any directives or tool results.
         *
         * @return true if first message
         */
        public boolean isFirstMessage() {
            return (lastDirectiveIntent == null || lastDirectiveIntent.isBlank())
                    && !currentToolResultAvailable;
        }

        /**
         * Checks if current phase is the first phase (ordinal 0).
         *
         * @return true if first phase
         */
        public boolean isFirstPhase() {
            return currentPhaseOrdinal == 0;
        }
    }

    /**
     * ConfigView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing config view.
     */
    public static final class ConfigView {
        private final ReinsConfig raw;

        /**
         * Constructs a new instance of {@link ConfigView} wrapping {@link ReinsConfig}.
         *
         * @param config the underlying ReinsConfig instance
         */
        public ConfigView(ReinsConfig config) {
            this.raw = config != null ? config : new ReinsConfig();
        }

        /**
         * Gets the raw underlying {@link ReinsConfig} instance.
         *
         * @return the ReinsConfig instance
         */
        public ReinsConfig getRaw() { return raw; }

        /**
         * Gets the raw underlying {@link ReinsConfig} instance.
         *
         * @return the ReinsConfig instance
         */
        public ReinsConfig getReinsConfig() { return raw; }

        /**
         * Gets the model.
         *
         * @return the string result
         */
        public String getModel() {
            String resolved = raw.resolveModel();
            return resolved != null ? resolved : "unknown";
        }

        /**
         * Gets the max turns.
         *
         * @return the numeric value
         */
        public Integer getMaxTurns() {
            var activeSettings = raw.resolveActiveModelSettings();
            if (activeSettings != null && activeSettings.resolveMaximumTurns() > 0) {
                return activeSettings.resolveMaximumTurns();
            }
            return raw.getReasoning() != null ? raw.getReasoning().getMaxTurns() : null;
        }

        /**
         * Checks if the component is thinking out loud.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isThinkingOutLoud() {
            return raw.getReasoning() != null && raw.getReasoning().isThinkingOutLoud();
        }


        /**
         * Checks if the component is fail on error.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isFailOnError() { return raw.isFailOnError(); }

        /**
         * Checks if the component is verbose.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isVerbose() { return raw.isVerbose(); }

        /**
         * Checks if the component is dry run.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isDryRun() { return raw.isDryRun(); }

        /**
         * Checks if the component is add reasoning notes.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isAddReasoningNotes() {
            return raw.getTooling() != null && raw.getTooling().isAddReasoningNotes();
        }

        /**
         * Gets the summarize cycle turns.
         *
         * @return the numeric value
         */
        public int getSummarizeCycleTurns() {
            return raw.getReasoning() != null ? raw.getReasoning().getSummarizeCycleTurns() : 0;
        }

        /**
         * Gets the script dir.
         *
         * @return the string result
         */
        public String getScriptDir() {
            return raw.getReasoning() != null ? raw.getScriptsPath() : null;
        }

        // Delegating getters reflecting ReinsConfig settings & nested beans
        public GeminiSettings getGemini() { return raw.getGemini(); }
        public OllamaSettings getOllama() { return raw.getOllama(); }
        public OpenAiSettings getOpenai() { return raw.getOpenai(); }
        public String getProvider() { return raw.getProvider(); }
        public java.util.Map<String, java.io.File> getSourceBases() { return raw.getSourceBases(); }
        public boolean isSkipTest() { return raw.isSkipTest(); }
        public String getIncludePattern() { return raw.getIncludePattern(); }
        public boolean isValidateAll() { return raw.isValidateAll(); }
        public boolean isFreshCompilation() { return raw.isFreshCompilation(); }
        public TargetSettings getTarget() { return raw.getTarget(); }
        public ReasoningSettings getReasoning() { return raw.getReasoning(); }
        public RecompileOnSettings getRecompileOn() { return raw.getRecompileOn(); }
        public EagerlyProvideSettings getEagerlyProvide() { return raw.getEagerlyProvide(); }
        public LogSettings getLog() { return raw.getLog(); }
        public LoggingSettings getLogging() { return raw.getLogging(); }
        public ModelSettings getModelSettings() { return raw.getModel(); }
        public BuildSettings getBuild() { return raw.getBuild(); }
        public ToolingSettings getTooling() { return raw.getTooling(); }
        public FileToolsSettings getFileTools() { return raw.getFileTools(); }
        public ContextSettings getContext() { return raw.getContext(); }
        public TrackingSettings getTracking() { return raw.getTracking(); }
        public String getSource() { return raw.getSource(); }
        public String getNote() { return raw.getNote(); }
        public boolean isExplicitSourceMode() { return raw.isExplicitSourceMode(); }
        public String getScriptsPath() { return raw.getScriptsPath(); }
    }

    /**
     * PolicyView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing policy view.
     */
    public static final class PolicyView {
        private final List<String> listFilesBases;
        private final List<String> readFilesBases;
        private final List<String> writeFilesBases;
        private final List<String> patchFilesBases;
        private final List<String> deleteFilesBases;
        private final List<String> appendFilesBases;
        private final List<String> prependFilesBases;
        private final List<String> moveFilesBases;
        private final List<String> copyFilesBases;
        private final List<String> listCompiledFilesBases;
        private final boolean scriptRunnerEnabled;
        private final String toolOpsReference;

        public PolicyView(List<String> listFilesBases,
                          List<String> readFilesBases,
                          List<String> writeFilesBases,
                          List<String> patchFilesBases,
                          List<String> deleteFilesBases,
                          List<String> listCompiledFilesBases,
                          boolean scriptRunnerEnabled,
                          String toolOpsReference) {
            this(listFilesBases, readFilesBases, writeFilesBases, patchFilesBases, deleteFilesBases,
                 writeFilesBases, writeFilesBases, writeFilesBases, writeFilesBases, listCompiledFilesBases,
                 scriptRunnerEnabled, toolOpsReference);
        }

        public PolicyView(List<String> listFilesBases,
                          List<String> readFilesBases,
                          List<String> writeFilesBases,
                          List<String> patchFilesBases,
                          List<String> deleteFilesBases,
                          List<String> appendFilesBases,
                          List<String> prependFilesBases,
                          List<String> moveFilesBases,
                          List<String> listCompiledFilesBases,
                          boolean scriptRunnerEnabled,
                          String toolOpsReference) {
            this(listFilesBases, readFilesBases, writeFilesBases, patchFilesBases, deleteFilesBases,
                 appendFilesBases, prependFilesBases, moveFilesBases, writeFilesBases, listCompiledFilesBases,
                 scriptRunnerEnabled, toolOpsReference);
        }

        public PolicyView(List<String> listFilesBases,
                          List<String> readFilesBases,
                          List<String> writeFilesBases,
                          List<String> patchFilesBases,
                          List<String> deleteFilesBases,
                          List<String> appendFilesBases,
                          List<String> prependFilesBases,
                          List<String> moveFilesBases,
                          List<String> copyFilesBases,
                          List<String> listCompiledFilesBases,
                          boolean scriptRunnerEnabled,
                          String toolOpsReference) {
            this.listFilesBases = listFilesBases != null
                    ? Collections.unmodifiableList(listFilesBases) : Collections.emptyList();
            this.readFilesBases = readFilesBases != null
                    ? Collections.unmodifiableList(readFilesBases) : Collections.emptyList();
            this.writeFilesBases = writeFilesBases != null
                    ? Collections.unmodifiableList(writeFilesBases) : Collections.emptyList();
            this.patchFilesBases = patchFilesBases != null
                    ? Collections.unmodifiableList(patchFilesBases) : Collections.emptyList();
            this.deleteFilesBases = deleteFilesBases != null
                    ? Collections.unmodifiableList(deleteFilesBases) : Collections.emptyList();
            this.appendFilesBases = appendFilesBases != null
                    ? Collections.unmodifiableList(appendFilesBases) : Collections.emptyList();
            this.prependFilesBases = prependFilesBases != null
                    ? Collections.unmodifiableList(prependFilesBases) : Collections.emptyList();
            this.moveFilesBases = moveFilesBases != null
                    ? Collections.unmodifiableList(moveFilesBases) : Collections.emptyList();
            this.copyFilesBases = copyFilesBases != null
                    ? Collections.unmodifiableList(copyFilesBases) : Collections.emptyList();
            this.listCompiledFilesBases = listCompiledFilesBases != null
                    ? Collections.unmodifiableList(listCompiledFilesBases) : Collections.emptyList();
            this.scriptRunnerEnabled = scriptRunnerEnabled;
            this.toolOpsReference = toolOpsReference != null ? toolOpsReference : "";
        }

        public List<String> getListFilesBases() { return listFilesBases; }
        public List<String> getReadFilesBases() { return readFilesBases; }
        public List<String> getWriteFilesBases() { return writeFilesBases; }
        public List<String> getPatchFilesBases() { return patchFilesBases; }
        public List<String> getDeleteFilesBases() { return deleteFilesBases; }
        public List<String> getAppendFilesBases() { return appendFilesBases; }
        public List<String> getPrependFilesBases() { return prependFilesBases; }
        public List<String> getMoveFilesBases() { return moveFilesBases; }
        public List<String> getCopyFilesBases() { return copyFilesBases; }
        /**
         * Gets the list compiled files bases.
         *
         * @return the string result
         */
        public List<String> getListCompiledFilesBases() { return listCompiledFilesBases; }
        /**
         * Checks if the component is script runner enabled.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isScriptRunnerEnabled() { return scriptRunnerEnabled; }
        /**
         * Gets the tool ops reference.
         *
         * @return the string result
         */
        public String getToolOpsReference() { return toolOpsReference; }

        /**
         * Checks if list files base group is enabled.
         *
         * @return true if list files bases are configured
         */
        public boolean isHasListFiles() {
            return listFilesBases != null && !listFilesBases.isEmpty();
        }

        /**
         * Checks if read files base group is enabled.
         *
         * @return true if read files bases are configured
         */
        public boolean isHasReadFiles() {
            return readFilesBases != null && !readFilesBases.isEmpty();
        }

        /**
         * Checks if write files base group is enabled.
         *
         * @return true if write files bases are configured
         */
        public boolean isHasWriteFiles() {
            return writeFilesBases != null && !writeFilesBases.isEmpty();
        }

        /**
         * Checks if patch files base group is enabled.
         *
         * @return true if patch files bases are configured
         */
        public boolean isHasPatchFiles() {
            return patchFilesBases != null && !patchFilesBases.isEmpty();
        }

        /**
         * Checks if delete files base group is enabled.
         *
         * @return true if delete files bases are configured
         */
        public boolean isHasDeleteFiles() {
            return deleteFilesBases != null && !deleteFilesBases.isEmpty();
        }

        /**
         * Checks if list compiled files base group is enabled.
         *
         * @return true if list compiled files bases are configured
         */
        public boolean isHasListCompiledFiles() {
            return listCompiledFilesBases != null && !listCompiledFilesBases.isEmpty();
        }

        /**
         * Checks if any read or list tool group is enabled.
         *
         * @return true if read/list operations active
         */
        public boolean isHasAnyReadOrList() {
            return isHasListFiles() || isHasReadFiles() || isHasListCompiledFiles();
        }

        /**
         * Checks if any mutation tool group is enabled (write, patch, delete).
         *
         * @return true if mutation operations active
         */
        public boolean isHasAnyMutation() {
            return isHasWriteFiles() || isHasPatchFiles() || isHasDeleteFiles();
        }

        /**
         * Checks if any tool group or script runner is enabled.
         *
         * @return true if any tool operation group active
         */
        public boolean isHasAnyToolEnabled() {
            return isHasAnyReadOrList() || isHasAnyMutation() || scriptRunnerEnabled;
        }
    }

    /**
     * ToolResultView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing tool result view.
     */
    public static final class ToolResultView {
        private final String status;
        private final String operation;
        private final String qualifiedPath;
        private final String resolvedBase;
        private final String failureReason;
        private final String policyCode;
        private final String resolvedPath;
        private final Integer exitCode;
        private final boolean started;
        private final boolean truncated;
        private final String stdout;
        private final String stderr;
        private final String content;
        private final boolean readFileSuccess;
        private final List<String> listedPaths;
        private final List<CompiledFileStatusView> compiledFileStatuses;
        private final List<ReadFileStatusView> readFileStatuses;
        private final List<String> excludedPaths;
        private final String exclusionReason;

        /**
         * Constructs a new instance of {@link ToolResultView}.
         *
         * @param status the status
         * @param operation the operation
         * @param qualifiedPath the qualified path
         * @param resolvedBase the resolved base
         * @param failureReason the failure reason
         * @param policyCode the policy code
         * @param resolvedPath the resolved path
         * @param exitCode the exit code
         * @param started the started
         * @param truncated the truncated
         * @param stdout the stdout
         * @param stderr the stderr
         * @param content the content
         * @param readFileSuccess the read file success
         * @param listedPaths the listed paths
         * @param compiledFileStatuses the compiled file statuses
         * @param readFileStatuses the read file statuses
         * @param excludedPaths the excluded paths
         * @param exclusionReason the exclusion reason
         */
        public ToolResultView(String status, String operation, String qualifiedPath,
                             String resolvedBase, String failureReason, String policyCode,
                             String resolvedPath, Integer exitCode, boolean started, boolean truncated,
                             String stdout, String stderr, String content, boolean readFileSuccess,
                             List<String> listedPaths,
                             List<CompiledFileStatusView> compiledFileStatuses,
                             List<ReadFileStatusView> readFileStatuses,
                             List<String> excludedPaths, String exclusionReason) {
            this.status = status;
            this.operation = operation;
            this.qualifiedPath = qualifiedPath;
            this.resolvedBase = resolvedBase;
            this.failureReason = failureReason;
            this.policyCode = policyCode;
            this.resolvedPath = resolvedPath;
            this.exitCode = exitCode;
            this.started = started;
            this.truncated = truncated;
            this.stdout = stdout;
            this.stderr = stderr;
            this.content = content;
            this.readFileSuccess = readFileSuccess;
            this.listedPaths = listedPaths != null
                    ? Collections.unmodifiableList(listedPaths) : Collections.emptyList();
            this.compiledFileStatuses = compiledFileStatuses != null
                    ? Collections.unmodifiableList(compiledFileStatuses) : Collections.emptyList();
            this.readFileStatuses = readFileStatuses != null
                    ? Collections.unmodifiableList(readFileStatuses) : Collections.emptyList();
            this.excludedPaths = excludedPaths != null
                    ? Collections.unmodifiableList(excludedPaths) : Collections.emptyList();
            this.exclusionReason = exclusionReason;
        }

        /**
         * Gets the status.
         *
         * @return the string result
         */
        public String getStatus() { return status; }
        /**
         * Gets the operation.
         *
         * @return the string result
         */
        public String getOperation() { return operation; }
        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() { return qualifiedPath; }
        /**
         * Gets the resolved base.
         *
         * @return the string result
         */
        public String getResolvedBase() { return resolvedBase; }
        /**
         * Gets the failure reason.
         *
         * @return the string result
         */
        public String getFailureReason() { return failureReason; }
        /**
         * Gets the policy code.
         *
         * @return the string result
         */
        public String getPolicyCode() { return policyCode; }
        /**
         * Gets the resolved path.
         *
         * @return the string result
         */
        public String getResolvedPath() { return resolvedPath; }
        /**
         * Gets the exit code.
         *
         * @return the numeric value
         */
        public Integer getExitCode() { return exitCode; }
        /**
         * Checks if the component is started.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isStarted() { return started; }
        /**
         * Checks if the component is truncated.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isTruncated() { return truncated; }
        /**
         * Gets the stdout.
         *
         * @return the string result
         */
        public String getStdout() { return stdout; }
        /**
         * Gets the stderr.
         *
         * @return the string result
         */
        public String getStderr() { return stderr; }
        /**
         * Gets the content.
         *
         * @return the string result
         */
        public String getContent() { return content; }
        /**
         * Checks if the component is read file success.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isReadFileSuccess() { return readFileSuccess; }
        /**
         * Gets the listed paths.
         *
         * @return the string result
         */
        public List<String> getListedPaths() { return listedPaths; }
        /**
         * Gets the compiled file statuses.
         *
         * @return the collection of elements
         */
        public List<CompiledFileStatusView> getCompiledFileStatuses() { return compiledFileStatuses; }
        /**
         * Gets the read file statuses.
         *
         * @return the collection of elements
         */
        public List<ReadFileStatusView> getReadFileStatuses() { return readFileStatuses; }
        /**
         * Gets the excluded paths.
         *
         * @return the string result
         */
        public List<String> getExcludedPaths() { return excludedPaths; }
        /**
         * Gets the exclusion reason.
         *
         * @return the string result
         */
        public String getExclusionReason() { return exclusionReason; }
    }

    /**
     * CompiledFileStatusView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing compiled file status view.
     */
    public static final class CompiledFileStatusView {
        private final String qualifiedPath;
        private final String attachStatus;
        private final String reason;

        /**
         * Constructs a new instance of {@link CompiledFileStatusView}.
         *
         * @param qualifiedPath the qualified path
         * @param attachStatus the attach status
         * @param reason the reason
         */
        public CompiledFileStatusView(String qualifiedPath, String attachStatus, String reason) {
            this.qualifiedPath = qualifiedPath;
            this.attachStatus = attachStatus;
            this.reason = reason;
        }

        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() { return qualifiedPath; }
        /**
         * Gets the attach status.
         *
         * @return the string result
         */
        public String getAttachStatus() { return attachStatus; }
        /**
         * Gets the reason.
         *
         * @return the string result
         */
        public String getReason() { return reason; }
    }

    /**
     * ReadFileStatusView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing read file status view.
     */
    public static final class ReadFileStatusView {
        private final String qualifiedPath;
        private final String attachStatus;
        private final String reason;

        /**
         * Constructs a new instance of {@link ReadFileStatusView}.
         *
         * @param qualifiedPath the qualified path
         * @param attachStatus the attach status
         * @param reason the reason
         */
        public ReadFileStatusView(String qualifiedPath, String attachStatus, String reason) {
            this.qualifiedPath = qualifiedPath;
            this.attachStatus = attachStatus;
            this.reason = reason;
        }

        /**
         * Gets the qualified path.
         *
         * @return the string result
         */
        public String getQualifiedPath() { return qualifiedPath; }
        /**
         * Gets the attach status.
         *
         * @return the string result
         */
        public String getAttachStatus() { return attachStatus; }
        /**
         * Gets the reason.
         *
         * @return the string result
         */
        public String getReason() { return reason; }
    }

    public static SourceView emptySource() {
        return new SourceView("", "", "", "", "", "", Collections.emptyList());
    }

    public static FileBasesView emptyFileBases() {
        return new FileBasesView("", "", "", "");
    }

    public static TrackingView emptyTracking() {
        return new TrackingView("", Collections.emptyList(), "", "", Collections.emptyList());
    }

    public static InferenceStateView emptyInferenceState() {
        return new InferenceStateView("", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static ConfigView emptyConfig() {
        return new ConfigView(new ReinsConfig());
    }

    public static CycleView emptyCycle() {
        return new CycleView("", 0, 0, "", "");
    }

    public static PolicyView emptyPolicy() {
        return new PolicyView(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, "");
    }

    public static PipelineView emptyPipeline() {
        return new PipelineView("", 0, 0, Collections.emptyList(), "COMPILE", "", "", "", "", false);
    }

    public static ToolResultView emptyToolResult() {
        return new ToolResultView("", "", "", "", "", "", "", 0, false, false, "", "", "", false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "");
    }
}