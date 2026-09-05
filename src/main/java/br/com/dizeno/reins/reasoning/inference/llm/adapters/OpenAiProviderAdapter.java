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
import br.com.dizeno.reins.reasoning.inference.llm.providers.openai.OpenAiClient;
import br.com.dizeno.reins.reasoning.inference.llm.providers.openai.OpenAiRequestParams;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.ProviderCapabilityProfile;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * OpenAiProviderAdapter is part of the general application functions in the reins architecture.
 * Acts as a adapter mapping external APIs to reins's internal interfaces.
 */
public class OpenAiProviderAdapter implements ProviderAdapter {
    private final OpenAiClient openaiClient;

    /**
     * Constructs a new instance of {@link OpenAiProviderAdapter}.
     */
    public OpenAiProviderAdapter() {
        this(new OpenAiClient());
    }

    /**
     * Constructs a new instance of {@link OpenAiProviderAdapter}.
     *
     * @param openaiClient the openai client
     */
    public OpenAiProviderAdapter(OpenAiClient openaiClient) {
        this.openaiClient = openaiClient;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        if (openaiClient != null) {
            openaiClient.setLog(log);
        }
    }

    /**
     * Provider Id.
     *
     * @return the string result
     */
    @Override
    public String providerId() {
        return "openai";
    }

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public AdapterValidationResult validateCompatibility(ReinsConfig config) {
        if (config == null || config.getOpenai() == null) {
            return AdapterValidationResult.failure(providerId(), "OpenAI configuration section is missing");
        }

        OpenAiSettings settings = config.getOpenai();

        if (settings.getModel() == null || settings.getModel().isBlank()) {
            return AdapterValidationResult.failure(providerId(), "OpenAI model is required");
        }

        if (settings.getTimeoutSeconds() <= 0) {
            return AdapterValidationResult.failure(providerId(), "OpenAI timeoutSeconds must be greater than 0");
        }

        if (settings.getRetryAttempts() < 0) {
            return AdapterValidationResult.failure(providerId(), "OpenAI retryAttempts must be >= 0");
        }

        String endpoint = settings.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            return AdapterValidationResult.failure(providerId(), "OpenAI endpoint cannot be blank");
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            return AdapterValidationResult.failure(providerId(), "OpenAI endpoint must start with http:// or https://");
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
        OpenAiSettings settings = config.getOpenai();
        OpenAiRequestParams params = new OpenAiRequestParams(
                settings.getEndpoint(),
                settings.getModel(),
                settings.getApiKey(),
                settings.getTimeoutSeconds(),
                settings.getRetryAttempts(),
                settings.getOptions()
        );

        List<ConversationMessage> history = request.getConversationHistory();
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("Conversation history is required for OpenAI provider invocation");
        }
        String raw = openaiClient.compileChat(params, history);

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
        throw new UnsupportedOperationException("OpenAI does not support cached content");
    }

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    @Override
     public void deleteCachedContent(ReinsConfig config, String cachedContentId) throws Exception {
        throw new UnsupportedOperationException("OpenAI does not support cached content");
    }
}
