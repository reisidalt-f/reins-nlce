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

package br.com.dizeno.reins.reasoning.inference.llm.registry;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmServiceException;

/**
 * LlmProviderResolver is part of the general application functions in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class LlmProviderResolver {
    private String resolvedProvider;

    /**
     * Resolves the configured value or path.
     *
     * @param config the Reins configuration settings
     * @return the string result
     */
    public synchronized String resolve(ReinsConfig config) throws LlmServiceException {
        if (config == null || config.getProvider() == null || config.getProvider().isBlank()) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setRetryable(false);
            error.setMessage("Provider is not configured");
            throw new LlmServiceException(error);
        }
        String configured = config.getProvider().trim().toLowerCase();
        if (resolvedProvider == null) {
            resolvedProvider = configured;
            return resolvedProvider;
        }
        if (!resolvedProvider.equals(configured)) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setRetryable(false);
            error.setMessage("Provider changed during execution run: " + resolvedProvider + " -> " + configured);
            error.setProviderId(resolvedProvider);
            throw new LlmServiceException(error);
        }
        return resolvedProvider;
    }
}
