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

package br.com.dizeno.reins.run.mojo;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.compilation.CompilationService;
import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.PreFilterResult;
import br.com.dizeno.reins.compilation.ReprocessingPreFilterService;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

 
class CompileMojoSourceEmptyTest {

    @TempDir
    Path projectDir;

    private CompileMojo mojo;
    private CompilationService compilationService;
    private ReprocessingPreFilterService preFilterService;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new CompileMojo();

        MavenProject project = new MavenProject() {
            @Override
            public File getBasedir() {
                return projectDir.toFile();
            }
        };

        compilationService = mock(CompilationService.class);
        preFilterService = mock(ReprocessingPreFilterService.class);
        log = mock(Log.class);

        mojo.setCompilationService(compilationService);
        mojo.setPreFilterService(preFilterService);
        mojo.setLog(log);

        setField(mojo, "project", project);
        setField(mojo, "includePattern", "**/*.md");
        TargetSettings targetSettings = new TargetSettings();
        targetSettings.setProject(projectDir.toFile());
        targetSettings.setMain("src/main/java");
        targetSettings.setTest("src/test/java");
        setField(mojo, "target", targetSettings);

        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        gemini.setTimeoutSeconds(30);
        gemini.setRetryAttempts(1);
        setField(mojo, "gemini", gemini);

        
        
    }

    

    @Test
    void execute_sourceEmpty_inferenceEnabled_callsProcessFilesWithEmptyList() throws Exception {
        File projectFile = projectDir.resolve("project.md").toFile();
        projectFile.createNewFile();
        setField(mojo, "projectContextFile", projectFile);
        setField(mojo, "enableProjectInference", true);

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        when(compilationService.processFiles(any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new CompilationSummary());

        mojo.execute();

        verify(compilationService).processFiles(
                org.mockito.ArgumentMatchers.eq(List.of()),
                any(Boolean.class),
                any(),
                any(),
                any());
    }

    @Test
    void execute_sourceEmpty_inferenceDisabled_doesNotCallProcessFiles() throws Exception {
        setField(mojo, "enableProjectInference", false);
        

        assertDoesNotThrow(() -> mojo.execute());

        verify(compilationService, never()).processFiles(any(), any(Boolean.class), any(), any(), any());
    }

    @Test
    void execute_inferenceEnabled_missingProjectFile_throwsMojoExecutionException() throws Exception {
        File missingFile = projectDir.resolve("project.md").toFile();
        
        setField(mojo, "projectContextFile", missingFile);
        setField(mojo, "enableProjectInference", true);

        assertThrows(MojoExecutionException.class, () -> mojo.execute());

        verify(compilationService, never()).processFiles(any(), any(Boolean.class), any(), any(), any());
    }

    @Test
    void execute_sourceEmpty_inferenceEnabled_buildSucceeds() throws Exception {
        File projectFile = projectDir.resolve("project.md").toFile();
        projectFile.createNewFile();
        setField(mojo, "projectContextFile", projectFile);
        setField(mojo, "enableProjectInference", true);

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        when(compilationService.processFiles(any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new CompilationSummary());

        assertDoesNotThrow(() -> mojo.execute());
    }

    

    @Test
    void execute_sourceEmpty_inferenceEnabled_logsFR003Message() throws Exception {
        File projectFile = projectDir.resolve("project.md").toFile();
        projectFile.createNewFile();
        setField(mojo, "projectContextFile", projectFile);
        setField(mojo, "enableProjectInference", true);

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        when(compilationService.processFiles(any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new CompilationSummary());

        mojo.execute();

        verify(log).info(
                "No source instruction files found in scan roots. Project inference cycle will proceed.");
    }

    @Test
    void execute_sourceEmpty_inferenceDisabled_logsFR004Message() throws Exception {
        setField(mojo, "enableProjectInference", false);

        mojo.execute();

        verify(log).info(
                "No source instruction files found in scan roots. No inference will be performed.");
    }

    @Test
    void execute_verboseMode_doesNotLogProviderDetails() throws Exception {
        setField(mojo, "verbose", true);
        setField(mojo, "enableProjectInference", false);

        mojo.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(captor.capture());
        List<String> infoMessages = captor.getAllValues();
        assertTrue(infoMessages.stream().noneMatch(m -> m.startsWith("Gemini model:")),
                "Gemini model details must not be logged");
        assertTrue(infoMessages.stream().noneMatch(m -> m.startsWith("Gemini endpoint:")),
                "Gemini endpoint must not be logged");
        assertTrue(infoMessages.stream().noneMatch(m -> m.startsWith("Gemini apiKey:")),
                "Gemini apiKey must not be logged");
    }

    @Test
    void execute_warnsWhenDeprecatedTargetRootIsConfigured() throws Exception {
        TargetSettings targetSettings = new TargetSettings();
        targetSettings.setLegacyRootAlias(projectDir.resolve("compiled/project").toFile());
        setField(mojo, "target", targetSettings);

        mojo.execute();

        verify(log, times(1)).warn("target.root is deprecated; use target.project instead.");
    }

    

    @Test
    void execute_sourceEmpty_inferenceEnabled_skippedSummaryLogged() throws Exception {
        File projectFile = projectDir.resolve("project.md").toFile();
        projectFile.createNewFile();
        setField(mojo, "projectContextFile", projectFile);
        setField(mojo, "enableProjectInference", true);

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        CompilationSummary summary = new CompilationSummary();
        summary.incrementSkipped(); 
        when(compilationService.processFiles(any(), any(Boolean.class), any(), any(), any())).thenReturn(summary);

        mojo.execute();

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(logCaptor.capture());
        boolean skippedInSummary = logCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("skipped=1"));
        assertTrue(skippedInSummary,
                "Expected summary INFO log to contain 'skipped=1' but logged: "
                        + logCaptor.getAllValues());
    }

    

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }
}
