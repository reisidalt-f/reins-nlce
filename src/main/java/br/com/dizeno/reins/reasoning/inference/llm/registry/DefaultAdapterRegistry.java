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
import br.com.dizeno.reins.reasoning.inference.llm.adapters.GeminiProviderAdapter;
import br.com.dizeno.reins.reasoning.inference.llm.adapters.OllamaProviderAdapter;
import br.com.dizeno.reins.reasoning.inference.llm.adapters.OpenAiProviderAdapter;
import br.com.dizeno.reins.reasoning.inference.llm.adapters.StubProviderAdapter;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmServiceException;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.reasoning.inference.llm.spi.ProviderAdapter;
import org.apache.maven.plugin.logging.Log;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * DefaultAdapterRegistry is part of the general application functions in the reins architecture.
 * Acts as a component managing default adapter registry.
 */
public class DefaultAdapterRegistry {
    private final Map<String, ProviderAdapter> adapters = new LinkedHashMap<>();
    private Log log;

    /**
     * Constructs a new instance of {@link DefaultAdapterRegistry}.
     */
    public DefaultAdapterRegistry() {
        register(new GeminiProviderAdapter());
        register(new OllamaProviderAdapter());
        register(new OpenAiProviderAdapter());
        register(new StubProviderAdapter());
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        this.log = log;
        ProviderAdapter gemini = adapters.get("gemini");
        if (gemini instanceof GeminiProviderAdapter geminiProviderAdapter) {
            geminiProviderAdapter.setLog(log);
        }
        ProviderAdapter ollama = adapters.get("ollama");
        if (ollama instanceof OllamaProviderAdapter ollamaProviderAdapter) {
            ollamaProviderAdapter.setLog(log);
        }
        ProviderAdapter openai = adapters.get("openai");
        if (openai instanceof OpenAiProviderAdapter openAiProviderAdapter) {
            openAiProviderAdapter.setLog(log);
        }
    }

    /**
     * Register.
     *
     * @param adapter the adapter
     */
    public void register(ProviderAdapter adapter) {
        if (adapter == null || adapter.providerId() == null) {
            return;
        }
        adapters.put(adapter.providerId().trim().toLowerCase(Locale.ROOT), adapter);
    }

    /**
     * Get.
     *
     * @param providerId the provider id
     * @return the resolved or constructed object
     */
    public ProviderAdapter get(String providerId) {
        if (providerId == null) {
            return null;
        }
        return adapters.get(providerId.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * All.
     *
     * @return the collection of elements
     */
    public Collection<ProviderAdapter> all() {
        return adapters.values();
    }

    /**
     * Provider Ids.
     *
     * @return the string result
     */
    public Set<String> providerIds() {
        return adapters.keySet();
    }

    /**
     * Validates the inputs or files configured adapters.
     *
     * @param config the Reins configuration settings
     */
    public void validateConfiguredAdapters(ReinsConfig config) throws LlmServiceException {
        if (config == null || config.getProvider() == null || config.getProvider().isBlank()) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setRetryable(false);
            error.setMessage("Provider is not configured");
            throw new LlmServiceException(error);
        }
        String selectedProvider = config.getProvider().trim().toLowerCase(Locale.ROOT);
        ProviderAdapter selected = get(selectedProvider);
        if (selected == null) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setRetryable(false);
            error.setProviderId(selectedProvider);
            error.setMessage("Unknown provider: " + selectedProvider + ". Supported providers: " + String.join(",", providerIds()));
            throw new LlmServiceException(error);
        }
        AdapterValidationResult result = selected.validateCompatibility(config);
        if (!result.isValid()) {
            LlmError error = new LlmError();
            error.setCategory(LlmError.Category.COMPATIBILITY);
            error.setRetryable(false);
            error.setProviderId(selectedProvider);
            String violations = result.getViolations() == null ? "" : String.join("; ", result.getViolations());
            error.setMessage("Adapter compatibility validation failed for provider '" + selectedProvider + "': " + violations);
            throw new LlmServiceException(error);
        }
        if (log != null) {
            log.debug("[llm] adapter validation passed for provider=" + selectedProvider);
        }
    }
}
