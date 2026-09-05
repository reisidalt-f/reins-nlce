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

package br.com.dizeno.reins.reasoning.tooling.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

 
public class CollisionDetectionDebugTest {

    @TempDir
    Path tempDir;

    private FileComposedViewFixture fixture;
    private br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet mappings;
    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService;

    @BeforeEach
    void setUp() {
        fixture = new FileComposedViewFixture(tempDir);

        mappings = new br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet();
        mappings.setMainRoot(fixture.getBasePath("main-source"));
        mappings.setTestRoot(fixture.getBasePath("test-source"));
        mappings.setTargetRoot(fixture.getBasePath("main-target"));

        resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new br.com.dizeno.reins.security.PathValidator(tempDir));
        toolingService = new br.com.dizeno.reins.reasoning.tooling.ToolingService();
    }

    @Test
    void testCollisionDetectionDebug() throws Exception {
        
        fixture.writeSourceFile("test-source", "helpers.java", "public class TestHelpers {}");
        fixture.writeSourceFile("main-source", "helpers.java", "public class MainHelpers {}");

        System.out.println("Test base: " + fixture.getBasePath("test-source"));
        System.out.println("Main base: " + fixture.getBasePath("main-source"));

        
        fixture.readFile("test-source", "helpers.java");
        System.out.println("Test helpers.java found");

        fixture.readFile("main-source", "helpers.java");
        System.out.println("Main helpers.java found");

        
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
        request.setBase("main");  
        request.setPath("helpers.java");

        System.out.println("\nCalling executeWithScope with sourceScope='test'");
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

        System.out.println("Result status: " + result.getStatus());
        System.out.println("Result failure reason: " + result.getFailureReason());
        System.out.println("Has conflict: " + result.hasConflict());
        System.out.println("Conflict candidates: " + result.getConflictCandidates());
        System.out.println("Conflict message: " + result.getConflictMessage());

        
        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus(),
                "Status should be ERROR for collision");
        assertTrue(result.hasConflict(), "Should have conflict markers");
        assertNotNull(result.getConflictCandidates());
        assertEquals(2, result.getConflictCandidates().size(), "Should have 2 candidates");
    }
}
