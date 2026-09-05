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
        ReinsConfig config = configWithTarget("compiled/project", "src/main/java", "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "main");

        Path expected = config.getTarget().resolveTargetOutput("main", projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for main scope must equal resolveTargetOutput(\"main\", projectRoot)");
    }

    @Test
    void forScope_test_returnsTestOutputRoot_whenTargetTestIsConfigured() {
        ReinsConfig config = configWithTarget("compiled/project", "src/main/java", "src/test/java");

        BasePathMappingSet mappings = BasePathMappingSet.forScope(config, projectDir, "test");

        Path expected = config.getTarget().resolveTargetOutput("test", projectDir);
        assertEquals(expected, mappings.getTargetRoot(),
                "targetRoot for test scope must equal resolveTargetOutput(\"test\", projectRoot)");
    }

    private ReinsConfig configWithTarget(String project, String main, String test) {
        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        if (main != null) {
            target.setTargetBase("main", main);
        }
        if (test != null) {
            target.setTargetBase("test", test);
        }
        config.setTarget(target);
        return config;
    }
}
