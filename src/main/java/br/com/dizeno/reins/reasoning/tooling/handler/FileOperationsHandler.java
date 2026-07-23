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
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePatchApplier;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FileOperationsHandler is part of the general application functions in the reins architecture.
 * Acts as a component managing file operations handler.
 */
public class FileOperationsHandler implements ToolOperationHandler {
    private final FilePatchApplier patchApplier;
    private final ScopeValidationGuard securityGuard;

    /**
     * Constructs a new instance of {@link FileOperationsHandler}.
     *
     * @param patchApplier the patch applier
     * @param securityGuard the security guard
     */
    public FileOperationsHandler(FilePatchApplier patchApplier, ScopeValidationGuard securityGuard) {
        this.patchApplier = patchApplier;
        this.securityGuard = securityGuard;
    }

    /**
     * Supports.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean supports(ToolExecutionRequest.Operation operation) {
        return operation == ToolExecutionRequest.Operation.LIST_FILES
                || operation == ToolExecutionRequest.Operation.READ_FILE
                || operation == ToolExecutionRequest.Operation.WRITE_FILE
                || operation == ToolExecutionRequest.Operation.PATCH_FILE
                || operation == ToolExecutionRequest.Operation.DELETE_FILE;
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
            return switch (request.getOperation()) {
                case LIST_FILES -> listFilesWithScope(request, resolver, qualifiedPath, sourceScope);
                case READ_FILE -> readFileWithScope(request, resolver, qualifiedPath, sourceScope);
                case WRITE_FILE -> writeFileWithScope(request, resolver, qualifiedPath, sourceScope);
                case PATCH_FILE -> patchFileWithScope(request, resolver, qualifiedPath, sourceScope);
                case DELETE_FILE -> deleteFileWithScope(request, resolver, qualifiedPath, sourceScope);
                default -> throw new IllegalArgumentException("Unsupported file operation: " + request.getOperation());
            };
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

    private ToolExecutionResult listFiles(ToolExecutionRequest request,
                                          BasePathResolver resolver,
                                          String qualifiedPath) throws IOException {
        Path root = resolver.resolve(request.getBase(), request.getPath());
        if (!Files.exists(root)) {
            ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "listed");
            result.setListedPaths(new ArrayList<>());
            return result;
        }

        List<String> listed = new ArrayList<>();
        boolean hasSubPath = request.getPath() != null && !request.getPath().isEmpty() && !".".equals(request.getPath());
        int maxDepth = (request.isRecursive() || hasSubPath) ? Integer.MAX_VALUE : 1;
        try (var stream = Files.walk(root, maxDepth)) {
            stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        String relative = resolver.relativize(request.getBase(), path);
                        listed.add(relative);
                    });
        }

        ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "listed");
        result.setListedPaths(listed);
        return result;
    }

    private ToolExecutionResult readFile(ToolExecutionRequest request,
                                         BasePathResolver resolver,
                                         String qualifiedPath) throws IOException {
        String requestedBase = resolver.normalizeBase(request.getBase());
        Path primary = resolver.resolve(requestedBase, request.getPath());
        if (Files.exists(primary)) {
            ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "read");
            result.setContent(Files.readString(primary, StandardCharsets.UTF_8));
            result.setResolvedBase(requestedBase);
            return result;
        }

        if ("test".equals(requestedBase)) {
            Path fallback = resolver.resolve("main", request.getPath());
            if (Files.exists(fallback)) {
                ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "read");
                result.setContent(Files.readString(fallback, StandardCharsets.UTF_8));
                result.setResolvedBase("main");
                return result;
            }
            return ToolExecutionResult.error(
                    request.getOperation(),
                    qualifiedPath,
                    "File does not exist in test or main base."
            );
        }

        ToolExecutionResult error = ToolExecutionResult.error(request.getOperation(), qualifiedPath, "File does not exist.");
        error.setResolvedBase(requestedBase);
        return error;
    }

    private ToolExecutionResult listFilesWithScope(ToolExecutionRequest request,
                                                   BasePathResolver resolver,
                                                   String qualifiedPath,
                                                   String sourceScope) throws IOException {
        if (!"test".equals(sourceScope)) {
            return listFiles(request, resolver, qualifiedPath);
        }

        BasePathMappingSet mappings = resolver.getMappings();
        if (mappings == null || !mappings.isCompositionEnabled(sourceScope)) {
            return listFiles(request, resolver, qualifiedPath);
        }

        Set<String> mergedPaths = new LinkedHashSet<>();
        boolean hasSubPath = request.getPath() != null && !request.getPath().isEmpty() && !".".equals(request.getPath());
        int maxDepth = (request.isRecursive() || hasSubPath) ? Integer.MAX_VALUE : 1;

        Path[] composedBases = "target".equals(request.getBase())
                ? mappings.getComposedTargetBases(sourceScope)
                : mappings.getComposedSourceBases(sourceScope);

        for (Path base : composedBases) {
            Path baseRoot = base;
            if (request.getPath() != null && !request.getPath().isEmpty() && !".".equals(request.getPath())) {
                baseRoot = base.resolve(request.getPath()).normalize();
            }

            if (!Files.exists(baseRoot)) {
                continue;
            }

            try (var stream = Files.walk(baseRoot, maxDepth)) {
                stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(Path::toString))
                        .forEach(path -> {
                            String relative = base.relativize(path).toString().replace("\\", "/");
                            String baseLabel = getBaseLabelForPath(base, mappings);
                            String qualified = baseLabel + ":" + relative;
                            mergedPaths.add(qualified);
                        });
            }
        }

        if (mergedPaths.isEmpty() && "target".equals(request.getBase()) && hasSubPath) {
            String normalizedPrefix = request.getPath().replace("\\", "/");
            final String prefix = normalizedPrefix.endsWith("/") ? normalizedPrefix : normalizedPrefix + "/";
            for (Path base : composedBases) {
                if (base == null || !Files.exists(base)) {
                    continue;
                }
                try (var stream = Files.walk(base, Integer.MAX_VALUE)) {
                    stream.filter(Files::isRegularFile)
                            .sorted(Comparator.comparing(Path::toString))
                            .forEach(path -> {
                                String relative = base.relativize(path).toString().replace("\\", "/");
                                if (relative.startsWith(prefix)) {
                                    String baseLabel = getBaseLabelForPath(base, mappings);
                                    mergedPaths.add(baseLabel + ":" + relative);
                                }
                            });
                }
            }
        }

        ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "listed");
        result.setListedPaths(new ArrayList<>(mergedPaths));
        return result;
    }

    private ToolExecutionResult readFileWithScope(ToolExecutionRequest request,
                                                  BasePathResolver resolver,
                                                  String qualifiedPath,
                                                  String sourceScope) throws IOException {
        String requestedBase = resolver.normalizeBase(request.getBase());

        if (isExplicitlyQualified(requestedBase)) {
            return readFile(request, resolver, qualifiedPath);
        }

        if (!"test".equals(sourceScope)) {
            return readFile(request, resolver, qualifiedPath);
        }

        BasePathMappingSet mappings = resolver.getMappings();
        if (mappings == null || !mappings.isCompositionEnabled(sourceScope)) {
            return readFile(request, resolver, qualifiedPath);
        }

        List<String> candidates = "target".equals(request.getBase())
                ? resolver.findComposedTargetCandidates(request.getPath(), sourceScope)
                : resolver.findComposedSourceCandidates(request.getPath(), sourceScope);

        if (candidates.isEmpty()) {
            return ToolExecutionResult.error(request.getOperation(), qualifiedPath,
                    "File does not exist in any composed base (test or main).");
        }

        if (candidates.size() == 1) {
            String qualified = candidates.get(0);
            String[] parts = qualified.split(":", 2);
            if (parts.length == 2) {
                Path file = resolveComposedCandidatePath(parts[0], parts[1], resolver, sourceScope);
                if (Files.exists(file)) {
                    ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualified, "read");
                    result.setContent(Files.readString(file, StandardCharsets.UTF_8));
                    result.setResolvedBase(parts[0]);
                    return result;
                }
            }
        }

        if ("target".equals(request.getBase())) {
            String qualified = candidates.get(0);
            String[] parts = qualified.split(":", 2);
            if (parts.length == 2) {
                Path file = resolveComposedCandidatePath(parts[0], parts[1], resolver, sourceScope);
                if (Files.exists(file)) {
                    ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualified, "read");
                    result.setContent(Files.readString(file, StandardCharsets.UTF_8));
                    result.setResolvedBase("target");
                    return result;
                }
            }
        }

        ToolExecutionResult result = ToolExecutionResult.error(request.getOperation(), qualifiedPath,
                "Ambiguous path exists in multiple composed bases. Retry with explicit base prefix.");
        result.setConflictCandidates(candidates);
        String conflictMsg = candidates.size() + " matches found: " + String.join(", ", candidates) +
                ". Retry with explicit qualified path (e.g., '" + candidates.get(0) + "')";
        result.setConflictMessage(conflictMsg);
        return result;
    }

    private String getBaseLabelForPath(Path absolutePath, BasePathMappingSet mappings) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        if (mappings.getTestTargetRoot() != null
                && normalized.equals(mappings.getTestTargetRoot().toAbsolutePath().normalize())) {
            return "target";
        }
        if (mappings.getMainTargetRoot() != null
                && normalized.equals(mappings.getMainTargetRoot().toAbsolutePath().normalize())) {
            return "target";
        }
        if (mappings.getMainRoot() != null && normalized.equals(mappings.getMainRoot().toAbsolutePath().normalize())) {
            return "main";
        }
        if (mappings.getTestRoot() != null && normalized.equals(mappings.getTestRoot().toAbsolutePath().normalize())) {
            return "test";
        }
        return "unknown";
    }

    private boolean isExplicitlyQualified(String base) {
        return base != null && base.contains(":");
    }

    private Path resolveComposedCandidatePath(String baseLabel,
                                              String relativePath,
                                              BasePathResolver resolver,
                                              String sourceScope) {
        BasePathMappingSet mappings = resolver.getMappings();
        if ("target".equals(baseLabel)) {
            Path[] composed = mappings.getComposedTargetBases(sourceScope);
            for (Path root : composed) {
                if (root == null) {
                    continue;
                }
                Path candidate = root.resolve(relativePath).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
            return securityGuard.resolveScopedTargetPath(resolver, relativePath, sourceScope);
        }
        return resolver.resolve(baseLabel, relativePath);
    }

    private ToolExecutionResult writeFileWithScope(ToolExecutionRequest request,
                                                   BasePathResolver resolver,
                                                   String qualifiedPath,
                                                   String sourceScope) throws IOException {
        Path file = securityGuard.resolveScopedTargetPath(resolver, request.getPath(), sourceScope);
        if (request.isCreateParents()) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, request.getContent(), StandardCharsets.UTF_8);
        return ToolExecutionResult.success(request.getOperation(), qualifiedPath, "written");
    }

    private ToolExecutionResult patchFileWithScope(ToolExecutionRequest request,
                                                   BasePathResolver resolver,
                                                   String qualifiedPath,
                                                   String sourceScope) throws IOException {
        Path file = securityGuard.resolveScopedTargetPath(resolver, request.getPath(), sourceScope);
        if (!Files.exists(file)) {
            return ToolExecutionResult.error(request.getOperation(), qualifiedPath, "File does not exist.");
        }

        String current = Files.readString(file, StandardCharsets.UTF_8);
        String patched;
        if (request.getAtLine() == null) {
            patched = request.getContent();
        } else {
            patched = patchApplier.apply(
                    current,
                    request.getAtLine(),
                    request.getReplacing() != null ? request.getReplacing() : 0,
                    request.getContent()
            );
        }
        Files.writeString(file, patched, StandardCharsets.UTF_8);

        ToolExecutionResult result = ToolExecutionResult.success(request.getOperation(), qualifiedPath, "patched");
        result.setContent(patched);
        return result;
    }

    private ToolExecutionResult deleteFileWithScope(ToolExecutionRequest request,
                                                    BasePathResolver resolver,
                                                    String qualifiedPath,
                                                    String sourceScope) throws IOException {
        Path file = securityGuard.resolveScopedTargetPath(resolver, request.getPath(), sourceScope);
        if (!Files.exists(file)) {
            return ToolExecutionResult.error(request.getOperation(), qualifiedPath, "File does not exist.");
        }
        Files.delete(file);
        return ToolExecutionResult.success(request.getOperation(), qualifiedPath, "deleted");
    }
}
