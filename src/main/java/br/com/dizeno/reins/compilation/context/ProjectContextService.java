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

package br.com.dizeno.reins.compilation.context;

import br.com.dizeno.reins.source.graph.MarkdownReferenceExtractor;
import br.com.dizeno.reins.security.PathValidator;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ProjectContextService is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class ProjectContextService {
    private final MarkdownReferenceExtractor referenceExtractor;

    /**
     * SourcesLoadResult is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public static class SourcesLoadResult {
        private final List<CompilationBackgroundFile> combinedFiles;
        private final List<String> loadedExprs;
        private final List<String> blankExprs;

        SourcesLoadResult(List<CompilationBackgroundFile> combinedFiles, List<String> loadedExprs, List<String> blankExprs) {
            this.combinedFiles = List.copyOf(combinedFiles);
            this.loadedExprs = List.copyOf(loadedExprs);
            this.blankExprs = List.copyOf(blankExprs);
        }

        /**
         * Empty.
         *
         * @return the resulting result
         */
        public static SourcesLoadResult empty() {
            return new SourcesLoadResult(List.of(), List.of(), List.of());
        }

        /**
         * Gets the combined files.
         *
         * @return the collection of elements
         */
        public List<CompilationBackgroundFile> getCombinedFiles() { return combinedFiles; }
        /**
         * Gets the loaded exprs.
         *
         * @return the string result
         */
        public List<String> getLoadedExprs() { return loadedExprs; }
        /**
         * Gets the blank exprs.
         *
         * @return the string result
         */
        public List<String> getBlankExprs() { return blankExprs; }
    }

    /**
     * Constructs a new instance of {@link ProjectContextService}.
     */
    public ProjectContextService() {
        this(new MarkdownReferenceExtractor());
    }

    /**
     * Constructs a new instance of {@link ProjectContextService}.
     *
     * @param referenceExtractor the reference extractor
     */
    public ProjectContextService(MarkdownReferenceExtractor referenceExtractor) {
        this.referenceExtractor = referenceExtractor;
    }

    /**
     * Loads the resource or context.
     *
     * @param contextFileConfig the context file config
     * @param projectRoot the root path of the project
     * @return the resulting payload
     */
    public CompilationBackgroundPayload load(File contextFileConfig, Path projectRoot) throws MojoExecutionException {
        return load(contextFileConfig, projectRoot, Set.of(), ReferenceDepthPolicy.defaultPolicy(), true);
    }

    /**
     * Loads the resource or context.
     *
     * @param contextFileConfig the context file config
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @return the resulting payload
     */
    public CompilationBackgroundPayload load(File contextFileConfig, Path projectRoot, Set<Path> scanRoots) throws MojoExecutionException {
        return load(contextFileConfig, projectRoot, scanRoots, ReferenceDepthPolicy.defaultPolicy(), true);
    }

    /**
     * Loads the resource or context.
     *
     * @param contextFileConfig the context file config
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @param referenceDepthPolicy the reference depth policy
     * @param includeReferences the include references
     * @return the resulting payload
     */
    public CompilationBackgroundPayload load(File contextFileConfig,
                                             Path projectRoot,
                                             Set<Path> scanRoots,
                                             ReferenceDepthPolicy referenceDepthPolicy,
                                             boolean includeReferences) throws MojoExecutionException {
        return load(contextFileConfig, projectRoot, scanRoots, referenceDepthPolicy, includeReferences, null);
    }

    /**
     * Loads the resource or context.
     *
     * @param contextFileConfig the context file config
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @param referenceDepthPolicy the reference depth policy
     * @return the resulting payload
     */
    public CompilationBackgroundPayload load(File contextFileConfig,
                                             Path projectRoot,
                                             Set<Path> scanRoots,
                                             ReferenceDepthPolicy referenceDepthPolicy) throws MojoExecutionException {
        return load(contextFileConfig, projectRoot, scanRoots, referenceDepthPolicy, true, null);
    }

    private CompilationBackgroundPayload load(File contextFileConfig,
                                              Path projectRoot,
                                              Set<Path> scanRoots,
                                              ReferenceDepthPolicy referenceDepthPolicy,
                                              boolean includeReferences,
                                              Set<Path> alreadyVisited) throws MojoExecutionException {
        if (contextFileConfig == null) {
            return CompilationBackgroundPayload.empty();
        }

        ReferenceDepthPolicy policy = referenceDepthPolicy == null
                ? ReferenceDepthPolicy.defaultPolicy()
                : referenceDepthPolicy;

        Path contextPath = contextFileConfig.toPath().toAbsolutePath().normalize();
        PathValidator validator = new PathValidator(projectRoot);

        
        try {
            validator.validateInProject(contextPath);
        } catch (SecurityException e) {
            return CompilationBackgroundPayload.empty();
        }

        
        if (!Files.exists(contextPath)) {
            return CompilationBackgroundPayload.empty();
        }

        String rootContent;
        try {
            rootContent = Files.readString(contextPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            
            throw new MojoExecutionException(
                    "Failed to read compilation background file: " + contextPath + ": " + e.getMessage(), e);
        }

        
        if (rootContent.isBlank()) {
            return CompilationBackgroundPayload.empty();
        }

        List<CompilationBackgroundFile> files = new ArrayList<>();
        Set<Path> visited = alreadyVisited == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(alreadyVisited);
        discoverFile(contextPath, rootContent, projectRoot, validator, scanRoots, files, visited, new ArrayDeque<>(), policy, 0, includeReferences);

        return new CompilationBackgroundPayload(files);
    }

    /**
     * Loads the resource or context sources.
     *
     * @param sources the sources
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @param alreadyVisited the already visited
     * @return the resulting result
     */
    public SourcesLoadResult loadSources(List<File> sources,
                                          Path projectRoot,
                                          Set<Path> scanRoots,
                                          Set<Path> alreadyVisited) throws MojoExecutionException {
        return loadSources(sources, projectRoot, scanRoots, alreadyVisited, ReferenceDepthPolicy.defaultPolicy(), true);
    }

    /**
     * Loads the resource or context sources.
     *
     * @param sources the sources
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @param alreadyVisited the already visited
     * @param referenceDepthPolicy the reference depth policy
     * @return the resulting result
     */
    public SourcesLoadResult loadSources(List<File> sources,
                                         Path projectRoot,
                                         Set<Path> scanRoots,
                                         Set<Path> alreadyVisited,
                                         ReferenceDepthPolicy referenceDepthPolicy) throws MojoExecutionException {
        return loadSources(sources, projectRoot, scanRoots, alreadyVisited, referenceDepthPolicy, true);
    }

    /**
     * Loads the resource or context sources.
     *
     * @param sources the sources
     * @param projectRoot the root path of the project
     * @param scanRoots the scan roots
     * @param alreadyVisited the already visited
     * @param referenceDepthPolicy the reference depth policy
     * @param includeReferences the include references
     * @return the resulting result
     */
    public SourcesLoadResult loadSources(List<File> sources,
                                         Path projectRoot,
                                         Set<Path> scanRoots,
                                         Set<Path> alreadyVisited,
                                         ReferenceDepthPolicy referenceDepthPolicy,
                                         boolean includeReferences) throws MojoExecutionException {
        if (sources == null || sources.isEmpty()) {
            return SourcesLoadResult.empty();
        }

        ReferenceDepthPolicy policy = referenceDepthPolicy == null
                ? ReferenceDepthPolicy.defaultPolicy()
                : referenceDepthPolicy;

        PathValidator validator = new PathValidator(projectRoot);
        List<CompilationBackgroundFile> combinedFiles = new ArrayList<>();
        List<String> loadedExprs = new ArrayList<>();
        List<String> blankExprs = new ArrayList<>();
        Set<Path> visited = new LinkedHashSet<>(alreadyVisited);

        for (File sourceFile : sources) {
            String configuredExpr = sourceFile.toString();
            Path absolutePath = sourceFile.toPath().toAbsolutePath().normalize();

            
            if (!absolutePath.getFileName().toString().toLowerCase().endsWith(".md")) {
                throw new MojoExecutionException(
                        "context.sources: entry must be a .md file: " + absolutePath);
            }

            
            try {
                validator.validateInProject(absolutePath);
            } catch (SecurityException e) {
                throw new MojoExecutionException(
                        "context.sources: source file is outside project root: " + absolutePath);
            }

            
            if (!Files.exists(absolutePath)) {
                throw new MojoExecutionException(
                        "context.sources: source file does not exist: " + absolutePath);
            }

            
            String content;
            try {
                content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "context.sources: failed to read source file: " + absolutePath + ": " + e.getMessage(), e);
            }

            
            if (content.isBlank()) {
                blankExprs.add(configuredExpr);
                continue;
            }

            
            for (Path scanRoot : scanRoots) {
                if (absolutePath.startsWith(scanRoot)) {
                    throw new MojoExecutionException(
                            "context.sources: source file resolves under a source scan root '" + scanRoot + "': " + absolutePath);
                }
            }

            discoverFile(absolutePath, content, projectRoot, validator, scanRoots, combinedFiles, visited, new ArrayDeque<>(), policy, 0, includeReferences);
            loadedExprs.add(configuredExpr);
        }

        return new SourcesLoadResult(combinedFiles, loadedExprs, blankExprs);
    }

    private void discoverFile(Path absolutePath,
                               String content,
                               Path projectRoot,
                               PathValidator validator,
                               Set<Path> scanRoots,
                               List<CompilationBackgroundFile> result,
                               Set<Path> visited,
                               Deque<Path> activeStack,
                               ReferenceDepthPolicy referenceDepthPolicy,
                               int depthFromRoot,
                               boolean includeReferences) throws MojoExecutionException {
        
        if (activeStack.contains(absolutePath)) {
            List<String> cycle = new ArrayList<>();
            for (Path p : activeStack) {
                cycle.add(projectRoot.relativize(p).toString().replace('\\', '/'));
            }
            cycle.add(projectRoot.relativize(absolutePath).toString().replace('\\', '/'));
            throw new MojoExecutionException(
                    "Cycle detected in compilation background reference tree: " + String.join(" -> ", cycle));
        }

        
        if (visited.contains(absolutePath)) {
            return;
        }

        visited.add(absolutePath);
        activeStack.addLast(absolutePath);
        try {
            String displayPath = projectRoot.relativize(absolutePath).toString().replace('\\', '/');
            result.add(new CompilationBackgroundFile(absolutePath, displayPath, content));

            if (!includeReferences && depthFromRoot == 0) {
                return;
            }

            if (!referenceDepthPolicy.canTraverseChildren(depthFromRoot)) {
                return;
            }

            List<String> refs = referenceExtractor.extract(content);
            for (String rawRef : refs) {
                int childDepth = depthFromRoot + 1;
                if (!referenceDepthPolicy.includesDepth(childDepth)) {
                    continue;
                }
                Path refAbsolute = absolutePath.getParent().resolve(rawRef).normalize();

                
                try {
                    validator.validateInProject(refAbsolute);
                } catch (SecurityException e) {
                    throw new MojoExecutionException(
                            "Compilation background reference escapes project root: '" + rawRef + "' referenced from " + absolutePath);
                }

                
                for (Path scanRoot : scanRoots) {
                    if (refAbsolute.startsWith(scanRoot)) {
                        throw new MojoExecutionException(
                            "Compilation background reference resolves under a source scan root '" + scanRoot + "': '" + rawRef + "' referenced from " + absolutePath);
                    }
                }

                
                if (!Files.exists(refAbsolute)) {
                    throw new MojoExecutionException(
                            "Compilation background referenced file does not exist: " + refAbsolute
                                    + " (referenced from " + absolutePath + ")");
                }

                String refContent;
                try {
                    refContent = Files.readString(refAbsolute, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new MojoExecutionException(
                            "Failed to read compilation background referenced file: " + refAbsolute + ": " + e.getMessage(), e);
                }

                discoverFile(refAbsolute, refContent, projectRoot, validator, scanRoots, result, visited, activeStack, referenceDepthPolicy, childDepth, includeReferences);
            }
        } finally {
            activeStack.removeLast();
        }
    }
}
