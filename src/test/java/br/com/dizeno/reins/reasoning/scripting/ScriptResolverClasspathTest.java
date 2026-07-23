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

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptResolverClasspathTest {

    @Test
    void resolvesCustomClasspathScriptBeforeBundledDefault() throws IOException {
        ScriptResolver resolver = new ScriptResolver(null, "custom-scripts");
        ScriptDescriptor descriptor = resolver.resolve("custom-classpath-only.ftl", PhaseType.PROMPT_ASSEMBLY);

        assertEquals(ScriptSource.CUSTOM_CLASSPATH, descriptor.getResolvedFrom());
        assertEquals("custom-scripts/custom-classpath-only.ftl", descriptor.getResolvedLocation());
    }

    @Test
    void fallsBackToBundledDefaultWhenScriptIsNotInCustomClasspath() throws IOException {
        ScriptResolver resolver = new ScriptResolver(null, "custom-scripts");
        ScriptDescriptor descriptor = resolver.resolve("tool-result.ftl", PhaseType.TOOL_RESPONSE);

        assertEquals(ScriptSource.BUNDLED_DEFAULT, descriptor.getResolvedFrom());
        assertTrue(descriptor.getResolvedLocation().contains("classpath:reasoning/tool-result.ftl"));
    }
}
