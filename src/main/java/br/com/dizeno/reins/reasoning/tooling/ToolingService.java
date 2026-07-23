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

package br.com.dizeno.reins.reasoning.tooling;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePatchApplier;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.tooling.handler.ToolOperationHandler;
import br.com.dizeno.reins.reasoning.tooling.handler.ScopeValidationGuard;
import br.com.dizeno.reins.reasoning.tooling.handler.FileOperationsHandler;
import br.com.dizeno.reins.reasoning.tooling.handler.ScriptRunnerHandler;
import br.com.dizeno.reins.reasoning.tooling.handler.ReasoningNotesHandler;
import br.com.dizeno.reins.reasoning.tooling.handler.CompiledFileRegistryHandler;

import java.util.List;
import java.util.Set;

/**
 * ToolingService is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ToolingService {
    public static final String TARGET_BASE_REQUIRED_MESSAGE =
            ScopeValidationGuard.TARGET_BASE_REQUIRED_MESSAGE;

    private final ScopeValidationGuard securityGuard;
    private final ReasoningNotesHandler inferenceNotesHandler;
    private final List<ToolOperationHandler> handlers;

    /**
     * Constructs a new instance of {@link ToolingService}.
     */
    public ToolingService() {
        this(new FilePatchApplier());
    }

    /**
     * Constructs a new instance of {@link ToolingService}.
     *
     * @param patchApplier the patch applier
     */
    public ToolingService(FilePatchApplier patchApplier) {
        this(patchApplier, new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link ToolingService}.
     *
     * @param patchApplier the patch applier
     * @param trackingStore the persistence store for file tracking records
     */
    public ToolingService(FilePatchApplier patchApplier, CompilationTrackingStore trackingStore) {
        this.securityGuard = new ScopeValidationGuard();
        SourceTrackingManager trackingManager = new SourceTrackingManager();
        this.inferenceNotesHandler = new ReasoningNotesHandler(trackingStore, trackingManager);
        this.handlers = List.of(
                new FileOperationsHandler(patchApplier, securityGuard),
                new ScriptRunnerHandler(),
                this.inferenceNotesHandler,
                new CompiledFileRegistryHandler(trackingStore)
        );
    }

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @return the resulting result
     */
    public ToolExecutionResult execute(ToolExecutionRequest request,
                                       BasePathResolver resolver) {
        return execute(request, resolver, null, null);
    }

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param permission the permission
     * @return the resulting result
     */
    public ToolExecutionResult execute(ToolExecutionRequest request,
                                       BasePathResolver resolver,
                                       FilePolicy permission) {
        return execute(request, resolver, permission, null);
    }

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param permission the permission
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    public ToolExecutionResult execute(ToolExecutionRequest request,
                                       BasePathResolver resolver,
                                       FilePolicy permission,
                                       ScriptRunnerConfig scriptRunnerConfig) {
        return executeWithScope(request, resolver, null, permission, scriptRunnerConfig);
    }

    /**
     * Executes the operation with scope.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @return the resulting result
     */
    public ToolExecutionResult executeWithScope(ToolExecutionRequest request,
                                                BasePathResolver resolver,
                                                String sourceScope) {
        return executeWithScope(request, resolver, sourceScope, null, null);
    }

    /**
     * Executes the operation with scope.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @param permission the permission
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    public ToolExecutionResult executeWithScope(ToolExecutionRequest request,
                                                BasePathResolver resolver,
                                                String sourceScope,
                                                FilePolicy permission,
                                                ScriptRunnerConfig scriptRunnerConfig) {
        String qualifiedPath = safeQualify(request, resolver);
        try {
            
            String permissionViolation = securityGuard.validatePermissions(request, permission);
            if (permissionViolation != null) {
                return ToolExecutionResult.error(request.getOperation(), request.getPath(), permissionViolation);
            }

            
            String scopeViolation = securityGuard.validateMutationScope(request, resolver, sourceScope);
            if (scopeViolation != null) {
                return ToolExecutionResult.scopeViolation(request.getOperation(), qualifiedPath, scopeViolation);
            }

            for (ToolOperationHandler handler : handlers) {
                if (handler.supports(request.getOperation())) {
                    return handler.execute(request, resolver, sourceScope, scriptRunnerConfig);
                }
            }

            return ToolExecutionResult.error(request.getOperation(), request.getPath(), "Unsupported operation: " + request.getOperation());
        } catch (Exception ex) {
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

    /**
     * Validates the inputs or files mutation scope.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @return the string result
     */
    public String validateMutationScope(ToolExecutionRequest request, BasePathResolver resolver, String sourceScope) {
        return securityGuard.validateMutationScope(request, resolver, sourceScope);
    }

    /**
     * Validates the inputs or files mutation scope.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @return the string result
     */
    public String validateMutationScope(ToolExecutionRequest request, BasePathResolver resolver) {
        return securityGuard.validateMutationScope(request, resolver, null);
    }

    /**
     * Gets the and clear noted source paths.
     *
     * @return the string result
     */
    public Set<String> getAndClearNotedSourcePaths() {
        return inferenceNotesHandler.getAndClearNotedSourcePaths();
    }
}
