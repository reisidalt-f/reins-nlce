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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.RecompilationDecider;
import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundFile;
import br.com.dizeno.reins.compilation.context.CompilationBackgroundPayload;
import br.com.dizeno.reins.reasoning.ReasoningPromptBuilder;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import br.com.dizeno.reins.reasoning.ReferenceTreeContextService;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultProjectReasoningCycleServiceTest {

    @TempDir
    Path projectDir;

    ReasoningService reasoningService;
    CompilationTrackingStore trackingStore;
    RecompilationDecider recompilationDecider;
    SourceFingerprintService fingerprintService;
    ReferenceTreeContextService referenceTreeContextService;
    ReasoningPromptBuilder promptBuilder;
    Log log;

    DefaultProjectReasoningCycleService service;

    @BeforeEach
        void setUp() throws Exception {
        reasoningService = mock(ReasoningService.class);
        trackingStore = mock(CompilationTrackingStore.class);
        recompilationDecider = mock(RecompilationDecider.class);
        fingerprintService = mock(SourceFingerprintService.class);
        referenceTreeContextService = mock(ReferenceTreeContextService.class);
        promptBuilder = mock(ReasoningPromptBuilder.class);
        log = mock(Log.class);
        when(trackingStore.load(any(), anyString())).thenReturn(Optional.empty());

        service = new DefaultProjectReasoningCycleService(
                reasoningService, trackingStore, recompilationDecider,
                fingerprintService, referenceTreeContextService, promptBuilder,
                log, projectDir
        );
    }

    

    @Test
    void upsertsTrackingRecord_afterSuccessfulCycle() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash-abc");
        when(recompilationDecider.evaluate(anyString(), any(), anyString(), any(), any(), any(), any(long.class), any(), any(), any(), any()))
                .thenReturn(reprocessDecision());
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, config());

        ArgumentCaptor<SourceTrackingRecord> recordCaptor = ArgumentCaptor.forClass(SourceTrackingRecord.class);
        verify(trackingStore).save(any(), anyString(), recordCaptor.capture());

        SourceTrackingRecord stored = recordCaptor.getValue();
        assertEquals("project.md", stored.getSourcePath());
        assertEquals("project", stored.getSourceCategory());
        assertEquals("hash-abc", stored.getSourceHash());
        assertEquals("", stored.getResolvedTargetRoot(),
                "resolvedTargetRoot should be project-root-relative and empty when target base is the project root");
        assertEquals("success", stored.getLastStatus());
        assertTrue(stored.getBlockFingerprints().contains("hash-abc"),
                "blockFingerprints should include source hash");
        assertTrue(new java.util.ArrayList<>(stored.getCompiledFiles().keySet()).isEmpty(),
                "compiledFiles should be empty for project cycle");
    }

    @Test
    void upsertsRecord_evenWhenCycleProducesNoFileOutput() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash-xyz");
        when(recompilationDecider.evaluate(anyString(), any(), anyString(), any(), any(), any(), any(long.class), any(), any(), any(), any()))
                .thenReturn(reprocessDecision());
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, config());

        verify(trackingStore).save(any(), anyString(), any());
    }

    

    @Test
    void throwsMojoExecutionException_whenCycleThrowsChecked() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash");
        when(recompilationDecider.evaluate(anyString(), any(), anyString(), any(), any(), any(), any(long.class), any(), any(), any(), any()))
                .thenReturn(reprocessDecision());
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenThrow(new MojoExecutionException("api failure"));

        assertThrows(MojoExecutionException.class,
                () -> service.run(payload, config()));
    }

    @Test
    void doesNotUpsertRecord_whenCycleFails() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash");
        when(recompilationDecider.evaluate(anyString(), any(), anyString(), any(), any(), any(), any(long.class), any(), any(), any(), any()))
                .thenReturn(reprocessDecision());
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenThrow(new RuntimeException("boom"));

        assertThrows(MojoExecutionException.class,
                () -> service.run(payload, config()));

        verify(trackingStore, never()).save(any(), anyString(), any());
    }

    

    @Test
    void reasoningRequest_hasProjectInferenceCycleTrue() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("h");
        when(recompilationDecider.evaluate(anyString(), any(), anyString(), any(), any(), any(), any(long.class), any(), any(), any(), any()))
                .thenReturn(reprocessDecision());
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, config());

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> captor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(captor.capture(), any());

        assertTrue(captor.getValue().isProjectInferenceCycle(),
                "ReasoningRequest.projectInferenceCycle must be true for project inference cycle");
        assertEquals("project", captor.getValue().getSourceScope(),
                "ReasoningRequest.sourceScope must reflect the project scope");
    }

    @Test
    void reasoningRequest_routesTargetBaseToCanonicalProjectTarget() throws Exception {
        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash-target");
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, config());

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> captor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(captor.capture(), any());

        assertEquals(projectDir.toAbsolutePath().normalize(),
                captor.getValue().getBaseMappings().getTargetRoot(),
                "Project inference should route MCP target operations to the project target base");
    }

    @Test
    void reasoningRequest_keepsCanonicalProjectPathForSharedReferenceTreeFlow() throws Exception {
        Path projectFile = projectDir.resolve("docs/architecture/project.md");
        Files.createDirectories(projectFile.getParent());
        Files.writeString(projectFile, "# Project\n");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        when(fingerprintService.sha256(anyString())).thenReturn("hash-path");
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, config());

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> captor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(captor.capture(), any());

        assertEquals("docs/architecture/project.md", captor.getValue().getSourcePath());
        assertEquals("main:docs/architecture/project.md", captor.getValue().getMainSourceQualifiedPath(),
                "Project inference should preserve the canonical source path used by the shared reference-tree flow");
    }

    @Test
    void reasoningRequest_usesSourceBasePhaseOrderOverrideFromCustomScript() throws Exception {
        Path customScripts = projectDir.resolve("custom-scripts");
        Files.createDirectories(customScripts);
        Files.writeString(customScripts.resolve("source-base-phase-list.ftl"),
                "system-context\nreference-tree\nproject-context");

        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        ReinsConfig cfg = config();
        cfg.setScriptsPath("custom-scripts");

        when(fingerprintService.sha256(anyString())).thenReturn("hash-phases");
        when(referenceTreeContextService.build(any(), any(), any(), any())).thenReturn(emptyTreeContext());
        when(reasoningService.runCycle(any(), any())).thenReturn(noOutputResult());

        service.run(payload, cfg);

        ArgumentCaptor<br.com.dizeno.reins.reasoning.ReasoningRequest> captor =
                ArgumentCaptor.forClass(br.com.dizeno.reins.reasoning.ReasoningRequest.class);
        verify(reasoningService).runCycle(captor.capture(), any());

        assertEquals(List.of("system-context", "reference-tree", "project-context"),
                captor.getValue().getPhaseOrderOverride());
    }

    @Test
    void failsProjectInferenceWhenSourceBasePhaseListContainsUnknownPhaseAndFailOnErrorIsTrue() throws Exception {
        Path customScripts = projectDir.resolve("custom-scripts");
        Files.createDirectories(customScripts);
        Files.writeString(customScripts.resolve("source-base-phase-list.ftl"),
                "system-context\nunknown-phase\nproject-context");

        Path projectFile = projectDir.resolve("project.md");
        Files.writeString(projectFile, "# Project");

        CompilationBackgroundPayload payload = payloadFor(projectFile);
        ReinsConfig cfg = config();
        cfg.setScriptsPath("custom-scripts");
        cfg.setFailOnError(true);

        when(fingerprintService.sha256(anyString())).thenReturn("hash-error");

        MojoExecutionException error = assertThrows(
                MojoExecutionException.class,
                () -> service.run(payload, cfg));

        assertTrue(error.getMessage().contains("Project source-base phase resolution failed"));
        assertTrue(error.getMessage().contains("Unknown phase name in custom phase list: unknown-phase"));
        verify(reasoningService, never()).runCycle(any(), any());
    }

        @Test
        void failsProjectInferenceWhenSourceBasePhaseListContainsUnknownPhaseEvenWhenFailOnErrorIsFalse() throws Exception {
                Path customScripts = projectDir.resolve("custom-scripts");
                Files.createDirectories(customScripts);
                Files.writeString(customScripts.resolve("source-base-phase-list.ftl"),
                                "system-context\nunknown-phase\nproject-context");

                Path projectFile = projectDir.resolve("project.md");
                Files.writeString(projectFile, "# Project");

                CompilationBackgroundPayload payload = payloadFor(projectFile);
                ReinsConfig cfg = config();
                cfg.setScriptsPath("custom-scripts");
                cfg.setFailOnError(false);

                when(fingerprintService.sha256(anyString())).thenReturn("hash-error");

                MojoExecutionException error = assertThrows(
                                MojoExecutionException.class,
                                () -> service.run(payload, cfg));

                assertTrue(error.getMessage().contains("Project source-base phase resolution failed"));
                assertTrue(error.getMessage().contains("Unknown phase name in custom phase list: unknown-phase"));
                verify(reasoningService, never()).runCycle(any(), any());
        }

    

    private CompilationBackgroundPayload payloadFor(Path file) throws Exception {
        String content = Files.readString(file);
        CompilationBackgroundFile ctxFile = new CompilationBackgroundFile(file, file.getFileName().toString(), content);
        return new CompilationBackgroundPayload(List.of(ctxFile));
    }

    private ReinsConfig config() {
        ReinsConfig config = new ReinsConfig();
        ReasoningSettings reasoning = new ReasoningSettings();
        reasoning.setEnabled(true);
        config.setReasoning(reasoning);
                config.setTarget(new TargetSettings());
        return config;
    }

    private ReprocessingDecision reprocessDecision() {
        return new ReprocessingDecision("project.md", true, ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD);
    }

    private ReferenceTreeContextService.ReferenceTreeContext emptyTreeContext() {
        return new ReferenceTreeContextService.ReferenceTreeContext(
                "project.md",
                "project.md",
                new br.com.dizeno.reins.reasoning.ReferenceTreeRenderPolicy(8),
                java.util.Map.of()
        );
    }

    private ReasoningResult noOutputResult() {
        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setToolInfoPhrases(List.of());
        return result;
    }
}
