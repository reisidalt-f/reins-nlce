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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.service.MessageFormattingService;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ReasoningAttachmentManagementService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ReasoningAttachmentManagementService {
    private final ReferencedAttachmentBuilder referencedAttachmentBuilder;
    private final MessageFormattingService messageFormattingService;
    private final ReasoningCycleStateManager cycleStateManager;

    /**
     * Constructs a new instance of {@link ReasoningAttachmentManagementService}.
     *
     * @param referencedAttachmentBuilder the referenced attachment builder
     * @param messageFormattingService the message formatting service
     * @param cycleStateManager the cycle state manager
     */
    public ReasoningAttachmentManagementService(ReferencedAttachmentBuilder referencedAttachmentBuilder,
                                                MessageFormattingService messageFormattingService,
                                                ReasoningCycleStateManager cycleStateManager) {
        this.referencedAttachmentBuilder = referencedAttachmentBuilder;
        this.messageFormattingService = messageFormattingService;
        this.cycleStateManager = cycleStateManager;
    }

    /**
     * Builds the configured target first turn attachments.
     *
     * @param request the request containing path and scope metadata
     * @param sourceAttachment the source attachment
     * @param firstTurnReferenceTreeContext the first turn reference tree context
     * @param projectRoot the root path of the project
     * @param config the Reins configuration settings
     * @param cycleLog the reasoning cycle log instance
     * @return the collection of elements
     */
    public List<AttachedFilePayload> buildFirstTurnAttachments(ReasoningRequest request,
                                                                AttachedFilePayload sourceAttachment,
                                                                ReferenceTreeContextService.ReferenceTreeContext firstTurnReferenceTreeContext,
                                                                Path projectRoot,
                                                                ReinsConfig config,
                                                                ReasoningCycleLog cycleLog) {
        if (sourceAttachment == null) {
            return List.of();
        }

        List<AttachedFilePayload> attachments = new ArrayList<>();
        CompilationBackgroundPayload compilationBackgroundPayload = request.getCompilationBackgroundPayload();
        EagerlyProvideResult eagerlyProvide = request.getEagerlyProvide();

        if (eagerlyProvide != null) {
            attachments.addAll(eagerlyProvide.getCompiledAttachments());
            attachments.addAll(eagerlyProvide.getInspectedAttachments());
        }

        if (compilationBackgroundPayload != null && !compilationBackgroundPayload.isEmpty()) {
            for (CompilationBackgroundFile ctxFile : compilationBackgroundPayload.getFiles()) {
                AttachedFilePayload ctxAttachment = new AttachedFilePayload();
                String qualifiedContextPath = messageFormattingService.canonicalizeContextAttachmentPath(ctxFile.getDisplayPath());
                int separator = qualifiedContextPath.indexOf(':');
                ctxAttachment.setBase(separator > 0 ? qualifiedContextPath.substring(0, separator) : "main");
                ctxAttachment.setRelativePath(ctxFile.getDisplayPath());
                ctxAttachment.setQualifiedPath(qualifiedContextPath);
                ctxAttachment.setContent(ctxFile.getContent());
                attachments.add(ctxAttachment);
            }
        }

        attachments.add(sourceAttachment);

        boolean attachReferencedFiles = config.getContext() == null || config.getContext().isAttachReferencedFiles();
        if (attachReferencedFiles) {
            List<AttachedFilePayload> referencedAttachments = referencedAttachmentBuilder.build(
                    firstTurnReferenceTreeContext,
                    projectRoot);
            attachments.addAll(referencedAttachments);
        } else if (cycleLog != null) {
            cycleStateManager.writeLog(cycleLog, ReasoningLogEntry.Direction.OUTBOUND, "system", 1,
                    "lifecycle: referenced-source-attachments-disabled\n"
                            + "context.attachReferencedFiles=false");
        }

        return attachments;
    }

    /**
     * Merge Attachments.
     *
     * @param primary the primary
     * @param secondary the secondary
     * @return the collection of elements
     */
    public List<AttachedFilePayload> mergeAttachments(List<AttachedFilePayload> primary,
                                                       List<AttachedFilePayload> secondary) {
        List<AttachedFilePayload> merged = new ArrayList<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (secondary != null) {
            merged.addAll(secondary);
        }
        return merged;
    }

    /**
     * Same Source Request.
     *
     * @param sourceAttachment the source attachment
     * @param operationRequest the operation request
     * @param resolver the resolver
     * @return true if successful or matching, false otherwise
     */
    public boolean sameSourceRequest(AttachedFilePayload sourceAttachment,
                                     ToolExecutionRequest operationRequest,
                                     BasePathResolver resolver) {
        if (operationRequest.getOperation() != ToolExecutionRequest.Operation.READ_FILE) {
            return false;
        }
        try {
            String reqBase = resolver.normalizeBase(operationRequest.getBase());
            String reqPath = operationRequest.getPath() == null ? "" : operationRequest.getPath().trim();
            String srcBase = resolver.normalizeBase(sourceAttachment.getBase());
            String srcPath = sourceAttachment.getRelativePath() == null ? "" : sourceAttachment.getRelativePath().trim();
            return reqBase.equals(srcBase) && reqPath.equals(srcPath);
        } catch (Exception ex) {
            return false;
        }
    }
}
