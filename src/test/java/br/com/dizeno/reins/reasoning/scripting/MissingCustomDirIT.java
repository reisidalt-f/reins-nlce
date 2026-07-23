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

package br.com.dizeno.reins.reasoning.scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingCustomDirIT {

    @TempDir
    Path tempDir;

    @Test
    void missingCustomScriptDirectoryFailsValidationBeforeInference() {
        Path missingDir = tempDir.resolve("missing-custom-scripts");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(missingDir.toFile(), null), missingDir.toFile());

        IllegalStateException error = assertThrows(IllegalStateException.class, registry::validateAll);
        assertTrue(error.getMessage().contains("Cannot resolve custom script directory: " + missingDir));
    }
}
