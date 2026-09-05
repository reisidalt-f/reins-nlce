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

import br.com.dizeno.reins.compilation.pipeline.SourceCompilationContext;
import br.com.dizeno.reins.compilation.pipeline.TrackingLoadPhase;
import br.com.dizeno.reins.compilation.pipeline.TrackingRecordHelper;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.ReinsConfigLoader;
import br.com.dizeno.reins.run.config.settings.BuildSettings;
import br.com.dizeno.reins.run.config.settings.TargetSettings;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreshCompilationTest {

    @TempDir
    Path projectDir;

    private Log log;

    @BeforeEach
    void setUp() {
        log = mock(Log.class);
    }

    @Test
    void testFreshCompilationConfigDefaultsAndSetters() {
        ReinsConfig config = new ReinsConfig();
        assertFalse(config.isFreshCompilation(), "freshCompilation should default to false in ReinsConfig");

        config.setFreshCompilation(true);
        assertTrue(config.isFreshCompilation());

        BuildSettings buildSettings = new BuildSettings();
        assertFalse(buildSettings.isFreshCompilation(), "freshCompilation should default to false in BuildSettings");
        buildSettings.setFreshCompilation(true);
        assertTrue(buildSettings.isFreshCompilation());
    }

    @Test
    void testReinsConfigLoaderBindsFreshCompilation() {
        String[] cliArgs = new String[]{"--freshCompilation"};
        ReinsConfig configCli = ReinsConfigLoader.load(cliArgs, null, null, projectDir.toFile());
        assertTrue(configCli.getBuild().isFreshCompilation(), "--freshCompilation CLI flag should set build.freshCompilation");

        String[] sysPropsCli = new String[]{"-DfreshCompilation"};
        ReinsConfig configSysProps = ReinsConfigLoader.load(sysPropsCli, null, null, projectDir.toFile());
        assertTrue(configSysProps.getBuild().isFreshCompilation(), "-DfreshCompilation CLI property should set build.freshCompilation");

        Properties sysPropsBuild = new Properties();
        sysPropsBuild.setProperty("reins.build.freshCompilation", "true");
        ReinsConfig configBuild = ReinsConfigLoader.load(null, sysPropsBuild, null, projectDir.toFile());
        assertTrue(configBuild.isFreshCompilation());
        assertTrue(configBuild.getBuild().isFreshCompilation());
    }

    @Test
    void testPreFilterServiceForcesCompileWhenFreshCompilationIsTrue() throws Exception {
        DefaultReprocessingPreFilterService service = new DefaultReprocessingPreFilterService();
        ReinsConfig config = new ReinsConfig();
        config.setFreshCompilation(true);

        Path sourceFile = projectDir.resolve("src/main/nl/App.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# App Spec");

        PreFilterResult result = service.filter(List.of(sourceFile.toFile()), config, projectDir, log);

        assertEquals(1, result.getWorkSetEntries().size());
        CycleWorkSetEntry entry = result.getWorkSetEntries().get(0);
        assertEquals(SourceProcessingStatus.COMPILE, entry.getStatus());
        assertEquals(ReprocessingDecision.ReprocessingReason.FRESH_COMPILATION, entry.getSelectionReason());
    }

    @Test
    void testDeletePreviouslyGeneratedFilesDeletesTrackedOutputs() throws Exception {
        Path generatedFile = projectDir.resolve("target/generated-sources/App.java");
        Files.createDirectories(generatedFile.getParent());
        Files.writeString(generatedFile, "public class App {}");
        assertTrue(Files.exists(generatedFile));

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("src/main/nl/App.md");
        record.setResolvedTargetRoot("target/generated-sources");
        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put("target:App.java", new FileTrackingDetails("target:App.java", "main", 1000L));
        record.setCompiledFiles(compiledFiles);

        TrackingRecordHelper.deletePreviouslyGeneratedFiles(projectDir, record, log);

        assertFalse(Files.exists(generatedFile), "Previously generated file should have been deleted");
    }

    @Test
    void testTrackingLoadPhaseDeletesPreviousFilesWhenFreshCompilationIsTrue() throws Exception {
        Path generatedFile = projectDir.resolve("target/generated-sources/App.java");
        Files.createDirectories(generatedFile.getParent());
        Files.writeString(generatedFile, "public class App {}");
        assertTrue(Files.exists(generatedFile));

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("src/main/nl/App.md");
        record.setResolvedTargetRoot("target/generated-sources");
        LinkedHashMap<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
        compiledFiles.put("target:App.java", new FileTrackingDetails("target:App.java", "main", 1000L));
        record.setCompiledFiles(compiledFiles);

        CompilationTrackingStore trackingStore = mock(CompilationTrackingStore.class);
        when(trackingStore.load(any(), any())).thenReturn(java.util.Optional.of(record));

        ReinsConfig config = new ReinsConfig();
        config.setFreshCompilation(true);

        SourceCompilationContext ctx = new SourceCompilationContext();
        ctx.setProjectRoot(projectDir);
        ctx.setCanonicalSourcePath("src/main/nl/App.md");
        ctx.setConfig(config);
        ctx.setTrackingStore(trackingStore);
        ctx.setSourceTrackingManager(new br.com.dizeno.reins.compilation.tracking.SourceTrackingManager());
        ctx.setLog(log);

        TrackingLoadPhase phase = new TrackingLoadPhase();
        phase.execute(ctx, new br.com.dizeno.reins.compilation.pipeline.PhaseChain(List.of()));

        assertFalse(Files.exists(generatedFile), "Generated output should be deleted during TrackingLoadPhase when freshCompilation is true");
        assertNotNull(ctx.getPriorRecord());
        assertTrue(ctx.getPriorRecord().getCompiledFiles().isEmpty(), "priorRecord compiledFiles should be cleared");
        verify(trackingStore).save(eq(projectDir), eq("src/main/nl/App.md"), any(SourceTrackingRecord.class));
    }
}
