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
import java.util.List;

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
        Files.createDirectories(projectDir.resolve("src/main/nl"));
        Files.createDirectories(projectDir.resolve("src/test/nl"));
        java.util.Map<String, File> sourceBases = new java.util.LinkedHashMap<>();
        sourceBases.put("main", projectDir.resolve("src/main/nl").toFile());
        sourceBases.put("test", projectDir.resolve("src/test/nl").toFile());
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

        when(preFilterService.filter(any(), any(), any(), any()))
                .thenAnswer(inv -> PreFilterResult.failOpen(inv.getArgument(0)));
        when(compilationService.processFiles(any(PreFilterResult.class), any(), any(), any()))
                .thenReturn(new CompilationSummary());
        when(compilationService.processFiles(any(List.class), any(), any(), any()))
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
            argThat((PreFilterResult pfr) -> pfr != null && pfr.getSourceFiles().size() == 1
                && pfr.getSourceFiles().get(0).toPath().endsWith("src/main/nl/chosen.md")),
            any(ReinsConfig.class), any(Path.class), any(Log.class));
    }

    @Test
    void execute_multipleExplicitSources_processesAllMatchedSources() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainRoot);
        Files.writeString(mainRoot.resolve("first.md"), "# first");
        Files.writeString(mainRoot.resolve("second.md"), "# second");
        Files.writeString(mainRoot.resolve("third.md"), "# third");
        setField(mojo, "source", "first.md, second.md");

        mojo.execute();

        verify(compilationService).processFiles(
            argThat((PreFilterResult pfr) -> pfr != null && pfr.getSourceFiles().size() == 2
                && pfr.getSourceFiles().stream().anyMatch(f -> f.getName().equals("first.md"))
                && pfr.getSourceFiles().stream().anyMatch(f -> f.getName().equals("second.md"))),
            any(ReinsConfig.class), any(Path.class), any(Log.class));
    }

    @Test
    void execute_explicitDirectoryWithNoEligibleFiles_warnsAndSkipsCompilation() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainRoot.resolve("empty-dir"));
        Files.writeString(mainRoot.resolve("empty-dir/readme.txt"), "ignore");
        setField(mojo, "source", "empty-dir");

        mojo.execute();

        verify(compilationService, never()).processFiles(any(List.class), any(), any(), any());
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
                any(PreFilterResult.class), any(ReinsConfig.class), any(Path.class), any(Log.class));
        verify(log, never()).info(org.mockito.ArgumentMatchers.contains("Explicit source mode active"));
    }

    @Test
    void execute_explicitSources_runsThroughDependencyGraphAndSupportsMultiThreading() throws Exception {
        Path mainRoot = projectDir.resolve("src/main/nl");
        Files.createDirectories(mainRoot);
        Files.writeString(mainRoot.resolve("parent.md"), "# parent\nSee [child](child.md)");
        Files.writeString(mainRoot.resolve("child.md"), "# child");
        setField(mojo, "source", "parent.md, child.md");

        BuildSettings buildSettings = new BuildSettings();
        buildSettings.setCompilationThreads(4);
        setField(mojo, "build", buildSettings);

        mojo.execute();

        verify(compilationService).processFiles(
            argThat((PreFilterResult pfr) -> pfr != null && pfr.getSourceFiles().size() == 2),
            argThat((ReinsConfig cfg) -> cfg.getBuild() != null && cfg.getBuild().getCompilationThreads() == 4),
            any(Path.class), any(Log.class));
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
