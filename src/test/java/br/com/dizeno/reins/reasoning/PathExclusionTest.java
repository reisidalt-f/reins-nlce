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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PathExclusionTest {

    @TempDir
    Path projectRoot;

    @Test
    void blocksTestScopeWriteWhenRelativePathMatchesMainCompiledRelativePath_sameTargetRoot() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        
        
        saveRecord("main:Source.md", "main", Map.of("target:com/foo/Bar.java", new FileTrackingDetails("target:com/foo/Bar.java", "main", null)));

        
        ReasoningRequest request = requestForSource("test:TestSuite.md", "test");
        ToolExecutionRequest opRequest = mutation(ToolExecutionRequest.Operation.WRITE_FILE, "target", "com/foo/Bar.java");
        BasePathResolver resolver = resolver(false);

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(request, opRequest, resolver);

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_TEST_MAIN_EXCLUSION, decision.getReasonCode());
    }

    @Test
    void allowsTestScopeWriteWhenNoRelativePathOverlap() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        
        
        saveRecord("main:Source.md", "main", Map.of("target:com/foo/Bar.java", new FileTrackingDetails("target:com/foo/Bar.java", "main", null)));

        
        ReasoningRequest request = requestForSource("test:TestSuite.md", "test");
        ToolExecutionRequest opRequest = mutation(ToolExecutionRequest.Operation.WRITE_FILE, "target", "com/foo/Baz.java");
        BasePathResolver resolver = resolver(false);

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(request, opRequest, resolver);

        assertFalse(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.ALLOW_NO_CONFLICT, decision.getReasonCode());
    }

    @Test
    void allowsMainScopeWriteToOverlapRelativePaths_oneWayOnly() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        
        
        saveRecord("test:TestSuite.md", "test", Map.of("target:com/foo/Bar.java", new FileTrackingDetails("target:com/foo/Bar.java", "test", null)));
        
        saveRecord("main:Source.md", "main", Map.of());

        
        ReasoningRequest request = requestForSource("main:Source.md", "main");
        ToolExecutionRequest opRequest = mutation(ToolExecutionRequest.Operation.WRITE_FILE, "target", "com/foo/Bar.java");
        BasePathResolver resolver = resolver(false);

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(request, opRequest, resolver);

        assertFalse(decision.isBlocked());
    }

    @Test
    void blocksTestScopeWriteWhenRelativePathMatchesMainCompiledRelativePath_separateTargetRoots() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        
        
        
        saveRecord("main:Source.md", "main", "src/main/java", Map.of("target:com/foo/Bar.java", new FileTrackingDetails("target:com/foo/Bar.java", "main", null)));

        
        ReasoningRequest request = requestForSource("test:TestSuite.md", "test");
        ToolExecutionRequest opRequest = mutation(ToolExecutionRequest.Operation.WRITE_FILE, "target", "com/foo/Bar.java");
        BasePathResolver resolver = resolver(true);

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(request, opRequest, resolver);

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_TEST_MAIN_EXCLUSION, decision.getReasonCode());
    }

    private ReasoningToolOperationHandler newHandler() {
        return new ReasoningToolOperationHandler(
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                new CompilationTrackingStore(),
                new ConcurrentHashMap<>());
    }

    private BasePathResolver resolver(boolean separateRoots) {
        try {
            Files.createDirectories(projectRoot.resolve("src/main/nl"));
            Files.createDirectories(projectRoot.resolve("src/test/nl"));
            if (separateRoots) {
                Files.createDirectories(projectRoot.resolve("src/main/java/com/foo"));
                Files.createDirectories(projectRoot.resolve("src/test/java/com/foo"));
            } else {
                Files.createDirectories(projectRoot.resolve("target/com/foo"));
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectRoot.resolve("src/main/nl"));
        mappings.setTestRoot(projectRoot.resolve("src/test/nl"));
        
        if (separateRoots) {
            mappings.setMainTargetRoot(projectRoot.resolve("src/main/java"));
            mappings.setTestTargetRoot(projectRoot.resolve("src/test/java"));
            mappings.setTargetRoot(projectRoot.resolve("src/test/java")); 
        } else {
            mappings.setMainTargetRoot(projectRoot.resolve("target"));
            mappings.setTestTargetRoot(projectRoot.resolve("target"));
            mappings.setTargetRoot(projectRoot.resolve("target"));
        }
        return new BasePathResolver(mappings, new PathValidator(projectRoot));
    }

    private ReasoningRequest requestForSource(String sourcePath, String scope) {
        ReasoningRequest request = new ReasoningRequest();
        request.setSourcePath(sourcePath);
        request.setSourceScope(scope);
        return request;
    }

    private ToolExecutionRequest mutation(ToolExecutionRequest.Operation operation, String base, String path) {
        ToolExecutionRequest request = new ToolExecutionRequest();
        request.setOperation(operation);
        request.setBase(base);
        request.setPath(path);
        return request;
    }

    private void saveRecord(String sourcePath, String category, Map<String, FileTrackingDetails> compiled) throws IOException {
        saveRecord(sourcePath, category, "target", compiled);
    }

    private void saveRecord(String sourcePath, String category, String resolvedTargetRoot, Map<String, FileTrackingDetails> compiled) throws IOException {
        CompilationTrackingStore store = new CompilationTrackingStore();
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(sourcePath);
        record.setSourceCategory(category);
        record.setResolvedTargetRoot(resolvedTargetRoot);
        record.setCompiledFiles(compiled);
        store.save(projectRoot, sourcePath, record);
    }
}
