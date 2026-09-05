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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;

import java.util.EnumMap;
import java.util.Map;

/**
 * ToolInfoPhraseTemplateRegistry is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing tool info phrase template registry.
 */
public class ToolInfoPhraseTemplateRegistry {
    private static final String UNKNOWN_TEMPLATE = "Tool operation on <path> to <intention>";

    private final Map<ToolExecutionRequest.Operation, String> templates =
            new EnumMap<>(ToolExecutionRequest.Operation.class);

    /**
     * Constructs a new instance of {@link ToolInfoPhraseTemplateRegistry}.
     */
    public ToolInfoPhraseTemplateRegistry() {
        templates.put(ToolExecutionRequest.Operation.LIST_FILES, "List of files in <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.READ_FILE, "Read of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.WRITE_FILE, "Write of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.PATCH_FILE, "Patch of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.DELETE_FILE, "Delete of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.APPEND_FILE, "Append to file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.PREPEND_FILE, "Prepend to file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.MOVE_FILE, "Move of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.COPY_FILE, "Copy of file <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.LIST_COMPILED_FILES,
                "List of compiled files for <path> to <intention>");
        templates.put(ToolExecutionRequest.Operation.RUN_SCRIPT, "Run of script <path> to <intention>");
    }

    /**
     * Template For.
     *
     * @param operation the operation
     * @return the string result
     */
    public String templateFor(ToolExecutionRequest.Operation operation) {
        if (operation == null) {
            return UNKNOWN_TEMPLATE;
        }
        return templates.getOrDefault(operation, UNKNOWN_TEMPLATE);
    }
}