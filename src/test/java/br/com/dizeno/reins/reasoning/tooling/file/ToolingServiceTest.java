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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolingServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void listFilesReturnsEmptyWhenDirectoryDoesNotExist() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES);
        request.setBase("target");
        request.setPath("md/editor/layout");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getListedPaths().isEmpty());
    }

    @Test
    void listCompiledFilesReturnsDirectSourceResultsInDeterministicOrder() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        writeManifest(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("feature.md");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals(List.of(
            "target:main/java/app/FeatureController.java",
            "target:main/resources/app/feature-config.yml"
        ), result.getListedPaths());
    }

    @Test
    void listCompiledFilesReturnsEmptyWhenNoTrackingExists() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        writeManifest(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("missing.md");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getListedPaths().isEmpty());
    }

    @Test
    void listCompiledFilesRejectsOutOfSandboxSourcePath() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        writeManifest(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("../../../../../../etc/passwd");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("Path traversal not allowed"));
    }

    @Test
    void listCompiledFilesLoadsTrackingOnlyOnceWhenUnchanged() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:feature.md");
        record.setCompiledFiles(new java.util.LinkedHashMap<>());
        when(trackingStore.canonicalizePath(eq("src/main/nl/feature.md"))).thenReturn("main:feature.md");
        when(trackingStore.load(eq(tempDir.toAbsolutePath().normalize()), eq("main:feature.md")))
                .thenReturn(Optional.of(record));

        br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService = new br.com.dizeno.reins.reasoning.tooling.ToolingService(new FilePatchApplier(), trackingStore);
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("feature.md");

        toolingService.execute(request, resolver);
        toolingService.execute(request, resolver);

        verify(trackingStore, times(2)).load(eq(tempDir.toAbsolutePath().normalize()), eq("main:feature.md"));
    }

    @Test
    void deniedListCompiledFilesDoesNotLoadTrackingStore() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES);
        request.setBase("main");
        request.setPath("feature.md");

        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy permission = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService = new br.com.dizeno.reins.reasoning.tooling.ToolingService(new FilePatchApplier(), trackingStore);
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = toolingService.execute(request, resolver, permission);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("requires token: list_compiled"));
        assertFalse(result.getFailureReason().contains("requires token: list)"));
        verify(trackingStore, never()).load(eq(tempDir.toAbsolutePath().normalize()), eq("main:feature.md"));
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver createResolver(Path projectRoot) throws Exception {
        Path mainRoot = projectRoot.resolve("src/main/nl");
        Path testRoot = projectRoot.resolve("src/test/nl");
        Path targetRoot = projectRoot.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot.resolve("main/java/app"));
        Files.createDirectories(targetRoot.resolve("main/resources/app"));

        br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet mappings = new br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet();
        mappings.setMainRoot(mainRoot.toAbsolutePath().normalize());
        mappings.setTestRoot(testRoot.toAbsolutePath().normalize());
        mappings.setTargetRoot(targetRoot.toAbsolutePath().normalize());

        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(projectRoot));
    }

    private void writeManifest(Path projectRoot) throws Exception {
        Path featureController = projectRoot.resolve("src/main/java/app/FeatureController.java");
        Path featureConfig = projectRoot.resolve("src/main/resources/app/feature-config.yml");
        Path referencedOnly = projectRoot.resolve("src/main/java/app/ReferencedOnly.java");
        Files.writeString(featureController, "class FeatureController {}\n");
        Files.writeString(featureConfig, "name: demo\n");
        Files.writeString(referencedOnly, "class ReferencedOnly {}\n");

        SourceTrackingRecord direct = new SourceTrackingRecord();
        direct.setSourcePath("main:feature.md");
        java.util.Map<String, br.com.dizeno.reins.compilation.tracking.FileTrackingDetails> directFiles = new java.util.LinkedHashMap<>();
        directFiles.put("target:main/resources/app/feature-config.yml", null);
        directFiles.put("target:main/java/app/FeatureController.java", null);
        direct.setCompiledFiles(directFiles);
        direct.setResolvedTargetRoot("src");

        SourceTrackingRecord other = new SourceTrackingRecord();
        other.setSourcePath("main:referenced.md");
        java.util.Map<String, br.com.dizeno.reins.compilation.tracking.FileTrackingDetails> otherFiles = new java.util.LinkedHashMap<>();
        otherFiles.put("target:main/java/app/ReferencedOnly.java", null);
        other.setCompiledFiles(otherFiles);
        other.setResolvedTargetRoot("src");

        CompilationTrackingStore store = new CompilationTrackingStore();
        store.save(projectRoot, "main:feature.md", direct);
        store.save(projectRoot, "main:referenced.md", other);
    }

    
    
    

    @Test
    void patchFileInsertOnlyInsertsContentAtLine() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\nline2\nline3\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,3 +1,4 @@\n"
                + " line1\n"
                + "+inserted\n"
                + " line2\n"
                + " line3\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertNull(result.getContent(), "patch_file result should not contain file content");
        assertEquals("line1\ninserted\nline2\nline3\n", Files.readString(file));
    }

    @Test
    void patchFileReplacingLinesRemovesAndInserts() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\noldA\noldB\nline4\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,4 +1,4 @@\n"
                + " line1\n"
                + "-oldA\n"
                + "-oldB\n"
                + "+newA\n"
                + "+newB\n"
                + " line4\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("line1\nnewA\nnewB\nline4\n", Files.readString(file));
    }

    @Test
    void patchFileSupportsPatchKeyAlias() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "a\nb\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,2 +1,3 @@\n"
                + "+prefix\n"
                + " a\n"
                + " b\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("prefix\na\nb\n", Files.readString(file));
    }

    @Test
    void patchFileRejectsDeprecatedModeField() {
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromYaml(
                        "operation: patch_file\n"
                        + "base: target\n"
                        + "path: some/File.java\n"
                        + "mode: insert\n"
                        + "content: \"@@ -1 +1 @@\\n-x\\n+y\\n\"\n"));
        assertTrue(ex.getMessage().contains("'mode'"), "Error must mention the deprecated field");
        assertTrue(ex.getMessage().contains("no longer supported"), "Error must say it is no longer supported");
    }

    @Test
    void patchFileRejectsDeprecatedCoordinateAndLineFields() {
        for (String field : new String[]{"startLine", "startColumn", "endLine", "endColumn", "atLine", "replacing"}) {
            IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromYaml(
                            "operation: patch_file\n"
                            + "base: target\n"
                            + "path: some/File.java\n"
                            + field + ": 1\n"
                            + "content: \"@@ -1 +1 @@\\n-x\\n+y\\n\"\n"),
                    "Expected rejection for deprecated field: " + field);
            assertTrue(ex.getMessage().contains("'" + field + "'"),
                    "Error must mention the deprecated field: " + field);
        }
    }

    @Test
    void patchFileFailsWhenTargetFileDoesNotExist() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Missing.java\n"
                + "\n"
                + "@@ -1 +1 @@\n"
                + "-x\n"
                + "+y\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("File does not exist"));
    }

    @Test
    void patchFileDeniedWhenPatchPermissionMissing() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\nline2\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("read,list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy permission = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,2 +1,2 @@\n"
                + " line1\n"
                + "-line2\n"
                + "+x\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver, permission);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("not permitted"));
        assertTrue(result.getFailureReason().contains("requires token: patch"));
        assertEquals("line1\nline2\n", Files.readString(file));
    }

    @Test
    void patchFileDeniedWhenBaseIsNotTarget() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path file = mainRoot.resolve("feature.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "alpha\nbeta\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE main feature.md\n"
                + "\n"
                + "@@ -1,2 +1,2 @@\n"
                + "-alpha\n"
                + "+patched\n"
                + " beta\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains(br.com.dizeno.reins.reasoning.tooling.ToolingService.TARGET_BASE_REQUIRED_MESSAGE));
        assertEquals("alpha\nbeta\n", Files.readString(file));
    }

    @Test
    void patchFileCanInsertOnePastLastLine() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\nline2\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,2 +1,3 @@\n"
                + " line1\n"
                + " line2\n"
                + "+line3\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("line1\nline2\nline3\n", Files.readString(file));
    }

    @Test
    void patchFileInsertsEmptyLine() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\nline2\nline3\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,3 +1,4 @@\n"
                + " line1\n"
                + "+\n"
                + " line2\n"
                + " line3\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("line1\n\nline2\nline3\n", Files.readString(file));
    }

    @Test
    void patchFileReplacesBlockWithNothingDeletingLines() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\nline2\nline3\nline4\nline5\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,5 +1,2 @@\n"
                + " line1\n"
                + "-line2\n"
                + "-line3\n"
                + "-line4\n"
                + " line5\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("line1\nline5\n", Files.readString(file));
    }

    @Test
    void patchFileReplacesBlockWithFewerLines() throws Exception {
        Path targetRoot = tempDir.resolve("src");
        Path file = targetRoot.resolve("main/java/app/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "line1\noldA\noldB\noldC\nline5\n");

        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.fromText(
                "--reins-boundary\n"
                + "PATCH_FILE target main/java/app/Hello.java\n"
                + "\n"
                + "@@ -1,5 +1,3 @@\n"
                + " line1\n"
                + "-oldA\n"
                + "-oldB\n"
                + "-oldC\n"
                + "+newA\n"
                + " line5\n"
                + "--reins-boundary--\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertEquals("line1\nnewA\nline5\n", Files.readString(file));
    }

    @Test
    void patchFileStripsYamlBlockScalarIndentation() throws Exception {
        // Reproduces the real failure observed when the model writes patch content
        // inside a YAML block scalar (content: |-), which prepends 2 spaces to every
        // diff line. The applier must strip that indent before parsing.
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver(tempDir);
        // targetRoot is tempDir/src — file must be inside it to pass scope validation
        Path file = tempDir.resolve("src/Foo.java");
        Files.writeString(file, "class Foo {\n    int snake_case = 0;\n}\n");

        // This is exactly how the model encodes patch content in YAML block scalar:
        // every line is indented by 2 spaces.
        String yamlIndentedPatch =
            "  @@ -1,3 +1,3 @@\n" +
            "   class Foo {\n" +
            "  -    int snake_case = 0;\n" +
            "  +    int camelCase = 0;\n" +
            "   }\n";

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE);
        request.setBase("target");
        request.setPath("Foo.java");
        request.setContent(yamlIndentedPatch);

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus(),
            "patch should succeed even when diff lines carry YAML block scalar indent: " + result.getFailureReason());
        assertEquals("class Foo {\n    int camelCase = 0;\n}\n", Files.readString(file));
    }
}
