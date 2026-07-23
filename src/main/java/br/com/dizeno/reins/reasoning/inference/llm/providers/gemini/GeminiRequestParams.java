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

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;

 
/**
 * GeminiRequestParams is part of the general application functions in the reins architecture.
 * Acts as a component managing gemini request params.
 */
public record GeminiRequestParams(
        String endpoint,
        String apiKey,
        String model,
        int timeoutSeconds,
        int retryAttempts,
        int emptyResponseRetryDelayMs,
        boolean verbose,
        GenerationSettings generation
) {}
