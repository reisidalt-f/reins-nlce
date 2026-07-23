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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompileMojoExplicitSourceTest {

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
        setField(mojo, "mainNlRoot", projectDir.resolve("src/main/nl").toFile());
        setField(mojo, "testNlRoot", projectDir.resolve("src/test/nl").toFile());
        setField(mojo, "enableProjectInference", false);

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

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        when(compilationService.processFiles(any(), any(Boolean.class), any(), any(), any()))
                .thenReturn(new CompilationSummary());
    }

    @Test
    void execute_explicitFile_processesOnlyMatchedSource() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainRoot);
        Files.writeString(mainRoot.resolve("chosen.md"), "# chosen");
        Files.writeString(mainRoot.resolve("other.md"), "# other");
        setField(mojo, "source", "chosen.md");

        mojo.execute();

        verify(compilationService).processFiles(
            argThat(processed -> processed.size() == 1
                && processed.get(0).toPath().endsWith("src/main/nl/chosen.md")),
            any(Boolean.class), any(), any(), any());
    }

    @Test
    void execute_explicitDirectoryWithNoEligibleFiles_warnsAndSkipsCompilation() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainRoot.resolve("empty-dir"));
        Files.writeString(mainRoot.resolve("empty-dir/readme.txt"), "ignore");
        setField(mojo, "source", "empty-dir");

        mojo.execute();

        verify(compilationService, never()).processFiles(any(), any(Boolean.class), any(), any(), any());
        verify(log).warn("Explicit source directory contains zero eligible files: src/main/nl/empty-dir");
    }

    @Test
    void execute_withoutSource_keepsDefaultScanBehavior() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Path testRoot = projectDir.resolve("src/test/nl");
        Files.createDirectories(mainRoot);
        Files.createDirectories(testRoot);
        Files.writeString(mainRoot.resolve("main.md"), "# main");
        Files.writeString(testRoot.resolve("test.md"), "# test");

        mojo.execute();

        verify(compilationService).processFiles(
                argThat(processed -> processed.size() == 2),
                any(Boolean.class), any(), any(), any());
        verify(log, never()).info(org.mockito.ArgumentMatchers.contains("Explicit source mode active"));
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
