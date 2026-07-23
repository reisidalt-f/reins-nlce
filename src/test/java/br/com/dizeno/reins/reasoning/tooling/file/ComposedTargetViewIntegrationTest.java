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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

 
class ComposedTargetViewIntegrationTest {

    @TempDir
    Path tempDir;

    private FileComposedViewFixture fixture;
    private br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet mappings;
    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver;
    private br.com.dizeno.reins.reasoning.tooling.ToolingService mcpService;

    @BeforeEach
    void setUp() {
        fixture = new FileComposedViewFixture(tempDir);

        
        mappings = new br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet();
        mappings.setMainRoot(fixture.getBasePath("main-source"));
        mappings.setTestRoot(fixture.getBasePath("test-source"));
        mappings.setTargetRoot(fixture.getBasePath("main-target"));
        
        

        resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new br.com.dizeno.reins.security.PathValidator(tempDir));
        mcpService = new br.com.dizeno.reins.reasoning.tooling.ToolingService();
    }

    

    @Nested
    @DisplayName("Composed Target List Operations (Test-Source)")
    class ComposedTargetList {

        @Test
        @DisplayName("Target list merges entries from both test-target and main-target")
        void testTargetListMergesBothBases() throws Exception {
            
            fixture.writeTargetFile("main-target", "Manager.compiled.java",
                    "// Compiled from main\npublic class Manager {}");
            fixture.writeTargetFile("test-target", "TestRunner.compiled.java",
                    "// Compiled from test\npublic class TestRunner {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("target");  
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertTrue(listed.size() >= 2, "Should have at least 2 files from targets");
            assertTrue(listed.stream().anyMatch(p -> p.contains("TestRunner.compiled.java")),
                    "Should include test-compiled output");
            assertTrue(listed.stream().anyMatch(p -> p.contains("Manager.compiled.java")),
                    "Should include main-compiled output");
        }

        @Test
        @DisplayName("Target list preserves discovery order (test-target before main-target)")
        void testTargetListDiscoveryOrder() throws Exception {
            
            fixture.writeTargetFile("test-target", "Compiled.java", "// From test target");
            fixture.writeTargetFile("main-target", "Compiled.java", "// From main target");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("target");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            List<String> listed = result.getListedPaths();
            assertTrue(listed.contains("target:Compiled.java"),
                    "Target-qualified entry should be present");
        }
    }

    

    @Nested
    @DisplayName("Composed Target Read Operations (Test-Source)")
    class ComposedTargetRead {

        @Test
        @DisplayName("Target read unprefixed returns unique candidate from test-target")
        void testTargetReadUnprefixedFromTest() throws Exception {
            
            fixture.writeTargetFile("test-target", "TestHelper.compiled.java",
                    "// Compiled for test\npublic class TestHelper {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("TestHelper.compiled.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("TestHelper"));
            assertEquals("target", result.getResolvedBase());
        }

        @Test
        @DisplayName("Target read unprefixed fallsback to main-target when test empty")
        void testTargetReadUnprefixedFallbackToMain() throws Exception {
            
            fixture.writeTargetFile("main-target", "Manager.compiled.java",
                    "// Compiled for main\npublic class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("Manager.compiled.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("Manager"));
            assertEquals("target", result.getResolvedBase());
        }

        @Test
        @DisplayName("Target read prefers active-scope target when ambiguous")
        void testTargetReadCollisionDetection() throws Exception {
            
            fixture.writeTargetFile("test-target", "Common.compiled.java", "// From test");
            fixture.writeTargetFile("main-target", "Common.compiled.java", "// From main");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("Common.compiled.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("From test"));
            assertEquals("target", result.getResolvedBase());
        }
    }

    

    @Nested
    @DisplayName("Single-Base Target Behavior (Main-Source)")
    class SingleBaseTargetBehavior {

        @Test
        @DisplayName("Main-source target list uses only main-target")
        void testMainSourceTargetListSingleBase() throws Exception {
            
            fixture.writeTargetFile("main-target", "Manager.compiled.java", "// From main target");
            fixture.writeTargetFile("test-target", "TestRunner.compiled.java", "// From test target");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("target");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertTrue(listed.stream().anyMatch(p -> p.contains("Manager.compiled.java")),
                    "Should include main-target's Manager.compiled.java");
            assertFalse(listed.stream().anyMatch(p -> p.contains("TestRunner.compiled.java")),
                    "Should NOT include test-target's TestRunner.compiled.java");
        }

        @Test
        @DisplayName("Main-source target read doesn't fallback to test-target")
        void testMainSourceTargetReadNoFallback() throws Exception {
            
            fixture.writeTargetFile("test-target", "TestHelper.compiled.java", "// From test");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("TestHelper.compiled.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
            assertTrue(result.getFailureReason().contains("not exist"),
                    "Should report file not found, not fallback to test-target");
        }
    }

    

    @Nested
    @DisplayName("Nested Directory Composition")
    class NestedDirectoryComposition {

        @Test
        @DisplayName("Composed list includes entries from nested subdirectories")
        void testComposedListNestedDirectories() throws Exception {
            
            fixture.writeTargetFile("test-target", "compiled/test/TestRunner.java", "public class TestRunner {}");
            fixture.writeTargetFile("main-target", "compiled/main/Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("target");
            request.setPath("compiled");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(result.getListedPaths().size() > 0, "Should have entries from nested directories");
        }
    }
}
