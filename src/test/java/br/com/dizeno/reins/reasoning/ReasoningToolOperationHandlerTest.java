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
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ReasoningToolOperationHandlerTest {

    @TempDir
    Path projectRoot;

    @Test
    void buildDedupeKey_distinguishesDifferentNoteTargets() {
        ReasoningToolOperationHandler handler = new ReasoningToolOperationHandler(
            mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
            mock(CompilationTrackingStore.class),
            new ConcurrentHashMap<>());

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest first = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        first.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.ADD_INFERENCE_NOTE);
        first.setCompiled("target:com/dizeno/mdwriter/editor/EditorState.java");
        first.setNote("Add setDocument support for MainWindow");

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest second = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        second.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.ADD_INFERENCE_NOTE);
        second.setCompiled("target:com/dizeno/mdwriter/editor/input/InputHandler.java");
        second.setNote("Fix missing command constructors and symbols");

        String firstKey = handler.buildDedupeKey(first);
        String secondKey = handler.buildDedupeKey(second);

        assertNotEquals(firstKey, secondKey);
    }

    @Test
    void buildDedupeKey_distinguishesDifferentRunScriptRequests() {
        ReasoningToolOperationHandler handler = new ReasoningToolOperationHandler(
            mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
            mock(CompilationTrackingStore.class),
            new ConcurrentHashMap<>());

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest first = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        first.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT);
        first.setScript("compile-one-java.sh");
        first.setArgs(java.util.List.of("com/example/App.java"));

        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest second = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        second.setOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.RUN_SCRIPT);
        second.setScript("compile-all.sh");
        second.setArgs(java.util.List.of());

        String firstKey = handler.buildDedupeKey(first);
        String secondKey = handler.buildDedupeKey(second);

        assertNotEquals(firstKey, secondKey);
    }

    @Test
    void allowsMutationWhenReferenceSetIsSelfOnly_write() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Self.md"), Map.of());

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "target", "gen/Self.java"),
                resolver());

        assertFalse(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.ALLOW_NO_CONFLICT, decision.getReasonCode());
        assertEquals(1, decision.getIgnoredSelfReferences());
    }

    @Test
    void allowsMutationWhenReferenceSetIsSelfOnly_patch() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Self.md"), Map.of());

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE, "target", "gen/Self.java"),
                resolver());

        assertFalse(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.ALLOW_NO_CONFLICT, decision.getReasonCode());
    }

    @Test
    void allowsMutationWhenReferenceSetIsSelfOnly_delete() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Self.md"), Map.of());

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE, "target", "gen/Self.java"),
                resolver());

        assertTrue(decision.isBlocked() == false);
        assertEquals(ReferenceMutationDecision.ReasonCode.ALLOW_NO_CONFLICT, decision.getReasonCode());
    }

    @Test
    void allowsMutationWhenSelfReferenceUsesCanonicalVariant() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("./src/main/nl/Self.md"), Map.of());

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "target", "gen/Self.java"),
                resolver());

        assertFalse(decision.isBlocked());
        assertEquals(1, decision.getIgnoredSelfReferences());
    }

    @Test
    void blocksMutationWhenTargetIsReferencedSourcePath() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Other.md"), Map.of());

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "main", "Other.md"),
                resolver());

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_REFERENCED_SOURCE, decision.getReasonCode());
    }

    @Test
    void blocksMutationWhenTargetIsCompiledByReferencedSource() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Other.md"), Map.of());
        saveRecord("main:Other.md", Map.of(), mapOf("target:gen/Other.java"));

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE, "target", "gen/Other.java"),
                resolver());

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_REFERENCED_COMPILED, decision.getReasonCode());
    }

    @Test
    void blocksMutationForMixedSelfAndExternalReferences() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Self.md", "main:Other.md"), Map.of());
        saveRecord("main:Other.md", Map.of(), mapOf("target:gen/Other.java"));

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE, "target", "gen/Other.java"),
                resolver());

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_REFERENCED_COMPILED, decision.getReasonCode());
        assertEquals(1, decision.getIgnoredSelfReferences());
    }

    @Test
    void preservesBaselineBehaviorWhenNoSelfReferencesExist() throws Exception {
        ReasoningToolOperationHandler handler = newHandler();
        saveRecord("main:Self.md", mapOf("main:Other.md"), Map.of());

        boolean blocked = handler.isReferenceMutationBlocked(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "main", "Other.md"),
                resolver());

        assertTrue(blocked);
    }

    @Test
    void returnsAllowForNullOrBlankSourcePathRegressionSafety() {
        ReasoningToolOperationHandler handler = new ReasoningToolOperationHandler(
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                new CompilationTrackingStore(),
                new ConcurrentHashMap<>());

        ReasoningRequest nullSource = requestForSource(null);
        ReasoningRequest blankSource = requestForSource("   ");
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "target", "gen/Any.java");
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = resolver();

        assertFalse(handler.evaluateReferenceMutationDecision(nullSource, request, resolver).isBlocked());
        assertFalse(handler.evaluateReferenceMutationDecision(blankSource, request, resolver).isBlocked());
    }

    @Test
    void formatDecisionReasonIncludesReasonAndIgnoredSelfCount() {
        ReasoningToolOperationHandler handler = new ReasoningToolOperationHandler(
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(CompilationTrackingStore.class),
                new ConcurrentHashMap<>());

        String reason = handler.formatDecisionReason(ReferenceMutationDecision.blockReferencedCompiled(2, 1));

        assertTrue(reason.contains("BLOCK_REFERENCED_COMPILED"));
        assertTrue(reason.contains("ignoredSelfReferences=1"));
    }

    @Test
    void blocksMutationWhenActiveSourceHasNoTrackingRecordButTargetIsOwnedByAnother() throws Exception {
        
        
        
        ReasoningToolOperationHandler handler = newHandler();
        
        
        saveRecord("main:Other.md", Map.of(), mapOf("target:gen/Other.java"));

        ReferenceMutationDecision decision = handler.evaluateReferenceMutationDecision(
                requestForSource("main:Self.md"),
                mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE, "target", "gen/Other.java"),
                resolver());

        assertTrue(decision.isBlocked());
        assertEquals(ReferenceMutationDecision.ReasonCode.BLOCK_FOREIGN_OWNED, decision.getReasonCode());
    }

    private ReasoningToolOperationHandler newHandler() {
        return new ReasoningToolOperationHandler(
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                new CompilationTrackingStore(),
                new ConcurrentHashMap<>());
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver() {
        try {
            Files.createDirectories(projectRoot.resolve("src/main/nl"));
            Files.createDirectories(projectRoot.resolve("src/test/nl"));
            Files.createDirectories(projectRoot.resolve("target/gen"));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectRoot.resolve("src/main/nl"));
        mappings.setTestRoot(projectRoot.resolve("src/test/nl"));
        mappings.setTargetRoot(projectRoot.resolve("target"));
        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(mappings, new PathValidator(projectRoot));
    }

    private ReasoningRequest requestForSource(String sourcePath) {
        ReasoningRequest request = new ReasoningRequest();
        request.setSourcePath(sourcePath);
        return request;
    }

    private br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest mutation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation operation, String base, String path) {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest request = new br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest();
        request.setOperation(operation);
        request.setBase(base);
        request.setPath(path);
        return request;
    }

    private void saveRecord(String sourcePath,
                            Map<String, FileTrackingDetails> references,
                            Map<String, FileTrackingDetails> compiled) throws IOException {
        CompilationTrackingStore store = new CompilationTrackingStore();
        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath(sourcePath);
        record.setMarkdownReferences(references);
        record.setCompiledFiles(compiled);
        store.save(projectRoot, sourcePath, record);
    }

    private Map<String, FileTrackingDetails> mapOf(String... canonicalPaths) {
        LinkedHashMap<String, FileTrackingDetails> map = new LinkedHashMap<>();
        for (String path : canonicalPaths) {
            map.put(path, new FileTrackingDetails(path, "main", null));
        }
        return map;
    }
}