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
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceImmutabilityReasoningTest {
    @TempDir
    Path tempDir;

    @Test
    void blocksMutationOnReferencedCompiledOutputs() throws Exception {
        writeManifest(tempDir);

        InferenceService inferenceService = mock(InferenceService.class);
        ResponseDirectiveParser parser = mock(ResponseDirectiveParser.class);
        ReasoningPromptBuilder promptBuilder = mock(ReasoningPromptBuilder.class);
        br.com.dizeno.reins.reasoning.tooling.ToolingService toolingService = mock(br.com.dizeno.reins.reasoning.tooling.ToolingService.class);
        br.com.dizeno.reins.reasoning.ToolResultFormatter formatter = mock(br.com.dizeno.reins.reasoning.ToolResultFormatter.class);

        DefaultReasoningService service = new DefaultReasoningService(
                inferenceService,
                parser,
                promptBuilder,
                toolingService,
                formatter,
                new FileReasoningLogService(),
            new CompilationTrackingStore()
        );

        MarkdownInferenceResponse firstResponse = mock(MarkdownInferenceResponse.class);
        MarkdownInferenceResponse secondResponse = mock(MarkdownInferenceResponse.class);
        when(firstResponse.getRawResponseText()).thenReturn("first");
        when(secondResponse.getRawResponseText()).thenReturn("second");
        when(inferenceService.infer(any(), any())).thenReturn(firstResponse, secondResponse);

        when(promptBuilder.buildPrompt(any(), any(), any(), anyBoolean())).thenReturn("prompt-1", "prompt-2");
        when(formatter.format(any())).thenReturn("blocked");
        when(toolingService.execute(any(), any(), any(), any())).thenReturn(
            br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE,
                "target:/main/java/ref/RefCompiled.java",
                "Mutation of referenced markdown artifacts is not allowed."
            )
        );

        ResponseDirective waitDirective = new ResponseDirective();
        waitDirective.setValid(true);
        waitDirective.setIntent(ResponseDirective.Intent.WAITING_FOR_NEXT_MESSAGE);
        waitDirective.setContentType(ResponseDirective.ContentType.TOOL_REQUEST);

        ResponseDirective doneDirective = new ResponseDirective();
        doneDirective.setValid(true);
        doneDirective.setIntent(ResponseDirective.Intent.FINISH_SUCCESS);
        doneDirective.setContentType(ResponseDirective.ContentType.MESSAGE_TO_USER);

        ResponseDirectiveParser.ParseResult firstParse = new ResponseDirectiveParser.ParseResult(
                waitDirective,
                "operation: write_file\nbase: target\npath: main/java/ref/RefCompiled.java\ncontent: blocked\n"
        );
        ResponseDirectiveParser.ParseResult secondParse = new ResponseDirectiveParser.ParseResult(doneDirective, "done");
        when(parser.parse("first")).thenReturn(firstParse);
        when(parser.parse("second")).thenReturn(secondParse);

        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("start");
        request.setProjectRoot(tempDir);
        request.setSourcePath("src/main/nl/main.md");
        request.setSourceScope("main");
        request.setBaseMappings(createBaseMappings());

        ReinsConfig config = new ReinsConfig();
        config.setProvider("gemini");
        ReasoningResult result = service.runCycle(request, config);

        verify(toolingService, times(1)).execute(any(), any(), any(), any());
        assertEquals("finish_success", result.getFinalIntent());
    }

    private BasePathMappingSet createBaseMappings() throws Exception {
        Path mainRoot = tempDir.resolve("src/main/nl");
        Path testRoot = tempDir.resolve("src/test/nl");
        Path targetRoot = tempDir.resolve("src");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.createDirectories(targetRoot.resolve("main/java/ref"));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainRoot);
        mappings.setTestRoot(testRoot);
        mappings.setTargetRoot(targetRoot);
        return mappings;
    }

    private void writeManifest(Path projectRoot) throws Exception {
        SourceTrackingRecord mainRecord = new SourceTrackingRecord();
        mainRecord.setSourcePath("src/main/nl/main.md");
        java.util.Map<String, br.com.dizeno.reins.compilation.tracking.FileTrackingDetails> markdownRefs = new java.util.LinkedHashMap<>();
        markdownRefs.put("src/main/nl/ref.md", new br.com.dizeno.reins.compilation.tracking.FileTrackingDetails("src/main/nl/ref.md", "main", null));
        mainRecord.setMarkdownReferences(markdownRefs);

        SourceTrackingRecord refRecord = new SourceTrackingRecord();
        refRecord.setSourcePath("src/main/nl/ref.md");
        java.util.Map<String, br.com.dizeno.reins.compilation.tracking.FileTrackingDetails> refFiles = new java.util.LinkedHashMap<>();
        refFiles.put("src/main/java/ref/RefCompiled.java", null);
        refRecord.setCompiledFiles(refFiles);

        CompilationTrackingStore trackingStore = new CompilationTrackingStore();
        trackingStore.save(projectRoot, mainRecord.getSourcePath(), mainRecord);
        trackingStore.save(projectRoot, refRecord.getSourcePath(), refRecord);
    }
}
