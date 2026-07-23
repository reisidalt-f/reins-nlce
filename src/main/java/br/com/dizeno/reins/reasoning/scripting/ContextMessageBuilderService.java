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

/**
 * ContextMessageBuilderService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface ContextMessageBuilderService {
    /**
     * Builds the configured target.
     *
     * @param request the request containing path and scope metadata
     * @param stepScriptExecutor the step script executor
     * @return the resolved or constructed object
     */
    ContextMessageBundle build(ContextMessageBuildRequest request,
                               StepScriptExecutor stepScriptExecutor) throws Exception;

    /**
     * StepScriptExecutor is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a component managing step script executor.
     */
    @FunctionalInterface
    interface StepScriptExecutor {
        /**
         * Executes the operation.
         *
         * @param step the step
         * @return the string result
         */
        String execute(String step) throws Exception;
    }
}
