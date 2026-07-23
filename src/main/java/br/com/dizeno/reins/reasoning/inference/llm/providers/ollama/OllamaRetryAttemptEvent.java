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

package br.com.dizeno.reins.reasoning.inference.llm.providers.ollama;

 
/**
 * OllamaRetryAttemptEvent is part of the general application functions in the reins architecture.
 * Acts as a component managing ollama retry attempt event.
 */
record OllamaRetryAttemptEvent(int attempt,
                               int maxRetryAttempts,
                               Exception cause,
                               String responseClass,
                               boolean retryScheduled,
                               int delayMs) {
}
