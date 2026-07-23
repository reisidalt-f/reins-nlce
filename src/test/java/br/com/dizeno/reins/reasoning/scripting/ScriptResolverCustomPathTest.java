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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptResolverCustomPathTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesCustomFilesystemScriptBeforeBundledDefault() throws IOException {
        Path customSystemContext = tempDir.resolve("system-context.ftl");
        Files.writeString(customSystemContext, "custom-system-context");

        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), null);
        ScriptDescriptor descriptor = resolver.resolve("system-context.ftl", PhaseType.PROMPT_ASSEMBLY);

        assertEquals(ScriptSource.CUSTOM_FILESYSTEM, descriptor.getResolvedFrom());
        assertEquals(customSystemContext.toAbsolutePath().toString(), descriptor.getResolvedLocation());
    }

    @Test
    void fallsBackToBundledDefaultWhenCustomFilesystemScriptIsMissing() throws IOException {
        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), null);
        ScriptDescriptor descriptor = resolver.resolve("retry-message.ftl", PhaseType.PROMPT_ASSEMBLY);

        assertEquals(ScriptSource.BUNDLED_DEFAULT, descriptor.getResolvedFrom());
        assertTrue(descriptor.getResolvedLocation().contains("classpath:reasoning/retry-message.ftl"));
    }
}
