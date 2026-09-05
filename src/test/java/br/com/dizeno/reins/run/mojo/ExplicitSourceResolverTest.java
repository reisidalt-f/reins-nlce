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

package br.com.dizeno.reins.run.mojo;

import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitSourceResolverTest {

    @TempDir
    Path projectRoot;

    private Path mainRoot;
    private Path testRoot;
    private Map<String, File> sourceBases;

    @BeforeEach
    void setUp() throws Exception {
        mainRoot = Files.createDirectories(projectRoot.resolve("src/main/nl"));
        testRoot = Files.createDirectories(projectRoot.resolve("src/test/nl"));

        sourceBases = new LinkedHashMap<>();
        sourceBases.put("main", mainRoot.toFile());
        sourceBases.put("test", testRoot.toFile());
    }

    @Test
    void resolve_prefersMainDirectoryWhenBothBasesMatch() throws Exception {
        Files.createDirectories(mainRoot.resolve("feature"));
        Files.createDirectories(testRoot.resolve("feature"));
        Files.writeString(mainRoot.resolve("feature/main.md"), "# main");
        Files.writeString(testRoot.resolve("feature/test.md"), "# test");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "feature",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertTrue(result.directory());
        assertEquals(mainRoot.resolve("feature").toAbsolutePath().normalize(), result.resolvedPath());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/feature/main.md"));
    }

    @Test
    void resolve_failsWhenFileMatchIsAmbiguousAcrossMainAndTest() throws Exception {
        Files.createDirectories(mainRoot.resolve("nested"));
        Files.createDirectories(testRoot.resolve("nested"));
        Files.writeString(mainRoot.resolve("nested/spec.md"), "# main");
        Files.writeString(testRoot.resolve("nested/spec.md"), "# test");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                "nested/spec.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot)));

        assertTrue(ex.getMessage().contains("Ambiguous explicit source"));
    }

    @Test
    void resolve_findsFileRecursivelyInSubdirectory() throws Exception {
        Files.createDirectories(mainRoot.resolve("nested/sub"));
        Files.writeString(mainRoot.resolve("nested/sub/graphic-element.md"), "# Graphic Element");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "graphic-element.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertFalse(result.directory());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/nested/sub/graphic-element.md"));
    }

    @Test
    void resolve_prefersMainWhenFileExistsInBothRecursively() throws Exception {
        Files.createDirectories(mainRoot.resolve("a/b"));
        Files.createDirectories(testRoot.resolve("c/d"));
        Files.writeString(mainRoot.resolve("a/b/config.md"), "# main config");
        Files.writeString(testRoot.resolve("c/d/config.md"), "# test config");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "config.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertFalse(result.directory());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/a/b/config.md"));
    }

    @Test
    void resolve_findsFilesInNestedDirectory() throws Exception {
        Path targetDir = Files.createDirectories(mainRoot.resolve("br/com/dizeno/parlive/frontend/dominio"));
        Files.writeString(targetDir.resolve("comentario.md"), "# comentario");
        Files.writeString(targetDir.resolve("raia.md"), "# raia");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "br/com/dizeno/parlive/frontend/dominio",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertTrue(result.directory());
        assertEquals(2, result.files().size());
    }

    @Test
    void resolve_findsFilesInNestedDirectoryWithSourceBasePrefixInPath() throws Exception {
        Path targetDir = Files.createDirectories(mainRoot.resolve("br/com/dizeno/parlive/frontend/dominio"));
        Files.writeString(targetDir.resolve("comentario.md"), "# comentario");
        Files.writeString(targetDir.resolve("raia.md"), "# raia");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "src/main/nl/br/com/dizeno/parlive/frontend/dominio",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertTrue(result.directory());
        assertEquals(2, result.files().size());
    }

    @Test
    void resolve_findsFilesInNestedDirectoryWithPrefixedIncludePattern() throws Exception {
        Path targetDir = Files.createDirectories(mainRoot.resolve("br/com/dizeno/parlive/frontend/dominio"));
        Files.writeString(targetDir.resolve("comentario.md"), "# comentario");
        Files.writeString(targetDir.resolve("raia.md"), "# raia");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "main:br/com/dizeno/parlive/frontend/dominio",
                projectRoot,
                sourceBases,
                "br/com/dizeno/parlive/frontend/**/*.md",
                new PathValidator(projectRoot));

        assertTrue(result.directory());
        assertEquals(2, result.files().size());
    }

    @Test
    void resolve_multipleCommaSeparatedSources_resolvesAllAndDeduplicates() throws Exception {
        Files.createDirectories(mainRoot.resolve("domain"));
        Files.writeString(mainRoot.resolve("domain/Customer.md"), "# Customer");
        Files.writeString(mainRoot.resolve("domain/Order.md"), "# Order");
        Files.createDirectories(testRoot.resolve("domain"));
        Files.writeString(testRoot.resolve("domain/CustomerTest.md"), "# CustomerTest");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "domain/Customer.md, domain/Order.md, test:domain/CustomerTest.md, domain/Customer.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(3, result.files().size());
        assertEquals(3, result.resolvedPaths().size());
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("Customer.md")));
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("Order.md")));
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("CustomerTest.md")));
    }

    @Test
    void resolve_listOfSources_resolvesAll() throws Exception {
        Files.createDirectories(mainRoot.resolve("domain"));
        Files.writeString(mainRoot.resolve("domain/Customer.md"), "# Customer");
        Files.writeString(mainRoot.resolve("domain/Order.md"), "# Order");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                java.util.List.of("domain/Customer.md", "domain/Order.md"),
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(2, result.files().size());
        assertEquals(2, result.resolvedPaths().size());
    }

    @Test
    void resolve_globPattern_matchesFilesInSubdirectory() throws Exception {
        Files.createDirectories(mainRoot.resolve("domain"));
        Files.writeString(mainRoot.resolve("domain/Customer.md"), "# Customer");
        Files.writeString(mainRoot.resolve("domain/Order.md"), "# Order");
        Files.createDirectories(testRoot.resolve("domain"));
        Files.writeString(testRoot.resolve("domain/CustomerTest.md"), "# CustomerTest");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "domain/*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(3, result.files().size());
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("Customer.md")));
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("Order.md")));
        assertTrue(result.files().stream().anyMatch(f -> f.getName().equals("CustomerTest.md")));
    }

    @Test
    void resolve_globPatternWithScheme_matchesOnlyInSchemeBase() throws Exception {
        Files.createDirectories(mainRoot.resolve("domain"));
        Files.writeString(mainRoot.resolve("domain/Customer.md"), "# Customer");
        Files.writeString(mainRoot.resolve("domain/Order.md"), "# Order");
        Files.createDirectories(testRoot.resolve("domain"));
        Files.writeString(testRoot.resolve("domain/CustomerTest.md"), "# CustomerTest");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "main:domain/*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(2, result.files().size());
        assertTrue(result.files().stream().allMatch(f -> f.getPath().contains("src/main/nl")));
    }

    @Test
    void resolve_globPatternRecursive_matchesDeepFiles() throws Exception {
        Files.createDirectories(mainRoot.resolve("a/b"));
        Files.writeString(mainRoot.resolve("a/b/deep.md"), "# deep");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "**/*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(1, result.files().size());
        assertEquals("deep.md", result.files().get(0).getName());
    }

    @Test
    void resolve_globPatternWildcardFilename_matchesFilesByName() throws Exception {
        Files.createDirectories(mainRoot.resolve("nested/sub"));
        Files.writeString(mainRoot.resolve("nested/sub/graphic-element.md"), "# graphic");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "graphic-*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(1, result.files().size());
        assertEquals("graphic-element.md", result.files().get(0).getName());
    }

    @Test
    void resolve_globPatternNoMatches_throwsIllegalArgumentException() throws Exception {
        ExplicitSourceResolver resolver = new ExplicitSourceResolver();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                "nonexistent/*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot)));

        assertTrue(ex.getMessage().contains("does not resolve to an eligible directory or file"));
    }

    @Test
    void resolve_mixedExplicitAndGlobSources_resolvesAllAndDeduplicates() throws Exception {
        Files.createDirectories(mainRoot.resolve("domain"));
        Files.writeString(mainRoot.resolve("domain/Customer.md"), "# Customer");
        Files.writeString(mainRoot.resolve("domain/Order.md"), "# Order");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "domain/Customer.md, main:domain/*.md",
                projectRoot,
                sourceBases,
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(2, result.files().size());
    }
}
