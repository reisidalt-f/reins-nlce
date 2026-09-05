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

import static org.junit.jupiter.api.Assertions.*;

 
class ComposedFileMutationIsolationTest {

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
    @DisplayName("Write Mutation Isolation")
    class WriteMutationIsolation {

        @Test
        @DisplayName("Write to test-source always targets test-target during composition")
        void testWriteToTestTargetDuringComposition() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            request.setBase("target");
            request.setPath("NewClass.java");
            request.setContent("public class NewClass {}");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(Files.exists(fixture.getBasePath("test-target").resolve("NewClass.java")),
                    "File should be written to test-target");
            assertFalse(Files.exists(fixture.getBasePath("main-target").resolve("NewClass.java")),
                    "File should NOT be written to main-target");
        }

        @Test
        @DisplayName("Write to main-source uses main-target, ignoring composition")
        void testWriteToMainTargetIgnoresComposition() throws Exception {
            

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            request.setBase("target");
            request.setPath("MainClass.java");
            request.setContent("public class MainClass {}");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertTrue(Files.exists(fixture.getBasePath("main-target").resolve("MainClass.java")),
                    "File should be written to main-target");
        }

        @Test
        @DisplayName("Write with explicit base prefix always targets that base")
        void testWriteExplicitBasePrefix() throws Exception {
            

            
            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            request.setBase("main-target");  
            request.setPath("Violation.java");
            request.setContent("public class Violation {}");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            
            if (result.getStatus() == br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR) {
                assertTrue(result.getFailureReason().toLowerCase().contains("scope"),
                        "Should report scope violation");
            } else {
                
                assertTrue(Files.exists(fixture.getBasePath("test-target").resolve("Violation.java")),
                        "Should be routed to test-target (active scope)");
            }
        }
    }

    

    @Nested
    @DisplayName("Patch Mutation Isolation")
    class PatchMutationIsolation {

        @Test
        @DisplayName("Patch during test-source only updates test-target")
        void testPatchOnlyUpdatesActiveTarget() throws Exception {
            
            fixture.writeTargetFile("test-target", "Config.java", "public class Config { int val = 1; }");
            fixture.writeTargetFile("main-target", "Config.java", "public class Config { int val = 0; }");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE);
            request.setBase("target");
            request.setPath("Config.java");
            
            request.setContent("@@ -1 +1 @@\n-public class Config { int val = 1; }\n+public class Config { int val = 2; }");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            String testContent = new String(Files.readAllBytes(fixture.getBasePath("test-target").resolve("Config.java")));
            String mainContent = new String(Files.readAllBytes(fixture.getBasePath("main-target").resolve("Config.java")));
            assertTrue(testContent.contains("val = 2"), "Test target should be updated to 2");
            assertTrue(mainContent.contains("val = 0"), "Main target should remain 0");
        }

        @Test
        @DisplayName("Patch to main-source only updates main-target")
        void testPatchMainSourceStaysSingleBase() throws Exception {
            
            fixture.writeTargetFile("test-target", "Helper.java", "public class Helper {}");
            fixture.writeTargetFile("main-target", "Helper.java", "public class Helper {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE);
            request.setBase("target");
            request.setPath("Helper.java");
            request.setContent("@@ -1 +1 @@\n-public class Helper {}\n+public class Helper { String updated = true; }");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            String mainContent = new String(Files.readAllBytes(fixture.getBasePath("main-target").resolve("Helper.java")));
            assertTrue(mainContent.contains("updated"), "Main target should be updated");
        }
    }

    

    @Nested
    @DisplayName("Delete Mutation Isolation")
    class DeleteMutationIsolation {

        @Test
        @DisplayName("Delete during test-source only removes from test-target")
        void testDeleteOnlyRemovesFromActiveTarget() throws Exception {
            
            fixture.writeTargetFile("test-target", "Temporary.java", "public class Temporary {}");
            fixture.writeTargetFile("main-target", "Temporary.java", "public class Temporary {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE);
            request.setBase("target");
            request.setPath("Temporary.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertFalse(Files.exists(fixture.getBasePath("test-target").resolve("Temporary.java")),
                    "Test target file should be deleted");
            assertTrue(Files.exists(fixture.getBasePath("main-target").resolve("Temporary.java")),
                    "Main target file should still exist");
        }

        @Test
        @DisplayName("Delete during main-source only removes from main-target")
        void testDeleteMainSourceStaysSingleBase() throws Exception {
            
            fixture.writeTargetFile("test-target", "ToDelete.java", "public class ToDelete {}");
            fixture.writeTargetFile("main-target", "ToDelete.java", "public class ToDelete {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE);
            request.setBase("target");
            request.setPath("ToDelete.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "main");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
            assertFalse(Files.exists(fixture.getBasePath("main-target").resolve("ToDelete.java")),
                    "Main target file should be deleted");
            assertTrue(Files.exists(fixture.getBasePath("test-target").resolve("ToDelete.java")),
                    "Test target file should still exist");
        }

        @Test
        @DisplayName("Delete nonexistent returns error without affecting other base")
        void testDeleteNonexistentPreservesOtherBase() throws Exception {
            
            fixture.writeTargetFile("main-target", "OnlyInMain.java", "public class OnlyInMain {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE);
            request.setBase("target");
            request.setPath("OnlyInMain.java");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
            
            assertTrue(Files.exists(fixture.getBasePath("main-target").resolve("OnlyInMain.java")),
                    "Main target file should not be affected by delete error");
        }
    }

    

    @Nested
    @DisplayName("Cross-Scope Violation Detection")
    class CrossScopeViolationDetection {

        @Test
        @DisplayName("Mutation attempt on wrong target detected as scope violation")
        void testMutationScopeViolationDetection() throws Exception {
            
            fixture.writeTargetFile("main-target", "MainOnly.java", "public class MainOnly {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE);
            request.setBase("target");
            request.setPath("MainOnly.java");  
            request.setContent("@@ -1 +1 @@\n-public class MainOnly {}\n+public class MainOnly { /* patched */ }");

            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.executeWithScope(request, resolver, "test");

            
            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus(),
                    "Should reject mutation outside active scope");
            assertTrue(result.getFailureReason().toLowerCase().contains("scope"),
                    "Error should mention scope violation");
        }
    }

    

    @Nested
    @DisplayName("Composition Isolation from Mutations")
    class CompositionIsolationFromMutations {

        @Test
        @DisplayName("Write creates file only in active target despite composition")
        void testWriteIgnoresComposedReadBases() throws Exception {
            
            fixture.writeTargetFile("main-target", "ExistingFile.java", "public class ExistingFile {}");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest readRequest = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            readRequest.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE);
            readRequest.setBase("target");
            readRequest.setPath("ExistingFile.java");
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult readResult = toolingService.executeWithScope(readRequest, resolver, "test");
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, readResult.getStatus(),
                    "Composition should allow reading main-target file");

            
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest writeRequest = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
            writeRequest.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
            writeRequest.setBase("target");
            writeRequest.setPath("NewFileInTest.java");
            writeRequest.setContent("public class NewFileInTest {}");
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult writeResult = toolingService.executeWithScope(writeRequest, resolver, "test");

            
            assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, writeResult.getStatus());
            assertTrue(Files.exists(fixture.getBasePath("test-target").resolve("NewFileInTest.java")),
                    "Write should go to test-target (active scope), not composed!");
        }
    }
}
