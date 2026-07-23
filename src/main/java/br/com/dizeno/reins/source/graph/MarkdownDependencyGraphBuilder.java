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

package br.com.dizeno.reins.source.graph;

import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.PathNormalizer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MarkdownDependencyGraphBuilder is part of the general application functions in the reins architecture.
 * Constructs a dependency graph by extracting markdown references and resolving relative paths.
 */
public class MarkdownDependencyGraphBuilder {
    /**
     * VisitState is part of the general application functions in the reins architecture.
     * Acts as a component managing visit state.
     */
    private enum VisitState {
        WHITE,
        GRAY,
        BLACK
    }

    private final MarkdownReferenceExtractor referenceExtractor;
    private final ReferenceResolverPipeline resolverPipeline;
    private final Map<String, List<String>> resolutionDiagnostics = new LinkedHashMap<>();
    private final Map<String, List<String>> winningStrategies = new LinkedHashMap<>();

    /**
     * Constructs a new instance of {@link MarkdownDependencyGraphBuilder}.
     */
    public MarkdownDependencyGraphBuilder() {
        this(new MarkdownReferenceExtractor(), new ReferenceResolverPipeline(List.of(
                new RelativeReferenceResolutionStrategy(),
                new TestToMainFallbackResolutionStrategy()
        )));
    }

    /**
     * Constructs a new instance of {@link MarkdownDependencyGraphBuilder}.
     *
     * @param referenceExtractor the reference extractor
     */
    public MarkdownDependencyGraphBuilder(MarkdownReferenceExtractor referenceExtractor) {
        this(referenceExtractor, new ReferenceResolverPipeline(List.of(
                new RelativeReferenceResolutionStrategy(),
                new TestToMainFallbackResolutionStrategy()
        )));
    }

    /**
     * Constructs a new instance of {@link MarkdownDependencyGraphBuilder}.
     *
     * @param referenceExtractor the reference extractor
     * @param resolverPipeline the resolver pipeline
     */
    public MarkdownDependencyGraphBuilder(MarkdownReferenceExtractor referenceExtractor,
                                          ReferenceResolverPipeline resolverPipeline) {
        this.referenceExtractor = referenceExtractor;
        this.resolverPipeline = resolverPipeline;
    }

    /**
     * Builds the configured target.
     *
     * @param scannedFiles the scanned files
     * @param projectRoot the root path of the project
     * @param validator the path validator for security boundary checks
     * @return the resolved or constructed object
     */
    public MarkdownDependencyGraph build(List<File> scannedFiles,
                                         Path projectRoot,
                                         PathValidator validator) throws IOException {
        resolutionDiagnostics.clear();
        winningStrategies.clear();

        List<File> sortedFiles = scannedFiles.stream()
                .sorted(Comparator.comparing(file -> file.getAbsolutePath().toLowerCase()))
                .toList();

        Map<String, MarkdownSourceNode> nodes = new LinkedHashMap<>();
        Map<Path, String> relativeByAbsolute = new LinkedHashMap<>();
        Map<String, List<String>> edges = new LinkedHashMap<>();
        Map<String, List<String>> reverseEdges = new LinkedHashMap<>();

        Deque<String> pending = initializeNodes(sortedFiles, projectRoot, validator, nodes, relativeByAbsolute, edges, reverseEdges);
        buildEdges(pending, projectRoot, validator, nodes, relativeByAbsolute, edges, reverseEdges);
        detectCycles(edges);

        List<String> roots = collectRoots(reverseEdges);
        return new MarkdownDependencyGraph(nodes, edges, reverseEdges, roots,
                new LinkedHashMap<>(resolutionDiagnostics), new LinkedHashMap<>(winningStrategies));
    }

    private Deque<String> initializeNodes(List<File> sortedFiles,
                                          Path projectRoot,
                                          PathValidator validator,
                                          Map<String, MarkdownSourceNode> nodes,
                                          Map<Path, String> relativeByAbsolute,
                                          Map<String, List<String>> edges,
                                          Map<String, List<String>> reverseEdges) throws IOException {
        Deque<String> pending = new ArrayDeque<>();
        for (File scannedFile : sortedFiles) {
            Path absolutePath = validator.validateInProject(scannedFile.toPath());
            String sourcePath = addNode(projectRoot, absolutePath, nodes, relativeByAbsolute);
            edges.put(sourcePath, new ArrayList<>());
            reverseEdges.put(sourcePath, new ArrayList<>());
            pending.addLast(sourcePath);
        }
        return pending;
    }

    private void buildEdges(Deque<String> pending,
                             Path projectRoot,
                             PathValidator validator,
                             Map<String, MarkdownSourceNode> nodes,
                             Map<Path, String> relativeByAbsolute,
                             Map<String, List<String>> edges,
                             Map<String, List<String>> reverseEdges) throws IOException {
        while (!pending.isEmpty()) {
            String sourcePath = pending.removeFirst();
            MarkdownSourceNode node = nodes.get(sourcePath);
            if (node == null) {
                continue;
            }
            edges.computeIfAbsent(sourcePath, key -> new ArrayList<>());
            reverseEdges.computeIfAbsent(sourcePath, key -> new ArrayList<>());

            for (String rawReference : node.outboundRefs()) {
                resolveAndAddEdge(sourcePath, rawReference, projectRoot, validator,
                        nodes, relativeByAbsolute, edges, reverseEdges, pending);
            }
        }
    }

    private void resolveAndAddEdge(String sourcePath,
                                    String rawReference,
                                    Path projectRoot,
                                    PathValidator validator,
                                    Map<String, MarkdownSourceNode> nodes,
                                    Map<Path, String> relativeByAbsolute,
                                    Map<String, List<String>> edges,
                                    Map<String, List<String>> reverseEdges,
                                    Deque<String> pending) throws IOException {
        try {
            ResolverContext resolverContext = new ResolverContext(
                    projectRoot,
                    projectRoot.resolve(ProjectDirectoryPaths.MAIN_NL_ROOT).normalize(),
                    List.of("relative", "test-to-main-fallback"),
                    validator,
                    relativeByAbsolute
            );
            ReferenceRequest request = new ReferenceRequest(
                    sourcePath,
                    classifyScope(sourcePath),
                    nodes.get(sourcePath).absolutePath().getParent(),
                    rawReference
            );
            ResolutionResult resolution = resolverPipeline.resolve(request, resolverContext);
            recordResolution(sourcePath, rawReference, resolution);

            assertNotAmbiguous(sourcePath, rawReference, resolution);
            assertResolved(sourcePath, rawReference, resolution);

            String targetSourcePath = resolution.getResolvedPath();
            Path targetAbsolutePath = validator.validateInProject(projectRoot.resolve(targetSourcePath));
            assertTargetExists(sourcePath, rawReference, targetSourcePath, targetAbsolutePath);

            insertReferencedNode(targetSourcePath, targetAbsolutePath, nodes, relativeByAbsolute, edges, reverseEdges, pending, projectRoot);
            addEdge(sourcePath, targetSourcePath, edges, reverseEdges);
        } catch (GraphProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unresolvedReferenceException(sourcePath, rawReference);
        }
    }

    private void insertReferencedNode(String targetSourcePath,
                                       Path targetAbsolutePath,
                                       Map<String, MarkdownSourceNode> nodes,
                                       Map<Path, String> relativeByAbsolute,
                                       Map<String, List<String>> edges,
                                       Map<String, List<String>> reverseEdges,
                                       Deque<String> pending,
                                       Path projectRoot) throws IOException {
        if (!nodes.containsKey(targetSourcePath)) {
            addNode(projectRoot, targetAbsolutePath, nodes, relativeByAbsolute);
            pending.addLast(targetSourcePath);
            edges.computeIfAbsent(targetSourcePath, key -> new ArrayList<>());
            reverseEdges.computeIfAbsent(targetSourcePath, key -> new ArrayList<>());
        }
    }

    private void addEdge(String sourcePath,
                          String targetSourcePath,
                          Map<String, List<String>> edges,
                          Map<String, List<String>> reverseEdges) {
        addUnique(edges.get(sourcePath), targetSourcePath);
        reverseEdges.computeIfAbsent(targetSourcePath, key -> new ArrayList<>());
        addUnique(reverseEdges.get(targetSourcePath), sourcePath);
    }

    private List<String> collectRoots(Map<String, List<String>> reverseEdges) {
        return reverseEdges.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private static void assertNotAmbiguous(String sourcePath, String rawReference, ResolutionResult resolution) {
        if (resolution.getStatus() == ResolutionResult.ResolutionStatus.AMBIGUOUS) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.AMBIGUOUS_REFERENCE,
                    buildInvolvedPaths(sourcePath, rawReference, resolution.getCandidatePaths()),
                    "Ambiguous markdown reference in '" + sourcePath
                            + "': target '" + rawReference + "' matched multiple candidates: "
                            + String.join(",", resolution.getCandidatePaths())
            );
        }
    }

    private static void assertResolved(String sourcePath, String rawReference, ResolutionResult resolution) {
        if (resolution.getStatus() != ResolutionResult.ResolutionStatus.RESOLVED) {
            throw unresolvedReferenceException(sourcePath, rawReference);
        }
    }

    private static void assertTargetExists(String sourcePath, String rawReference,
                                            String targetSourcePath, Path targetAbsolutePath) {
        if (!Files.exists(targetAbsolutePath)) {
            throw new GraphProcessingException(
                    GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                    List.of(sourcePath, rawReference),
                    "Unresolved markdown reference in '" + sourcePath
                            + "': target '" + rawReference + "' resolved to '" + targetSourcePath + "' but file does not exist"
            );
        }
    }

    private static GraphProcessingException unresolvedReferenceException(String sourcePath, String rawReference) {
        return new GraphProcessingException(
                GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                List.of(sourcePath, rawReference),
                "Unresolved markdown reference in '" + sourcePath
                        + "': target '" + rawReference + "' not found in scan roots"
        );
    }

    private String addNode(Path projectRoot,
                           Path absolutePath,
                           Map<String, MarkdownSourceNode> nodes,
                           Map<Path, String> relativeByAbsolute) throws IOException {
        Path normalizedAbsolute = absolutePath.toAbsolutePath().normalize();
        String sourcePath = PathNormalizer.toForwardSlashes(projectRoot.relativize(normalizedAbsolute));
        if (nodes.containsKey(sourcePath)) {
            return sourcePath;
        }

        List<String> outboundRefs = referenceExtractor.extract(Files.readString(normalizedAbsolute, StandardCharsets.UTF_8));
        long lastModifiedMillis = Files.getLastModifiedTime(normalizedAbsolute).toMillis();
        nodes.put(sourcePath, new MarkdownSourceNode(sourcePath, normalizedAbsolute, lastModifiedMillis, outboundRefs));
        relativeByAbsolute.put(normalizedAbsolute, sourcePath);
        return sourcePath;
    }

    private void recordResolution(String sourcePath, String rawReference, ResolutionResult result) {
        List<String> diagnostics = resolutionDiagnostics.computeIfAbsent(sourcePath, key -> new ArrayList<>());
        String attempts = result.getAttemptedStrategies().isEmpty()
                ? "none"
                : String.join(",", result.getAttemptedStrategies());
        diagnostics.add("ref=" + rawReference + " status=" + result.getStatus() + " attempts=" + attempts
                + (result.getDiagnostic() == null || result.getDiagnostic().isBlank() ? "" : " detail=" + result.getDiagnostic()));

        if (result.getWinningStrategyId() != null && !result.getWinningStrategyId().isBlank()) {
            List<String> winners = winningStrategies.computeIfAbsent(sourcePath, key -> new ArrayList<>());
            winners.add(result.getWinningStrategyId());
        }
    }

    private ReferenceRequest.NlScope classifyScope(String sourcePath) {
        String normalized = PathNormalizer.toForwardSlashes(Path.of(sourcePath));
        if (normalized.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return ReferenceRequest.NlScope.MAIN;
        }
        if (normalized.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return ReferenceRequest.NlScope.TEST;
        }
        return ReferenceRequest.NlScope.UNCLASSIFIED;
    }

    private void addUnique(List<String> paths, String value) {
        Set<String> dedup = new LinkedHashSet<>(paths);
        dedup.add(value);
        paths.clear();
        paths.addAll(dedup);
    }

    private static List<String> buildInvolvedPaths(String sourcePath,
                                                    String rawReference,
                                                    List<String> candidatePaths) {
        List<String> involved = new ArrayList<>();
        involved.add(sourcePath);
        involved.add(rawReference);
        involved.addAll(candidatePaths);
        return involved;
    }

    private void detectCycles(Map<String, List<String>> edges) {
        Map<String, VisitState> states = new LinkedHashMap<>();
        for (String node : edges.keySet()) {
            states.put(node, VisitState.WHITE);
        }

        Deque<String> stack = new ArrayDeque<>();
        for (String node : edges.keySet()) {
            if (states.get(node) == VisitState.WHITE) {
                visit(node, edges, states, stack);
            }
        }
    }

    private void visit(String node,
                       Map<String, List<String>> edges,
                       Map<String, VisitState> states,
                       Deque<String> stack) {
        states.put(node, VisitState.GRAY);
        stack.addLast(node);
        for (String child : edges.getOrDefault(node, List.of())) {
            VisitState childState = states.getOrDefault(child, VisitState.WHITE);
            if (childState == VisitState.GRAY) {
                List<String> cycle = new ArrayList<>();
                boolean collect = false;
                for (String current : stack) {
                    if (current.equals(child)) {
                        collect = true;
                    }
                    if (collect) {
                        cycle.add(current);
                    }
                }
                cycle.add(child);
                throw new GraphProcessingException(GraphProcessingException.ViolationType.CYCLE, cycle);
            }
            if (childState == VisitState.WHITE) {
                visit(child, edges, states, stack);
            }
        }
        stack.removeLast();
        states.put(node, VisitState.BLACK);
    }

}