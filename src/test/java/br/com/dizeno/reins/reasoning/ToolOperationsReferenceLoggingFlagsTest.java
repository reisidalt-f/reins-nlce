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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolOperationsReferenceLoggingFlagsTest {

    @Test
    void hidesConfiguredOperationSectionsWhenFlagsAreDisabled() {
        FilePolicy policy = policyWithAllBasesEnabled();

        String reference = ToolOperationsReference.build(policy, true, false, false, false);

        assertFalse(reference.contains("- **list_files**:"));
        assertFalse(reference.contains("- **read_file**:"));
        assertFalse(reference.contains("- **write_file**:"));
        assertFalse(reference.contains("- **patch_file**:"));
        assertFalse(reference.contains("- **delete_file**:"));
        assertFalse(reference.contains("- **list_compiled_files**:"));
        assertFalse(reference.contains("- **run_script**:"));
    }

    @Test
    void showsConfiguredOperationSectionsWhenFlagsAreEnabled() {
        FilePolicy policy = policyWithAllBasesEnabled();

        String reference = ToolOperationsReference.build(policy, true, true, true, true);

        assertTrue(reference.contains("- **list_files**:"));
        assertTrue(reference.contains("- **read_file**:"));
        assertTrue(reference.contains("- **write_file**:"));
        assertTrue(reference.contains("- **patch_file**:"));
        assertTrue(reference.contains("- **delete_file**:"));
        assertTrue(reference.contains("- **list_compiled_files**:"));
        assertTrue(reference.contains("- **run_script**:"));
        assertTrue(reference.contains("operation: list_files | base: target | path: com/example | recursive: true"));
        assertTrue(reference.contains("operation: write_file | base: target | path: compiled/NewType.java"));
        assertTrue(reference.contains("operation: patch_file | base: target | path: domain/Task.java"));
        assertFalse(reference.contains("base: target | path: main/java/"));
    }

    private static FilePolicy policyWithAllBasesEnabled() {
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setMain("list,read,write,patch,delete,list_compiled");
        settings.setTest("list,read,write,patch,delete,list_compiled");
        settings.setTarget("list,read,write,patch,delete,list_compiled");
        return new FilePolicy(settings);
    }
}
