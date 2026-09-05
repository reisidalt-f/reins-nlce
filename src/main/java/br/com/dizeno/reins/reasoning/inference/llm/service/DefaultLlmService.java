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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmErrorMapper;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmServiceException;
import br.com.dizeno.reins.reasoning.inference.llm.logging.LlmLifecycleLogger;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.registry.DefaultAdapterRegistry;
import br.com.dizeno.reins.reasoning.inference.llm.registry.LlmProviderResolver;
import br.com.dizeno.reins.reasoning.inference.llm.spi.LlmService;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import org.apache.maven.plugin.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultLlmService is part of the general application functions in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class DefaultLlmService implements LlmService {
    private final DefaultAdapterRegistry adapterRegistry;
    private final LlmProviderResolver providerResolver;
    private final LlmCapabilityGuard capabilityGuard;
    private final LlmResponseNormalizer responseNormalizer;
    private final LlmErrorMapper errorMapper;
    private final LlmLifecycleLogger lifecycleLogger;

    /**
     * Constructs a new instance of {@link DefaultLlmService}.
     */
    public DefaultLlmService() {
        this(new DefaultAdapterRegistry(), new LlmProviderResolver(), new LlmCapabilityGuard(),
                new LlmResponseNormalizer(), new LlmErrorMapper(), new LlmLifecycleLogger());
    }

    /**
     * Constructs a new instance of {@link DefaultLlmService}.
     *
     * @param adapterRegistry the adapter registry
     * @param providerResolver the provider resolver
     * @param capabilityGuard the capability guard
     * @param responseNormalizer the response normalizer
     * @param errorMapper the error mapper
     * @param lifecycleLogger the lifecycle logger
     */
    public DefaultLlmService(DefaultAdapterRegistry adapterRegistry,
                             LlmProviderResolver providerResolver,
                             LlmCapabilityGuard capabilityGuard,
                             LlmResponseNormalizer responseNormalizer,
                             LlmErrorMapper errorMapper,
                             LlmLifecycleLogger lifecycleLogger) {
        this.adapterRegistry = adapterRegistry;
        this.providerResolver = providerResolver;
        this.capabilityGuard = capabilityGuard;
        this.responseNormalizer = responseNormalizer;
        this.errorMapper = errorMapper;
        this.lifecycleLogger = lifecycleLogger;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    @Override
    public void setLog(Log log) {
        this.adapterRegistry.setLog(log);
        this.lifecycleLogger.setLog(log);
    }

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     */
    @Override
    public void validateCompatibility(ReinsConfig config) {
        try {
            adapterRegistry.validateConfiguredAdapters(config);
            providerResolver.resolve(config);
        } catch (LlmServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
        String providerId = providerResolver.resolve(config);
        ProviderAdapter adapter = adapterRegistry.get(providerId);
        if (adapter == null) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setProviderId(providerId);
            error.setRetryable(false);
            error.setMessage("No adapter registered for provider: " + providerId);
            throw new LlmServiceException(error);
        }

        boolean printProvider = config.getLogging() != null && config.getLogging().isLlmProvider();
        lifecycleLogger.logSelection(providerId, printProvider);
        lifecycleLogger.logStart(request.getRequestId(), providerId);
        try {
            capabilityGuard.validate(request, adapter.capabilities());
            LlmResponse adapterResponse = adapter.invoke(request, config);
            Map<String, Object> providerMetadata = new HashMap<>();
            providerMetadata.put("provider", providerId);
            if (adapterResponse.getMetadata().containsKey("model")) {
                providerMetadata.put("model", adapterResponse.getMetadata().get("model"));
            }
            LlmResponse normalized = responseNormalizer.normalizeSuccess(
                    request.getRequestId(),
                    providerId,
                    adapterResponse.getContent(),
                    providerMetadata);
            lifecycleLogger.logSuccess(request.getRequestId(), providerId);
            br.com.dizeno.reins.reasoning.inference.llm.logging.ModelRequestResponseLogger.log(
                    request, normalized.getContent(), config);
            return normalized;
        } catch (Throwable throwable) {
            LlmError error = errorMapper.map(throwable, providerId);
            lifecycleLogger.logFailure(request.getRequestId(), providerId, error);
            throw new LlmServiceException(error);
        }
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
        String providerId = providerResolver.resolve(config);
        ProviderAdapter adapter = adapterRegistry.get(providerId);
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for provider: " + providerId);
        }
        return adapter.createCachedContent(config, systemMessages);
    }

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    @Override
    public void deleteCachedContent(ReinsConfig config, String cachedContentId) throws Exception {
        String providerId = providerResolver.resolve(config);
        ProviderAdapter adapter = adapterRegistry.get(providerId);
        if (adapter == null) {
            throw new IllegalStateException("No adapter registered for provider: " + providerId);
        }
        adapter.deleteCachedContent(config, cachedContentId);
    }
}
