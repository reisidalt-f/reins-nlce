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
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasePathMappingSetScopeTest {

    @TempDir
    Path projectDir;

    

    @Test
    void forScope_main_returnsMainOutputRoot_whenTargetMainIsConfigured() {
        ReinsConfig config = configWithTarget("compiled/project", null, "src/main/java", null);

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "main");

        Path expected = config.getTarget().resolveMainOutput(projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for main scope must equal resolveMainOutput(projectRoot)");
    }

    @Test
    void forScope_main_defaultsToSrcMainJava_whenTargetMainIsAbsent() {
        ReinsConfig config = configWithTarget("compiled/project", null, null, null);

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "main");

        Path expected = projectDir.resolve("src/main/java").toAbsolutePath().normalize();
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for main scope must default to src/main/java when target.main is absent");
    }

    @Test
    void forScope_null_fallsBackToTargetRoot() {
        ReinsConfig config = configWithTarget("compiled/project", null, "src/main/java", "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, null);

        Path expected = config.getTarget().resolveProjectTarget(projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
            "null sourceScope must fall back to resolveProjectTarget");
    }

    @Test
        void forScope_projectScope_routesToProjectTarget() {
        ReinsConfig config = configWithTarget("compiled/project", null, "src/main/java", "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "project");

        Path expected = config.getTarget().resolveProjectTarget(projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
            "project sourceScope must route to resolveProjectTarget");
    }

    @Test
    void forScope_noTargetGroup_returnsProjectRoot() {
        ReinsConfig config = new ReinsConfig();
        

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "main");

        assertEquals(projectDir.toAbsolutePath().normalize(), mappings.getTargetRoot(),
                "when no <target> group is configured, targetRoot must be projectRoot");
    }

    

    @Test
    void forScope_test_returnsTestOutputRoot_whenTargetTestIsConfigured() {
        ReinsConfig config = configWithTarget("compiled/project", null, null, "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "test");

        Path expected = config.getTarget().resolveTestOutput(projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for test scope must equal resolveTestOutput(projectRoot)");
    }

    @Test
    void forScope_test_defaultsToSrcTestJava_whenTargetTestIsAbsent() {
        ReinsConfig config = configWithTarget("compiled/project", null, null, null);

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "test");

        Path expected = projectDir.resolve("src/test/java").toAbsolutePath().normalize();
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for test scope must default to src/test/java when target.test is absent");
    }

    @Test
    void forScope_main_doesNotResolveRelativeToProjectTarget() {
        ReinsConfig config = configWithTarget("compiled/project", null, "src/main/java", null);

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "main");

        assertEquals(projectDir.resolve("src/main/java").toAbsolutePath().normalize(), mappings.getTargetRoot());
    }

    @Test
    void forScope_test_doesNotResolveRelativeToProjectTarget() {
        ReinsConfig config = configWithTarget("compiled/project", null, null, "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "test");

        assertEquals(projectDir.resolve("src/test/java").toAbsolutePath().normalize(), mappings.getTargetRoot());
    }

    

    private ReinsConfig configWithTarget(String project, String root, String main, String test) {
        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        if (project != null) {
            target.setProject(projectDir.resolve(project).toFile());
        }
        if (root != null) {
            target.setLegacyRootAlias(projectDir.resolve(root).toFile());
        }
        target.setMain(main);
        target.setTest(test);
        config.setTarget(target);
        return config;
    }
}
