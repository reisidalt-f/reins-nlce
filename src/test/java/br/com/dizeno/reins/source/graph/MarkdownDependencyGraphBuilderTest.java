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

import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownDependencyGraphBuilderTest {
    @TempDir
    Path projectDir;

    @Test
    void buildsGraphWithSharedChildWithoutDuplicatingNodes() throws Exception {
        Path root = Files.createDirectories(projectDir.resolve("src/main/nl"));
        Path shared = write(root.resolve("shared/common.md"), "# shared\n");
        Path rootA = write(root.resolve("roots/root-a.md"), "Uses [../shared/common.md]\n");
        Path rootB = write(root.resolve("roots/root-b.md"), "Uses (../shared/common.md)\n");

        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(
                List.of(shared.toFile(), rootA.toFile(), rootB.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertEquals(3, graph.size());
        assertEquals(List.of("src/main/nl/shared/common.md"), graph.getChildren("src/main/nl/roots/root-a.md"));
        assertEquals(List.of("src/main/nl/shared/common.md"), graph.getChildren("src/main/nl/roots/root-b.md"));
        assertEquals(List.of("src/main/nl/roots/root-a.md", "src/main/nl/roots/root-b.md"), graph.getParents("src/main/nl/shared/common.md"));
    }

    @Test
    void resolvesTestToMainFallbackWhenRelativeFails() throws Exception {
        Path mainRoot = Files.createDirectories(projectDir.resolve("src/main/nl/app"));
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        Path fallbackMain = write(mainRoot.resolve("fallback-runner.md"), "Main fallback\n");
        Path testRef = write(testRoot.resolve("fallback-runner-test.md"), "Uses [fallback-runner.md]\n");

        MarkdownDependencyGraphBuilder builder = new MarkdownDependencyGraphBuilder();
        MarkdownDependencyGraph graph = builder.build(
                List.of(testRef.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertTrue(graph.getNodes().containsKey("src/main/nl/app/fallback-runner.md"));
        assertEquals(List.of("src/main/nl/app/fallback-runner.md"), graph.getChildren("src/test/nl/app/fallback-runner-test.md"));
        assertTrue(graph.getWinningStrategies("src/test/nl/app/fallback-runner-test.md").contains("test-to-main-fallback"));
        assertTrue(Files.exists(fallbackMain));
    }

    @Test
    void doesNotMatchByFilenameSuffixOnly() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        write(projectDir.resolve("src/main/nl/app/runner.md"), "runner\n");
        write(projectDir.resolve("src/main/nl/app/fallback-runner.md"), "fallback\n");
        Path source = write(testRoot.resolve("runner-test.md"), "Uses [runner.md]\n");

        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(
                List.of(source.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertEquals(List.of("src/main/nl/app/runner.md"), graph.getChildren("src/test/nl/app/runner-test.md"));
    }

    @Test
    void prefersMirroredMainPathFromRefererRelativeReference() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        write(projectDir.resolve("src/main/nl/app/runner.md"), "runner\n");
        write(projectDir.resolve("src/main/nl/roots/runner.md"), "other runner\n");
        Path source = write(testRoot.resolve("runner-test.md"), "Uses [runner.md]\n");

        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(
                List.of(source.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertEquals(List.of("src/main/nl/app/runner.md"), graph.getChildren("src/test/nl/app/runner-test.md"));
    }

    @Test
    void prefersSameSubtreeFallbackCandidateWhenMultipleExist() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app/foo"));
        Path mainFoo = write(projectDir.resolve("src/main/nl/app/foo/shared.md"), "foo\n");
        write(projectDir.resolve("src/main/nl/app/bar/shared.md"), "bar\n");
        Path source = write(testRoot.resolve("ambiguous-runner-test.md"), "Uses [shared.md]\n");

        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(
                List.of(source.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertEquals(List.of("src/main/nl/app/foo/shared.md"), graph.getChildren("src/test/nl/app/foo/ambiguous-runner-test.md"));
        assertTrue(Files.exists(mainFoo));
    }

    @Test
    void failsFastOnAmbiguousFallbackWhenNoSubtreeWinner() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app/baz"));
        write(projectDir.resolve("src/main/nl/app/foo/shared.md"), "foo\n");
        write(projectDir.resolve("src/main/nl/app/bar/shared.md"), "bar\n");
        Path source = write(testRoot.resolve("ambiguous-no-subtree.md"), "Uses [shared.md]\n");

        GraphProcessingException exception = assertThrows(GraphProcessingException.class, () ->
                new MarkdownDependencyGraphBuilder().build(
                        List.of(source.toFile()),
                        projectDir,
                        new PathValidator(projectDir)
                ));

        assertEquals(GraphProcessingException.ViolationType.AMBIGUOUS_REFERENCE, exception.getViolationType());
    }

    @Test
    void recursivelyExpandsFallbackInsertedNodes() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        write(projectDir.resolve("src/main/nl/shared/base.md"), "Base\n");
        write(projectDir.resolve("src/main/nl/domain/fallback-child.md"), "Uses [../shared/base.md]\n");
        write(projectDir.resolve("src/main/nl/app/fallback-root.md"), "Uses [../domain/fallback-child.md]\n");
        Path source = write(testRoot.resolve("recursive-runner-test.md"), "Uses [fallback-root.md]\n");

        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(
                List.of(source.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertTrue(graph.getNodes().containsKey("src/main/nl/app/fallback-root.md"));
        assertTrue(graph.getNodes().containsKey("src/main/nl/domain/fallback-child.md"));
        assertEquals(List.of("src/main/nl/domain/fallback-child.md"), graph.getChildren("src/main/nl/app/fallback-root.md"));
    }

    @Test
    void failsFastOnRecursiveFallbackCycles() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        write(projectDir.resolve("src/main/nl/cycles/fallback-cycle-a.md"), "Uses [fallback-cycle-b.md]\n");
        write(projectDir.resolve("src/main/nl/cycles/fallback-cycle-b.md"), "Uses [fallback-cycle-a.md]\n");
        Path source = write(testRoot.resolve("cycle-runner-test.md"), "Uses [fallback-cycle-a.md]\n");

        GraphProcessingException exception = assertThrows(GraphProcessingException.class, () ->
                new MarkdownDependencyGraphBuilder().build(
                        List.of(source.toFile()),
                        projectDir,
                        new PathValidator(projectDir)
                ));

        assertEquals(GraphProcessingException.ViolationType.CYCLE, exception.getViolationType());
    }

    @Test
    void doesNotApplyFallbackForMainScopeReferers() throws Exception {
        Path source = write(projectDir.resolve("src/main/nl/app/main-runner.md"), "Uses [fallback-runner.md]\n");
        write(projectDir.resolve("src/main/nl/domain/fallback-runner.md"), "main target\\n");

        GraphProcessingException exception = assertThrows(GraphProcessingException.class, () ->
                new MarkdownDependencyGraphBuilder().build(
                        List.of(source.toFile()),
                        projectDir,
                        new PathValidator(projectDir)
                ));

        assertEquals(GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE, exception.getViolationType());
    }

    @Test
    void supportsCustomStrategyPipelineOrdering() throws Exception {
        Path testRoot = Files.createDirectories(projectDir.resolve("src/test/nl/app"));
        Path custom = write(projectDir.resolve("src/main/nl/app/custom.md"), "custom\n");
        Path source = write(testRoot.resolve("custom-test.md"), "Uses [anything.md]\n");

        ResolutionStrategy customStrategy = new ResolutionStrategy() {
            @Override
            public String strategyId() {
                return "custom-strategy";
            }

            @Override
            public boolean applies(ReferenceRequest request, ResolverContext context) {
                return true;
            }

            @Override
            public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) {
                return StrategyAttemptResult.fromCandidates(List.of(custom), "custom resolve");
            }
        };

        MarkdownDependencyGraphBuilder builder = new MarkdownDependencyGraphBuilder(
                new MarkdownReferenceExtractor(),
                new ReferenceResolverPipeline(List.of(customStrategy))
        );

        MarkdownDependencyGraph graph = builder.build(
                List.of(source.toFile()),
                projectDir,
                new PathValidator(projectDir)
        );

        assertEquals(List.of("src/main/nl/app/custom.md"), graph.getChildren("src/test/nl/app/custom-test.md"));
        assertEquals(List.of("custom-strategy"), graph.getWinningStrategies("src/test/nl/app/custom-test.md"));
    }

    private Path write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
