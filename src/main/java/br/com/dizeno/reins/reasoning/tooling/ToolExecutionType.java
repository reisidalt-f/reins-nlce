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

package br.com.dizeno.reins.reasoning.tooling;

import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;

import java.util.HashMap;
import java.util.Map;

 
/**
 * ToolExecutionType is part of the tool execution environments (like tools and local file tools) exposed to LLMs in the reins architecture.
 * Acts as a component managing tool execution type.
 */
public enum ToolExecutionType {
    LIST_FILES(ToolExecutionRequest.Operation.LIST_FILES, FilePolicy.OperationToken.LIST),
    LIST_COMPILED_FILES(ToolExecutionRequest.Operation.LIST_COMPILED_FILES, FilePolicy.OperationToken.LIST_COMPILED),
    READ_FILE(ToolExecutionRequest.Operation.READ_FILE, FilePolicy.OperationToken.READ),
    WRITE_FILE(ToolExecutionRequest.Operation.WRITE_FILE, FilePolicy.OperationToken.WRITE),
    PATCH_FILE(ToolExecutionRequest.Operation.PATCH_FILE, FilePolicy.OperationToken.PATCH),
    DELETE_FILE(ToolExecutionRequest.Operation.DELETE_FILE, FilePolicy.OperationToken.DELETE),
    APPEND_FILE(ToolExecutionRequest.Operation.APPEND_FILE, FilePolicy.OperationToken.APPEND),
    PREPEND_FILE(ToolExecutionRequest.Operation.PREPEND_FILE, FilePolicy.OperationToken.PREPEND),
    MOVE_FILE(ToolExecutionRequest.Operation.MOVE_FILE, FilePolicy.OperationToken.MOVE),
    COPY_FILE(ToolExecutionRequest.Operation.COPY_FILE, FilePolicy.OperationToken.COPY),
    RUN_SCRIPT(ToolExecutionRequest.Operation.RUN_SCRIPT, null),
    ADD_REASONING_NOTE(ToolExecutionRequest.Operation.ADD_REASONING_NOTE, FilePolicy.OperationToken.WRITE),
    CLEAR_REASONING_NOTES(ToolExecutionRequest.Operation.CLEAR_REASONING_NOTES, FilePolicy.OperationToken.WRITE);

    private final ToolExecutionRequest.Operation operation;
    private final FilePolicy.OperationToken requiredToken;

    ToolExecutionType(ToolExecutionRequest.Operation operation, FilePolicy.OperationToken requiredToken) {
        this.operation = operation;
        this.requiredToken = requiredToken;
    }

    public ToolExecutionRequest.Operation getOperation() {
        return operation;
    }

    public FilePolicy.OperationToken getRequiredToken() {
        return requiredToken;
    }

    private static final Map<ToolExecutionRequest.Operation, ToolExecutionType> BY_OPERATION = new HashMap<>();

    static {
        for (ToolExecutionType type : ToolExecutionType.values()) {
            BY_OPERATION.put(type.operation, type);
        }
    }

    /**
     * From Operation.
     *
     * @param operation the operation
     * @return the resolved or constructed object
     */
    public static ToolExecutionType fromOperation(ToolExecutionRequest.Operation operation) {
        return BY_OPERATION.get(operation);
    }
}
