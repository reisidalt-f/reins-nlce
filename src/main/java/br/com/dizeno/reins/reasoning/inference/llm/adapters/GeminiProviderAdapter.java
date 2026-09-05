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
import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiClient;
import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiRequestParams;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.ProviderCapabilityProfile;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import org.apache.maven.plugin.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GeminiProviderAdapter is part of the general application functions in the reins architecture.
 * Acts as a adapter mapping external APIs to reins's internal interfaces.
 */
public class GeminiProviderAdapter implements ProviderAdapter {
    private final GeminiClient geminiClient;

    /**
     * Constructs a new instance of {@link GeminiProviderAdapter}.
     */
    public GeminiProviderAdapter() {
        this(new GeminiClient());
    }

    /**
     * Constructs a new instance of {@link GeminiProviderAdapter}.
     *
     * @param geminiClient the gemini client
     */
    public GeminiProviderAdapter(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        this.geminiClient.setLog(log);
    }

    /**
     * Provider Id.
     *
     * @return the string result
     */
    @Override
    public String providerId() {
        return "gemini";
    }

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public AdapterValidationResult validateCompatibility(ReinsConfig config) {
        if (config == null || config.getGemini() == null) {
            return AdapterValidationResult.failure(providerId(), "Gemini configuration section is missing");
        }
        if (config.getGemini().getApiKey() == null || config.getGemini().getApiKey().isBlank()) {
            return AdapterValidationResult.failure(providerId(), "Gemini apiKey is required");
        }
        if (config.getGemini().getModel() == null || config.getGemini().getModel().isBlank()) {
            return AdapterValidationResult.failure(providerId(), "Gemini model is required");
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
    public LlmResponse invoke(LlmRequest request, ReinsConfig config) throws Exception {
        GeminiRequestParams params = buildParams(config);
        List<ConversationMessage> history = request.getConversationHistory();
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("Conversation history is required for Gemini provider invocation");
        }
        String raw = geminiClient.compileWithHistory(
                params,
                history,
                request.getCachedContentId(),
                request.isUseCachedContent());

        LlmResponse response = new LlmResponse();
        response.setRequestId(request.getRequestId());
        response.setStatus(LlmResponse.Status.SUCCESS);
        response.setContent(raw);
        response.getMetadata().put("provider", providerId());
        response.getMetadata().put("model", config.getGemini().getModel());
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
        return geminiClient.createCachedContent(buildParams(config), systemMessages);
    }

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    @Override
    public void deleteCachedContent(ReinsConfig config, String cachedContentId) throws Exception {
        geminiClient.deleteCachedContent(buildParams(config), cachedContentId);
    }

    private GeminiRequestParams buildParams(ReinsConfig config) {
        return new GeminiRequestParams(
                config.getGemini().getEndpoint(),
                config.getGemini().getApiKey(),
                config.getGemini().getModel(),
                config.getGemini().getTimeoutSeconds(),
                config.getGemini().getRetryAttempts(),
                config.getGemini().getEmptyResponseRetryDelayMs(),
                config.isVerbose(),
                config.getGemini().getGeneration(),
                config.getContext());
    }

}
