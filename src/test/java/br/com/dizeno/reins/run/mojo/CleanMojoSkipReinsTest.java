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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CleanMojoSkipReinsTest {

    private CleanMojo mojo;
    private Log log;

    @BeforeEach
    void setUp() {
        mojo = new CleanMojo();
        log = mock(Log.class);
        mojo.setLog(log);
    }

    @Test
    void execute_skipReinsTrue_returnsBeforeCleanup() throws Exception {
        setField(mojo, "skipReins", "true");

        assertDoesNotThrow(() -> mojo.execute());
        verify(log).info("[skipReins] reins:clean skipped (reasonCode=SKIP_ENABLED, value=true).");
    }

    @Test
    void execute_skipReinsInvalid_failsFast() throws Exception {
        setField(mojo, "skipReins", "maybe");

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
