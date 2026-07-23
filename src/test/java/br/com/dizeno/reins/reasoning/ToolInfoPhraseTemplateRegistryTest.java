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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolInfoPhraseTemplateRegistryTest {

    @Test
    void returnsDeterministicTemplateForSupportedOperations() {
        ToolInfoPhraseTemplateRegistry registry = new ToolInfoPhraseTemplateRegistry();

        assertEquals("List of files in <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_FILES));
        assertEquals("Read of file <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE));
        assertEquals("Write of file <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.WRITE_FILE));
        assertEquals("Patch of file <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.PATCH_FILE));
        assertEquals("Delete of file <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.DELETE_FILE));
        assertEquals("List of compiled files for <path> to <intention>",
                registry.templateFor(br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.LIST_COMPILED_FILES));
    }

    @Test
    void fallsBackToGenericTemplateForUnknownOperation() {
        ToolInfoPhraseTemplateRegistry registry = new ToolInfoPhraseTemplateRegistry();

        assertEquals("Tool operation on <path> to <intention>", registry.templateFor(null));
    }
}
