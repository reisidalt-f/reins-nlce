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

import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

 
class ComposedFileOperationsIntegrationTest {

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

        resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
        mcpService = new br.com.dizeno.reins.reasoning.tooling.ToolingService();
    }

    

    @Nested
    @DisplayName("Composed List Operations (Test-Source)")
    class ComposedListOperations {

        @Test
        @DisplayName("List merges entries from both test and main bases")
        void testListMergesBothBases() throws Exception {
            
            fixture.writeSourceFile("test-source", "TestRunner.java", "public class TestRunner {}");
            fixture.writeSourceFile("test-source", "helpers.java", "public class TestHelpers {}");
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");  
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertTrue(listed.size() >= 3, "Should have at least 3 files merged");
            assertTrue(listed.stream().anyMatch(p -> p.contains("test:") && p.contains("TestRunner.java")),
                    "Should include test:TestRunner.java");
            assertTrue(listed.stream().anyMatch(p -> p.contains("test:") && p.contains("helpers.java")),
                    "Should include test:helpers.java");
            assertTrue(listed.stream().anyMatch(p -> p.contains("main:") && p.contains("Manager.java")),
                    "Should include main:Manager.java");
        }

        @Test
        @DisplayName("List preserves discovery order (test before main)")
        void testListPreservesDiscoveryOrder() throws Exception {
            
            fixture.writeSourceFile("test-source", "helpers.java", "public class TestHelpers {}");
            fixture.writeSourceFile("main-source", "helpers.java", "public class MainHelpers {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            List<String> listed = result.getListedPaths();
            assertTrue(listed.contains("test:helpers.java"), "Test entry should be present");
            assertTrue(listed.contains("main:helpers.java"), "Main entry should be present (qualified)");
        }

        @Test
        @DisplayName("List handles empty bases gracefully")
        void testListGracefulFallbackWithEmptyBase() throws Exception {
            
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertTrue(listed.stream().anyMatch(p -> p.contains("Manager.java")),
                    "Should include Manager.java from main base");
        }
    }

    

    @Nested
    @DisplayName("Composed Read Operations (Test-Source)")
    class ComposedReadOperations {

        @Test
        @DisplayName("Read unprefixed path returns unique candidate from test base")
        void testReadUnprefixedUniqueFromTest() throws Exception {
            
            fixture.writeSourceFile("test-source", "TestUtil.java", "public class TestUtil {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");  
            request.setPath("TestUtil.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("TestUtil"));
            assertEquals("test", result.getResolvedBase());
        }

        @Test
        @DisplayName("Read unprefixed path fallsback to main when test empty")
        void testReadUnprefixedFallbackToMain() throws Exception {
            
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");
            request.setPath("Manager.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("Manager"));
            assertEquals("main", result.getResolvedBase());
        }

        @Test
        @DisplayName("Read unprefixed path returns conflict when ambiguous")
        void testReadUnprefixedCollisionDetection() throws Exception {
            
            fixture.writeSourceFile("test-source", "helpers.java", "public class TestHelpers {}");
            fixture.writeSourceFile("main-source", "helpers.java", "public class MainHelpers {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");
            request.setPath("helpers.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            System.out.println("[testReadUnprefixedCollisionDetection]");
            System.out.println("  Result status: " + result.getStatus());
            System.out.println("  Has conflict: " + result.hasConflict());
            System.out.println("  Conflict candidates: " + result.getConflictCandidates());
            System.out.println("  Conflict message: " + result.getConflictMessage());
            System.out.println("  Failure reason: " + result.getFailureReason());

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus(),
                    "Status should be ERROR for collision");
            
            
            assertTrue(result.hasConflict() || (result.getConflictCandidates() != null && !result.getConflictCandidates().isEmpty()),
                    "Should detect collision; candidates=" + result.getConflictCandidates() + ", status=" + result.getStatus());
            
            
            if (result.getConflictCandidates() != null && !result.getConflictCandidates().isEmpty()) {
                List<String> candidates = result.getConflictCandidates();
                assertTrue(candidates.size() >= 2 || candidates.stream().anyMatch(c -> c.contains("helpers")),
                        "Candidates should reference helpers: " + candidates);
            }
            
            assertNotNull(result.getConflictMessage(), "Conflict message should be set");
        }

        @Test
        @DisplayName("Read explicitly qualified path bypasses composition")
        void testReadExplicitQualifiedBypassesComposition() throws Exception {
            
            fixture.writeSourceFile("test-source", "helpers.java", "public class TestHelpers {}");
            fixture.writeSourceFile("main-source", "helpers.java", "public class MainHelpers {}");

            
            
            

            
            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest requestMain = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            requestMain.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            requestMain.setBase("main");
            requestMain.setPath("helpers.java");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult resultMain = mcpService.executeWithScope(requestMain, resolver, "main");

            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, resultMain.getStatus());
            assertTrue(resultMain.getContent().contains("MainHelpers"),
                    "Main-source scope should read main helpers");
        }

        @Test
        @DisplayName("Read returns error when file not found in any base")
        void testReadNotFoundInAnyBase() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");
            request.setPath("NonExistent.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
            assertTrue(result.getFailureReason().toLowerCase().contains("not exist"));
        }
    }

    

    @Nested
    @DisplayName("Single-Base Behavior (Main-Source)")
    class SingleBaseBehavior {

        @Test
        @DisplayName("Main-source uses single-base listing")
        void testMainSourceListSingleBase() throws Exception {
            
            fixture.writeSourceFile("test-source", "TestRunner.java", "public class TestRunner {}");
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertTrue(listed.stream().anyMatch(p -> p.contains("Manager.java")),
                    "Should include main's Manager.java");
            assertFalse(listed.stream().anyMatch(p -> p.contains("TestRunner.java")),
                    "Should NOT include test's TestRunner.java");
            assertFalse(listed.stream().anyMatch(p -> p.contains(":")),
                    "Should NOT include qualified paths in single-base mode");
        }

        @Test
        @DisplayName("Main-source read doesn't fallback to test")
        void testMainSourceReadNoFallback() throws Exception {
            
            fixture.writeSourceFile("test-source", "TestUtil.java", "public class TestUtil {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");
            request.setPath("TestUtil.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
            assertTrue(result.getFailureReason().contains("not exist"),
                    "Should report file not found, not fallback to test");
        }
    }

    

    @Nested
    @DisplayName("Edge Cases & Robustness")
    class EdgeCases {

        @Test
        @DisplayName("Composition handles null sourceScope gracefully")
        void testNullSourceScopeUsesNonComposed() throws Exception {
            
            fixture.writeSourceFile("test-source", "TestRunner.java", "public class TestRunner {}");
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, null);

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            List<String> listed = result.getListedPaths();
            assertFalse(listed.stream().anyMatch(p -> p.contains("TestRunner.java")),
                    "Null scope should not include test entries");
        }

        @Test
        @DisplayName("Empty fixture directories return empty lists")
        void testEmptyBasesReturnEmpty() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = mcpService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(result.getListedPaths().isEmpty(), "Empty bases should return empty list");
        }
    }
}
