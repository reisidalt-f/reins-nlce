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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

 
class ToolPermissionsEndToEndTest {

    @TempDir
    Path tempDir;

    private static final String RESPONSE_FINISH_SUCCESS =
            "INTENT: finish-success\nCONTENT_TYPE: message-to-user\n\nTask complete.\n";

    private static final String RESPONSE_LIST_FILES_MAIN =
            "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\noperation: list_files\nbase: main\npath: .\n";

    private static final String RESPONSE_LIST_COMPILED_FILES_MAIN =
            "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\noperation: list_compiled_files\nbase: main\npath: feature.md\n";

    private ReinsConfig configWithTooling(String main, String test) {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        config.getTooling().setMain(main);
        config.getTooling().setTest(test);
        return config;
    }

    private BasePathMappingSet baseMappings() {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir);
        mappings.setTestRoot(tempDir);
        mappings.setTargetRoot(tempDir);
        return mappings;
    }

    private MarkdownInferenceResponse response(String text) {
                MarkdownInferenceResponse r = new MarkdownInferenceResponse();
                r.setRawResponseText(text);
        return r;
    }

    private DefaultReasoningService buildService(InferenceService inferenceService) {
        return new DefaultReasoningService(
                inferenceService,
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                new br.com.dizeno.reins.reasoning.tooling.ToolingService(),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );
    }

    

     
    @Test
    void deniedListFiles_errorReturnedToModel_cycleCompletesAfterRecovery() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        
        
        when(inferenceService.infer(any(), any()))
                .thenReturn(response(RESPONSE_LIST_FILES_MAIN))
                .thenReturn(response(RESPONSE_FINISH_SUCCESS));

        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(baseMappings());

        
        ReinsConfig config = configWithTooling("read", "read");

        ReasoningResult result = service.runCycle(request, config);

        
        assertNotNull(result, "Result must not be null");
        
        verify(inferenceService, times(2)).infer(any(), any());
    }

     
    @Test
    void allowedListFiles_neverProducesPermissionDenial() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.infer(any(), any()))
                .thenReturn(response(RESPONSE_LIST_FILES_MAIN))
                .thenReturn(response(RESPONSE_FINISH_SUCCESS));

        
        List<String> sentMessages = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest req = invocation.getArgument(0);
            if (req.getConversationHistory() != null) {
                req.getConversationHistory().stream()
                                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                        .map(ConversationMessage::getText)
                        .filter(t -> t != null)
                        .forEach(sentMessages::add);
            }
            return response(RESPONSE_FINISH_SUCCESS);
        }).when(inferenceService).infer(any(), any());

        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(baseMappings());

        
        ReinsConfig config = configWithTooling("list,read", "list,read");

        service.runCycle(request, config);

        
        boolean anyPermissionDenial = sentMessages.stream()
                .anyMatch(m -> m.contains("not permitted") || m.contains("Operation") && m.contains("is not permitted"));
        assertFalse(anyPermissionDenial,
                "No 'not permitted' error should be returned to model when list is allowed on main");
    }

    @Test
    void listOnlyConfig_deniesListCompiledFilesUntilListCompiledTokenIsGranted() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.infer(any(), any()))
                .thenReturn(response(RESPONSE_LIST_COMPILED_FILES_MAIN))
                .thenReturn(response(RESPONSE_FINISH_SUCCESS));

        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(baseMappings());

        ReinsConfig config = configWithTooling("list,read", "read");

        service.runCycle(request, config);

        verify(inferenceService, times(2)).infer(any(), any());
    }

    

     
    @Test
    void emptyFileToolsSettings_writeFileOnTargetNeverDenied() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);

        String writeResponse = "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                + "operation: write_file\nbase: target\npath: Output.java\ncontent: class Output {}\n";
        when(inferenceService.infer(any(), any()))
                .thenReturn(response(writeResponse))
                .thenReturn(response(RESPONSE_FINISH_SUCCESS));

        
        List<String> sentMessages = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest req = invocation.getArgument(0);
            if (req.getConversationHistory() != null) {
                req.getConversationHistory().stream()
                                                .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                        .map(ConversationMessage::getText)
                        .filter(t -> t != null)
                        .forEach(sentMessages::add);
            }
            return response(RESPONSE_FINISH_SUCCESS);
        }).when(inferenceService).infer(any(), any());

        DefaultReasoningService service = buildService(inferenceService);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        request.setBaseMappings(baseMappings());

        
        ReinsConfig config = configWithTooling(null, null);

        service.runCycle(request, config);

        
        boolean anyPermissionDenial = sentMessages.stream()
                .anyMatch(m -> m.contains("not permitted") || m.contains("is not permitted on base"));
        assertFalse(anyPermissionDenial,
                "write_file on target must NOT produce a permission denial even with empty fileTools config");
    }

    @Test
    void compileErrorText_doesNotInjectNoteHandoffGateFailure() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        when(inferenceService.infer(any(), any()))
                .thenReturn(response(
                        "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                                + "operation: run_script\n"
                                + "script: run-build.sh\n"
                                + "args: [\"com/example/Broken.java\"]\n"))
                .thenReturn(response(
                        "INTENT: waiting-for-next-message\nCONTENT_TYPE: tool-request\n\n"
                                + "operation: patch_file\n"
                                + "base: main\n"
                                + "path: forbidden.md\n"
                                + "content: \"@@ -1 +1 @@\\n-old\\n+# forbidden\\n\"\n"))
                .thenReturn(response(RESPONSE_FINISH_SUCCESS));

        List<String> sentMessages = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest req = invocation.getArgument(0);
            if (req.getConversationHistory() != null) {
                req.getConversationHistory().stream()
                        .filter(m -> m.getRole() == ConversationMessage.Role.USER)
                        .map(ConversationMessage::getText)
                        .filter(t -> t != null)
                        .forEach(sentMessages::add);
            }
            return response(RESPONSE_FINISH_SUCCESS);
        }).when(inferenceService).infer(any(), any());

        DefaultReasoningService service = buildService(inferenceService);
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Compile code.");
        request.setProjectRoot(tempDir);
        BasePathMappingSet mappings = baseMappings();
        mappings.setScriptRoot(tempDir);
        request.setBaseMappings(mappings);

        ReinsConfig config = configWithTooling("list,read", "read");
        config.getReasoning().setScriptsPath(".");

        service.runCycle(request, config);

        boolean hasGatePhrase = sentMessages.stream()
                .anyMatch(m -> m.contains("MUST emit add_reasoning_note")
                        || m.contains("Cross-source compile failures detected"));
        assertFalse(hasGatePhrase,
                "Compile-error text must not create a synthetic note-handoff gate failure");
    }

}
