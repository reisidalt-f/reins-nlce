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

package br.com.dizeno.reins.reasoning.inference;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequestOptions;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.spi.LlmService;
import br.com.dizeno.reins.reasoning.inference.llm.service.DefaultLlmService;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InferenceService is part of the API interactions with LLM endpoints,
 * configuring connections, and logging payloads in the reins architecture.
 * Performs API invocations to configured providers via LlmService, managing
 * configuration endpoints and request/response logging.
 */
public class InferenceService {
    private static final Pattern FIXTURE_RESPONSE = Pattern.compile("<!--\\s*reins-fixture-response\\s*(.*?)-->",
            Pattern.DOTALL);

    private final LlmService llmService;

    /**
     * Constructs a new instance of {@link InferenceService}.
     */
    public InferenceService() {
        this(new DefaultLlmService());
    }

    /**
     * Constructs a new instance of {@link InferenceService}.
     *
     * @param llmService the llm service
     */
    public InferenceService(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(org.apache.maven.plugin.logging.Log log) {
        this.llmService.setLog(log);
    }

    /**
     * Infer.
     *
     * @param request the request containing path and scope metadata
     * @param config  the Reins configuration settings
     * @return the resolved or constructed object
     */
    public MarkdownInferenceResponse infer(MarkdownInferenceRequest request,
            ReinsConfig config) throws Exception {
        long start = System.currentTimeMillis();

        String rawResponseText = null;

        if (request.getConversationHistory() != null) {
            rawResponseText = extractFixtureResponse(request.getConversationHistory());
        }

        if (rawResponseText == null) {
            if (config.isDryRun()) {
                rawResponseText = buildDryRunResponse(request);
                br.com.dizeno.reins.reasoning.inference.llm.logging.ModelRequestResponseLogger.log(
                        toLlmRequest(request, config), rawResponseText, config);
            } else {
                LlmRequest llmRequest = toLlmRequest(request, config);
                LlmResponse llmResponse = llmService.invoke(llmRequest, config);
                rawResponseText = llmResponse.getContent();
            }
        } else {
            br.com.dizeno.reins.reasoning.inference.llm.logging.ModelRequestResponseLogger.log(
                    toLlmRequest(request, config), rawResponseText, config);
        }
        MarkdownInferenceResponse response = new MarkdownInferenceResponse();
        response.setRawResponseText(rawResponseText);
        response.setDurationMs(System.currentTimeMillis() - start);
        return response;
    }

    private String extractFixtureResponse(List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ConversationMessage lastMessage = history.get(history.size() - 1);
        return extractFixtureResponse(lastMessage.getText());
    }

    private LlmRequest toLlmRequest(MarkdownInferenceRequest request, ReinsConfig config) {
        LlmRequest llmRequest = new LlmRequest();
        llmRequest.setRequestId(request.getSourcePath() == null ? "inference" : request.getSourcePath());
        llmRequest.setSourcePath(request.getSourcePath());
        llmRequest.setSourceScope(request.getSourceScope());
        llmRequest.setMarkdownContent(request.getMarkdownContent());
        llmRequest.setConversationHistory(request.getConversationHistory());
        llmRequest.setCachedContentId(request.getCachedContentId());
        llmRequest.setUseCachedContent(request.isUseCachedContent());

        LlmRequestOptions options = new LlmRequestOptions();
        ModelProviderSetting snapshot = request.getModelConfigSnapshot() != null
                ? request.getModelConfigSnapshot()
                : (config != null ? config.resolveActiveModelSettings() : null);
        if (snapshot != null) {
            options.setTimeoutSeconds(snapshot.getTimeoutSeconds());
            options.setRetryAttempts(snapshot.getRetryAttempts());
            if (snapshot.getTemperature() != null) {
                options.setTemperature(snapshot.getTemperature());
            }
        }
        llmRequest.setOptions(options);
        return llmRequest;
    }

    /**
     * Creates a new resource cached content.
     *
     * @param config         the Reins configuration settings
     * @param systemMessages the system messages
     * @return the string result
     */
    public String createCachedContent(ReinsConfig config,
            List<ConversationMessage> systemMessages) throws Exception {
        return llmService.createCachedContent(config, systemMessages);
    }

    /**
     * Deletes the target cached content.
     *
     * @param config          the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    public void deleteCachedContent(ReinsConfig config,
            String cachedContentId) throws Exception {
        llmService.deleteCachedContent(config, cachedContentId);
    }

    private String extractFixtureResponse(String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return null;
        }
        Matcher matcher = FIXTURE_RESPONSE.matcher(markdownContent);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String buildDryRunResponse(MarkdownInferenceRequest request) {
        String sourceName = Path.of(request.getSourcePath()).getFileName().toString();
        String baseName = sourceName.endsWith(".md")
                ? sourceName.substring(0, sourceName.length() - 3)
                : sourceName;
        return "``` path=\"" + baseName + ".compiled.out\"\n"
                + "// dry-run placeholder for " + request.getSourcePath() + "\n"
                + "```\n";
    }
}
