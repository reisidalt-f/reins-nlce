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

package br.com.dizeno.reins.source.scanner;

import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoderMdScannerTest {
    @TempDir
    Path projectDir;

    @Test
    void findsPlainAndLegacyMarkdownFilesInSortedOrder() throws IOException {
        Path root = Files.createDirectories(projectDir.resolve("src/main/nl"));
        Files.writeString(root.resolve("CurrentFeatures.md"), "# current\n");
        Files.writeString(root.resolve("LegacyCurrentFeatures.code.md"), "# legacy\n");
        Files.writeString(root.resolve("LegacyCurrentFeatures.coder.md"), "# legacy coder\n");
        Files.createDirectories(root.resolve("target"));
        Files.writeString(root.resolve("target/ignored.md"), "# ignored\n");

        List<String> found = new CoderMdScanner().scan(List.of(root.toFile()), "**/*.md", new PathValidator(projectDir))
                .stream()
                .map(File::getName)
                .toList();

        assertEquals(List.of(
                "CurrentFeatures.md",
                "LegacyCurrentFeatures.code.md",
                "LegacyCurrentFeatures.coder.md"
        ), found);
    }

    @Test
    void skipsMissingRoots() throws IOException {
        Path root = Files.createDirectories(projectDir.resolve("src/main/nl"));
        Files.writeString(root.resolve("CurrentFeatures.md"), "# current\n");

        List<File> found = new CoderMdScanner().scan(
                List.of(root.toFile(), projectDir.resolve("src/test/nl").toFile()),
                "**/*.md",
                new PathValidator(projectDir)
        );

        assertEquals(1, found.size());
        assertEquals("CurrentFeatures.md", found.get(0).getName());
    }

    @Test
    void scannedMarkdownFilesCanBePromotedIntoDependencyGraphNodes() throws Exception {
        Path root = Files.createDirectories(projectDir.resolve("src/main/nl"));
        Files.createDirectories(root.resolve("shared"));
        Files.createDirectories(root.resolve("domain"));
        Files.writeString(root.resolve("shared/base.md"), "# base\n");
        Files.writeString(root.resolve("domain/service.md"), "Uses [../shared/base.md]\n");

        PathValidator pathValidator = new PathValidator(projectDir);
        List<File> found = new CoderMdScanner().scan(List.of(root.toFile()), "**/*.md", pathValidator);
        MarkdownDependencyGraph graph = new MarkdownDependencyGraphBuilder().build(found, projectDir, pathValidator);

        assertEquals(2, graph.size());
        assertEquals(List.of("src/main/nl/shared/base.md"), graph.getChildren("src/main/nl/domain/service.md"));
    }

    @Test
    void explicitSourceModeSkipsDefaultDiscovery() throws IOException {
        Path root = Files.createDirectories(projectDir.resolve("src/main/nl"));
        Files.writeString(root.resolve("CurrentFeatures.md"), "# current\n");

        List<File> found = new CoderMdScanner().scan(
                List.of(root.toFile()),
                "**/*.md",
                new PathValidator(projectDir),
                SourceDiscoveryMode.EXPLICIT_SOURCE);

        assertEquals(0, found.size());
    }
}
