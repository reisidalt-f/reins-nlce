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

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.PathNormalizer;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * AttachmentNormalizer is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing attachment normalizer.
 */
public class AttachmentNormalizer {
    private final Function<BasePathMappingSet, BasePathResolver> resolverFactory;

    /**
     * Constructs a new instance of {@link AttachmentNormalizer}.
     */
    public AttachmentNormalizer() {
        this(m -> new BasePathResolver(m, new PathValidator(inferProjectRoot(m))));
    }

    /**
     * Constructs a new instance of {@link AttachmentNormalizer}.
     *
     * @param resolverFactory the resolver factory
     */
    public AttachmentNormalizer(Function<BasePathMappingSet, BasePathResolver> resolverFactory) {
        this.resolverFactory = resolverFactory;
    }
    /**
     * NormalizationOutcome is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing normalization outcome.
     */
    public static class NormalizationOutcome {
        private final List<AttachedFilePayload> attachments;
        private final List<ToolExecutionResult.CompiledFileStatus> compiledFileStatuses;

        /**
         * Constructs a new instance of {@link NormalizationOutcome}.
         *
         * @param attachments the list of attachments
         * @param compiledFileStatuses the compiled file statuses
         */
        public NormalizationOutcome(List<AttachedFilePayload> attachments,
                                    List<ToolExecutionResult.CompiledFileStatus> compiledFileStatuses) {
            this.attachments = attachments;
            this.compiledFileStatuses = compiledFileStatuses;
        }

        /**
         * Gets the attachments.
         *
         * @return the collection of elements
         */
        public List<AttachedFilePayload> getAttachments() {
            return attachments;
        }

        public List<ToolExecutionResult.CompiledFileStatus> getCompiledFileStatuses() {
            return compiledFileStatuses;
        }
    }

    /**
     * ReadFileNormalizationOutcome is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing read file normalization outcome.
     */
    public static class ReadFileNormalizationOutcome {
        private final List<AttachedFilePayload> attachments;
        private final List<ToolExecutionResult.ReadFileStatus> readFileStatuses;

        /**
         * Constructs a new instance of {@link ReadFileNormalizationOutcome}.
         *
         * @param attachments the list of attachments
         * @param readFileStatuses the read file statuses
         */
        public ReadFileNormalizationOutcome(List<AttachedFilePayload> attachments,
                                            List<ToolExecutionResult.ReadFileStatus> readFileStatuses) {
            this.attachments = attachments;
            this.readFileStatuses = readFileStatuses;
        }

        /**
         * Gets the attachments.
         *
         * @return the collection of elements
         */
        public List<AttachedFilePayload> getAttachments() {
            return attachments;
        }

        public List<ToolExecutionResult.ReadFileStatus> getReadFileStatuses() {
            return readFileStatuses;
        }
    }

    /**
     * Normalize.
     *
     * @param files the list of files
     * @param mappings the mappings
     * @return the collection of elements
     */
    public List<AttachedFilePayload> normalize(List<Path> files,
                                               BasePathMappingSet mappings) throws Exception {
        List<AttachedFilePayload> normalized = new ArrayList<>();
        if (files == null) {
            return normalized;
        }
        for (Path file : files) {
            if (file == null || !Files.exists(file) || Files.isDirectory(file)) {
                continue;
            }
            AttachedFilePayload payload = new AttachedFilePayload();
            Path absolute = file.toAbsolutePath().normalize();
            if (absolute.startsWith(mappings.getMainRoot())) {
                payload.setBase("main");
                payload.setRelativePath(PathNormalizer.toForwardSlashes(mappings.getMainRoot().relativize(absolute)));
            } else if (mappings.getTestRoot() != null && absolute.startsWith(mappings.getTestRoot())) {
                payload.setBase("test");
                payload.setRelativePath(PathNormalizer.toForwardSlashes(mappings.getTestRoot().relativize(absolute)));
            } else {
                payload.setBase("target");
                payload.setRelativePath(PathNormalizer.toForwardSlashes(mappings.getTargetRoot().relativize(absolute)));
            }
            payload.setQualifiedPath(payload.getBase() + ":" + payload.getRelativePath());
            payload.setContent(Files.readString(absolute, StandardCharsets.UTF_8));
            normalized.add(payload);
        }
        return normalized;
    }

    /**
     * Normalize Qualified Paths.
     *
     * @param qualifiedPaths the qualified paths
     * @param mappings the mappings
     * @return the resolved or constructed object
     */
    public NormalizationOutcome normalizeQualifiedPaths(List<String> qualifiedPaths,
                                                        BasePathMappingSet mappings) {
        List<AttachedFilePayload> attachments = new ArrayList<>();
        List<ToolExecutionResult.CompiledFileStatus> statuses = new ArrayList<>();
        if (qualifiedPaths == null || qualifiedPaths.isEmpty()) {
            return new NormalizationOutcome(attachments, statuses);
        }

        BasePathResolver resolver = resolverFactory.apply(mappings);
        for (String qualifiedPath : qualifiedPaths) {
            String[] parsed = parseQualifiedPath(qualifiedPath);
            String base = parsed[0];
            String relative = parsed[1];
            try {
                Path absolute = resolver.resolve(base, relative);
                if (!Files.exists(absolute) || Files.isDirectory(absolute)) {
                    statuses.add(new ToolExecutionResult.CompiledFileStatus(qualifiedPath, "read_failed", "File does not exist or is a directory."));
                    continue;
                }

                byte[] bytes = Files.readAllBytes(absolute);
                if (!isLikelyText(bytes)) {
                    statuses.add(new ToolExecutionResult.CompiledFileStatus(qualifiedPath, "not_attachable", "Non-text file."));
                    continue;
                }

                AttachedFilePayload payload = new AttachedFilePayload();
                payload.setBase(base);
                payload.setRelativePath(relative);
                payload.setQualifiedPath(base + ":" + relative);
                payload.setContent(new String(bytes, StandardCharsets.UTF_8));
                attachments.add(payload);
                statuses.add(new ToolExecutionResult.CompiledFileStatus(qualifiedPath, "attached", null));
            } catch (Exception ex) {
                statuses.add(new ToolExecutionResult.CompiledFileStatus(qualifiedPath, "read_failed", ex.getMessage()));
            }
        }

        return new NormalizationOutcome(attachments, statuses);
    }

    /**
     * Normalize Read File Result.
     *
     * @param request the request containing path and scope metadata
     * @param result the result
     * @param resolver the resolver
     * @return the resolved or constructed object
     */
    public ReadFileNormalizationOutcome normalizeReadFileResult(ToolExecutionRequest request,
                                                                ToolExecutionResult result,
                                                                BasePathResolver resolver) {
        List<AttachedFilePayload> attachments = new ArrayList<>();
        List<ToolExecutionResult.ReadFileStatus> statuses = new ArrayList<>();

        if (request == null || result == null || request.getOperation() != ToolExecutionRequest.Operation.READ_FILE) {
            return new ReadFileNormalizationOutcome(attachments, statuses);
        }

        String normalizedBase = result.getResolvedBase();
        if (normalizedBase == null || normalizedBase.isBlank()) {
            normalizedBase = resolver.normalizeBase(request.getBase());
        }
        String relativePath = request.getPath() == null ? "" : request.getPath().replace('\\', '/');
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        String qualifiedPath = resolver.qualify(normalizedBase, relativePath);

        if (result.getStatus() != ToolExecutionResult.Status.SUCCESS) {
            statuses.add(new ToolExecutionResult.ReadFileStatus(
                    qualifiedPath,
                    "read_failed",
                    result.getFailureReason() == null || result.getFailureReason().isBlank()
                            ? "Read failed."
                            : result.getFailureReason()
            ));
            return new ReadFileNormalizationOutcome(attachments, statuses);
        }

        if (result.getContent() == null) {
            statuses.add(new ToolExecutionResult.ReadFileStatus(qualifiedPath, "read_failed", "Read returned null content."));
            return new ReadFileNormalizationOutcome(attachments, statuses);
        }

        AttachedFilePayload payload = new AttachedFilePayload();
        payload.setBase(normalizedBase);
        payload.setRelativePath(relativePath);
        payload.setQualifiedPath(qualifiedPath);
        payload.setContent(result.getContent());
        attachments.add(payload);
        statuses.add(new ToolExecutionResult.ReadFileStatus(qualifiedPath, "attached", null));
        return new ReadFileNormalizationOutcome(attachments, statuses);
    }

    private String[] parseQualifiedPath(String qualifiedPath) {
        if (qualifiedPath == null) {
            throw new IllegalArgumentException("Qualified path cannot be null.");
        }
        int split = qualifiedPath.indexOf(':');
        if (split <= 0) {
            throw new IllegalArgumentException("Qualified path must use '<base>:<relative path>' format: " + qualifiedPath);
        }
        String base = qualifiedPath.substring(0, split).trim().toLowerCase();
        String relative = qualifiedPath.substring(split + 1).replace('\\', '/');
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return new String[]{base, relative};
    }

    private static Path inferProjectRoot(BasePathMappingSet mappings) {
        Path targetRoot = mappings.getTargetRoot().toAbsolutePath().normalize();
        Path parent = targetRoot.getParent();
        return parent == null ? targetRoot : parent;
    }

    private boolean isLikelyText(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }
}
