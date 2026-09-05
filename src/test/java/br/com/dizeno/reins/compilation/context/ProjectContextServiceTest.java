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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectContextServiceTest {
    @TempDir
    Path projectRoot;

    private Path write(String relative, String content) throws Exception {
        Path file = projectRoot.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    

    @Test
    void nonEmptyFile_returnsPayloadWithOneFile() throws Exception {
        Path file = write("project.md", "# My compilation background");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(file.toFile(), projectRoot);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getFiles().size());
        assertEquals("project.md", result.getFiles().get(0).getDisplayPath());
        assertEquals("# My compilation background", result.getFiles().get(0).getContent());
    }

    @Test
    void defaultDepthLoadsOnlyDirectReferencedFiles() throws Exception {
        Path root = write("project.md", "# Root\n[conventions.md]\n");
        write("conventions.md", "# Conventions\n[deep.md]\n");
        write("deep.md", "# Deep");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(root.toFile(), projectRoot, Set.of(), ReferenceDepthPolicy.parse("1"));
        assertEquals(2, result.getFiles().size());
        assertEquals("project.md", result.getFiles().get(0).getDisplayPath());
        assertEquals("conventions.md", result.getFiles().get(1).getDisplayPath());
        assertFalse(result.getFiles().stream().anyMatch(file -> file.getDisplayPath().equals("deep.md")));
    }

    @Test
    void explicitDepthTwoLoadsSecondLevelDescendants() throws Exception {
        Path root = write("project.md", "# Root\n[conventions.md]\n");
        write("conventions.md", "# Conventions\n[deep.md]\n");
        write("deep.md", "# Deep");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(
                root.toFile(),
                projectRoot,
                Set.of(),
                ReferenceDepthPolicy.parse("2"));
        assertEquals(3, result.getFiles().size());
        assertEquals("deep.md", result.getFiles().get(2).getDisplayPath());
    }

    @Test
    void includeReferencesFalse_loadsOnlyRootFile() throws Exception {
        Path root = write("project.md", "# Root\n[conventions.md]\n");
        write("conventions.md", "# Conventions\n[deep.md]\n");
        write("deep.md", "# Deep");

        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(
                root.toFile(),
                projectRoot,
                Set.of(),
                ReferenceDepthPolicy.parse("*"),
                false);

        assertEquals(1, result.getFiles().size());
        assertEquals("project.md", result.getFiles().get(0).getDisplayPath());
    }

    @Test
    void cycleInRefTree_throwsMojoExecutionExceptionWithCycleDescription() throws Exception {
        Path root = write("project.md", "[a.md]");
        write("a.md", "[project.md]");
        ProjectContextService service = new ProjectContextService();
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> service.load(root.toFile(), projectRoot, Set.of(), ReferenceDepthPolicy.parse("*")));
        assertTrue(ex.getMessage().contains("Cycle detected"), "Exception should describe the cycle");
    }

    @Test
    void referenceOutsideProjectRoot_throwsMojoExecutionException() throws Exception {
        Path root = write("project.md", "[../outside.md]");
        ProjectContextService service = new ProjectContextService();
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> service.load(root.toFile(), projectRoot));
        assertTrue(ex.getMessage().contains("escapes project root"), "Exception should mention root escaping");
    }

    @Test
    void missingReferencedFile_throwsMojoExecutionException() throws Exception {
        Path root = write("project.md", "[missing-ref.md]");
        ProjectContextService service = new ProjectContextService();
        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> service.load(root.toFile(), projectRoot));
        assertTrue(ex.getMessage().contains("does not exist") || ex.getMessage().contains("Failed to read"),
                "Exception should mention the missing file");
    }

    @Test
    void diamondReferencePattern_deduplicatesSharedFile() throws Exception {
        write("project.md", "[a.md]\n[b.md]\n");
        write("a.md", "[shared.md]");
        write("b.md", "[shared.md]");
        write("shared.md", "# Shared");
        Path root = projectRoot.resolve("project.md");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(root.toFile(), projectRoot, Set.of(), ReferenceDepthPolicy.parse("2"));
        long sharedCount = result.getFiles().stream()
                .filter(f -> f.getDisplayPath().equals("shared.md"))
                .count();
        assertEquals(1, sharedCount, "shared.md should appear exactly once");
    }

    

    @Test
    void nullConfig_returnsEmpty() throws Exception {
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(null, projectRoot);
        assertTrue(result.isEmpty());
    }

    @Test
    void absentFile_returnsEmpty() throws Exception {
        java.io.File absent = projectRoot.resolve("project.md").toFile();
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(absent, projectRoot);
        assertTrue(result.isEmpty());
    }

    @Test
    void blankFile_returnsEmpty() throws Exception {
        Path file = write("project.md", "   ");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(file.toFile(), projectRoot);
        assertTrue(result.isEmpty());
    }

    @Test
    void whitespaceOnlyContent_returnsEmpty() throws Exception {
        Path file = write("project.md", "\n\n\t\n");
        ProjectContextService service = new ProjectContextService();
        CompilationBackgroundPayload result = service.load(file.toFile(), projectRoot);
        assertTrue(result.isEmpty());
    }

    

    @Test
    void referenceUnderMainNlRoot_throwsMojoExecutionException() throws Exception {
        
        Path mainNlRoot = projectRoot.resolve("src/main/nl");
        Files.createDirectories(mainNlRoot);
        write("src/main/nl/Foo.md", "# Foo");
        Path root = write("project.md", "[src/main/nl/Foo.md]");

        Set<Path> scanRoots = Set.of(mainNlRoot.toAbsolutePath().normalize());
        ProjectContextService service = new ProjectContextService();

        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> service.load(root.toFile(), projectRoot, scanRoots));
        assertTrue(ex.getMessage().contains("scan root"),
                "Exception should mention 'scan root', got: " + ex.getMessage());
    }

    @Test
    void referenceUnderTestNlRoot_throwsMojoExecutionException() throws Exception {
        
        Path testNlRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(testNlRoot);
        write("src/test/nl/BarTest.md", "# BarTest");
        Path root = write("project.md", "[src/test/nl/BarTest.md]");

        Set<Path> scanRoots = Set.of(testNlRoot.toAbsolutePath().normalize());
        ProjectContextService service = new ProjectContextService();

        MojoExecutionException ex = assertThrows(MojoExecutionException.class,
                () -> service.load(root.toFile(), projectRoot, scanRoots));
        assertTrue(ex.getMessage().contains("scan root"),
                "Exception should mention 'scan root', got: " + ex.getMessage());
    }

    @Test
    void referenceOutsideScanRoots_includedNormally() throws Exception {
        
        Path mainNlRoot = projectRoot.resolve("src/main/nl").toAbsolutePath().normalize();
        Path testNlRoot = projectRoot.resolve("src/test/nl").toAbsolutePath().normalize();
        write("docs/conventions.md", "# Conventions");
        Path root = write("project.md", "[docs/conventions.md]");

        Set<Path> scanRoots = Set.of(mainNlRoot, testNlRoot);
        ProjectContextService service = new ProjectContextService();

        CompilationBackgroundPayload result = service.load(root.toFile(), projectRoot, scanRoots);
        assertFalse(result.isEmpty());
        assertEquals(2, result.getFiles().size(), "Should include project.md + docs/conventions.md");
        assertTrue(result.getFiles().stream().anyMatch(f -> f.getDisplayPath().equals("docs/conventions.md")));
    }
}

