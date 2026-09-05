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
import br.com.dizeno.reins.compilation.tracking.CompiledFileListing;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.source.domain.FileReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * CompiledFileRegistryHandler is part of the general application functions in the reins architecture.
 * Acts as a component managing compiled file registry handler.
 */
public class CompiledFileRegistryHandler implements ToolOperationHandler {
    private final CompilationTrackingStore trackingStore;

    /**
     * Constructs a new instance of {@link CompiledFileRegistryHandler}.
     *
     * @param trackingStore the persistence store for file tracking records
     */
    public CompiledFileRegistryHandler(CompilationTrackingStore trackingStore) {
        this.trackingStore = trackingStore;
    }

    /**
     * Supports.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean supports(ToolExecutionRequest.Operation operation) {
        return operation == ToolExecutionRequest.Operation.LIST_COMPILED_FILES;
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
        String qualifiedPath = safeQualify(request, resolver);
        try {
            return listCompiledFiles(request, resolver, qualifiedPath);
        } catch (IOException ex) {
            return ToolExecutionResult.error(request.getOperation(), qualifiedPath, ex.getMessage());
        }
    }

    private String safeQualify(ToolExecutionRequest request, BasePathResolver resolver) {
        try {
            return resolver.qualify(request.getBase(), request.getPath());
        } catch (Exception ignored) {
            return request.getBase() + ":" + (request.getPath() == null ? "" : request.getPath());
        }
    }

    private ToolExecutionResult listCompiledFiles(ToolExecutionRequest request,
                                                  BasePathResolver resolver,
                                                  String qualifiedPath) throws IOException {
        Path resolvedSource = resolver.resolve(request.getBase(), request.getPath());
        String canonicalSource = trackingStore.canonicalizePath(resolver.toProjectRelativePath(resolvedSource));

        Path projectRoot = resolver.getProjectRoot().toAbsolutePath().normalize();
        Optional<SourceTrackingRecord> recordOpt = trackingStore.load(projectRoot, canonicalSource);
        ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "listed-compiled-files");

        if (recordOpt.isEmpty()) {
            result.setListedPaths(new ArrayList<>());
            return result;
        }

        SourceTrackingRecord record = recordOpt.get();
        Path allowedBase = resolver.resolveActiveOutputBaseRoot("target");
        CompiledFileListing listing = buildCompiledFileListing(record, projectRoot, allowedBase);

        List<String> qualifiedPaths = new ArrayList<>();
        for (String compiledPath : listing.getExistingPaths()) {
            Path compiledAbsolute = TrackedPathResolver.resolveTrackedPath(
                    projectRoot,
                    compiledPath,
                    record.getResolvedTargetRoot()
            );
            qualifiedPaths.add(resolver.qualifyAbsolute(compiledAbsolute));
        }
        qualifiedPaths.sort(String::compareTo);
        result.setListedPaths(qualifiedPaths);

        List<String> excluded = new ArrayList<>(listing.getExcludedPaths());
        excluded.sort(String::compareTo);
        result.setExcludedPaths(excluded);
        if (!excluded.isEmpty()) {
            result.setExclusionReason("Excluded compiled paths outside active output base scope.");
        }
        return result;
    }

    private CompiledFileListing buildCompiledFileListing(SourceTrackingRecord record, Path projectRoot, Path allowedBase) {
        java.util.Set<String> compiledPaths = record.getCompiledFiles() != null
                ? record.getCompiledFiles().keySet() : java.util.Set.of();
        List<String> existingPaths = new ArrayList<>();
        List<String> excludedPaths = new ArrayList<>();
        int staleCount = 0;
        Path allowedRoot = allowedBase == null ? null : allowedBase.toAbsolutePath().normalize();
        for (String compiledPath : compiledPaths) {
            if (compiledPath == null || compiledPath.isBlank()) {
                staleCount++;
                continue;
            }

            Path absolutePath = TrackedPathResolver.resolveTrackedPath(
                    projectRoot,
                    compiledPath,
                    record.getResolvedTargetRoot()
            );
            if (allowedRoot != null && !absolutePath.startsWith(allowedRoot)) {
                excludedPaths.add(compiledPath);
                continue;
            }
            if (Files.exists(absolutePath)) {
                existingPaths.add(compiledPath);
            } else {
                staleCount++;
            }
        }
        existingPaths.sort(Comparator.naturalOrder());
        excludedPaths.sort(Comparator.naturalOrder());
        CompiledFileListing listing = new CompiledFileListing(existingPaths);
        listing.setStaledPathsRemoved(staleCount);
        listing.setExcludedPaths(excludedPaths);
        if (!excludedPaths.isEmpty()) {
            listing.setExclusionReason("Excluded compiled paths outside active output base scope.");
        }
        return listing;
    }
}
