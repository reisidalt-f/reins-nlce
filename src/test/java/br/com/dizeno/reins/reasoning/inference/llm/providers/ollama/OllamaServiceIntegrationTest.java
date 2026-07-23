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

package br.com.dizeno.reins.reasoning.inference.llm.providers.ollama;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmServiceException;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.service.DefaultLlmService;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.testutil.RecordingLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaServiceIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private MockWebServer server;
    private RecordingLog log;
    private DefaultLlmService llmService;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        baseUrl = server.url("/").toString().replaceAll("/$", "");
        log = new RecordingLog();
        llmService = new DefaultLlmService();
        llmService.setLog(log);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void successfulSingleTurnInvocationCarriesNormalizedMetadataAndLogsSelection() throws Exception {
        server.enqueue(json(200, Map.of(
                "model", "llama3",
                "response", "compiled text",
                "done", true
        )));

        ReinsConfig config = config(baseUrl, "llama3", null);
        config.getLogging().setLlmProvider(true);

        LlmResponse response = llmService.invoke(singleTurnRequest("req-1", "prompt"), config);

        assertEquals("compiled text", response.getContent());
        assertEquals("ollama", response.getMetadata().get("provider"));
        assertEquals("llama3", response.getMetadata().get("model"));
        assertNotNull(response.getExtensions());
        assertEquals("ollama", response.getExtensions().getProviderId());
        assertEquals("ollama", response.getExtensions().getPayload().get("provider"));
        assertEquals("llama3", response.getExtensions().getPayload().get("model"));
        assertTrue(log.hasInfoContaining("Selected provider: ollama"));
        assertTrue(log.hasDebugContaining("[llm] start requestId=req-1 provider=ollama"));
        assertTrue(log.hasDebugContaining("[llm] success requestId=req-1 provider=ollama"));

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().endsWith("/api/compile"));
    }

    @Test
    void successfulInvocationSuppressesSelectionLogByDefault() throws Exception {
        server.enqueue(json(200, Map.of(
                "model", "llama3",
                "response", "compiled text",
                "done", true
        )));

        LlmResponse response = llmService.invoke(singleTurnRequest("req-1", "prompt"), config(baseUrl, "llama3", null));

        assertEquals("compiled text", response.getContent());
        assertFalse(log.hasInfoContaining("Selected provider:"));
    }

    @Test
    void successfulConversationInvocationUsesChatEndpoint() throws Exception {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("content", "conversation response");
        server.enqueue(json(200, Map.of(
                "model", "llama3",
                "message", message,
                "done", true
        )));

        LlmRequest request = singleTurnRequest("req-2", null);
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "hello")));

        LlmResponse response = llmService.invoke(request, config(baseUrl + "/", "llama3", null));

        assertEquals("conversation response", response.getContent());
        RecordedRequest recordedRequest = server.takeRequest();
        assertTrue(recordedRequest.getPath().endsWith("/api/chat"));
    }

    @Test
    void authenticationFailureIsMappedToAuthenticationCategory() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"authentication failed\"}"));

        LlmServiceException error = assertThrows(
                LlmServiceException.class,
                () -> llmService.invoke(singleTurnRequest("req-3", "prompt"), config(baseUrl, "llama3", "secret-key-1234"))
        );

        assertEquals(LlmError.Category.AUTHENTICATION, error.getError().getCategory());
        assertEquals("ollama", error.getError().getProviderId());
        assertTrue(log.hasWarnContaining("provider=ollama"));
    }

    @Test
    void authenticatedInvocationSendsBearerHeader() throws Exception {
        server.enqueue(json(200, Map.of(
                "model", "llama3",
                "response", "authorized",
                "done", true
        )));

        llmService.invoke(singleTurnRequest("req-4", "prompt"), config(baseUrl, "llama3", "token-1234"));

        RecordedRequest recordedRequest = server.takeRequest();
        assertEquals("Bearer token-1234", recordedRequest.getHeader("Authorization"));
    }

    private ReinsConfig config(String endpoint, String model, String apiKey) {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("ollama");
        OllamaSettings ollama = new OllamaSettings();
        ollama.setEndpoint(endpoint);
        ollama.setModel(model);
        ollama.setApiKey(apiKey);
        ollama.setTimeoutSeconds(5);
        ollama.setRetryAttempts(0);
        config.setOllama(ollama);
        return config;
    }

    private LlmRequest singleTurnRequest(String requestId, String markdownContent) {
        LlmRequest request = new LlmRequest();
        request.setRequestId(requestId);
        request.setMarkdownContent(markdownContent);
        return request;
    }

    private MockResponse json(int code, Map<String, Object> body) throws IOException {
        return new MockResponse()
                .setResponseCode(code)
                .addHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(body));
    }
}
