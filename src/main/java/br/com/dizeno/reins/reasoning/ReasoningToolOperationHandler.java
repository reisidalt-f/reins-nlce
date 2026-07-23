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
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ReasoningToolOperationHandler is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing reasoning tool operation handler.
 */
public class ReasoningToolOperationHandler {
    private final ToolingService toolingService;
    private final CompilationTrackingStore trackingStore;
    private final ConcurrentHashMap<String, ReentrantLock> sourceLocks;
    /**
     * Constructs a new instance of {@link ReasoningToolOperationHandler}.
     *
     * @param toolingService the tooling service
     * @param trackingStore the persistence store for file tracking records
     * @param sourceLocks the source locks
     */
    public ReasoningToolOperationHandler(ToolingService toolingService,
                                         CompilationTrackingStore trackingStore,
                                         ConcurrentHashMap<String, ReentrantLock> sourceLocks) {
        this.toolingService = toolingService;
        this.trackingStore = trackingStore;
        this.sourceLocks = sourceLocks;
    }

    /**
     * Executes the operation with serialization.
     *
     * @param operationRequest the operation request
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @param permission the permission
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    public ToolExecutionResult executeWithSerialization(ToolExecutionRequest operationRequest,
                                                       BasePathResolver resolver,
                                                       String sourceScope,
                                                       FilePolicy permission,
                                                       ScriptRunnerConfig scriptRunnerConfig) {
        if (operationRequest.getOperation() != ToolExecutionRequest.Operation.LIST_COMPILED_FILES) {
            ToolExecutionResult result = toolingService.executeWithScope(
                    operationRequest,
                    resolver,
                    sourceScope,
                    permission,
                    scriptRunnerConfig);
            return result != null ? result : toolingService.execute(operationRequest, resolver, permission, scriptRunnerConfig);
        }
        String sourceKey = resolver.canonicalizeSourcePath(operationRequest.getBase(), operationRequest.getPath());
        ReentrantLock lock = sourceLocks.computeIfAbsent(sourceKey, key -> new ReentrantLock());
        lock.lock();
        try {
            ToolExecutionResult result = toolingService.executeWithScope(
                    operationRequest,
                    resolver,
                    sourceScope,
                    permission,
                    scriptRunnerConfig);
            return result != null ? result : toolingService.execute(operationRequest, resolver, permission, scriptRunnerConfig);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Validates the inputs or files batch mutation scope.
     *
     * @param requests the requests
     * @param resolver the resolver
     * @return the string result
     */
    public String validateBatchMutationScope(List<ToolExecutionRequest> requests,
                                             BasePathResolver resolver) {
        for (ToolExecutionRequest request : requests) {
            String violation = toolingService.validateMutationScope(request, resolver);
            if (violation != null) {
                return "Batch request rejected: " + violation + " No operation was applied.";
            }
        }
        return null;
    }

    /**
     * Builds the configured target dedupe key.
     *
     * @param request the request containing path and scope metadata
     * @return the string result
     */
    public String buildDedupeKey(ToolExecutionRequest request) {
        StringBuilder key = new StringBuilder();
        key.append(request.getOperation())
            .append('|').append(request.getBase())
            .append('|').append(request.getPath())
            .append('|').append(request.getScript())
            .append('|').append(request.getArgs())
            .append('|').append(request.getAtLine())
            .append('|').append(request.getReplacing())
            .append('|').append(request.isRecursive())
            .append('|').append(request.isCreateParents())
            .append('|').append(request.getContent());

        if (request.getOperation() == ToolExecutionRequest.Operation.ADD_INFERENCE_NOTE
            || request.getOperation() == ToolExecutionRequest.Operation.CLEAR_INFERENCE_NOTES) {
            key.append('|').append(request.getSource())
                .append('|').append(request.getCompiled())
                .append('|').append(request.getNote());
        }

        return key.toString();
    }

    /**
     * Checks if the component is reference mutation blocked.
     *
     * @param request the request containing path and scope metadata
     * @param operationRequest the operation request
     * @param resolver the resolver
     * @return true if successful or matching, false otherwise
     */
    public boolean isReferenceMutationBlocked(ReasoningRequest request,
                                              ToolExecutionRequest operationRequest,
                                              BasePathResolver resolver) {
        return evaluateReferenceMutationDecision(request, operationRequest, resolver).isBlocked();
    }

    /**
     * Evaluate Reference Mutation Decision.
     *
     * @param request the request containing path and scope metadata
     * @param operationRequest the operation request
     * @param resolver the resolver
     * @return the resolved or constructed object
     */
    public ReferenceMutationDecision evaluateReferenceMutationDecision(ReasoningRequest request,
                                                                       ToolExecutionRequest operationRequest,
                                                                       BasePathResolver resolver) {
        if (operationRequest.getOperation() != ToolExecutionRequest.Operation.WRITE_FILE
                && operationRequest.getOperation() != ToolExecutionRequest.Operation.PATCH_FILE
                && operationRequest.getOperation() != ToolExecutionRequest.Operation.DELETE_FILE) {
            return ReferenceMutationDecision.allowNoConflict(0, 0);
        }
        if (request.getSourcePath() == null || request.getSourcePath().isBlank()) {
            return ReferenceMutationDecision.allowNoConflict(0, 0);
        }

        try {
            Path projectRoot = resolver.getProjectRoot().toAbsolutePath().normalize();
            Path operationAbsolute = resolver.resolve(operationRequest.getBase(), operationRequest.getPath());
            String operationCanonical = trackingStore.canonicalizePath(resolver.qualifyAbsolute(operationAbsolute));

            if ("test".equals(request.getSourceScope())) {
                Path testTargetRoot = resolver.getMappings().getTestTargetRoot();
                Path mainTargetRoot = resolver.getMappings().getMainTargetRoot();
                if (testTargetRoot != null && mainTargetRoot != null) {
                    if (operationAbsolute.startsWith(testTargetRoot)) {
                        Path relativePath = testTargetRoot.relativize(operationAbsolute);
                        String relativePathStr = relativePath.toString().replace('\\', '/');
                        while (relativePathStr.startsWith("/")) {
                            relativePathStr = relativePathStr.substring(1);
                        }

                        List<String> trackedSources = trackingStore.listAllTrackedSourcePaths(projectRoot);
                        for (String trackedSource : trackedSources) {
                            Optional<SourceTrackingRecord> recordOpt = trackingStore.load(projectRoot, trackedSource);
                            if (recordOpt.isPresent()) {
                                SourceTrackingRecord rec = recordOpt.get();
                                if ("main".equals(rec.getSourceCategory())) {
                                    for (String compiledPath : rec.getCompiledFiles().keySet()) {
                                        Path compiledAbsolute = TrackedPathResolver.resolveTrackedPath(
                                                projectRoot,
                                                compiledPath,
                                                rec.getResolvedTargetRoot()
                                        );
                                        if (compiledAbsolute.startsWith(mainTargetRoot)) {
                                            Path mainRelPath = mainTargetRoot.relativize(compiledAbsolute);
                                            String mainRelPathStr = mainRelPath.toString().replace('\\', '/');
                                            while (mainRelPathStr.startsWith("/")) {
                                                mainRelPathStr = mainRelPathStr.substring(1);
                                            }
                                            if (mainRelPathStr.equals(relativePathStr)) {
                                                return ReferenceMutationDecision.blockTestMainExclusion();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            String activeSourceCanonical = canonicalSourceIdentity(request.getSourcePath());

            Optional<SourceTrackingRecord> mainRecord = trackingStore.load(
                    projectRoot,
                    activeSourceCanonical
            );
            if (mainRecord.isEmpty()) {
                
                
                
                Optional<String> ownerSource = trackingStore.findOwnerSource(projectRoot, operationCanonical);
                if (ownerSource.isPresent() && !ownerSource.get().equals(activeSourceCanonical)) {
                    return ReferenceMutationDecision.blockForeignOwned();
                }
                return ReferenceMutationDecision.allowNoConflict(0, 0);
            }

            Set<String> normalizedReferences = new HashSet<>();
            for (String reference : mainRecord.get().getMarkdownReferences().keySet()) {
                normalizedReferences.add(canonicalSourceIdentity(reference));
            }
            int consideredReferences = normalizedReferences.size();
            Set<String> nonSelfReferences = normalizedNonSelfReferences(normalizedReferences, activeSourceCanonical);
            int ignoredSelfReferences = consideredReferences - nonSelfReferences.size();

            if (nonSelfReferences.contains(operationCanonical)) {
                return ReferenceMutationDecision.blockReferencedSource(consideredReferences, ignoredSelfReferences);
            }

            for (String referencedSource : nonSelfReferences) {
                Optional<SourceTrackingRecord> refRecord = trackingStore.load(projectRoot, referencedSource);
                if (refRecord.isPresent()) {
                    for (String compiledPath : refRecord.get().getCompiledFiles().keySet()) {
                        String canonicalCompiled = trackingStore.canonicalizePath(compiledPath);
                        if (canonicalCompiled.equals(operationCanonical)) {
                            return ReferenceMutationDecision.blockReferencedCompiled(consideredReferences, ignoredSelfReferences);
                        }
                    }
                }
            }
            return ReferenceMutationDecision.allowNoConflict(consideredReferences, ignoredSelfReferences);
        } catch (Exception ex) {
            return ReferenceMutationDecision.errorFallback(0, 0);
        }
    }

    /**
     * Format Decision Reason.
     *
     * @param decision the decision
     * @return the string result
     */
    public String formatDecisionReason(ReferenceMutationDecision decision) {
        return switch (decision.getReasonCode()) {
            case BLOCK_REFERENCED_SOURCE -> "reason=BLOCK_REFERENCED_SOURCE ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
            case BLOCK_REFERENCED_COMPILED -> "reason=BLOCK_REFERENCED_COMPILED ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
            case BLOCK_FOREIGN_OWNED -> "reason=BLOCK_FOREIGN_OWNED ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
            case BLOCK_TEST_MAIN_EXCLUSION -> "reason=BLOCK_TEST_MAIN_EXCLUSION ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
            case ERROR_FALLBACK -> "reason=ERROR_FALLBACK ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
            case ALLOW_NO_CONFLICT -> "reason=ALLOW_NO_CONFLICT ignoredSelfReferences=" + decision.getIgnoredSelfReferences();
        };
    }

    private String canonicalSourceIdentity(String sourcePath) {
        return trackingStore.canonicalizePath(sourcePath);
    }

    private Set<String> normalizedNonSelfReferences(Set<String> normalizedReferences, String activeSourceCanonical) {
        Set<String> filtered = new LinkedHashSet<>();
        for (String candidate : normalizedReferences) {
            if (!activeSourceCanonical.equals(candidate)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }

    /**
     * Builds the configured target source attachment.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceBase the source base
     * @return the resulting payload
     */
    public AttachedFilePayload buildSourceAttachment(ReasoningRequest request,
                                                     BasePathResolver resolver,
                                                     String sourceBase) {
        try {
            if (request.getSourcePath() == null || request.getSourcePath().isBlank()) {
                return null;
            }

            Path sourceAbsolute;
            String base;
            String relative;

            if (TrackedPathResolver.looksCanonical(request.getSourcePath())) {
                FileReference ref = FileReference.fromCanonical(request.getSourcePath());
                sourceAbsolute = resolver.resolve(ref);
                base = ref.getBase().value();
                relative = ref.getPath();
            } else if (request.getSourcePath().startsWith("src/")) {
                sourceAbsolute = resolver.resolveProjectPath(request.getSourcePath());
                base = sourceBase;
                relative = resolver.relativize(base, sourceAbsolute);
            } else {
                base = sourceBase;
                sourceAbsolute = resolver.resolve(base, request.getSourcePath());
                relative = resolver.relativize(base, sourceAbsolute);
            }

            if (!Files.exists(sourceAbsolute)) {
                return null;
            }

            AttachedFilePayload payload = new AttachedFilePayload();
            payload.setBase(base);
            payload.setRelativePath(relative);
            payload.setQualifiedPath(resolver.qualify(base, relative));
            payload.setContent(Files.readString(sourceAbsolute, StandardCharsets.UTF_8));
            return payload;
        } catch (Exception ex) {
            return null;
        }
    }
}
