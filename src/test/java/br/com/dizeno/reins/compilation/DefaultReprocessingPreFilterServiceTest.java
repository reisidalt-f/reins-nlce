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

package br.com.dizeno.reins.compilation;

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.compilation.context.ProjectContextService;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultReprocessingPreFilterServiceTest {

    @TempDir
    Path projectDir;

    private RecompilationDecider recompilationDecider;
    private CompilationTrackingStore trackingStore;
    private SourceFingerprintService fingerprintService;
    private ProjectContextService projectContextService;
    private Log log;

    private DefaultReprocessingPreFilterService service;

    private ReinsConfig config;

    @BeforeEach
    void setUp() {
        recompilationDecider = mock(RecompilationDecider.class);
        trackingStore = mock(CompilationTrackingStore.class);
        fingerprintService = mock(SourceFingerprintService.class);
        projectContextService = mock(ProjectContextService.class);
        log = mock(Log.class);

        service = new DefaultReprocessingPreFilterService(
                recompilationDecider, trackingStore, fingerprintService, projectContextService);

        config = new ReinsConfig();

        TargetSettings target = new TargetSettings();
        target.setMain("src/main/java");
        target.setTest("src/test/java");
        config.setTarget(target);

        config.setEnableProjectInference(false);
    }

    

    @Test
    void filter_manifestLoadFails_returnsAllFilesAsFilesToProcess() throws Exception {
        File f1 = createFile("src/main/nl/A.md", "content A");
        File f2 = createFile("src/main/nl/B.md", "content B");

        when(trackingStore.load(any(Path.class), anyString())).thenThrow(new IOException("disk error"));

        PreFilterResult result = service.filter(List.of(f1, f2), config, projectDir, log);

        assertEquals(List.of(f1, f2), result.getFilesToProcess());
    }

    @Test
    void filter_manifestLoadFails_returnsEmptySkipDecisions() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");

        when(trackingStore.load(any(Path.class), anyString())).thenThrow(new IOException("disk error"));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getSkipDecisions().isEmpty());
    }

    @Test
    void filter_manifestLoadFails_runProjectInferenceIsTrue() throws Exception {
        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    @Test
    void filter_manifestLoadFails_logsWarning() throws Exception {
        when(trackingStore.load(any(Path.class), anyString())).thenThrow(new IOException("disk error"));

        service.filter(Collections.emptyList(), config, projectDir, log);

        
    }

    

    @Test
    void filter_noPriorRecord_fileAddedToFilesToProcess() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");

        when(trackingStore.load(any(Path.class), anyString())).thenReturn(Optional.empty());

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(f));
        assertTrue(result.getSkipDecisions().isEmpty());
                assertEquals(1, result.getWorkSetEntries().size());
                assertEquals(SourceProcessingStatus.COMPILE, result.getWorkSetEntries().get(0).getStatus());
    }

    

    @Test
    void filter_sourceFileUnreadable_fileAddedToFilesToProcess() throws Exception {
        
        File ghost = projectDir.resolve("src/main/nl/Ghost.md").toFile();
        String relPath = "src/main/nl/Ghost.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:old")));

        PreFilterResult result = service.filter(List.of(ghost), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(ghost));
        assertTrue(result.getSkipDecisions().isEmpty());
    }

    

    @Test
    void filter_deciderSaysSkipUpToDate_fileAddedToSkipDecisions() throws Exception {
        File f = createFile("src/main/nl/A.md", "same content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:abc")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:abc");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().isEmpty());
        assertEquals(1, result.getSkipDecisions().size());
                assertEquals(1, result.getWorkSetEntries().size());
                assertEquals(SourceProcessingStatus.SKIP, result.getWorkSetEntries().get(0).getStatus());
    }

    @Test
    void filter_deciderSaysSkipUpToDate_skipDecisionContainsCorrectPathAndReason() throws Exception {
        File f = createFile("src/main/nl/A.md", "same content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:abc")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:abc");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        PreFilterSkipDecision decision = result.getSkipDecisions().get(0);
        assertEquals(relPath, decision.filePath());
        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE.name(), decision.reason());
    }

    

    @Test
    void filter_deciderSaysReprocess_fileAddedToFilesToProcess() throws Exception {
        File f = createFile("src/main/nl/A.md", "updated content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:old")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:new");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relPath, ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(f));
        assertTrue(result.getSkipDecisions().isEmpty());
                assertEquals(1, result.getWorkSetEntries().size());
                assertEquals(SourceProcessingStatus.COMPILE, result.getWorkSetEntries().get(0).getStatus());
    }

    

    @Test
    void filter_emptySourceFiles_returnsEmptyFilesToProcessAndSkipDecisions() throws Exception {
        

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.getFilesToProcess().isEmpty());
        assertTrue(result.getSkipDecisions().isEmpty());
    }

    

    @Test
    void filter_mixedFiles_unchangedFileSkipped_changedFileIncluded() throws Exception {
        File changed = createFile("src/main/nl/Changed.md", "new content");
        File unchanged = createFile("src/main/nl/Unchanged.md", "same content");

        
        when(trackingStore.load(any(Path.class), eq("src/main/nl/Changed.md")))
                .thenReturn(Optional.of(priorRecord("src/main/nl/Changed.md", "sha256:old")));
        when(trackingStore.load(any(Path.class), eq("src/main/nl/Unchanged.md")))
                .thenReturn(Optional.of(priorRecord("src/main/nl/Unchanged.md", "sha256:same")));

        when(fingerprintService.sha256("new content")).thenReturn("sha256:new");
        when(fingerprintService.sha256("same content")).thenReturn("sha256:same");

        when(recompilationDecider.evaluate(eq("src/main/nl/Changed.md"),
                any(), any(), any(), any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess("src/main/nl/Changed.md",
                        ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED));
        when(recompilationDecider.evaluate(eq("src/main/nl/Unchanged.md"),
                any(), any(), any(), any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(skip("src/main/nl/Unchanged.md"));

        PreFilterResult result = service.filter(List.of(changed, unchanged), config, projectDir, log);

        assertEquals(List.of(changed), result.getFilesToProcess());
        assertEquals(1, result.getSkipDecisions().size());
        assertEquals("src/main/nl/Unchanged.md", result.getSkipDecisions().get(0).filePath());
    }

    @Test
    void filter_mixedFiles_preservesOrderOfFilesToProcess() throws Exception {
        File a = createFile("src/main/nl/A.md", "content A");
        File b = createFile("src/main/nl/B.md", "content B");
        File c = createFile("src/main/nl/C.md", "content C");

        
        when(trackingStore.load(any(Path.class), eq("src/main/nl/A.md")))
                .thenReturn(Optional.of(priorRecord("src/main/nl/A.md", "sha256:a")));
        when(trackingStore.load(any(Path.class), eq("src/main/nl/B.md")))
                .thenReturn(Optional.empty()); 
        when(trackingStore.load(any(Path.class), eq("src/main/nl/C.md")))
                .thenReturn(Optional.of(priorRecord("src/main/nl/C.md", "sha256:c")));

        when(fingerprintService.sha256("content A")).thenReturn("sha256:a");
        when(fingerprintService.sha256("content C")).thenReturn("sha256:c");

        when(recompilationDecider.evaluate(eq("src/main/nl/A.md"), any(), any(), any(),
                any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(skip("src/main/nl/A.md"));
        when(recompilationDecider.evaluate(eq("src/main/nl/C.md"), any(), any(), any(),
                any(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(skip("src/main/nl/C.md"));

        PreFilterResult result = service.filter(List.of(a, b, c), config, projectDir, log);

        assertEquals(List.of(b), result.getFilesToProcess());
        assertEquals(2, result.getSkipDecisions().size());
    }

    

    @Test
    void filter_fileUnderMainNl_passesMainScopeTargetRootToDecider() throws Exception {
        File f = createFile("src/main/nl/Foo.md", "content");
        String relPath = "src/main/nl/Foo.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:x")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:x");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        service.filter(List.of(f), config, projectDir, log);

        ArgumentCaptor<String> targetRootCaptor = ArgumentCaptor.forClass(String.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), targetRootCaptor.capture(), any(), any());
        assertEquals("src/main/java", targetRootCaptor.getValue());
    }

    @Test
    void filter_fileUnderTestNl_passesTestScopeTargetRootToDecider() throws Exception {
        File f = createFile("src/test/nl/FooTest.md", "content");
        String relPath = "src/test/nl/FooTest.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:x")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:x");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        service.filter(List.of(f), config, projectDir, log);

        ArgumentCaptor<String> targetRootCaptor = ArgumentCaptor.forClass(String.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), targetRootCaptor.capture(), any(), any());
        assertEquals("src/test/java", targetRootCaptor.getValue());
    }

    @Test
    void filter_fileOutsideNlRoots_passesUnclassifiedTargetRootToDecider() throws Exception {
        File f = createFile("docs/Readme.md", "content");
        String relPath = "docs/Readme.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:x")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:x");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        service.filter(List.of(f), config, projectDir, log);

        
        ArgumentCaptor<String> targetRootCaptor = ArgumentCaptor.forClass(String.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), targetRootCaptor.capture(), any(), any());
        
        assertNotNull(targetRootCaptor.getValue());
    }

    

    @Test
    void filter_forwardsRecompileOnSettingsToDecider() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");
        String relPath = "src/main/nl/A.md";

        RecompileOnSettings recompileOn = new RecompileOnSettings();
        recompileOn.setMarkdownReferences(false);
        recompileOn.setInspectedFiles(true);
        config.setRecompileOn(recompileOn);

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:x")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:x");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        service.filter(List.of(f), config, projectDir, log);

        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), eq(recompileOn));
    }

    

    @Test
    void filter_inferenceDisabled_runProjectInferenceIsTrue() throws Exception {
        config.setEnableProjectInference(false);

        

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    @Test
    void filter_inferenceEnabled_nullProjectContextFile_runProjectInferenceIsTrue() throws Exception {
        config.setEnableProjectInference(true);
        config.setProjectContextFile(null);

        

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    

    @Test
    void filter_inferenceEnabled_noPriorProjectRecord_runProjectInferenceIsTrue() throws Exception {
        File projectFile = createFile("project.md", "project content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq("project.md"))).thenReturn(Optional.empty());

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    

    @Test
    void filter_inferenceEnabled_emptyProjectContextPayload_runProjectInferenceIsTrue() throws Exception {
        File projectFile = createFile("project.md", "");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet()))
                .thenReturn(CompilationBackgroundPayload.empty());
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir)))
                .thenReturn(CompilationBackgroundPayload.empty());

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    

    @Test
    void filter_inferenceEnabled_projectFileUnchanged_runProjectInferenceIsFalse() throws Exception {
        File projectFile = createFile("project.md", "project content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(eq(relProjectPath), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertFalse(result.isRunProjectInference());
    }

    

    @Test
    void filter_inferenceEnabled_projectFileChanged_runProjectInferenceIsTrue() throws Exception {
        File projectFile = createFile("project.md", "updated project content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:old");

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "updated project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("updated project content")).thenReturn("sha256:new");
        when(recompilationDecider.evaluate(eq(relProjectPath), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relProjectPath,
                        ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED));

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    

    @Test
    void filter_inferenceEnabled_projectContextServiceThrows_runProjectInferenceIsTrue() throws Exception {
        File projectFile = createFile("project.md", "content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        
        when(projectContextService.load(any(), any(), anySet())).thenThrow(new RuntimeException("service error"));
        lenient().when(projectContextService.load(any(), any())).thenThrow(new RuntimeException("service error"));

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference());
    }

    @Test
    void filter_inferenceEnabled_projectContextServiceThrows_logsWarning() throws Exception {
        File projectFile = createFile("project.md", "content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        
        when(projectContextService.load(any(), any(), anySet())).thenThrow(new RuntimeException("service error"));
        lenient().when(projectContextService.load(any(), any())).thenThrow(new RuntimeException("service error"));

        service.filter(Collections.emptyList(), config, projectDir, log);

        verify(log).warn(any(CharSequence.class));
    }

    
    
    
    
    
    
    
    
    

    @Test
    void filter_inferenceEnabled_priorAiReadFilesIncludedInEvaluationInputsCurrentProjectFilePaths() throws Exception {
        
        
        File projectFile = createFile("project.md", "project content");
        createFile("src/main/java/Foo.java", "class Foo {}"); 
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        String relAiReadPath = "src/main/java/Foo.java";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        projectRecord.getInspectedFiles().put(relAiReadPath,
                new FileTrackingDetails("project", "project", 1000L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        service.filter(Collections.emptyList(), config, projectDir, log);

        ArgumentCaptor<RecompilationDecider.EvaluationInputs> inputsCaptor =
                ArgumentCaptor.forClass(RecompilationDecider.EvaluationInputs.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), inputsCaptor.capture(), any());

        Set<String> capturedPaths = inputsCaptor.getValue().currentProjectFilePaths();
        assertTrue(capturedPaths.contains(relProjectPath),
                "payload project file must be in currentProjectFilePaths");
        assertTrue(capturedPaths.contains(relAiReadPath),
                "AI-read file from prior inspectedFiles must be in currentProjectFilePaths");
    }

    @Test
    void filter_inferenceEnabled_priorInspectedFilesUnchanged_skipDecisionFires_runProjectInferenceIsFalse() throws Exception {
        
        
        File projectFile = createFile("project.md", "project content");
        createFile("src/main/java/Foo.java", "class Foo {}");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        String relAiReadPath = "src/main/java/Foo.java";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        projectRecord.getInspectedFiles().put(relAiReadPath,
                new FileTrackingDetails("project", "project", 1000L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertFalse(result.isRunProjectInference(),
                "All files unchanged — skip decision must cause runProjectInference=false");
    }

    @Test
    void filter_inferenceEnabled_priorInspectedFilesModified_deciderReturnsReprocess_runProjectInferenceIsTrue() throws Exception {
        
        
        File projectFile = createFile("project.md", "project content");
        createFile("src/main/java/Foo.java", "class Foo {}");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        String relAiReadPath = "src/main/java/Foo.java";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        projectRecord.getInspectedFiles().put(relAiReadPath,
                new FileTrackingDetails("project", "project", 1000L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relProjectPath,
                        ReprocessingDecision.ReprocessingReason.PROJECT_RECORD_TIMESTAMP_ADVANCED));

        PreFilterResult result = service.filter(Collections.emptyList(), config, projectDir, log);

        assertTrue(result.isRunProjectInference(),
                "Reprocess decision via AI-read file change must set runProjectInference=true");
    }

    @Test
    void filter_inferenceEnabled_priorInspectedFileMissingOnDisk_includedInMissingOrUnreadablePaths() throws Exception {
        
        
        
        File projectFile = createFile("project.md", "project content");
        
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";
        String relGonePath = "src/main/java/Gone.java";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        projectRecord.getInspectedFiles().put(relGonePath,
                new FileTrackingDetails("project", "project", 1000L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        service.filter(Collections.emptyList(), config, projectDir, log);

        ArgumentCaptor<RecompilationDecider.EvaluationInputs> inputsCaptor =
                ArgumentCaptor.forClass(RecompilationDecider.EvaluationInputs.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), inputsCaptor.capture(), any());

        assertTrue(inputsCaptor.getValue().missingOrUnreadablePaths().contains(relGonePath),
                "Missing AI-read file must appear in missingOrUnreadablePaths");
    }

    @Test
    void filter_inferenceEnabled_inspectedFilesOnlyContainsPayloadFile_currentProjectFilePathsHasSingleEntry() throws Exception {
        
        
        File projectFile = createFile("project.md", "project content");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        
        projectRecord.getInspectedFiles().put(relProjectPath,
                new FileTrackingDetails("project", "project", 1000L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        service.filter(Collections.emptyList(), config, projectDir, log);

        ArgumentCaptor<RecompilationDecider.EvaluationInputs> inputsCaptor =
                ArgumentCaptor.forClass(RecompilationDecider.EvaluationInputs.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), inputsCaptor.capture(), any());

        assertEquals(Set.of(relProjectPath), inputsCaptor.getValue().currentProjectFilePaths(),
                "currentProjectFilePaths should contain only the single payload file");
    }

    @Test
    void filter_inferenceEnabled_multipleAiReadFiles_allIncludedInCurrentProjectFilePaths() throws Exception {
        
        File projectFile = createFile("project.md", "project content");
        createFile("src/main/java/A.java", "class A {}");
        createFile("src/main/java/B.java", "class B {}");
        createFile("docs/README.md", "readme");
        config.setEnableProjectInference(true);
        config.setProjectContextFile(projectFile);

        String relProjectPath = "project.md";

        SourceTrackingRecord projectRecord = priorRecord(relProjectPath, "sha256:same");
        projectRecord.getInspectedFiles().put("src/main/java/A.java",
                new FileTrackingDetails("project", "project", 1000L));
        projectRecord.getInspectedFiles().put("src/main/java/B.java",
                new FileTrackingDetails("project", "project", 1001L));
        projectRecord.getInspectedFiles().put("docs/README.md",
                new FileTrackingDetails("project", "project", 1002L));

        CompilationBackgroundPayload payload = new CompilationBackgroundPayload(List.of(
                new CompilationBackgroundFile(projectFile.toPath().toAbsolutePath().normalize(),
                        "project.md", "project content")));

        
        when(projectContextService.load(eq(projectFile), eq(projectDir), anySet())).thenReturn(payload);
        lenient().when(projectContextService.load(eq(projectFile), eq(projectDir))).thenReturn(payload);
        when(trackingStore.load(any(Path.class), eq(relProjectPath)))
                .thenReturn(Optional.of(projectRecord));
        when(fingerprintService.sha256("project content")).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relProjectPath));

        service.filter(Collections.emptyList(), config, projectDir, log);

        ArgumentCaptor<RecompilationDecider.EvaluationInputs> inputsCaptor =
                ArgumentCaptor.forClass(RecompilationDecider.EvaluationInputs.class);
        verify(recompilationDecider).evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), inputsCaptor.capture(), any());

        Set<String> paths = inputsCaptor.getValue().currentProjectFilePaths();
        assertAll(
                () -> assertTrue(paths.contains(relProjectPath)),
                () -> assertTrue(paths.contains("src/main/java/A.java")),
                () -> assertTrue(paths.contains("src/main/java/B.java")),
                () -> assertTrue(paths.contains("docs/README.md"))
        );
    }

    

    @Test
    void filter_resultFilesToProcessIsDefensiveCopy() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");

        
        when(trackingStore.load(any(Path.class), any())).thenReturn(Optional.empty());

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        
        assertDoesNotThrow(() -> result.getFilesToProcess().clear());
    }

    

    @Test
    void filter_deciderReturnsOutputMissing_fileIncluded() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:same")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relPath, ReprocessingDecision.ReprocessingReason.OUTPUT_MISSING));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(f));
    }

    @Test
    void filter_deciderReturnsTargetRootChanged_fileIncluded() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:same")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relPath, ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(f));
    }

    @Test
    void filter_deciderReturnsSourceNewerThanOutputs_fileIncluded() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");
        String relPath = "src/main/nl/A.md";

        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:same")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:same");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relPath, ReprocessingDecision.ReprocessingReason.SOURCE_NEWER_THAN_OUTPUTS));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertTrue(result.getFilesToProcess().contains(f));
    }

    @Test
    void filter_skipDecision_reasonMatchesDeciderReason() throws Exception {
        File f = createFile("src/main/nl/A.md", "content");
        String relPath = "src/main/nl/A.md";

        
        
        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:x")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:x");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(skip(relPath));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE.name(),
                result.getSkipDecisions().get(0).reason());
    }

    

    @Test
    void filter_deciderSaysReprocess_workSetEntryCarriesSelectionReason() throws Exception {
        File f = createFile("src/main/nl/A.md", "updated content");
        String relPath = "src/main/nl/A.md";

        when(trackingStore.load(any(Path.class), eq(relPath)))
                .thenReturn(Optional.of(priorRecord(relPath, "sha256:old")));
        when(fingerprintService.sha256(anyString())).thenReturn("sha256:new");
        when(recompilationDecider.evaluate(any(), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any()))
                .thenReturn(reprocess(relPath, ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED));

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertEquals(1, result.getWorkSetEntries().size());
        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED,
                result.getWorkSetEntries().get(0).getSelectionReason());
    }

    @Test
    void filter_noPriorRecord_workSetEntryHasNoPriorRecordSelectionReason() throws Exception {
        File f = createFile("src/main/nl/New.md", "content");

        when(trackingStore.load(any(Path.class), anyString())).thenReturn(Optional.empty());

        PreFilterResult result = service.filter(List.of(f), config, projectDir, log);

        assertEquals(1, result.getWorkSetEntries().size());
        assertEquals(ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD,
                result.getWorkSetEntries().get(0).getSelectionReason());
    }

    

    private File createFile(String relPath, String content) throws IOException {
        Path path = projectDir.resolve(relPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path.toFile();
    }

    private SourceTrackingRecord priorRecord(String sourcePath, String sourceHash) {
        SourceTrackingRecord r = new SourceTrackingRecord();
        r.setSourcePath(sourcePath);
        r.setSourceHash(sourceHash);
        r.setBlockFingerprints(List.of(sourceHash));
        r.setCompiledFiles(new LinkedHashMap<>());
        r.setInspectedFiles(new LinkedHashMap<>());
        r.setSourceModificationTime(1000L);
        return r;
    }

    private ReprocessingDecision skip(String path) {
        return new ReprocessingDecision(path, false, ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE);
    }

    private ReprocessingDecision reprocess(String path, ReprocessingDecision.ReprocessingReason reason) {
        return new ReprocessingDecision(path, true, reason);
    }
}
