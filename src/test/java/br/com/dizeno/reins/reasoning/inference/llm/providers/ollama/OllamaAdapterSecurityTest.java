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
import br.com.dizeno.reins.reasoning.inference.llm.service.DefaultLlmService;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.testutil.RecordingLog;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaAdapterSecurityTest {
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
    void failureLogsDoNotExposeRawApiKey() {
        String apiKey = "dummy-api-key";
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"Bearer " + apiKey + " rejected\"}"));

        assertThrows(LlmServiceException.class, () -> llmService.invoke(request(), config(apiKey)));

        String joinedWarn = String.join("\n", log.getWarnMessages());
        String joinedDebug = String.join("\n", log.getDebugMessages());
        String joinedInfo = String.join("\n", log.getInfoMessages());

        assertFalse(joinedWarn.contains(apiKey));
        assertFalse(joinedDebug.contains(apiKey));
        assertFalse(joinedInfo.contains(apiKey));
        assertTrue(joinedWarn.contains("<redacted>"));
    }

    private ReinsConfig config(String apiKey) {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("ollama");
        OllamaSettings ollama = new OllamaSettings();
        ollama.setEndpoint(baseUrl);
        ollama.setModel("llama3");
        ollama.setApiKey(apiKey);
        ollama.setTimeoutSeconds(5);
        ollama.setRetryAttempts(0);
        config.setOllama(ollama);
        return config;
    }

    private LlmRequest request() {
        LlmRequest request = new LlmRequest();
        request.setRequestId("security-request");
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "prompt")));
        return request;
    }
}
