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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplicitSourceResolverTest {

    @TempDir
    Path projectRoot;

    @Test
    void resolve_prefersMainDirectoryWhenBothBasesMatch() throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(mainRoot.resolve("feature"));
        Files.createDirectories(testRoot.resolve("feature"));
        Files.writeString(mainRoot.resolve("feature/main.md"), "# main");
        Files.writeString(testRoot.resolve("feature/test.md"), "# test");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "feature",
                projectRoot,
                mainRoot.toFile(),
                testRoot.toFile(),
                "**/*.md",
                new PathValidator(projectRoot));

        assertTrue(result.directory());
        assertEquals(mainRoot.resolve("feature").toAbsolutePath().normalize(), result.resolvedPath());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/feature/main.md"));
    }

    @Test
    void resolve_failsWhenFileMatchIsAmbiguousAcrossMainAndTest() throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(mainRoot.resolve("nested"));
        Files.createDirectories(testRoot.resolve("nested"));
        Files.writeString(mainRoot.resolve("nested/spec.md"), "# main");
        Files.writeString(testRoot.resolve("nested/spec.md"), "# test");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                "nested/spec.md",
                projectRoot,
                mainRoot.toFile(),
                testRoot.toFile(),
                "**/*.md",
                new PathValidator(projectRoot)));

        assertTrue(ex.getMessage().contains("Ambiguous explicit source"));
    }

    @Test
    void resolve_rejectsAbsoluteSourcePath() throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        String absolute = projectRoot.resolve("src/main/nl/file.md").toString();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                absolute,
                projectRoot,
                mainRoot.toFile(),
                testRoot.toFile(),
                "**/*.md",
                new PathValidator(projectRoot)));

        assertTrue(ex.getMessage().contains("must be a relative path"));
    }

    @Test
    void resolve_findsFileRecursivelyInSubdirectory() throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(mainRoot.resolve("nested/sub"));
        Files.createDirectories(testRoot);
        Files.writeString(mainRoot.resolve("nested/sub/graphic-element.md"), "# Graphic Element");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "graphic-element.md",
                projectRoot,
                mainRoot.toFile(),
                testRoot.toFile(),
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(false, result.directory());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/nested/sub/graphic-element.md"));
    }

    @Test
    void resolve_prefersMainWhenFileExistsInBothRecursively() throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Files.createDirectories(mainRoot.resolve("a/b"));
        Files.createDirectories(testRoot.resolve("c/d"));
        Files.writeString(mainRoot.resolve("a/b/config.md"), "# main config");
        Files.writeString(testRoot.resolve("c/d/config.md"), "# test config");

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "config.md",
                projectRoot,
                mainRoot.toFile(),
                testRoot.toFile(),
                "**/*.md",
                new PathValidator(projectRoot));

        assertEquals(false, result.directory());
        assertEquals(1, result.files().size());
        assertTrue(result.files().get(0).toPath().endsWith("src/main/nl/a/b/config.md"));
    }
}
