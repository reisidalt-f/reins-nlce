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
            this.path = path;
            this.absolutePath = absolutePath;
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

    /**
     * InferenceStateView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing inference state view.
     */
    public static final class InferenceStateView {
        private final String context;
        private final List<TurnView> conversationHistory;
        private final List<String> inspectedFiles;
        private final List<String> compiledFiles;
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
         * Gets the tool operations.
         *
         * @return the collection of elements
         */
        public List<ToolOpView> getToolOperations() { return toolOperations; }
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
    }

    /**
     * ConfigView is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing config view.
     */
    public static final class ConfigView {
        private final String model;
        private final Integer maxTurns;
        private final boolean thinkingOutLoud;
        private final boolean projectCycle;
        private final boolean failOnError;
        private final boolean verbose;
        private final boolean dryRun;
        private final boolean addReasoningNotes;
        private final String scriptDir;

        /**
         * Constructs a new instance of {@link ConfigView}.
         *
         * @param model the model name string
         * @param maxTurns the maximum turn limit for the reasoning cycle
         * @param thinkingOutLoud the thinking out loud
         * @param projectCycle the project cycle
         * @param failOnError the fail on error
         * @param verbose the verbose
         * @param dryRun the dry run
         * @param scriptDir the script dir
         */
        public ConfigView(String model, Integer maxTurns, boolean thinkingOutLoud, boolean projectCycle,
                          boolean failOnError, boolean verbose, boolean dryRun, String scriptDir) {
            this(model, maxTurns, thinkingOutLoud, projectCycle, failOnError, verbose, dryRun, false, scriptDir);
        }

        /**
         * Constructs a new instance of {@link ConfigView}.
         *
         * @param model the model name string
         * @param maxTurns the maximum turn limit for the reasoning cycle
         * @param thinkingOutLoud the thinking out loud
         * @param projectCycle the project cycle
         * @param failOnError the fail on error
         * @param verbose the verbose
         * @param dryRun the dry run
         * @param addReasoningNotes the add inference notes
         * @param scriptDir the script dir
         */
        public ConfigView(String model, Integer maxTurns, boolean thinkingOutLoud, boolean projectCycle,
                          boolean failOnError, boolean verbose, boolean dryRun, boolean addReasoningNotes,
                          String scriptDir) {
            this.model = model;
            this.maxTurns = maxTurns;
            this.thinkingOutLoud = thinkingOutLoud;
            this.projectCycle = projectCycle;
            this.failOnError = failOnError;
            this.verbose = verbose;
            this.dryRun = dryRun;
            this.addReasoningNotes = addReasoningNotes;
            this.scriptDir = scriptDir;
        }

        /**
         * Gets the model.
         *
         * @return the string result
         */
        public String getModel() { return model; }
        /**
         * Gets the max turns.
         *
         * @return the numeric value
         */
        public Integer getMaxTurns() { return maxTurns; }
        /**
         * Checks if the component is thinking out loud.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isThinkingOutLoud() { return thinkingOutLoud; }
        /**
         * Checks if the component is project cycle.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isProjectCycle() { return projectCycle; }
        /**
         * Checks if the component is fail on error.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isFailOnError() { return failOnError; }
        /**
         * Checks if the component is verbose.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isVerbose() { return verbose; }
        /**
         * Checks if the component is dry run.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isDryRun() { return dryRun; }
        /**
         * Checks if the component is add inference notes.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isAddReasoningNotes() { return addReasoningNotes; }
        /**
         * Gets the script dir.
         *
         * @return the string result
         */
        public String getScriptDir() { return scriptDir; }
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
        private final List<String> listCompiledFilesBases;
        private final boolean scriptRunnerEnabled;
        private final String toolOpsReference;

        /**
         * Constructs a new instance of {@link PolicyView}.
         *
         * @param listFilesBases the list files bases
         * @param readFilesBases the read files bases
         * @param writeFilesBases the write files bases
         * @param patchFilesBases the patch files bases
         * @param deleteFilesBases the delete files bases
         * @param listCompiledFilesBases the list compiled files bases
         * @param scriptRunnerEnabled the script runner enabled
         * @param toolOpsReference the tool ops reference
         */
        public PolicyView(List<String> listFilesBases,
                          List<String> readFilesBases,
                          List<String> writeFilesBases,
                          List<String> patchFilesBases,
                          List<String> deleteFilesBases,
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
            this.listCompiledFilesBases = listCompiledFilesBases != null
                    ? Collections.unmodifiableList(listCompiledFilesBases) : Collections.emptyList();
            this.scriptRunnerEnabled = scriptRunnerEnabled;
            this.toolOpsReference = toolOpsReference != null ? toolOpsReference : "";
        }

        /**
         * Gets the list files bases.
         *
         * @return the string result
         */
        public List<String> getListFilesBases() { return listFilesBases; }
        /**
         * Gets the read files bases.
         *
         * @return the string result
         */
        public List<String> getReadFilesBases() { return readFilesBases; }
        /**
         * Gets the write files bases.
         *
         * @return the string result
         */
        public List<String> getWriteFilesBases() { return writeFilesBases; }
        /**
         * Gets the patch files bases.
         *
         * @return the string result
         */
        public List<String> getPatchFilesBases() { return patchFilesBases; }
        /**
         * Gets the delete files bases.
         *
         * @return the string result
         */
        public List<String> getDeleteFilesBases() { return deleteFilesBases; }
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
}