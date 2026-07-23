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

import br.com.dizeno.reins.compilation.CompilationService;
import br.com.dizeno.reins.compilation.ReprocessingPreFilterService;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CompileMojoSkipReinsTest {

    @TempDir
    Path projectDir;

    private CompileMojo mojo;
    private CompilationService compilationService;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new CompileMojo();
        compilationService = mock(CompilationService.class);
        ReprocessingPreFilterService preFilterService = mock(ReprocessingPreFilterService.class);
        log = mock(Log.class);

        MavenProject project = new MavenProject() {
            @Override
            public File getBasedir() {
                return projectDir.toFile();
            }
        };

        mojo.setCompilationService(compilationService);
        mojo.setPreFilterService(preFilterService);
        mojo.setLog(log);

        setField(mojo, "project", project);
        setField(mojo, "includePattern", "**/*.md");
    }

    @Test
    void execute_skipReinsTrue_skipsBeforeCompilation() throws Exception {
        setField(mojo, "skipReins", "true");

        assertDoesNotThrow(() -> mojo.execute());

        verify(log).info("[skipReins] reins:compile skipped (reasonCode=SKIP_ENABLED, value=true).");
        verify(compilationService, never()).processFiles(any(), any(Boolean.class), any(), any(), any());
    }

    @Test
    void execute_skipReinsInvalid_failsFast() throws Exception {
        setField(mojo, "skipReins", "maybe");

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
        verify(compilationService, never()).processFiles(any(), any(Boolean.class), any(), any(), any());
    }

    @Test
    void execute_skipReinsFalse_doesNotSkip() throws Exception {
        setField(mojo, "skipReins", "false");

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
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
