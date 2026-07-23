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
import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiClient;
import br.com.dizeno.reins.reasoning.inference.llm.providers.gemini.GeminiPromptBuilder;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InferenceServiceCachedContentTest {

    @Test
    void infer_withHistory_passesCachedContentMetadataToGeminiClient() throws Exception {
        GeminiPromptBuilder promptBuilder = mock(GeminiPromptBuilder.class);
        GeminiClient geminiClient = mock(GeminiClient.class);
        InferenceService service = new InferenceService(promptBuilder, geminiClient);

        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("k");
        gemini.setEndpoint("https://example.test");
        gemini.setModel("gemini-model");
        config.setGemini(gemini);

        MarkdownInferenceRequest request = new MarkdownInferenceRequest();
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "hello")));
        request.setCachedContentId("cachedContents/abc");
        request.setUseCachedContent(true);

        when(geminiClient.compileWithHistory(any(), any(), eq("cachedContents/abc"), eq(true)))
                .thenReturn("ok");

        MarkdownInferenceResponse response = service.infer(request, config);

        assertEquals("ok", response.getRawResponseText());
        verify(geminiClient).compileWithHistory(any(), any(), eq("cachedContents/abc"), eq(true));
    }

    @Test
    void infer_withHistory_disabledModeSkipsCachedContentUsage() throws Exception {
        GeminiPromptBuilder promptBuilder = mock(GeminiPromptBuilder.class);
        GeminiClient geminiClient = mock(GeminiClient.class);
        InferenceService service = new InferenceService(promptBuilder, geminiClient);

        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("k");
        gemini.setEndpoint("https://example.test");
        gemini.setModel("gemini-model");
        config.setGemini(gemini);

        MarkdownInferenceRequest request = new MarkdownInferenceRequest();
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "hello")));
        request.setCachedContentId("cachedContents/abc");
        request.setUseCachedContent(false);

        when(geminiClient.compileWithHistory(any(), any(), eq("cachedContents/abc"), eq(false)))
                .thenReturn("ok");

        MarkdownInferenceResponse response = service.infer(request, config);

        assertEquals("ok", response.getRawResponseText());
        verify(geminiClient).compileWithHistory(any(), any(), eq("cachedContents/abc"), eq(false));
    }
}
