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

import br.com.dizeno.reins.run.config.*;

/**
 * ReasoningService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface ReasoningService {
    /**
     * Runs the execution cycle cycle.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    ReasoningResult runCycle(ReasoningRequest request, ReinsConfig config) throws Exception;
}
