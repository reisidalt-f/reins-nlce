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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionType;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ScopeValidationGuard is part of the general application functions in the reins architecture.
 * Acts as a component managing scope validation guard.
 */
public class ScopeValidationGuard {
    public static final String TARGET_BASE_REQUIRED_MESSAGE =
            "Write operations are allowed only under the configured target base.";

    /**
     * Validates the inputs or files permissions.
     *
     * @param request the request containing path and scope metadata
     * @param permission the permission
     * @return the string result
     */
    public String validatePermissions(ToolExecutionRequest request, FilePolicy permission) {
        if (permission != null) {
            if (request.getOperation() == ToolExecutionRequest.Operation.ADD_REASONING_NOTE
                    || request.getOperation() == ToolExecutionRequest.Operation.CLEAR_REASONING_NOTES) {
                if (!permission.isAddReasoningNotes()) {
                    String operationName = request.getOperation().name().toLowerCase();
                    return "Operation " + operationName + " is disabled (tooling.addReasoningNotes is false).";
                }
            }
            if (request.getOperation() == ToolExecutionRequest.Operation.COPY_FILE) {
                if (request.getBase() != null) {
                    FilePolicy.Base originBase = parseBaseFromRequest(request.getBase());
                    if (originBase != null && !permission.isOperationAllowed(ToolExecutionType.READ_FILE, originBase)) {
                        return "Operation copy_file is not permitted on base " + originBase.name().toLowerCase() + " (requires token: read)";
                    }
                }
                if (!permission.isOperationAllowed(ToolExecutionType.COPY_FILE, FilePolicy.Base.TARGET)) {
                    return "Operation copy_file is not permitted on base target (requires token: copy)";
                }
            } else if (request.getBase() != null) {
                ToolExecutionType operationType = ToolExecutionType.fromOperation(request.getOperation());
                if (operationType != null) {
                    FilePolicy.Base base = parseBaseFromRequest(request.getBase());
                    if (base != null && !permission.isOperationAllowed(operationType, base)) {
                        String operationName = request.getOperation().name().toLowerCase();
                        String baseName = base.name().toLowerCase();
                        String reason = "Operation " + operationName + " is not permitted on base " + baseName;
                        FilePolicy.OperationToken requiredToken = operationType.getRequiredToken();
                        if (requiredToken != null) {
                            reason += " (requires token: " + requiredToken.getValue() + ")";
                        }
                        return reason;
                    }
                }
            }
        }
        return null;
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
        if (!isMutationOperation(request.getOperation())) {
            return null;
        }

        if (request.getOperation() == ToolExecutionRequest.Operation.COPY_FILE) {
            Path destCandidate = resolveScopedTargetPath(resolver, request.getDestination(), sourceScope);
            if (!isWithinAnyTargetBase(destCandidate, resolver.getMappings())) {
                return TARGET_BASE_REQUIRED_MESSAGE;
            }
            BasePathMappingSet mappings = resolver.getMappings();
            Path targetRoot = mappings.getActiveTargetRoot(sourceScope);
            if (targetRoot != null && !destCandidate.startsWith(targetRoot)) {
                String errorMsg;
                if ("test".equals(sourceScope)) {
                    errorMsg = "Mutation during test-source processing must target the active target output root.";
                } else if ("main".equals(sourceScope)) {
                    errorMsg = "Mutation during main-source processing must target the active target output root.";
                } else {
                    errorMsg = "Mutation must be within target base.";
                }
                return errorMsg;
            }
            return null;
        }

        String normalizedBase = resolver.normalizeBase(request.getBase());
        if (!"target".equals(normalizedBase)) {
            return TARGET_BASE_REQUIRED_MESSAGE;
        }

        Path candidate = resolveScopedTargetPath(resolver, request.getPath(), sourceScope);
        if (!isWithinAnyTargetBase(candidate, resolver.getMappings())) {
            return TARGET_BASE_REQUIRED_MESSAGE;
        }

        if ("test".equals(sourceScope) || "main".equals(sourceScope)) {
            Path opposite = resolveOppositeScopedTargetPath(resolver, request.getPath(), sourceScope);
            boolean isWriteLike = request.getOperation() == ToolExecutionRequest.Operation.WRITE_FILE
                    || request.getOperation() == ToolExecutionRequest.Operation.APPEND_FILE
                    || request.getOperation() == ToolExecutionRequest.Operation.PREPEND_FILE;
            if (opposite != null && Files.exists(opposite) && !Files.exists(candidate) && !isWriteLike) {
                return "Scope violation: mutation targets only the active scope output base.";
            }
        }

        BasePathMappingSet mappings = resolver.getMappings();
        Path targetRoot = mappings.getActiveTargetRoot(sourceScope);
        if (targetRoot != null && !candidate.startsWith(targetRoot)) {
            String errorMsg;
            if ("test".equals(sourceScope)) {
                errorMsg = "Mutation during test-source processing must target the active target output root.";
            } else if ("main".equals(sourceScope)) {
                errorMsg = "Mutation during main-source processing must target the active target output root.";
            } else {
                errorMsg = "Mutation must be within target base.";
            }
            return errorMsg;
        }

        if (request.getOperation() == ToolExecutionRequest.Operation.MOVE_FILE) {
            Path destCandidate = resolveScopedTargetPath(resolver, request.getDestination(), sourceScope);
            if (!isWithinAnyTargetBase(destCandidate, resolver.getMappings())) {
                return TARGET_BASE_REQUIRED_MESSAGE;
            }
            if (targetRoot != null && !destCandidate.startsWith(targetRoot)) {
                String errorMsg;
                if ("test".equals(sourceScope)) {
                    errorMsg = "Mutation during test-source processing must target the active target output root.";
                } else if ("main".equals(sourceScope)) {
                    errorMsg = "Mutation during main-source processing must target the active target output root.";
                } else {
                    errorMsg = "Mutation must be within target base.";
                }
                return errorMsg;
            }
        }

        return null;
    }

    /**
     * Resolves the configured value or path scoped target path.
     *
     * @param resolver the resolver
     * @param relativePath the relative path
     * @param sourceScope the source scope
     * @return the resolved or constructed object
     */
    public Path resolveScopedTargetPath(BasePathResolver resolver, String relativePath, String sourceScope) {
        BasePathMappingSet mappings = resolver.getMappings();
        Path targetRoot = mappings.getActiveTargetRoot(sourceScope);
        if (targetRoot == null) {
            targetRoot = mappings.getTargetRoot();
        }
        if (targetRoot == null) {
            throw new IllegalStateException("Target base is not configured.");
        }

        String normalizedRelative = relativePath == null ? "" : relativePath;
        Path projectRoot = resolver.getProjectRoot().toAbsolutePath().normalize();
        Path absoluteTargetRoot = targetRoot.toAbsolutePath().normalize();

        // If the agent included the target-root prefix inside the relative path, strip it.
        // Example: targetRoot = "src/main/nl", relativePath = "src/main/nl/br/com/..."
        // → strip to "br/com/..." so the final path is not duplicated.
        if (!absoluteTargetRoot.equals(projectRoot)) {
            String targetRootRelative = projectRoot.relativize(absoluteTargetRoot)
                    .toString().replace('\\', '/');
            String relFwd = normalizedRelative.replace('\\', '/');
            if (relFwd.startsWith(targetRootRelative + "/")) {
                normalizedRelative = relFwd.substring(targetRootRelative.length() + 1);
            } else if (relFwd.equals(targetRootRelative)) {
                normalizedRelative = "";
            }
        }

        Path candidate = absoluteTargetRoot.resolve(normalizedRelative).normalize();
        if (!candidate.startsWith(absoluteTargetRoot) || !candidate.startsWith(projectRoot)) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
        return candidate;
    }

    /**
     * Resolves the configured value or path opposite scoped target path.
     *
     * @param resolver the resolver
     * @param relativePath the relative path
     * @param sourceScope the source scope
     * @return the resolved or constructed object
     */
    public Path resolveOppositeScopedTargetPath(BasePathResolver resolver, String relativePath, String sourceScope) {
        BasePathMappingSet mappings = resolver.getMappings();
        Path root;
        if ("test".equals(sourceScope)) {
            root = mappings.getMainTargetRoot();
        } else if ("main".equals(sourceScope)) {
            root = mappings.getTestTargetRoot();
        } else {
            return null;
        }
        if (root == null) {
            return null;
        }
        Path candidate = root.resolve(relativePath == null ? "" : relativePath).normalize();
        Path projectRoot = resolver.getProjectRoot().toAbsolutePath().normalize();
        if (!candidate.startsWith(root.toAbsolutePath().normalize()) || !candidate.startsWith(projectRoot)) {
            return null;
        }
        return candidate;
    }

    /**
     * Checks if the component is within any target base.
     *
     * @param candidate the candidate
     * @param mappings the mappings
     * @return true if successful or matching, false otherwise
     */
    public boolean isWithinAnyTargetBase(Path candidate, BasePathMappingSet mappings) {
        Path normalized = candidate.toAbsolutePath().normalize();
        if (mappings.getTargetRoot() != null && normalized.startsWith(mappings.getTargetRoot().toAbsolutePath().normalize())) {
            return true;
        }
        if (mappings.getMainTargetRoot() != null && normalized.startsWith(mappings.getMainTargetRoot().toAbsolutePath().normalize())) {
            return true;
        }
        return mappings.getTestTargetRoot() != null
                && normalized.startsWith(mappings.getTestTargetRoot().toAbsolutePath().normalize());
    }

    /**
     * Checks if the component is mutation operation.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    public boolean isMutationOperation(ToolExecutionRequest.Operation operation) {
        return operation == ToolExecutionRequest.Operation.WRITE_FILE
                || operation == ToolExecutionRequest.Operation.PATCH_FILE
                || operation == ToolExecutionRequest.Operation.DELETE_FILE
                || operation == ToolExecutionRequest.Operation.APPEND_FILE
                || operation == ToolExecutionRequest.Operation.PREPEND_FILE
                || operation == ToolExecutionRequest.Operation.MOVE_FILE
                || operation == ToolExecutionRequest.Operation.COPY_FILE;
    }

    public FilePolicy.Base parseBaseFromRequest(String baseString) {
        if (baseString == null || baseString.isBlank()) {
            return null;
        }
        try {
            return FilePolicy.Base.valueOf(baseString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
