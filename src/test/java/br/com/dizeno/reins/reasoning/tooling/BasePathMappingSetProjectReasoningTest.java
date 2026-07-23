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
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasePathMappingSetProjectReasoningTest {

    @TempDir
    Path projectDir;

    

    @Test
    void forProjectInference_setsMainRootToProjectRoot() {
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, projectDir.resolve("target"));
        assertEquals(projectDir.toAbsolutePath().normalize(), mappings.getMainRoot(),
                "mainRoot should be the normalised project root");
    }

    @Test
    void forProjectInference_setsTargetRootToConfiguredRoot() {
        Path targetRoot = projectDir.resolve("target/project");
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, targetRoot);
        assertEquals(targetRoot.toAbsolutePath().normalize(), mappings.getTargetRoot(),
                "targetRoot should be the configured output root, not the project root");
    }

    @Test
    void fromConfig_routesProjectFallbackToCanonicalProjectTarget() {
        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        target.setProject(projectDir.resolve("compiled/project").toFile());
        config.setTarget(target);

        BasePathMappingSet mappings = BasePathMappingSet.fromConfig(config, projectDir);

        assertEquals(projectDir.resolve("compiled/project").toAbsolutePath().normalize(), mappings.getTargetRoot());
    }

    @Test
    void forProjectInference_setsTestRootToNull() {
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, projectDir.resolve("target"));
        assertNull(mappings.getTestRoot(), "testRoot must be null for project inference");
    }

    @Test
    void forProjectInference_normalisesRelativeProjectRoot() {
        Path relative = Path.of(".");
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(relative, relative.resolve("target"));
        assertEquals(relative.toAbsolutePath().normalize(), mappings.getMainRoot(),
                "A relative path should be normalised to absolute");
    }

    

    @Test
    void resolveBaseRoot_throwsIllegalStateException_whenTestBaseRequestedAndTestRootIsNull() {
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, projectDir.resolve("target"));
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(projectDir));
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveBaseRoot("test"),
                "Requesting 'test' base when testRoot is null should throw IllegalStateException");
    }

    @Test
    void qualifyAbsolute_doesNotThrow_whenPathIsUnderMainRootAndTestRootIsNull() {
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, projectDir.resolve("target"));
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(projectDir));
        Path fileUnderMain = projectDir.resolve("project.md").toAbsolutePath().normalize();
        
        String qualified = resolver.qualifyAbsolute(fileUnderMain);
        assertEquals("main:project.md", qualified,
            "A file under mainRoot should be qualified as canonical 'main:<relative>'");
    }

    @Test
    void resolveBaseRoot_acceptsProjectPrefix_andResolvesToProjectRoot() throws Exception {
        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(projectDir, projectDir.resolve("target"));
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(projectDir));

        Path root = resolver.resolveBaseRoot("project");
        assertEquals(projectDir.toAbsolutePath().normalize(), root,
                "project base should resolve to mainRoot during project inference");

        assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve("project", "project.md"),
            "FileReference canonicalization accepts only main/test/target bases");
    }
}
