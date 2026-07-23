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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReasoningScriptContextFactoryToolReferenceTest {

    @Test
    void derivesOperationGuidanceFromToolPolicyWhenLoggingFlagsAreUnset() {
        ReasoningScriptContextFactory factory = new ReasoningScriptContextFactory();
        ReinsConfig config = new ReinsConfig();
        config.getTooling().setAddReasoningNotes(true);
        FilePolicy policy = policyWithConfiguredOperations();

        ReasoningScriptContext context = factory.buildBase(
                null,
                config,
                null,
                policy,
                true,
                "",
                List.of(),
                List.of(),
                List.of(),
                false);

        String reference = context.getPolicy().getToolOpsReference();
        assertTrue(reference.contains("- **list_files**:"));
        assertTrue(reference.contains("- **read_file**:"));
        assertTrue(reference.contains("- **write_file**:"));
        assertTrue(reference.contains("- **delete_file**:"));
        assertTrue(reference.contains("- **list_compiled_files**:"));
        assertTrue(reference.contains("- **run_script**:"));
        assertFalse(reference.contains("- **patch_file**:"));
        assertTrue(context.getConfig().isAddReasoningNotes());
    }

    private static FilePolicy policyWithConfiguredOperations() {
        ToolingSettings settings = new ToolingSettings();
        settings.setMain("list_compiled");
        settings.setTest("list_compiled");
        settings.setTarget("list,read,write,delete");
        return new FilePolicy(settings);
    }
}
