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

package br.com.dizeno.reins.reasoning.tooling;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasePathResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesActiveOutputBaseWithDefaultTarget() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();

        assertEquals("target", resolver.resolveActiveOutputBase(null));
        assertEquals("main", resolver.resolveActiveOutputBase("main"));
    }

    @Test
    void detectsCanonicalContainmentInsideTargetBase() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        Path insideTarget = tempDir.resolve("src/main/java/demo/App.java");
        Path outsideTarget = tempDir.resolve("docs/spec.md");
        Files.createDirectories(insideTarget.getParent());
        Files.createDirectories(outsideTarget.getParent());
        Files.writeString(insideTarget, "class App {}\n");
        Files.writeString(outsideTarget, "# spec\n");

        assertTrue(resolver.isWithinTargetBase(insideTarget));
        assertFalse(resolver.isWithinTargetBase(outsideTarget));
    }

    @Test
    void rejectsPathEscapingProjectRootDuringResolve() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve("target", "../../../../etc/passwd"));
        assertTrue(ex.getMessage().contains("Path traversal not allowed"));
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver createResolver() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path testRoot = tempDir.resolve("src/test/nl");
        Path targetRoot = tempDir.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot);

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainRoot.toAbsolutePath().normalize());
        mappings.setTestRoot(testRoot.toAbsolutePath().normalize());
        mappings.setTargetRoot(targetRoot.toAbsolutePath().normalize());
        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
    }
}
