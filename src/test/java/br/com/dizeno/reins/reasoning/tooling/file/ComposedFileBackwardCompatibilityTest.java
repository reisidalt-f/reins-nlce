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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

 
class ComposedFileBackwardCompatibilityTest {

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

    @Nested
    @DisplayName("Backward Compatibility - Original execute() Method")
    class OriginalExecuteMethod {

        @Test
        @DisplayName("Original execute() method still available and functional")
        void testOriginalExecuteMethodExists() throws Exception {
            
            fixture.writeSourceFile("main-source", "Manager.java", "public class Manager {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("main");
            request.setPath("Manager.java");

            
            
            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, null);

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertNotNull(result.getContent());
            assertTrue(result.getContent().contains("Manager"));
        }

        @Test
        @DisplayName("Main-source processing produces same results as before")
        void testMainSourceBehaviorUnchanged() throws Exception {
            
            fixture.writeSourceFile("main-source", "MainClass.java", "public class MainClass {}");
            fixture.writeSourceFile("test-source", "TestClass.java", "public class TestClass {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
            request.setBase("main");
            request.setPath(".");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            List<String> listed = result.getListedPaths();
            assertTrue(listed.stream().anyMatch(p -> p.contains("MainClass")),
                    "Main source should include MainClass");
            assertFalse(listed.stream().anyMatch(p -> p.contains("TestClass")),
                    "Main source should NOT include TestClass");
            assertFalse(listed.stream().anyMatch(p -> p.contains(":")),
                    "No qualified paths in main-source mode");
        }
    }

    @Nested
    @DisplayName("Backward Compatibility - Write Operations")
    class WriteOperationsBackwardCompat {

        @Test
        @DisplayName("Write with main scope uses main-target (unchanged)")
        void testWriteMainScopeUnchanged() throws Exception {
            
            fixture.writeTargetFile("test-target", "TestOutput.java", "public class TestOutput {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            request.setBase("target");
            request.setPath("NewOutput.java");
            request.setContent("public class NewOutput {}");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(Files.exists(fixture.getBasePath("main-target").resolve("NewOutput.java")),
                    "Should write to main-target");
        }

        @Test
        @DisplayName("Write with null scope falls back to main-target")
        void testWriteNullScopeFallback() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            request.setBase("target");
            request.setPath("Output.java");
            request.setContent("public class Output {}");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, null);

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(Files.exists(fixture.getBasePath("main-target").resolve("Output.java")),
                    "Null scope should write to main-target");
        }
    }

    @Nested
    @DisplayName("Backward Compatibility - Read Operations")
    class ReadOperationsBackwardCompat {

        @Test
        @DisplayName("Read with main scope uses main-target only (no composition)")
        void testReadMainScopeUnchanged() throws Exception {
            
            fixture.writeTargetFile("test-target", "TestOnly.java", "public class TestOnly {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("TestOnly.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus(),
                    "Main scope should NOT fallback to test-target");
        }

        @Test
        @DisplayName("Read with existing main-target file works unchanged")
        void testReadMainTargetFileUnchanged() throws Exception {
            
            fixture.writeTargetFile("main-target", "MainOutput.java", "public class MainOutput {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("MainOutput.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(result.getContent().contains("MainOutput"));
        }
    }

    @Nested
    @DisplayName("Backward Compatibility - Patch & Delete")
    class PatchDeleteBackwardCompat {

        @Test
        @DisplayName("Patch with main scope only patches main-target")
        void testPatchMainScopeUnchanged() throws Exception {
            
            fixture.writeTargetFile("main-target", "Config.java", "public class Config { int x = 1; }");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE);
            request.setBase("target");
            request.setPath("Config.java");
            request.setContent("@@ -1 +1 @@\n-public class Config { int x = 1; }\n+public class Config { int x = 2; }");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus(),
                    "Patch should succeed; error: " + result.getFailureReason());
            String content = new String(java.nio.file.Files.readAllBytes(fixture.getBasePath("main-target").resolve("Config.java")));
            assertTrue(content.contains("x = 2"), "Should contain patched content");
        }

        @Test
        @DisplayName("Delete with main scope only removes from main-target")
        void testDeleteMainScopeUnchanged() throws Exception {
            
            fixture.writeTargetFile("main-target", "Temp.java", "public class Temp {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE);
            request.setBase("target");
            request.setPath("Temp.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            if (result.getStatus() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS) {
                assertFalse(java.nio.file.Files.exists(fixture.getBasePath("main-target").resolve("Temp.java")),
                        "File should be deleted");
            }
            
        }
    }

    @Nested
    @DisplayName("Backward Compatibility - Error Cases")
    class ErrorCasesBackwardCompat {

        @Test
        @DisplayName("Nonexistent file errors work unchanged")
        void testNonexistentFileErrorUnchanged() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("NonExistent.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
            assertTrue(result.getFailureReason().toLowerCase().contains("not exist"));
        }

        @Test
        @DisplayName("Invalid path errors work unchanged")
        void testInvalidPathErrorUnchanged() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            request.setBase("target");
            request.setPath("../../../etc/passwd");  

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        }
    }
}
