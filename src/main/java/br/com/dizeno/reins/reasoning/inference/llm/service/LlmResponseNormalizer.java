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

package br.com.dizeno.reins.reasoning.inference.llm.service;

import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponseExtensions;

import java.util.Map;

/**
 * LlmResponseNormalizer is part of the general application functions in the reins architecture.
 * Acts as a component managing llm response normalizer.
 */
public class LlmResponseNormalizer {
    /**
     * Normalize Success.
     *
     * @param requestId the request id
     * @param providerId the provider id
     * @param content the content
     * @param providerMetadata the provider metadata
     * @return the resolved or constructed object
     */
    public LlmResponse normalizeSuccess(String requestId,
                                        String providerId,
                                        String content,
                                        Map<String, Object> providerMetadata) {
        LlmResponse response = new LlmResponse();
        response.setRequestId(requestId);
        response.setStatus(LlmResponse.Status.SUCCESS);
        response.setContent(content == null ? "" : content);
        response.getMetadata().put("provider", providerId == null ? "" : providerId);
        if (providerMetadata != null && providerMetadata.get("model") != null) {
            response.getMetadata().put("model", String.valueOf(providerMetadata.get("model")));
        }

        if (providerMetadata != null && !providerMetadata.isEmpty()) {
            LlmResponseExtensions extensions = new LlmResponseExtensions();
            extensions.setProviderId(providerId);
            extensions.setPayload(providerMetadata);
            response.setExtensions(extensions);
        }
        return response;
    }
}
