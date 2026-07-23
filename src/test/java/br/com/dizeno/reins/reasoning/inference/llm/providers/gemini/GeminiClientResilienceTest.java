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

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiClientResilienceTest {

    private MockWebServer server;
    private GeminiClient client;
    private GeminiRequestParams params;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new GeminiClient(new ObjectMapper(), new RetryPolicy(), new OkHttpClient());
        params = new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                5,
                3,
            0,
                false,
                null
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private static String successBody(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + text + "\"}]}}]}";
    }

    private static MockResponse timeout500() {
        return new MockResponse().setResponseCode(500).setBody("{\"error\":\"server error\"}");
    }

    private static String emptyBody() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"\"}]}}]}";
    }

    private static String whitespaceBody() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"   \\n\\t\"}]}}]}";
    }

    

    @Test
    void succeedsAfterNFailuresWhenNEqualsRetryAttempts() throws Exception {
        
        server.enqueue(timeout500());
        server.enqueue(timeout500());
        server.enqueue(timeout500());
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("hello")));

        String result = client.compile(params, "prompt");
        assertEquals("hello", result);
        assertEquals(4, server.getRequestCount());
    }

    @Test
    void succeedsAfterFewerFailuresThanRetryAttempts() throws Exception {
        server.enqueue(timeout500());
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("world")));

        String result = client.compile(params, "prompt");
        assertEquals("world", result);
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void allRetriesExhaustedThrowsException() {
        
        server.enqueue(timeout500());
        server.enqueue(timeout500());
        server.enqueue(timeout500());
        server.enqueue(timeout500());

        assertThrows(IOException.class, () -> client.compile(params, "prompt"));
        assertEquals(4, server.getRequestCount());
    }

    

    @Test
    void multiBodyResponseUsesFirstBodyOnly() throws Exception {
        
        String multiBody = successBody("first-content") + "\n" + successBody("second-content");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(multiBody));

        String result = client.compile(params, "prompt");
        assertEquals("first-content", result);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void singleBodyEmptyCandidatesThrowsGeminiEmptyResponseException() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));

        GeminiEmptyResponseException ex = assertThrows(GeminiEmptyResponseException.class, () -> client.compile(params, "prompt"));
        assertEquals(4, server.getRequestCount());
        assertEquals(4, ex.getTotalAttempts());
        assertEquals(4, ex.getNoUsableContentCount());
    }

    @Test
    void multiBodyEmptyFirstBodyThrowsRetryableIOException() throws Exception {
        
        String multiBodyEmpty = emptyBody() + "\n" + successBody("ignored");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(multiBodyEmpty));
        
        server.enqueue(timeout500());
        server.enqueue(timeout500());
        server.enqueue(timeout500());

        assertThrows(IOException.class, () -> client.compile(params, "prompt"));
        assertEquals(4, server.getRequestCount());
    }

    @Test
    void verboseTrueLogsDiscardCount() throws Exception {
        List<String> infoMessages = new ArrayList<>();
        Log mockLog = mock(Log.class);
        doAnswer(inv -> { infoMessages.add(inv.getArgument(0).toString()); return null; })
                .when(mockLog).info(anyString());

        GeminiClient verboseClient = new GeminiClient(new ObjectMapper(), new RetryPolicy(), new OkHttpClient(), mockLog);
        GeminiRequestParams verboseParams = new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                5,
                3,
            0,
                true,
                null
        );
        String multiBody = successBody("first") + "\n" + successBody("second");
        server.enqueue(new MockResponse().setResponseCode(200).setBody(multiBody));

        String result = verboseClient.compile(verboseParams, "prompt");
        assertEquals("first", result);
        assertEquals(1, server.getRequestCount());
        assertTrue(infoMessages.stream().anyMatch(msg -> msg.contains("extra response") && msg.contains("using first only")),
            "Verbose mode should log discarded extra response bodies");
    }

    @Test
    void emptyThenSuccessRetriesAndSucceeds() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("recovered")));

        String result = client.compile(params, "prompt");

        assertEquals("recovered", result);
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void whitespaceOnlyResponseIsRetriedThenSucceeds() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(whitespaceBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("after-whitespace")));

        String result = client.compile(params, "prompt");

        assertEquals("after-whitespace", result);
        assertEquals(2, server.getRequestCount());
    }

    @Test
    void timeoutBudgetAppliesPerAttempt() {
        GeminiRequestParams timeoutParams = new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                1,
                1,
                0,
                false,
                null
        );
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("slow"))
                .setBodyDelay(2, TimeUnit.SECONDS));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("slow-again"))
                .setBodyDelay(2, TimeUnit.SECONDS));

        long start = System.currentTimeMillis();
        assertThrows(IOException.class, () -> client.compile(timeoutParams, "prompt"));
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(2, server.getRequestCount());
        assertTrue(elapsed >= 1800, "Expected elapsed time to include one timeout per attempt");
    }

    @Test
    void emptyResponseRetryLogsHumanReadableLine() throws Exception {
        List<String> infoMessages = new ArrayList<>();
        Log mockLog = mock(Log.class);
        doAnswer(inv -> { infoMessages.add(inv.getArgument(0).toString()); return null; })
                .when(mockLog).info(anyString());

        GeminiClient loggedClient = new GeminiClient(new ObjectMapper(), new RetryPolicy(), new OkHttpClient(), mockLog);
        GeminiRequestParams localParams = new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                5,
                1,
                0,
                false,
                null
        );

        server.enqueue(new MockResponse().setResponseCode(200).setBody(emptyBody()));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("ok")));

        String result = loggedClient.compile(localParams, "prompt");
        assertEquals("ok", result);
        assertTrue(infoMessages.stream().anyMatch(msg -> msg.contains("[empty-response-retry]")
                && msg.contains("class=empty-or-blank")
                && msg.contains("action=retrying")));
    }

        @Test
        void createCachedContent_postsSystemInstructionAndReturnsName() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200)
            .setBody("{\"name\":\"cachedContents/my-cache\"}"));

        String cacheId = client.createCachedContent(
            params,
            List.of(new ConversationMessage(ConversationMessage.Role.SYSTEM, "System context"))
        );

        assertEquals("cachedContents/my-cache", cacheId);

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().startsWith("/v1beta/cachedContents?key=test-api-key"));
        String payload = request.getBody().readUtf8();
        assertTrue(payload.contains("\"model\":\"models/gemini-model\""));
        assertTrue(payload.contains("\"systemInstruction\""));
        }

        @Test
        void compileWithHistory_usesCachedContentFieldWhenEnabled() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody("ok")));

        String result = client.compileWithHistory(
            params,
            List.of(
                new ConversationMessage(ConversationMessage.Role.SYSTEM, "System context"),
                new ConversationMessage(ConversationMessage.Role.USER, "Compile")
            ),
            "cachedContents/my-cache",
            true
        );

        assertEquals("ok", result);
        RecordedRequest request = server.takeRequest();
        String payload = request.getBody().readUtf8();
        assertTrue(payload.contains("\"cachedContent\":\"cachedContents/my-cache\""));
        assertFalse(payload.contains("\"systemInstruction\""),
            "System instruction should not be sent when cachedContent is used");
        }

        @Test
        void deleteCachedContent_usesDeleteMethodAndNormalizedPath() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        client.deleteCachedContent(params, "my-cache");

        RecordedRequest request = server.takeRequest();
        assertEquals("DELETE", request.getMethod());
        assertTrue(request.getPath().startsWith("/v1beta/cachedContents/my-cache?key=test-api-key"));
        }
}
