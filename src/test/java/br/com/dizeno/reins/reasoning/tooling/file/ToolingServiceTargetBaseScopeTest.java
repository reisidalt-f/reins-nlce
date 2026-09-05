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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolingServiceTargetBaseScopeTest {
    @TempDir
    Path tempDir;

    @Test
    void allowsWriteInsideTargetBase() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        request.setBase("target");
        request.setPath("main/java/demo/Allowed.java");
        request.setContent("class Allowed {}\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.SUCCESS, result.getStatus());
        assertTrue(Files.exists(tempDir.resolve("src/main/java/demo/Allowed.java")));
    }

    @Test
    void rejectsWriteOutsideTargetBaseUsingNonTargetBase() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        request.setBase("main");
        request.setPath("domain/InvalidWrite.md");
        request.setContent("# invalid\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.SCOPE_VIOLATION_CODE, result.getPolicyCode());
        assertTrue(result.getFailureReason().contains("target base"));
        assertFalse(Files.exists(tempDir.resolve("src/main/nl/domain/InvalidWrite.md")));
    }

    @Test
    void failFastBatchValidationPreventsAnyMutationWhenMixedScopeExists() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService = new br.com.dizeno.reins.reasoning.tooling.ToolingService();

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest allowed = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        allowed.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        allowed.setBase("target");
        allowed.setPath("main/java/demo/ShouldNotBeWritten.java");
        allowed.setContent("class ShouldNotBeWritten {}\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest invalid = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        invalid.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        invalid.setBase("main");
        invalid.setPath("domain/forbidden.md");
        invalid.setContent("# forbidden\n");

        List<br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest> batch = List.of(allowed, invalid);
        boolean hasViolation = batch.stream().anyMatch(r -> toolingService.validateMutationScope(r, resolver) != null);

        assertTrue(hasViolation);
        assertFalse(Files.exists(tempDir.resolve("src/main/java/demo/ShouldNotBeWritten.java")));
    }

    @Test
    void rejectsTraversalOutsideProjectDuringTargetWriteValidation() throws Exception {
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = createResolver();
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE);
        request.setBase("target");
        request.setPath("../../../../etc/passwd");
        request.setContent("blocked\n");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = new br.com.dizeno.reins.reasoning.tooling.ToolingService().execute(request, resolver);

        assertEquals(br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("Path traversal not allowed"));
    }

    @Test
    void targetListCompiledTokenAllowsCompiledListingWithoutDirectoryListing() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("list_compiled");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET),
            "write_file should be denied on target when write is not in config");
    }

    @Test
    void targetListTokenDoesNotAllowCompiledListing() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver createResolver() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path testRoot = tempDir.resolve("src/test/nl");
        Path targetRoot = tempDir.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot.resolve("main/java/demo"));

        br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet mappings = new br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet();
        mappings.setMainRoot(mainRoot.toAbsolutePath().normalize());
        mappings.setTestRoot(testRoot.toAbsolutePath().normalize());
        mappings.setTargetRoot(targetRoot.toAbsolutePath().normalize());
        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(tempDir));
    }
}
