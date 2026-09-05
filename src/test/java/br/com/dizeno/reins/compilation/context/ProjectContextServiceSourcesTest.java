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

import br.com.dizeno.reins.run.config.settings.ContextSourceSpec;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectContextServiceSourcesTest {

    @TempDir
    Path projectRoot;

    private Path write(String relative, String content) throws Exception {
        Path file = projectRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private ProjectContextService newService() {
        return new ProjectContextService();
    }

    private List<ContextSourceSpec> toSpecs(List<File> files) {
        return files == null ? List.of() : files.stream().map(ContextSourceSpec::new).toList();
    }

    @Test
    void nullSourcesList_returnsEmptyResult() throws Exception {
        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(null, projectRoot, Set.of(), Set.of());
        assertNotNull(result);
        assertTrue(result.getCombinedFiles().isEmpty());
        assertTrue(result.getLoadedExprs().isEmpty());
        assertTrue(result.getBlankExprs().isEmpty());
    }

    @Test
    void emptySourcesList_returnsEmptyResult() throws Exception {
        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(List.of(), projectRoot, Set.of(), Set.of());
        assertNotNull(result);
        assertTrue(result.getCombinedFiles().isEmpty());
        assertTrue(result.getLoadedExprs().isEmpty());
        assertTrue(result.getBlankExprs().isEmpty());
    }

    @Test
    void nonMdEntry_throwsMojoExecutionException() throws Exception {
        Path notMd = write("docs/notes.txt", "some content");
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> newService().loadSources(toSpecs(List.of(notMd.toFile())), projectRoot, Set.of(), Set.of()));
        assertTrue(ex.getMessage().contains("must be a .md file"),
                "Exception should mention .md requirement; got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("notes.txt"),
                "Exception should identify offending file; got: " + ex.getMessage());
    }

    @Test
    void missingFile_throwsMojoExecutionException() throws Exception {
        File missing = projectRoot.resolve("docs/missing.md").toFile();
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> newService().loadSources(toSpecs(List.of(missing)), projectRoot, Set.of(), Set.of()));
        assertTrue(ex.getMessage().contains("does not exist"),
                "Exception should mention file does not exist; got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("missing.md"),
                "Exception should identify missing file; got: " + ex.getMessage());
    }

    @Test
    void blankFile_addedToBlankExprsNotCombinedFiles() throws Exception {
        Path blank = write("docs/empty.md", "   ");
        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(toSpecs(List.of(blank.toFile())), projectRoot, Set.of(), Set.of());
        assertTrue(result.getCombinedFiles().isEmpty(), "blank file must not appear in combinedFiles");
        assertTrue(result.getLoadedExprs().isEmpty(), "blank file must not appear in loadedExprs");
        assertEquals(1, result.getBlankExprs().size(), "blank file should be in blankExprs");
        assertTrue(result.getBlankExprs().get(0).endsWith("empty.md"),
                "blankExpr should contain the file name");
    }

    @Test
    void validMdFile_addedToCombinedFilesAndLoadedExprs() throws Exception {
        Path source = write("docs/context.md", "# Context");
        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(toSpecs(List.of(source.toFile())), projectRoot, Set.of(), Set.of());
        assertEquals(1, result.getCombinedFiles().size());
        assertEquals(1, result.getLoadedExprs().size());
        assertTrue(result.getBlankExprs().isEmpty());
        assertEquals("docs/context.md", result.getCombinedFiles().get(0).getDisplayPath());
        assertEquals("# Context", result.getCombinedFiles().get(0).getContent());
    }

    @Test
    void alreadyVisited_fileSkipped() throws Exception {
        Path source = write("docs/shared.md", "# Shared context");
        Set<Path> alreadyVisited = Set.of(source.toAbsolutePath().normalize());
        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(toSpecs(List.of(source.toFile())), projectRoot, Set.of(), alreadyVisited);
        assertTrue(result.getCombinedFiles().isEmpty(),
                "File already in alreadyVisited must not appear in combinedFiles");
    }

    @Test
    void defaultDepthSkipsGrandchildrenForContextSources() throws Exception {
        Path rootSource = write("docs/context.md", "[child.md]");
        write("docs/child.md", "[grandchild.md]");
        write("docs/grandchild.md", "# Grandchild");

        ProjectContextService.SourcesLoadResult result =
                newService().loadSources(toSpecs(List.of(rootSource.toFile())), projectRoot, Set.of(), Set.of(), ReferenceDepthPolicy.parse("1"));

        assertEquals(2, result.getCombinedFiles().size());
        assertTrue(result.getCombinedFiles().stream().anyMatch(file -> file.getDisplayPath().equals("docs/context.md")));
        assertTrue(result.getCombinedFiles().stream().anyMatch(file -> file.getDisplayPath().equals("docs/child.md")));
        assertFalse(result.getCombinedFiles().stream().anyMatch(file -> file.getDisplayPath().equals("docs/grandchild.md")));
    }

    @Test
    void unlimitedDepthIncludesAllDescendantsForContextSources() throws Exception {
        Path rootSource = write("docs/context.md", "[child.md]");
        write("docs/child.md", "[grandchild.md]");
        write("docs/grandchild.md", "# Grandchild");

        ProjectContextService.SourcesLoadResult result = newService().loadSources(
                toSpecs(List.of(rootSource.toFile())),
                projectRoot,
                Set.of(),
                Set.of(),
                ReferenceDepthPolicy.parse("*"));

        assertEquals(3, result.getCombinedFiles().size());
        assertTrue(result.getCombinedFiles().stream().anyMatch(file -> file.getDisplayPath().equals("docs/grandchild.md")));
    }

    @Test
    void topLevelEntryUnderScanRoot_throwsMojoExecutionException() throws Exception {
        Path mainNlRoot = projectRoot.resolve("src/main/nl").toAbsolutePath();
        Files.createDirectories(mainNlRoot);
        Path sourceUnderScanRoot = write("src/main/nl/domain.md", "# Domain instructions");
        Set<Path> scanRoots = Set.of(mainNlRoot);

        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> newService().loadSources(toSpecs(List.of(sourceUnderScanRoot.toFile())), projectRoot, scanRoots, Set.of()));
        assertTrue(ex.getMessage().contains("scan root"),
                "Exception should mention scan root; got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("domain.md"),
                "Exception should identify offending file; got: " + ex.getMessage());
    }

    @Test
    void transitiveReferenceUnderScanRoot_throwsMojoExecutionException() throws Exception {
        Path testNlRoot = projectRoot.resolve("src/test/nl").toAbsolutePath();
        Files.createDirectories(testNlRoot);

        Path contextDoc = write("docs/doc.md", "[../src/test/nl/test-domain.md]");
        write("src/test/nl/test-domain.md", "# Test domain instructions");
        Set<Path> scanRoots = Set.of(testNlRoot);

        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> newService().loadSources(toSpecs(List.of(contextDoc.toFile())), projectRoot, scanRoots, Set.of()));
        assertTrue(ex.getMessage().contains("scan root"),
                "Exception should mention scan root; got: " + ex.getMessage());
    }

    @Test
    void transitiveReferenceOutsideProjectRoot_throwsMojoExecutionException() throws Exception {
        Path contextDoc = write("docs/doc.md", "[../../outside-project.md]");
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> newService().loadSources(toSpecs(List.of(contextDoc.toFile())), projectRoot, Set.of(), Set.of()));
        assertTrue(ex.getMessage().contains("escapes project root") || ex.getMessage().contains("outside project"),
                "Exception should mention root sandbox; got: " + ex.getMessage());
    }
}
