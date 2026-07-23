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

package br.com.dizeno.reins.reasoning.tooling.handler;

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;

/**
 * ToolOperationHandler is part of the general application functions in the reins architecture.
 * Acts as a component managing tool operation handler.
 */
public interface ToolOperationHandler {
    /**
     * Supports.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    boolean supports(ToolExecutionRequest.Operation operation);

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    ToolExecutionResult execute(ToolExecutionRequest request,
                                BasePathResolver resolver,
                                String sourceScope,
                                ScriptRunnerConfig scriptRunnerConfig);
}
