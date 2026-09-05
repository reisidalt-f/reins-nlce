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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        Files.createDirectories(projectDir.resolve("src/main/nl"));
        java.util.Map<String, File> sourceBases = new java.util.LinkedHashMap<>();
        sourceBases.put("main", projectDir.resolve("src/main/nl").toFile());
        setField(mojo, "sources", sourceBases);
        TargetSettings targetSettings = new TargetSettings();
        targetSettings.setTargetBase("main", "src/main/java");
        targetSettings.setTargetBase("test", "src/test/java");
        setField(mojo, "target", targetSettings);

        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        gemini.setTimeoutSeconds(30);
        gemini.setRetryAttempts(1);
        setField(mojo, "provider", "gemini");
        setField(mojo, "gemini", gemini);
    }

    @Test
    void execute_sourceEmpty_doesNotCallProcessFiles() throws Exception {
        assertDoesNotThrow(() -> mojo.execute());

        verify(compilationService, never()).processFiles(any(PreFilterResult.class), any(), any(), any());
    }

    @Test
    void execute_sourceEmpty_logsNoInferenceMessage() throws Exception {
        mojo.execute();

        verify(log).info(
                "No source instruction files found in scan roots. No inference will be performed.");
    }

    @Test
    void execute_verboseMode_doesNotLogProviderDetails() throws Exception {
        setField(mojo, "verbose", true);

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
