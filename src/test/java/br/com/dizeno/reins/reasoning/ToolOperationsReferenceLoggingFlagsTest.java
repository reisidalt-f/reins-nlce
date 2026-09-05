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

        assertTrue(reference.contains("LIST_FILES"));
        assertTrue(reference.contains("READ_FILE"));
        assertTrue(reference.contains("WRITE_FILE"));
        assertTrue(reference.contains("PATCH_FILE"));
        assertTrue(reference.contains("DELETE_FILE"));
        assertTrue(reference.contains("LIST_COMPILED_FILES"));
        assertTrue(reference.contains("RUN_SCRIPT"));
    }

    private static FilePolicy policyWithAllBasesEnabled() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("list,read,write,patch,delete,list_compiled");
        settings.setTest("list,read,write,patch,delete,list_compiled");
        settings.setTarget("list,read,write,patch,delete,list_compiled");
        return new FilePolicy(settings);
    }
}
