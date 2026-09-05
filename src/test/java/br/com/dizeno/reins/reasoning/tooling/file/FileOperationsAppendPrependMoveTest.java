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

import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.handler.FileOperationsHandler;
import br.com.dizeno.reins.reasoning.tooling.handler.ScopeValidationGuard;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationsAppendPrependMoveTest {

    @TempDir
    Path tempDir;

    private BasePathResolver resolver;
    private FileOperationsHandler handler;

    @BeforeEach
    void setUp() throws IOException {
        Path mainRoot = tempDir.resolve("src/main");
        Path testRoot = tempDir.resolve("src/test");
        Path targetRoot = tempDir.resolve("target/output");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot);

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainRoot);
        mappings.setTestRoot(testRoot);
        mappings.setTargetRoot(targetRoot);

        resolver = new BasePathResolver(mappings, new PathValidator(tempDir));
        FilePatchApplier patchApplier = new FilePatchApplier();
        ScopeValidationGuard securityGuard = new ScopeValidationGuard();
        handler = new FileOperationsHandler(patchApplier, securityGuard);
    }

    @Test
    void testAppendFileText() throws IOException {
        Path targetFile = tempDir.resolve("target/output/log.txt");
        Files.writeString(targetFile, "Header\n", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nAPPEND_FILE target log.txt append line\n\nLine 1\n--reins-boundary--"
        );

        assertEquals(ToolExecutionRequest.Operation.APPEND_FILE, req.getOperation());
        assertEquals("target", req.getBase());
        assertEquals("log.txt", req.getPath());
        assertEquals("Line 1", req.getContent());

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        assertEquals("Header\nLine 1", content);
    }

    @Test
    void testAppendFileBinaryBase64() throws IOException {
        Path targetFile = tempDir.resolve("target/output/data.bin");
        Files.write(targetFile, new byte[]{0x01, 0x02});

        byte[] toAppend = new byte[]{0x03, 0x04};
        String base64Payload = Base64.getEncoder().encodeToString(toAppend);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nAPPEND_FILE target data.bin base64 append binary\n\n" + base64Payload + "\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        byte[] bytes = Files.readAllBytes(targetFile);
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, bytes);
    }

    @Test
    void testPrependFileText() throws IOException {
        Path targetFile = tempDir.resolve("target/output/header.txt");
        Files.writeString(targetFile, "Body Content\n", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nPREPEND_FILE target header.txt prepend banner\n\nBanner Header\n\n--reins-boundary--"
        );

        assertEquals(ToolExecutionRequest.Operation.PREPEND_FILE, req.getOperation());
        assertEquals("target", req.getBase());
        assertEquals("header.txt", req.getPath());

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        assertEquals("Banner HeaderBody Content\n", content);
    }

    @Test
    void testPrependFileNewFile() throws IOException {
        Path targetFile = tempDir.resolve("target/output/new_file.txt");

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nPREPEND_FILE target new_file.txt create and prepend\n\nInitial Text\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        assertEquals("Initial Text", content);
    }

    @Test
    void testMoveFile() throws IOException {
        Path sourceFile = tempDir.resolve("target/output/old_location.txt");
        Files.writeString(sourceFile, "Moving content", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nMOVE_FILE target old_location.txt sub/new_location.txt move file\n--reins-boundary--"
        );

        assertEquals(ToolExecutionRequest.Operation.MOVE_FILE, req.getOperation());
        assertEquals("target", req.getBase());
        assertEquals("old_location.txt", req.getPath());
        assertEquals("sub/new_location.txt", req.getDestination());

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        assertFalse(Files.exists(sourceFile));
        Path destFile = tempDir.resolve("target/output/sub/new_location.txt");
        assertTrue(Files.exists(destFile));
        assertEquals("Moving content", Files.readString(destFile, StandardCharsets.UTF_8));
    }

    @Test
    void testMoveFileNonExistentSourceReturnsError() {
        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nMOVE_FILE target non_existent.txt dest.txt move non-existent\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("File does not exist"));
    }

    @Test
    void testYamlParsingForNewOperations() {
        String yaml = """
                operation: append_file
                base: target
                path: file.txt
                content: "Appended text"
                """;
        ToolExecutionRequest reqAppend = ToolExecutionRequest.fromYaml(yaml);
        assertEquals(ToolExecutionRequest.Operation.APPEND_FILE, reqAppend.getOperation());
        assertEquals("Appended text", reqAppend.getContent());

        String yamlPrepend = """
                operation: prepend_file
                base: target
                path: file.txt
                content: "Prepended text"
                """;
        ToolExecutionRequest reqPrepend = ToolExecutionRequest.fromYaml(yamlPrepend);
        assertEquals(ToolExecutionRequest.Operation.PREPEND_FILE, reqPrepend.getOperation());

        String yamlMove = """
                operation: move_file
                base: target
                path: file.txt
                destination: dest/file.txt
                """;
        ToolExecutionRequest reqMove = ToolExecutionRequest.fromYaml(yamlMove);
        assertEquals(ToolExecutionRequest.Operation.MOVE_FILE, reqMove.getOperation());
        assertEquals("dest/file.txt", reqMove.getDestination());
    }

    @Test
    void testFilePolicyPermissions() {
        FilePolicy policy = new FilePolicy(Map.of(
                FilePolicy.Base.TARGET, java.util.Set.of(FilePolicy.OperationToken.WRITE)
        ));

        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.APPEND_FILE, FilePolicy.Base.TARGET));
        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PREPEND_FILE, FilePolicy.Base.TARGET));
        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.MOVE_FILE, FilePolicy.Base.TARGET));
    }
}
