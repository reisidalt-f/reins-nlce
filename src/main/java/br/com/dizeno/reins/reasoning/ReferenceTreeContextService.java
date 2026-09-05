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

import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.compilation.context.ReferenceDepthPolicy;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.source.graph.MarkdownReferenceExtractor;
import br.com.dizeno.reins.source.graph.ReferenceRequest;
import br.com.dizeno.reins.source.graph.ReferenceResolverPipeline;
import br.com.dizeno.reins.source.graph.RelativeReferenceResolutionStrategy;
import br.com.dizeno.reins.source.graph.ResolutionResult;
import br.com.dizeno.reins.source.graph.ResolverContext;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.security.PathValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReferenceTreeContextService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ReferenceTreeContextService {
    private final MarkdownReferenceExtractor referenceExtractor;
    private final ReferenceResolverPipeline resolverPipeline;

    /**
     * Constructs a new instance of {@link ReferenceTreeContextService}.
     */
    public ReferenceTreeContextService() {
        this(new MarkdownReferenceExtractor(), new ReferenceResolverPipeline(List.of(
                new br.com.dizeno.reins.source.graph.NamedBaseReferenceResolutionStrategy(),
                new br.com.dizeno.reins.source.graph.RootBaseReferenceResolutionStrategy(),
                new RelativeReferenceResolutionStrategy(),
                new br.com.dizeno.reins.source.graph.ContextualFallbackReferenceResolutionStrategy()
        )));
    }

    /**
     * Constructs a new instance of {@link ReferenceTreeContextService}.
     *
     * @param referenceExtractor the reference extractor
     * @param resolverPipeline the resolver pipeline
     */
    public ReferenceTreeContextService(MarkdownReferenceExtractor referenceExtractor,
                                       ReferenceResolverPipeline resolverPipeline) {
        this.referenceExtractor = referenceExtractor;
        this.resolverPipeline = resolverPipeline;
    }

    /**
     * Builds the configured target.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param resolver the resolver
     * @return the resulting context
     */
    public ReferenceTreeContext build(ReasoningRequest request,
                                      ReinsConfig config,
                                      BasePathResolver resolver) throws IOException {
        return build(request, config, resolver, Set.of());
    }

    /**
     * Builds the configured target.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @param resolver the resolver
     * @param excludedRoots the excluded roots
     * @return the resulting context
     */
    public ReferenceTreeContext build(ReasoningRequest request,
                                      ReinsConfig config,
                                      BasePathResolver resolver,
                                      Set<Path> excludedRoots) throws IOException {
        Path projectRoot = resolver.getProjectRoot();
        PathValidator validator = new PathValidator(projectRoot);

        RootResolution root = resolveRoot(request, resolver, validator);

        ReferenceTreeContext context = new ReferenceTreeContext(
                root.canonicalPath,
                root.displayLabel,
            ReferenceTreeRenderPolicy.forPolicy(resolveReferenceDepthPolicy(request, config)),
                new LinkedHashMap<>()
        );

        Map<Path, String> knownByAbsolute = new LinkedHashMap<>();
        knownByAbsolute.put(root.absolutePath, root.canonicalPath);
        TraversalContext ctx = new TraversalContext(
            projectRoot,
            validator,
            knownByAbsolute,
            context,
            new ArrayDeque<>(),
            excludedRoots,
            resolveReferenceDepthPolicy(request, config));
        discoverNode(root.canonicalPath, root.absolutePath, root.scope, ctx, 0);

        return context;
    }

    private void discoverNode(String canonicalPath,
                              Path absolutePath,
                              ReferenceRequest.NlScope scope,
                      TraversalContext ctx,
                      int depthFromRoot) throws IOException {
        if (ctx.activeStack().contains(canonicalPath)) {
            throw cycleException(ctx.activeStack(), canonicalPath);
        }

        ReferenceNode node = ctx.context().nodesByCanonical.computeIfAbsent(canonicalPath, key -> new ReferenceNode(canonicalPath));
        if (node.discovered) {
            return;
        }

        ctx.activeStack().addLast(canonicalPath);
        node.discovered = true;

        String content;
        try {
            content = Files.readString(absolutePath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(canonicalPath, canonicalPath),
                    "Unreadable markdown source in reference tree: " + canonicalPath
            );
        }

        List<String> refs = referenceExtractor.extract(content);
        ResolverContext resolverContext = new ResolverContext(
                ctx.projectRoot(),
                Map.of("main", ctx.projectRoot().resolve(ProjectDirectoryPaths.MAIN_NL_ROOT).normalize(),
                       "test", ctx.projectRoot().resolve(ProjectDirectoryPaths.TEST_NL_ROOT).normalize()),
                List.of("named-base-scheme", "root-base-relative", "relative", "contextual-fallback"),
                ctx.validator(),
                ctx.knownByAbsolute()
        );

        for (String rawReference : refs) {
            processReference(canonicalPath, rawReference, absolutePath, scope, ctx, resolverContext, node, depthFromRoot);
        }

        ctx.activeStack().removeLast();
    }

    private void processReference(String canonicalPath,
                                   String rawReference,
                                   Path absolutePath,
                                   ReferenceRequest.NlScope scope,
                                   TraversalContext ctx,
                                   ResolverContext resolverContext,
                                   ReferenceNode node,
                                   int depthFromRoot) throws IOException {
        int childDepth = depthFromRoot + 1;
        if (!ctx.referenceDepthPolicy().includesDepth(childDepth)) {
            return;
        }

        ResolutionResult resolution;
        try {
            resolution = resolverPipeline.resolve(new ReferenceRequest(
                    canonicalPath, scope, absolutePath.getParent(), rawReference
            ), resolverContext);
        } catch (Exception ex) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(canonicalPath, rawReference),
                    "Unresolved markdown reference in '" + canonicalPath + "': target '" + rawReference + "' not found"
            );
        }

        if (resolution.getStatus() == ResolutionResult.ResolutionStatus.AMBIGUOUS) {
            List<String> involved = new ArrayList<>();
            involved.add(canonicalPath);
            involved.add(rawReference);
            involved.addAll(resolution.getCandidatePaths());
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.AMBIGUOUS_REFERENCE,
                    involved,
                    "Ambiguous markdown reference in '" + canonicalPath + "': target '" + rawReference + "' matched multiple candidates"
            );
        }

        if (resolution.getStatus() != ResolutionResult.ResolutionStatus.RESOLVED) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(canonicalPath, rawReference),
                    "Unresolved markdown reference in '" + canonicalPath + "': target '" + rawReference + "' not found"
            );
        }

        String targetCanonical = resolution.getResolvedPath();
        Path targetAbsolute = ctx.validator().validateInProject(ctx.projectRoot().resolve(targetCanonical));
        if (!Files.exists(targetAbsolute)) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(canonicalPath, rawReference),
                    "Unresolved markdown reference in '" + canonicalPath + "': target '" + rawReference + "' does not exist"
            );
        }

        if (!ctx.excludedRoots().isEmpty()) {
            Path normalizedTarget = targetAbsolute.toAbsolutePath().normalize();
            for (Path excluded : ctx.excludedRoots()) {
                if (normalizedTarget.startsWith(excluded.toAbsolutePath().normalize())) {
                    throw new GraphProcessingException(
                            GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                            List.of(canonicalPath, rawReference),
                            "Reference '" + rawReference + "' in '" + canonicalPath + "' resolves into a scan root and is not allowed in the project file reference tree"
                    );
                }
            }
        }

        if (ctx.activeStack().contains(targetCanonical)) {
            throw cycleException(ctx.activeStack(), targetCanonical);
        }

        ctx.knownByAbsolute().put(targetAbsolute, targetCanonical);
        node.edges.add(new ReferenceEdge(rawReference, targetCanonical));

        if (ctx.referenceDepthPolicy().canTraverseChildren(childDepth)) {
            discoverNode(targetCanonical, targetAbsolute, classifyScope(targetCanonical), ctx, childDepth);
        }
    }

    private GraphProcessingException cycleException(Deque<String> activeStack, String repeatedNode) {
        List<String> cycle = new ArrayList<>();
        boolean collect = false;
        for (String current : activeStack) {
            if (current.equals(repeatedNode)) {
                collect = true;
            }
            if (collect) {
                cycle.add(current);
            }
        }
        cycle.add(repeatedNode);
        return new GraphProcessingException(GraphProcessingException.ViolationType.CYCLE, cycle);
    }

    private RootResolution resolveRoot(ReasoningRequest request, BasePathResolver resolver, PathValidator validator) {
        String sourcePath = request.getSourcePath();
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of("<unknown>", "<root>"),
                    "Root markdown source path is missing."
            );
        }

        Path rootAbsolute;
        String rootCanonical;
        String displayLabel;

        if (TrackedPathResolver.looksCanonical(sourcePath)) {
            FileReference reference = FileReference.fromCanonical(sourcePath);
            rootAbsolute = validator.validateInProject(resolver.resolve(reference));
            rootCanonical = resolver.toProjectRelativePath(rootAbsolute);
            displayLabel = sourcePath;
        } else if (sourcePath.startsWith("src/")) {
            rootAbsolute = validator.validateInProject(resolver.resolveProjectPath(sourcePath));
            rootCanonical = resolver.toProjectRelativePath(rootAbsolute);
            displayLabel = toCanonicalDisplayLabel(rootAbsolute, resolver, sourcePath);
        } else {
            String base = request.getSourceScope() == null ? "main" : request.getSourceScope();
            rootAbsolute = validator.validateInProject(resolver.resolve(base, sourcePath));
            rootCanonical = resolver.toProjectRelativePath(rootAbsolute);
            displayLabel = toCanonicalDisplayLabel(rootAbsolute, resolver, sourcePath);
        }

        if (!Files.exists(rootAbsolute)) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(rootCanonical, displayLabel),
                    "Root markdown source path does not exist: " + displayLabel
            );
        }

        return new RootResolution(rootCanonical, rootAbsolute, displayLabel, classifyScope(rootCanonical));
    }

    private ReferenceDepthPolicy resolveReferenceDepthPolicy(ReasoningRequest request, ReinsConfig config) {
        if (request != null && request.getReferenceDepthPolicy() != null) {
            return request.getReferenceDepthPolicy();
        }
        if (config == null || config.getContext() == null || config.getContext().getReferencesTree() == null) {
            return ReferenceDepthPolicy.defaultPolicy();
        }
        return config.getContext().getReferencesTree().resolveReferenceDepthPolicy();
    }

    private String toCanonicalDisplayLabel(Path rootAbsolute, BasePathResolver resolver, String fallback) {
        try {
            return resolver.qualifyAbsolute(rootAbsolute);
        } catch (Exception e) {
            return fallback;
        }
    }

    private ReferenceRequest.NlScope classifyScope(String sourcePath) {
        if (sourcePath.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return ReferenceRequest.NlScope.MAIN;
        }
        if (sourcePath.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return ReferenceRequest.NlScope.TEST;
        }
        return ReferenceRequest.NlScope.UNCLASSIFIED;
    }

    /**
     * TraversalContext is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing traversal context.
     */
    private record TraversalContext(
            Path projectRoot,
            PathValidator validator,
            Map<Path, String> knownByAbsolute,
            ReferenceTreeContext context,
            Deque<String> activeStack,
            Set<Path> excludedRoots,
            ReferenceDepthPolicy referenceDepthPolicy) {}

    /**
     * RootResolution is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing root resolution.
     */
    private record RootResolution(String canonicalPath,
                                  Path absolutePath,
                                  String displayLabel,
                                  ReferenceRequest.NlScope scope) {
    }

    /**
     * ReferenceTreeContext is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing reference tree context.
     */
    public static class ReferenceTreeContext {
        private final String rootCanonicalPath;
        private final String rootDisplayLabel;
        private final ReferenceTreeRenderPolicy renderPolicy;
        private final Map<String, ReferenceNode> nodesByCanonical;

        /**
         * Constructs a new instance of {@link ReferenceTreeContext}.
         *
         * @param rootCanonicalPath the root canonical path
         * @param rootDisplayLabel the root display label
         * @param renderPolicy the render policy
         * @param nodesByCanonical the nodes by canonical
         */
        public ReferenceTreeContext(String rootCanonicalPath,
                                    String rootDisplayLabel,
                                    ReferenceTreeRenderPolicy renderPolicy,
                                    Map<String, ReferenceNode> nodesByCanonical) {
            this.rootCanonicalPath = rootCanonicalPath;
            this.rootDisplayLabel = rootDisplayLabel;
            this.renderPolicy = renderPolicy;
            this.nodesByCanonical = nodesByCanonical;
        }

        /**
         * Gets the root canonical path.
         *
         * @return the string result
         */
        public String getRootCanonicalPath() {
            return rootCanonicalPath;
        }

        /**
         * Gets the root display label.
         *
         * @return the string result
         */
        public String getRootDisplayLabel() {
            return rootDisplayLabel;
        }

        /**
         * Gets the render policy.
         *
         * @return the resolved or constructed object
         */
        public ReferenceTreeRenderPolicy getRenderPolicy() {
            return renderPolicy;
        }

        /**
         * Gets the nodes by canonical.
         *
         * @return the string result
         */
        public Map<String, ReferenceNode> getNodesByCanonical() {
            return nodesByCanonical;
        }
    }

    /**
     * ReferenceNode is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a model representation of a dependency graph node.
     */
    public static class ReferenceNode {
        private final String canonicalPath;
        private final List<ReferenceEdge> edges = new ArrayList<>();
        private boolean discovered;

        /**
         * Constructs a new instance of {@link ReferenceNode}.
         *
         * @param canonicalPath the canonical path
         */
        public ReferenceNode(String canonicalPath) {
            this.canonicalPath = canonicalPath;
        }

        /**
         * Gets the canonical path.
         *
         * @return the string result
         */
        public String getCanonicalPath() {
            return canonicalPath;
        }

        /**
         * Gets the edges.
         *
         * @return the collection of elements
         */
        public List<ReferenceEdge> getEdges() {
            return edges;
        }
    }

    /**
     * ReferenceEdge is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing reference edge.
     */
    public record ReferenceEdge(String literalPath, String targetCanonicalPath) {
    }
}
