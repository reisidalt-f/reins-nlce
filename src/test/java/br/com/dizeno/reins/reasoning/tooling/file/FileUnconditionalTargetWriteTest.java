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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

 
public class FileUnconditionalTargetWriteTest {

    @Test
    void testWriteFileAlwaysAllowedOnTargetWhenNotConfigured() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        assertTrue(allowed, "write_file on target should be allowed even when mcp.target is not configured");
    }

    @Test
    void testWriteFileAlwaysAllowedOnTargetWhenEmpty() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setTarget("");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        assertTrue(allowed, "write_file on target should be allowed even when mcp.target is blank");
    }

    @Test
    void testWriteFileAlwaysAllowedOnTargetWhenExplicitlyExcluded() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setTarget("list,read");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        assertTrue(allowed, "write_file on target should be allowed even when write is not in mcp.target config");
    }

    @Test
    void testWriteFileAlwaysAllowedOnTargetEvenWhenPatchDenied() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setTarget("read,list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        boolean writeAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        boolean patchAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        
        assertTrue(writeAllowed, "write_file on target should always be allowed");
        assertFalse(patchAllowed, "patch_file on target should be denied when not configured");
    }

    @Test
    void testOtherOperationsOnTargetRespectConfig() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setTarget("read");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        boolean readAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.READ_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        boolean listAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        boolean writeAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        boolean patchAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET);
        
        assertTrue(readAllowed, "read_file should be allowed when in config");
        assertFalse(listAllowed, "list_files should be denied when not in config");
        assertTrue(writeAllowed, "write_file should ALWAYS be allowed on target");
        assertFalse(patchAllowed, "patch_file should be denied when not in config");
    }

    @Test
    void testWriteFileIncludedInEnabledBasesForTarget() {
        
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setTarget("list");
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        var enabledBases = policy.getEnabledBasesForOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE);
        assertTrue(enabledBases.contains(br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET),
            "target should be in enabled bases for write_file even when write is not in config");
    }

    @Test
    void testFullConfigWithTargetWriteGuarantee() {
        McpFileBaseOpsSettings settings = new McpFileBaseOpsSettings();
        settings.setMain("read");
        settings.setTest("read,write");
        settings.setTarget("read");  
        br.com.dizeno.reins.reasoning.tooling.file.FilePolicy policy = new br.com.dizeno.reins.reasoning.tooling.file.FilePolicy(settings);
        
        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.MAIN),
            "write_file denied on main (not configured)");
        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TEST),
            "write_file allowed on test (explicitly configured)");
        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, br.com.dizeno.reins.reasoning.tooling.file.FilePolicy.Base.TARGET),
            "write_file allowed on target (unconditional guarantee)");
    }

}
