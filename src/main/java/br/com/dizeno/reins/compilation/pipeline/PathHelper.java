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

import br.com.dizeno.reins.compilation.tracking.TrackedPathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.util.PathNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

 
/**
 * PathHelper is part of the sequential execution of compilation phases (reading, tracking, LLM reasoning, writing, and printing) in the reins architecture.
 * Acts as a helper utility assisting in its prefix tasks.
 */
public final class PathHelper {

    private PathHelper() {}

    

    /**
     * Resolves the configured value or path source category.
     *
     * @param sourcePath the path of the source file
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public static String resolveSourceCategory(Path sourcePath, Path projectRoot) {
        Path normalized = sourcePath.toAbsolutePath().normalize();
        String relative = PathNormalizer.toForwardSlashes(projectRoot.relativize(normalized).toString());
        return SourceScope.fromPath(relative).value();
    }

    

    /**
     * Resolves the configured value or path target root for source category.
     *
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static String resolveTargetRootForSourceCategory(ReinsConfig config,
                                                             Path projectRoot,
                                                             String sourceCategory) {
        return PathNormalizer.toForwardSlashes(
                projectRoot.relativize(BasePathMappingSet.forScope(config, projectRoot, sourceCategory).getTargetRoot()).toString());
    }

    

    /**
     * Checks if the component is main or test scope.
     *
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return true if successful or matching, false otherwise
     */
    public static boolean isMainOrTestScope(String sourceCategory) {
        return SourceScope.MAIN.value().equals(sourceCategory)
                || SourceScope.TEST.value().equals(sourceCategory);
    }

    

    /**
     * Resolves the configured value or path main source qualified path.
     *
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static String resolveMainSourceQualifiedPath(String canonicalSourcePath,
                                                         String sourceCategory) {
        if (TrackedPathResolver.looksCanonical(canonicalSourcePath)) {
            return canonicalSourcePath;
        }
        String base = SourceScope.TEST.value().equals(sourceCategory) ? "test" : "main";
        String relative = canonicalSourcePath == null ? "" : canonicalSourcePath;
        if ("main".equals(base) && relative.startsWith("src/main/nl/")) {
            relative = relative.substring("src/main/nl/".length());
        } else if ("test".equals(base) && relative.startsWith("src/test/nl/")) {
            relative = relative.substring("src/test/nl/".length());
        }
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return base + ":" + relative;
    }

    

    /**
     * Canonicalize Tracked Artifact Paths.
     *
     * @param paths the paths
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param sourceCategory the category of the source file (e.g. main or test)
     * @return the string result
     */
    public static List<String> canonicalizeTrackedArtifactPaths(List<String> paths,
                                                                ReinsConfig config,
                                                                Path projectRoot,
                                                                String sourceCategory) {
        if (paths == null || paths.isEmpty()) {
            return new ArrayList<>();
        }

        BasePathResolver resolver = new BasePathResolver(
                BasePathMappingSet.forScope(config, projectRoot, sourceCategory),
                new PathValidator(projectRoot));
        List<String> canonicalPaths = new ArrayList<>(paths.size());
        for (String path : paths) {
            canonicalPaths.add(canonicalizeTrackedArtifactPath(path, projectRoot, resolver));
        }
        return canonicalPaths;
    }

    

    /**
     * Canonicalize Tracked Artifact Path.
     *
     * @param path the file or directory path
     * @param projectRoot the root path of the project
     * @param resolver the resolver
     * @return the string result
     */
    public static String canonicalizeTrackedArtifactPath(String path,
                                                          Path projectRoot,
                                                          BasePathResolver resolver) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Tracked artifact path cannot be null or blank.");
        }
        if (TrackedPathResolver.looksCanonical(path)) {
            return FileReference.fromCanonical(path).toCanonicalString();
        }

        Path absolutePath = Path.of(path);
        if (!absolutePath.isAbsolute()) {
            absolutePath = projectRoot.resolve(path);
        }
        return resolver.qualifyAbsolute(absolutePath.toAbsolutePath().normalize());
    }
}
