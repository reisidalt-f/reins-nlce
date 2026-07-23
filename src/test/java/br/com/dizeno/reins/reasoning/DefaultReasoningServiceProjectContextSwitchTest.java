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
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

 
class DefaultReasoningServiceProjectContextSwitchTest {

    @TempDir
    Path tempDir;

    @Test
    void systemContext_containsProjectInferenceScope_whenRequestHasProjectInferenceCycleTrue() throws Exception {
        Path projectFile = writeFile("project.md", "# project overview\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReasoningRequest request = projectInferenceRequest(projectFile);
        service.runCycle(request, new ReinsConfig());

        
        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        org.mockito.Mockito.verify(inferenceService).infer(captor.capture(), any());

        
        List<ConversationMessage> history = captor.getValue().getConversationHistory();
        String systemContext = history.get(0).getText();
        assertTrue("main".equals(captor.getValue().getSourceScope()),
            "Inference request should preserve main source scope for project inference requests");
        assertTrue("main".equals(captor.getValue().getSourceBase()),
            "Project scope should still use the main MCP base for source reads");
        assertTrue(systemContext.contains("=== PROJECT INFERENCE SCOPE ==="),
                "System context should include PROJECT INFERENCE SCOPE when projectInferenceCycle=true");
        assertTrue(systemContext.contains("test: where test .md files are located"),
                "System context should note test base unavailability for project inference");
        assertTrue(systemContext.contains("configured maximum of 10 reasoning turns"),
            "System context should include the configured max-turn budget");
    }

    @Test
    void systemContext_doesNotContainProjectInferenceScope_whenRequestHasProjectInferenceCycleFalse()
            throws Exception {
        Path source = writeFile("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                directiveParser,
                new ReasoningPromptBuilder(),
                mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
                mock(ToolResultFormatter.class),
                new FileReasoningLogService(),
                new CompilationTrackingStore()
        );

        ReasoningRequest request = standardRequest(source);
        service.runCycle(request, new ReinsConfig());

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        org.mockito.Mockito.verify(inferenceService).infer(captor.capture(), any());

        List<ConversationMessage> history = captor.getValue().getConversationHistory();
        String systemContext = history.get(0).getText();
        assertFalse(systemContext.contains("PROJECT INFERENCE SCOPE"),
                "System context should NOT include PROJECT INFERENCE SCOPE for regular source cycle");
    }

        @Test
        void systemContext_includesConfiguredTurnBudgetFromReasoningSettings() throws Exception {
        Path source = writeFile("src/main/nl/domain.md", "# domain\n");

        InferenceService inferenceService = mock(InferenceService.class);
        MarkdownInferenceResponse response = mock(MarkdownInferenceResponse.class);
        when(response.getRawResponseText()).thenReturn("done");
        when(inferenceService.infer(any(), any())).thenReturn(response);

        ResponseDirectiveParser directiveParser = mock(ResponseDirectiveParser.class);
        ResponseDirective finish = new ResponseDirective();
        finish.setValid(true);
        finish.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        finish.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);
        when(directiveParser.parse("done")).thenReturn(new ResponseDirectiveParser.ParseResult(finish, "ok"));

        DefaultReasoningService service = new DefaultReasoningService(
            inferenceService,
            directiveParser,
            new ReasoningPromptBuilder(),
            mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class),
            mock(ToolResultFormatter.class),
            new FileReasoningLogService(),
            new CompilationTrackingStore()
        );

        ReinsConfig config = new ReinsConfig();
        config.getReasoning().setMaxTurns(3);

        service.runCycle(standardRequest(source), config);

        ArgumentCaptor<MarkdownInferenceRequest> captor = ArgumentCaptor.forClass(MarkdownInferenceRequest.class);
        org.mockito.Mockito.verify(inferenceService).infer(captor.capture(), any());

        List<ConversationMessage> history = captor.getValue().getConversationHistory();
        String systemContext = history.get(0).getText();
        assertTrue(systemContext.contains("configured maximum of 3 reasoning turns"),
            "System context should tell Gemini the configured max-turn count");
        assertTrue(systemContext.contains("finish within this limit"),
            "System context should instruct Gemini to complete within the configured turn budget");
        }

    

    private ReasoningRequest projectInferenceRequest(Path projectFile) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Use attached file as context.");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(projectFile).toString().replace('\\', '/'));
        request.setProjectInferenceCycle(true);

        BasePathMappingSet mappings = BasePathMappingSet.forProjectInference(tempDir, tempDir.resolve("target"));
        request.setBaseMappings(mappings);
        return request;
    }

    private ReasoningRequest standardRequest(Path source) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Use attached source markdown as the primary context.");
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(source).toString().replace('\\', '/'));
        request.setProjectInferenceCycle(false);

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        return request;
    }

    private Path writeFile(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
