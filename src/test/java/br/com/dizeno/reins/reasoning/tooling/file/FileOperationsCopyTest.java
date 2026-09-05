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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationsCopyTest {

    @TempDir
    Path tempDir;

    private BasePathResolver resolver;
    private FileOperationsHandler handler;
    private ScopeValidationGuard securityGuard;

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
        securityGuard = new ScopeValidationGuard();
        handler = new FileOperationsHandler(patchApplier, securityGuard);
    }

    @Test
    void testCopyFromMainToTarget() throws IOException {
        Path mainFile = tempDir.resolve("src/main/templates/Header.java");
        Files.createDirectories(mainFile.getParent());
        Files.writeString(mainFile, "package templates;\npublic class Header {}", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nCOPY_FILE main templates/Header.java output/Header.java copy header template\n--reins-boundary--"
        );

        assertEquals(ToolExecutionRequest.Operation.COPY_FILE, req.getOperation());
        assertEquals("main", req.getBase());
        assertEquals("templates/Header.java", req.getPath());
        assertEquals("output/Header.java", req.getDestination());

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        // Origin file remains intact
        assertTrue(Files.exists(mainFile));

        // Destination file exists in target base
        Path destFile = tempDir.resolve("target/output/output/Header.java");
        assertTrue(Files.exists(destFile));
        assertEquals("package templates;\npublic class Header {}", Files.readString(destFile, StandardCharsets.UTF_8));
    }

    @Test
    void testCopyFromTestToTarget() throws IOException {
        Path testFile = tempDir.resolve("src/test/fixtures/Data.json");
        Files.createDirectories(testFile.getParent());
        Files.writeString(testFile, "{\"key\":\"value\"}", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nCOPY_FILE test fixtures/Data.json config/Data.json copy fixture\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        assertTrue(Files.exists(testFile));
        Path destFile = tempDir.resolve("target/output/config/Data.json");
        assertTrue(Files.exists(destFile));
        assertEquals("{\"key\":\"value\"}", Files.readString(destFile, StandardCharsets.UTF_8));
    }

    @Test
    void testCopyWithinTarget() throws IOException {
        Path targetFile = tempDir.resolve("target/output/draft.txt");
        Files.writeString(targetFile, "Draft Content", StandardCharsets.UTF_8);

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nCOPY_FILE target draft.txt final/copied.txt copy within target\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());

        // Both source and destination exist
        assertTrue(Files.exists(targetFile));
        Path destFile = tempDir.resolve("target/output/final/copied.txt");
        assertTrue(Files.exists(destFile));
        assertEquals("Draft Content", Files.readString(destFile, StandardCharsets.UTF_8));
    }

    @Test
    void testCopyNonExistentFileReturnsError() {
        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nCOPY_FILE main missing.txt dest.txt copy non-existent\n--reins-boundary--"
        );

        ToolExecutionResult result = handler.execute(req, resolver, "source", ScriptRunnerConfig.disabled());
        assertEquals(ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("File does not exist"));
    }

    @Test
    void testPermissionsValidation() {
        // Policy allowing READ on main, WRITE on target
        FilePolicy policyAllow = new FilePolicy(Map.of(
                FilePolicy.Base.MAIN, Set.of(FilePolicy.OperationToken.READ),
                FilePolicy.Base.TARGET, Set.of(FilePolicy.OperationToken.WRITE)
        ));

        ToolExecutionRequest req = ToolExecutionRequest.fromText(
                "--reins-boundary\nCOPY_FILE main file.txt target_file.txt copy file\n--reins-boundary--"
        );

        assertNull(securityGuard.validatePermissions(req, policyAllow));

        // Policy lacking READ on main
        FilePolicy policyNoRead = new FilePolicy(Map.of(
                FilePolicy.Base.MAIN, Set.of(FilePolicy.OperationToken.LIST),
                FilePolicy.Base.TARGET, Set.of(FilePolicy.OperationToken.WRITE)
        ));

        String errorNoRead = securityGuard.validatePermissions(req, policyNoRead);
        assertNotNull(errorNoRead);
        assertTrue(errorNoRead.contains("requires token: read"));

        // Policy lacking WRITE/COPY on target
        FilePolicy policyNoTargetWrite = new FilePolicy(Map.of(
                FilePolicy.Base.MAIN, Set.of(FilePolicy.OperationToken.READ),
                FilePolicy.Base.TARGET, Set.of(FilePolicy.OperationToken.READ)
        ));

        String errorNoWrite = securityGuard.validatePermissions(req, policyNoTargetWrite);
        assertNotNull(errorNoWrite);
        assertTrue(errorNoWrite.contains("requires token: copy"));
    }

    @Test
    void testYamlParsingForCopyFile() {
        String yaml = """
                operation: copy_file
                base: main
                path: template.txt
                destination: output/copy.txt
                """;
        ToolExecutionRequest req = ToolExecutionRequest.fromYaml(yaml);
        assertEquals(ToolExecutionRequest.Operation.COPY_FILE, req.getOperation());
        assertEquals("main", req.getBase());
        assertEquals("template.txt", req.getPath());
        assertEquals("output/copy.txt", req.getDestination());
    }
}
