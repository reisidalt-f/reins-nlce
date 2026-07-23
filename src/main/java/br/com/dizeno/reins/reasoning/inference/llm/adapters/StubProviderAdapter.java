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

package br.com.dizeno.reins.reasoning.inference.llm.adapters;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.ProviderCapabilityProfile;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.util.List;

/**
 * StubProviderAdapter is part of the general application functions in the reins architecture.
 * Acts as a adapter mapping external APIs to reins's internal interfaces.
 */
public class StubProviderAdapter implements ProviderAdapter {
    /**
     * Provider Id.
     *
     * @return the string result
     */
    @Override
    public String providerId() {
        return "stub";
    }

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public AdapterValidationResult validateCompatibility(ReinsConfig config) {
        return AdapterValidationResult.success(providerId());
    }

    /**
     * Capabilities.
     *
     * @return the resolved or constructed object
     */
    @Override
    public ProviderCapabilityProfile capabilities() {
        ProviderCapabilityProfile profile = new ProviderCapabilityProfile();
        profile.setSupportsTemperature(false);
        profile.setSupportsStructuredExtensions(true);
        return profile;
    }

    /**
     * Invoke.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @return the resolved or constructed object
     */
    @Override
    public LlmResponse invoke(LlmRequest request, ReinsConfig config) {
        LlmResponse response = new LlmResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus(LlmResponse.Status.SUCCESS);
        response.setContent("stub provider response");
        response.getMetadata().put("provider", providerId());
        return response;
    }

    /**
     * Creates a new resource cached content.
     *
     * @param config the Reins configuration settings
     * @param systemMessages the system messages
     * @return the string result
     */
    @Override
    public String createCachedContent(ReinsConfig config, List<ConversationMessage> systemMessages) {
        return "stub-cache-id";
    }

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    @Override
    public void deleteCachedContent(ReinsConfig config, String cachedContentId) {
        
    }
}
