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

package br.com.dizeno.reins.compilation.pipeline;

import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.reasoning.CompiledSourceGroup;
import br.com.dizeno.reins.reasoning.EagerlyProvideResult;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

 
/**
 * EagerlyProvidePhase is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public final class EagerlyProvidePhase implements CompilationPhase {

    /**
     * Executes the operation.
     *
     * @param ctx the ctx
     * @param next the next
     */
    @Override
    public void execute(SourceCompilationContext ctx, PhaseChain next) throws Exception {
        EagerlyProvideResult eagerlyProvide = buildEagerlyProvideResult(ctx);
        ctx.setEagerlyProvide(eagerlyProvide);
        next.execute(ctx);
    }

    private EagerlyProvideResult buildEagerlyProvideResult(SourceCompilationContext ctx) throws Exception {
        EagerlyProvideSettings settings = ctx.getConfig().getEagerlyProvide();
        ContextSettings contextSettings = ctx.getConfig().getContext();
        boolean eagerlyProvidedLoggingEnabled = ctx.getConfig().getLogging() != null && ctx.getConfig().getLogging().isEagerlyProvided();
        if (contextSettings == null
                || (!contextSettings.isCompiledFiles() && !contextSettings.isInspectedFiles())) {
            return ctx.getEagerlyProvideService().build(ctx.getPriorRecord(), settings, contextSettings, eagerlyProvidedLoggingEnabled, ctx.getProjectRoot(), ctx.getValidator(), ctx.getLog());
        }

        LinkedHashMap<String, AttachedFilePayload> compiledAttachments = new LinkedHashMap<>();
        LinkedHashMap<String, AttachedFilePayload> inspectedAttachments = new LinkedHashMap<>();
        LinkedHashMap<String, CompiledSourceGroup> groupedSources = new LinkedHashMap<>();

        groupedSources.put(ctx.getCanonicalSourcePath(), emptyGroupFor(ctx.getCanonicalSourcePath()));

        mergeEagerlyProvidedAttachments(
            ctx.getEagerlyProvideService().build(ctx.getPriorRecord(), settings, contextSettings, eagerlyProvidedLoggingEnabled, ctx.getProjectRoot(), ctx.getValidator(), ctx.getLog()),
                compiledAttachments,
                inspectedAttachments,
                groupedSources);

        for (String referencedSourcePath : collectReferencedTrackedSources(ctx.getRelativeSourcePath(), ctx.getGraph(), ctx.getReferenceDepthPolicy())) {
            String canonicalReferencedSourcePath = ctx.getTrackingStore().canonicalizePath(referencedSourcePath);
            if (ctx.getCanonicalSourcePath().equals(canonicalReferencedSourcePath)) {
                continue;
            }
            groupedSources.putIfAbsent(canonicalReferencedSourcePath, emptyGroupFor(canonicalReferencedSourcePath));
            SourceTrackingRecord referencedRecord = TrackingRecordHelper.sanitizeStoredTrackingRecord(
                    ctx.getTrackingStore().load(ctx.getProjectRoot(), canonicalReferencedSourcePath).orElse(null),
                    canonicalReferencedSourcePath,
                    ctx.getLog());
            if (referencedRecord == null) {
                continue;
            }
            mergeEagerlyProvidedAttachments(
                    ctx.getEagerlyProvideService().build(referencedRecord, settings, contextSettings, eagerlyProvidedLoggingEnabled, ctx.getProjectRoot(), ctx.getValidator(), ctx.getLog()),
                    compiledAttachments,
                    inspectedAttachments,
                    groupedSources);
        }

        return new EagerlyProvideResult(
                new ArrayList<>(compiledAttachments.values()),
                new ArrayList<>(inspectedAttachments.values()),
                new ArrayList<>(groupedSources.values()));
    }

    private void mergeEagerlyProvidedAttachments(EagerlyProvideResult source,
                                                  LinkedHashMap<String, AttachedFilePayload> compiledAttachments,
                                                  LinkedHashMap<String, AttachedFilePayload> inspectedAttachments,
                                                  LinkedHashMap<String, CompiledSourceGroup> groupedSources) {
        if (source == null) {
            return;
        }
        for (AttachedFilePayload attachment : source.getCompiledAttachments()) {
            compiledAttachments.putIfAbsent(attachment.getQualifiedPath(), attachment);
        }
        for (AttachedFilePayload attachment : source.getInspectedAttachments()) {
            inspectedAttachments.putIfAbsent(attachment.getQualifiedPath(), attachment);
        }
        for (CompiledSourceGroup group : source.getCompiledSourceGroups()) {
            if (group.getSourceCanonicalPath() == null) {
                continue;
            }
            CompiledSourceGroup existing = groupedSources.get(group.getSourceCanonicalPath());
            if (existing == null) {
                groupedSources.put(group.getSourceCanonicalPath(), group);
                continue;
            }
            groupedSources.put(group.getSourceCanonicalPath(), mergeSourceGroups(existing, group));
        }
    }

    private CompiledSourceGroup mergeSourceGroups(CompiledSourceGroup existing, CompiledSourceGroup incoming) {
        LinkedHashMap<String, AttachedFilePayload> merged = new LinkedHashMap<>();
        for (AttachedFilePayload attachment : existing.getCompiledAttachments()) {
            merged.putIfAbsent(attachment.getQualifiedPath(), attachment);
        }
        for (AttachedFilePayload attachment : incoming.getCompiledAttachments()) {
            merged.putIfAbsent(attachment.getQualifiedPath(), attachment);
        }
        String simpleName = incoming.getSourceSimpleName() == null || incoming.getSourceSimpleName().isBlank()
                ? existing.getSourceSimpleName()
                : incoming.getSourceSimpleName();
        return new CompiledSourceGroup(existing.getSourceCanonicalPath(), simpleName, new ArrayList<>(merged.values()));
    }

    private CompiledSourceGroup emptyGroupFor(String canonicalSourcePath) {
        return new CompiledSourceGroup(canonicalSourcePath, simpleNameFromCanonical(canonicalSourcePath), List.of());
    }

    private String simpleNameFromCanonical(String canonicalPath) {
        if (canonicalPath == null || canonicalPath.isBlank()) {
            return "unknown.md";
        }
        String pathPart = canonicalPath;
        int baseSeparator = canonicalPath.indexOf(':');
        if (baseSeparator >= 0 && baseSeparator + 1 < canonicalPath.length()) {
            pathPart = canonicalPath.substring(baseSeparator + 1);
        }
        String normalized = pathPart.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            return normalized.substring(slash + 1);
        }
        return normalized;
    }

    private List<String> collectReferencedTrackedSources(String relativeSourcePath,
                                                          MarkdownDependencyGraph graph,
                                                          ReferenceDepthPolicy referenceDepthPolicy) {
        if (graph == null) {
            return List.of();
        }

        ReferenceDepthPolicy effectivePolicy = referenceDepthPolicy == null
                ? ReferenceDepthPolicy.defaultPolicy()
                : referenceDepthPolicy;
        if (effectivePolicy.isDisabled()) {
            return List.of();
        }

        List<String> orderedSources = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String child : graph.getChildren(relativeSourcePath)) {
            collectReferencedTrackedSources(child, 1, graph, effectivePolicy, visited, orderedSources);
        }
        return Collections.unmodifiableList(orderedSources);
    }

    private void collectReferencedTrackedSources(String sourcePath,
                                                  int depthFromRoot,
                                                  MarkdownDependencyGraph graph,
                                                  ReferenceDepthPolicy referenceDepthPolicy,
                                                  Set<String> visited,
                                                  List<String> orderedSources) {
        if (!referenceDepthPolicy.includesDepth(depthFromRoot) || !visited.add(sourcePath)) {
            return;
        }

        orderedSources.add(sourcePath);
        if (!referenceDepthPolicy.canTraverseChildren(depthFromRoot)) {
            return;
        }

        for (String child : graph.getChildren(sourcePath)) {
            collectReferencedTrackedSources(child, depthFromRoot + 1, graph, referenceDepthPolicy, visited, orderedSources);
        }
    }
}
