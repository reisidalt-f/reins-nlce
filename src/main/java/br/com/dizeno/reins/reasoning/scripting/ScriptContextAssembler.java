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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningCycle;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * ScriptContextAssembler is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing script context assembler.
 */
public class ScriptContextAssembler {
    private final ScriptEvaluator scriptEvaluator;
    private final ReasoningScriptContextFactory scriptContextFactory;

    /**
     * Constructs a new instance of {@link ScriptContextAssembler}.
     *
     * @param scriptEvaluator the script evaluator instance
     * @param scriptContextFactory the script context factory
     */
    public ScriptContextAssembler(ScriptEvaluator scriptEvaluator,
                                  ReasoningScriptContextFactory scriptContextFactory) {
        this.scriptEvaluator = scriptEvaluator;
        this.scriptContextFactory = scriptContextFactory;
    }

    /**
     * Builds the configured target script base context.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param cycle the cycle
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param referenceTree the reference tree
     * @param inspectedPaths the inspected paths
     * @param compiledPaths the compiled paths
     * @param attachments the list of attachments
     * @param isProjectCycle the is project cycle
     * @return the resulting context
     */
    public ReasoningScriptContext buildScriptBaseContext(ReasoningRequest request,
                                                         ReinsConfig config,
                                                         ReasoningCycle cycle,
                                                         FilePolicy policy,
                                                         boolean scriptRunnerEnabled,
                                                         String referenceTree,
                                                         List<String> inspectedPaths,
                                                         List<String> compiledPaths,
                                                         List<AttachedFilePayload> attachments,
                                                         boolean isProjectCycle) {
        if (scriptEvaluator == null || scriptContextFactory == null) {
            return null;
        }
        try {
            return scriptContextFactory.buildBase(
                    request,
                    config,
                    cycle,
                    policy,
                    scriptRunnerEnabled,
                    referenceTree,
                    inspectedPaths == null ? List.of() : inspectedPaths,
                    compiledPaths == null ? List.of() : compiledPaths,
                    toScriptAttachments(attachments),
                    isProjectCycle);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<ReasoningScriptViews.AttachmentView> toScriptAttachments(List<AttachedFilePayload> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        List<ReasoningScriptViews.AttachmentView> views = new ArrayList<>(attachments.size());
        for (AttachedFilePayload attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String path = attachment.getQualifiedPath();
            if (path == null || path.isBlank()) {
                path = (attachment.getBase() == null ? "main" : attachment.getBase())
                        + ":"
                        + (attachment.getRelativePath() == null ? "" : attachment.getRelativePath());
            }
            views.add(new ReasoningScriptViews.AttachmentView(
                    path,
                    attachment.getContent() == null ? "" : attachment.getContent(),
                    "text/plain"));
        }
        return views;
    }

    /**
     * Gets the script context factory.
     *
     * @return the resolved or constructed object
     */
    public ReasoningScriptContextFactory getScriptContextFactory() {
        return scriptContextFactory;
    }
}
