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

/**
 * Verifies that write_file on target respects the configured token set.
 * write is no longer unconditionally granted on target; it must be explicitly
 * listed in the tooling.target configuration.
 */
public class FileUnconditionalTargetWriteTest {

    @Test
    void testWriteFileDeniedOnTargetWhenNotConfigured() {
        FileToolsSettings settings = new FileToolsSettings();
        FilePolicy policy = new FilePolicy(settings);

        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        assertFalse(allowed, "write_file on target should be denied when fileTools.target is not configured");
    }

    @Test
    void testWriteFileDeniedOnTargetWhenEmpty() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("");
        FilePolicy policy = new FilePolicy(settings);

        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        assertFalse(allowed, "write_file on target should be denied when fileTools.target is blank");
    }

    @Test
    void testWriteFileDeniedOnTargetWhenExplicitlyExcluded() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("list,read");
        FilePolicy policy = new FilePolicy(settings);

        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        assertFalse(allowed, "write_file on target should be denied when write is not in fileTools.target config");
    }

    @Test
    void testWriteFileDeniedButPatchAllowedOnTarget() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("read,patch");
        FilePolicy policy = new FilePolicy(settings);

        boolean writeAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        boolean patchAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, FilePolicy.Base.TARGET);

        assertFalse(writeAllowed, "write_file on target should be denied when write is not in config");
        assertTrue(patchAllowed, "patch_file on target should be allowed when patch is in config");
    }

    @Test
    void testOtherOperationsOnTargetRespectConfig() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("read");
        FilePolicy policy = new FilePolicy(settings);

        boolean readAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.READ_FILE, FilePolicy.Base.TARGET);
        boolean listAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.LIST_FILES, FilePolicy.Base.TARGET);
        boolean writeAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        boolean patchAllowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.PATCH_FILE, FilePolicy.Base.TARGET);

        assertTrue(readAllowed, "read_file should be allowed when in config");
        assertFalse(listAllowed, "list_files should be denied when not in config");
        assertFalse(writeAllowed, "write_file should be denied when not in config");
        assertFalse(patchAllowed, "patch_file should be denied when not in config");
    }

    @Test
    void testWriteFileNotInEnabledBasesForTargetWhenNotConfigured() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("list");
        FilePolicy policy = new FilePolicy(settings);

        var enabledBases = policy.getEnabledBasesForOperation(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE);
        assertFalse(enabledBases.contains(FilePolicy.Base.TARGET),
            "target should NOT be in enabled bases for write_file when write is not in config");
    }

    @Test
    void testWriteFileAllowedOnTargetWhenExplicitlyConfigured() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setTarget("list,read,write");
        FilePolicy policy = new FilePolicy(settings);

        boolean allowed = policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET);
        assertTrue(allowed, "write_file on target should be allowed when write is explicitly in config");
    }

    @Test
    void testFullConfigWriteRespectedPerBase() {
        FileToolsSettings settings = new FileToolsSettings();
        settings.setMain("read");
        settings.setTest("read,write");
        settings.setTarget("read");
        FilePolicy policy = new FilePolicy(settings);

        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.MAIN),
            "write_file denied on main (not configured)");
        assertTrue(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TEST),
            "write_file allowed on test (explicitly configured)");
        assertFalse(policy.isOperationAllowed(br.com.dizeno.reins.reasoning.tooling.ToolExecutionType.WRITE_FILE, FilePolicy.Base.TARGET),
            "write_file denied on target (write not in config, only read)");
    }

}
