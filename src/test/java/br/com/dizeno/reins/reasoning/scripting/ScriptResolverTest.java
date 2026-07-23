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

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsEvalBuiltinInTemplate() throws IOException {
        Path script = tempDir.resolve("eval-denied.ftl");
        Files.writeString(script, "${\"1+1\"?eval}");

        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), null);
        IOException error = assertThrows(
                IOException.class,
                () -> resolver.resolve("eval-denied.ftl", PhaseType.PROMPT_ASSEMBLY));
        assertTrue(error.getMessage().contains("Forbidden built-in (?eval or ?interpret)"));
    }

    @Test
    void rejectsInterpretBuiltinInTemplate() throws IOException {
        Path script = tempDir.resolve("interpret-denied.ftl");
        Files.writeString(script, "${\"x\"?interpret}");

        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), null);
        IOException error = assertThrows(
                IOException.class,
                () -> resolver.resolve("interpret-denied.ftl", PhaseType.PROMPT_ASSEMBLY));
        assertTrue(error.getMessage().contains("Forbidden built-in (?eval or ?interpret)"));
    }

    @Test
    void rejectsNewBuiltinInTemplate() throws IOException {
        Path script = tempDir.resolve("new-denied.ftl");
        Files.writeString(script, "${\"java.lang.String\"?new()}");

        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), null);
        Template template = resolver.resolve("new-denied.ftl", PhaseType.PROMPT_ASSEMBLY).getTemplate();

        assertThrows(TemplateException.class, () -> template.process(Map.of("project", "x"), new StringWriter()));
    }

    @Test
    void resolvesSourceOrderAsFilesystemThenClasspathThenBundled() throws IOException {
        Path script = tempDir.resolve("system-context.ftl");
        Files.writeString(script, "custom-system");

        ScriptResolver resolver = new ScriptResolver(tempDir.toFile(), "custom-scripts");
        List<ScriptSource> order = resolver.resolveSourceOrder("system-context.ftl");

        assertEquals(List.of(
                ScriptSource.CUSTOM_FILESYSTEM,
                ScriptSource.CUSTOM_CLASSPATH,
                ScriptSource.BUNDLED_DEFAULT), order);
    }
}
