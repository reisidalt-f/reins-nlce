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
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.llm.providers.ollama.OllamaClient;
import br.com.dizeno.reins.reasoning.inference.llm.providers.ollama.OllamaRequestParams;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.ProviderCapabilityProfile;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

 
/**
 * OllamaProviderAdapter is part of the general application functions in the reins architecture.
 * Acts as a adapter mapping external APIs to reins's internal interfaces.
 */
public class OllamaProviderAdapter implements ProviderAdapter {
    private final OllamaClient ollamaClient;

    /**
     * Constructs a new instance of {@link OllamaProviderAdapter}.
     */
    public OllamaProviderAdapter() {
        this(new OllamaClient());
    }

    /**
     * Constructs a new instance of {@link OllamaProviderAdapter}.
     *
     * @param ollamaClient the ollama client
     */
    public OllamaProviderAdapter(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        if (ollamaClient != null) {
            ollamaClient.setLog(log);
        }
    }

    /**
     * Provider Id.
     *
     * @return the string result
     */
    @Override
    public String providerId() {
        return "ollama";
    }

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public AdapterValidationResult validateCompatibility(ReinsConfig config) {
        if (config == null || config.getOllama() == null) {
            return AdapterValidationResult.failure(providerId(), "Ollama configuration section is missing");
        }

        OllamaSettings settings = config.getOllama();

        if (settings.getModel() == null || settings.getModel().isBlank()) {
            return AdapterValidationResult.failure(providerId(), "Ollama model is required");
        }

        if (settings.getTimeoutSeconds() <= 0) {
            return AdapterValidationResult.failure(providerId(), "Ollama timeoutSeconds must be greater than 0");
        }

        if (settings.getRetryAttempts() < 0) {
            return AdapterValidationResult.failure(providerId(), "Ollama retryAttempts must be >= 0");
        }

        
        String endpoint = settings.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return AdapterValidationResult.failure(providerId(), "Ollama endpoint cannot be blank");
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            return AdapterValidationResult.failure(providerId(), "Ollama endpoint must start with http:// or https://");
        }

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
        profile.setSupportsTemperature(true);
        profile.setSupportsStructuredExtensions(false);
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
    public LlmResponse invoke(LlmRequest request, ReinsConfig config) throws Exception {
        OllamaSettings settings = config.getOllama();
        OllamaRequestParams params = new OllamaRequestParams(
                settings.getEndpoint(),
                settings.getModel(),
                settings.getApiKey(),
                settings.getTimeoutSeconds(),
                settings.getRetryAttempts(),
                settings.getOptions()
        );

        String raw;
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            raw = ollamaClient.compileChat(params, request.getConversationHistory());
        } else {
            raw = ollamaClient.compile(params, request.getMarkdownContent());
        }

        LlmResponse response = new LlmResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus(LlmResponse.Status.SUCCESS);
        response.setContent(raw);
        response.getMetadata().put("provider", providerId());
        response.getMetadata().put("model", settings.getModel());
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
    public String createCachedContent(ReinsConfig config, List<ConversationMessage> systemMessages) throws Exception {
        throw new UnsupportedOperationException("Ollama does not support cached content");
    }

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    @Override
    public void deleteCachedContent(ReinsConfig config, String cachedContentId) throws Exception {
        throw new UnsupportedOperationException("Ollama does not support cached content");
    }
}
