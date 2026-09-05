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
                recompilationDecider, trackingStore, fingerprintService);

        config = new ReinsConfig();
        config.setSourceBase("main", projectDir.resolve("src/main/nl").toFile());
        config.setSourceBase("test", projectDir.resolve("src/test/nl").toFile());

        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        target.setTargetBase("test", "src/test/java");
        config.setTarget(target);
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
