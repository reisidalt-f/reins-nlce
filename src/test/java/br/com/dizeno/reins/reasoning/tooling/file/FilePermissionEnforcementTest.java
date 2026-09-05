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

package br.com.dizeno.reins.reasoning.tooling.file;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

 
public class FilePermissionEnforcementTest {

    private br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy;

    @BeforeEach
    void setUp() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("read,list,list_compiled");
        settings.setTest("read,write");
        settings.setTarget("list,read,list_compiled");
        policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
    }

    @Test
    void testReadOperationAllowedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.READ_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertTrue(allowed, "read_file should be allowed on main");
    }

    @Test
    void testListOperationAllowedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertTrue(allowed, "list_files should be allowed on main (via list token)");
    }

    @Test
    void testWriteOperationDeniedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertFalse(allowed, "write_file should NOT be allowed on main");
    }

    @Test
    void testPatchOperationDeniedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertFalse(allowed, "patch_file should NOT be allowed on main");
    }

    @Test
    void testDeleteOperationDeniedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.DELETE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertFalse(allowed, "delete_file should NOT be allowed on main");
    }

    @Test
    void testWriteOperationAllowedOnTest() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST);
        assertTrue(allowed, "write_file should be allowed on test");
    }

    @Test
    void testReadOperationAllowedOnTest() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.READ_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST);
        assertTrue(allowed, "read_file should be allowed on test");
    }

    @Test
    void testPatchOperationDeniedOnTest() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST);
        assertFalse(allowed, "patch_file should NOT be allowed on test");
    }

    @Test
    void testListCompiledFilesAllowedOnMain() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertTrue(allowed, "list_compiled_files should be allowed on main when list_compiled token is configured");
    }

    @Test
    void testListCompiledFilesDeniedOnTest() {
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST);
        assertFalse(allowed, "list_compiled_files should NOT be allowed on test (no list_compiled token)");
        }

        @Test
        void testListTokenDoesNotGrantListCompiledFiles() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy splitPolicy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        assertTrue(splitPolicy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN),
            "list_files should be allowed when list token is configured");
        assertFalse(splitPolicy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN),
            "list_compiled_files should be denied when list_compiled token is missing");
        }

        @Test
        void testListCompiledTokenDoesNotGrantListFiles() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("list_compiled");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy splitPolicy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);

        assertFalse(splitPolicy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN),
            "list_files should be denied when list token is missing");
        assertTrue(splitPolicy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_COMPILED_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN),
            "list_compiled_files should be allowed when list_compiled token is configured");
    }

    @Test
    void testOperationDeniedOnBaseWithoutPermission() {
        
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN);
        assertFalse(allowed, "write_file on main should be denied when write is not in config");
    }

    @Test
    void testGetEnabledBasesForOperation() {
        var bases = policy.getEnabledBasesForOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.READ_FILE);
        assertEquals(3, bases.size(), "read_file should be enabled on main, test, and target");
        assertTrue(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN));
        assertTrue(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST));
        assertTrue(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
    }

    @Test
    void testGetEnabledBasesForWriteOperation() {
        var bases = policy.getEnabledBasesForOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE);
        assertEquals(1, bases.size(), "write_file should only be enabled on test (target has no write token in this config)");
        assertTrue(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST));
        assertFalse(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET));
        assertFalse(bases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN));
    }

    @Test
    void testGetEnabledBasesForPatchOperation() {
        var bases = policy.getEnabledBasesForOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE);
        assertTrue(bases.isEmpty(), "patch_file should not be enabled on any base in this config");
    }

    @Test
    void testPolicyToString() {
        String str = policy.toString();
        assertNotNull(str);
        assertTrue(str.contains("br.com.dizeno.reins.reasoning.tooling.file.FilePolicy"), "toString should mention policy class");
    }

}
