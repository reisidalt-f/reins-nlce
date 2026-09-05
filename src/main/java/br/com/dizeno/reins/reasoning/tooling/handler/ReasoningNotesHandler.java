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

package br.com.dizeno.reins.reasoning.tooling.handler;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ReasoningNotesHandler is part of the general application functions in the reins architecture.
 * Acts as a component managing reasoning notes handler.
 */
public class ReasoningNotesHandler implements ToolOperationHandler {
    private final CompilationTrackingStore trackingStore;
    private final SourceTrackingManager trackingManager;
    private final Set<String> notedSourcePaths = new LinkedHashSet<>();

    /**
     * Constructs a new instance of {@link ReasoningNotesHandler}.
     *
     * @param trackingStore the persistence store for file tracking records
     * @param trackingManager the tracking manager
     */
    public ReasoningNotesHandler(CompilationTrackingStore trackingStore, SourceTrackingManager trackingManager) {
        this.trackingStore = trackingStore;
        this.trackingManager = trackingManager;
    }

    /**
     * Supports.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean supports(ToolExecutionRequest.Operation operation) {
        return operation == ToolExecutionRequest.Operation.ADD_REASONING_NOTE
                || operation == ToolExecutionRequest.Operation.CLEAR_REASONING_NOTES
                || operation == ToolExecutionRequest.Operation.LIST_REASONING_NOTES;
    }

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request,
                                       BasePathResolver resolver,
                                       String sourceScope,
                                       ScriptRunnerConfig scriptRunnerConfig) {
        if (scriptRunnerConfig != null && !scriptRunnerConfig.isAddReasoningNotes()) {
            String reportPath = request.getSource() != null ? request.getSource() : request.getCompiled();
            return ToolExecutionResult.error(request.getOperation(), reportPath,
                    "Operation " + request.getOperation().name().toLowerCase() + " is disabled (tooling.addReasoningNotes is false).");
        }
        try {
            return executeNoteOperation(request, resolver);
        } catch (IOException ex) {
            String reportPath = request.getSource() != null ? request.getSource() : request.getCompiled();
            return ToolExecutionResult.error(request.getOperation(), reportPath, ex.getMessage());
        }
    }

    /**
     * Gets the and clear noted source paths.
     *
     * @return the string result
     */
    public Set<String> getAndClearNotedSourcePaths() {
        synchronized (notedSourcePaths) {
            Set<String> snapshot = new LinkedHashSet<>(notedSourcePaths);
            notedSourcePaths.clear();
            return snapshot;
        }
    }

    private ToolExecutionResult executeNoteOperation(ToolExecutionRequest request,
                                                     BasePathResolver resolver) throws IOException {
        Path projectRoot = resolver.getProjectRoot().toAbsolutePath().normalize();

        if (request.getOperation() == ToolExecutionRequest.Operation.ADD_REASONING_NOTE) {
            String canonicalSource;
            String reportPath;
            if (request.getSource() != null && !request.getSource().isBlank()) {
                canonicalSource = trackingStore.canonicalizePath(request.getSource());
                reportPath = canonicalSource;
            } else {
                String canonicalCompiled = trackingStore.canonicalizePath(request.getCompiled());
                canonicalSource = resolveSourceForCompiled(projectRoot, canonicalCompiled);
                if (canonicalSource == null) {
                    return ToolExecutionResult.error(
                            request.getOperation(),
                            request.getCompiled(),
                            "No tracked source found owning compiled file: " + request.getCompiled());
                }
                reportPath = canonicalCompiled;
            }
            int noteCount = trackingManager.appendNote(
                    projectRoot, canonicalSource, request.getNote(), ReasoningNote.Origin.TOOL, trackingStore);
            synchronized (notedSourcePaths) {
                notedSourcePaths.add(canonicalSource);
            }
            ToolExecutionResult result = ToolExecutionResult.success(
                    request.getOperation(), reportPath, "note-appended");
            result.setContent("Note added to " + canonicalSource + " (total notes: " + noteCount + ")");
            return result;
        }

        if (request.getOperation() == ToolExecutionRequest.Operation.LIST_REASONING_NOTES) {
            StringBuilder sb = new StringBuilder();
            if (request.getSource() != null && !request.getSource().isBlank()) {
                String canonicalSource = trackingStore.canonicalizePath(request.getSource());
                Optional<SourceTrackingRecord> recOpt = trackingStore.load(projectRoot, canonicalSource);
                if (recOpt.isPresent() && recOpt.get().getNotes() != null && !recOpt.get().getNotes().isEmpty()) {
                    sb.append("Notes for ").append(canonicalSource).append(":\n");
                    for (ReasoningNote n : recOpt.get().getNotes()) {
                        sb.append("- [").append(n.getOrigin()).append(" | ").append(n.getCreatedAt()).append("] ").append(n.getText()).append("\n");
                    }
                } else {
                    sb.append("No notes for ").append(canonicalSource);
                }
                ToolExecutionResult result = ToolExecutionResult.success(
                        request.getOperation(), canonicalSource, "notes-listed");
                result.setContent(sb.toString().trim());
                return result;
            } else {
                List<String> allSources = trackingStore.listAllTrackedSourcePaths(projectRoot);
                int foundCount = 0;
                for (String sourcePath : allSources) {
                    Optional<SourceTrackingRecord> recOpt = trackingStore.load(projectRoot, sourcePath);
                    if (recOpt.isPresent() && recOpt.get().getNotes() != null && !recOpt.get().getNotes().isEmpty()) {
                        foundCount++;
                        sb.append("Source: ").append(sourcePath).append(" (").append(recOpt.get().getNotes().size()).append(" notes):\n");
                        for (ReasoningNote n : recOpt.get().getNotes()) {
                            sb.append("  - [").append(n.getOrigin()).append(" | ").append(n.getCreatedAt()).append("] ").append(n.getText()).append("\n");
                        }
                    }
                }
                if (foundCount == 0) {
                    sb.append("No notes found for any source.");
                }
                ToolExecutionResult result = ToolExecutionResult.success(
                        request.getOperation(), "all", "notes-listed");
                result.setContent(sb.toString().trim());
                return result;
            }
        }

        if (request.getSource() != null && !request.getSource().isBlank()) {
            String canonicalSource = trackingStore.canonicalizePath(request.getSource());
            int cleared = trackingManager.clearNotes(projectRoot, canonicalSource, trackingStore);
            ToolExecutionResult result = ToolExecutionResult.success(
                    request.getOperation(), canonicalSource, "notes-cleared");
            result.setContent("Cleared " + cleared + " note(s) from " + canonicalSource);
            return result;
        } else {
            List<String> allSources = trackingStore.listAllTrackedSourcePaths(projectRoot);
            int totalCleared = 0;
            for (String sourcePath : allSources) {
                totalCleared += trackingManager.clearNotes(projectRoot, sourcePath, trackingStore);
            }
            ToolExecutionResult result = ToolExecutionResult.success(
                    request.getOperation(), "all", "notes-cleared");
            result.setContent("Cleared total of " + totalCleared + " note(s) across all sources.");
            return result;
        }
    }

    private String resolveSourceForCompiled(Path projectRoot, String canonicalCompiled) throws IOException {
        List<String> allSources = trackingStore.listAllTrackedSourcePaths(projectRoot);
        for (String sourcePath : allSources) {
            Optional<SourceTrackingRecord> recordOpt = trackingStore.load(projectRoot, sourcePath);
            if (recordOpt.isPresent()) {
                SourceTrackingRecord record = recordOpt.get();
                if (record.getCompiledFiles() != null
                        && record.getCompiledFiles().containsKey(canonicalCompiled)) {
                    return sourcePath;
                }
            }
        }
        return null;
    }
}
