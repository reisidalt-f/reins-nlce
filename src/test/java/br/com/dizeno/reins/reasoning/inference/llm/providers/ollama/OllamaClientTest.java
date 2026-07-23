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

import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

 
public class OllamaClientTest {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private MockWebServer mockWebServer;
    private OllamaClient client;
    private String baseUrl;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        client = new OllamaClient();
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    

    @Test
    public void testCompileSuccess() throws Exception {
        
        String prompt = "Hello, world!";
        String expectedResponse = "Hello! How can I help you today?";
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("model", "llama2");
        responsePayload.put("response", expectedResponse);
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        
        String result = client.compile(params, prompt);

        
        assertEquals(expectedResponse, result);
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/api/compile"));
        assertEquals("application/json", request.getHeader("Content-Type"));
    }

    @Test
    public void testCompileSuccessAgainstCloudLikeEndpoint() throws Exception {
        client = new OllamaClient(dnsPinnedClient());

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("model", "llama2");
        responsePayload.put("response", "cloud response");
        responsePayload.put("done", true);

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            "http://api.ollama.example.com:" + mockWebServer.getPort(), "llama2", null, 30, 0, new HashMap<>()
        );

        String result = client.compile(params, "test prompt");

        assertEquals("cloud response", result);
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("api.ollama.example.com:" + mockWebServer.getPort(), request.getHeader("Host"));
    }

    @Test
    public void testCompileRequestEnvelope() throws Exception {
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("model", "llama2");
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        client.compile(params, "test prompt");

        RecordedRequest request = mockWebServer.takeRequest();
        String bodyString = request.getBody().readUtf8();
        Map<String, Object> requestBody = mapper.readValue(bodyString, MAP_TYPE);
        
        assertTrue(requestBody.containsKey("model"));
        assertTrue(requestBody.containsKey("prompt"));
        assertTrue(requestBody.containsKey("stream"));
        assertEquals(false, requestBody.get("stream"));
    }

    @Test
    public void testCompileWithOptions() throws Exception {
        
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, options
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        String bodyString = request.getBody().readUtf8();
        Map<String, Object> requestBody = mapper.readValue(bodyString, MAP_TYPE);
        
        assertTrue(requestBody.containsKey("options"));
    }

    

    @Test
    public void testCompileChatSuccess() throws Exception {
        
        List<ConversationMessage> messages = List.of(
            new ConversationMessage(ConversationMessage.Role.USER, "Hello")
        );
        
        String expectedResponse = "Hi there!";
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("role", "assistant");
        messagePayload.put("content", expectedResponse);
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("model", "llama2");
        responsePayload.put("message", messagePayload);
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        
        String result = client.compileChat(params, messages);

        
        assertEquals(expectedResponse, result);
        
        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/api/chat"));
    }

    @Test
    public void testCompileChatMessageRoleMapping() throws Exception {
        
        List<ConversationMessage> messages = List.of(
            new ConversationMessage(ConversationMessage.Role.USER, "test")
        );
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("message", new HashMap<String, String>() {{
            put("content", "response");
        }});
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        client.compileChat(params, messages);

        RecordedRequest request = mockWebServer.takeRequest();
        String bodyString = request.getBody().readUtf8();
        Map<String, Object> requestBody = mapper.readValue(bodyString, MAP_TYPE);
        
        assertTrue(requestBody.containsKey("messages"));
        List<?> msgList = (List<?>) requestBody.get("messages");
        Map<?, ?> firstMessage = (Map<?, ?>) msgList.get(0);
        assertEquals("user", firstMessage.get("role"));
    }

    

    @Test
    public void testAuthorizationHeaderPresent() throws Exception {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", "test-api-key", 30, 3, new HashMap<>()
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer test-api-key", request.getHeader("Authorization"));
    }

    @Test
    public void testAuthorizationHeaderAbsentWhenNullApiKey() throws Exception {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        assertNull(request.getHeader("Authorization"));
    }

    @Test
    public void testAuthorizationHeaderAbsentWhenBlankApiKey() throws Exception {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", "  ", 30, 3, new HashMap<>()
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        
        assertNull(request.getHeader("Authorization"));
    }

    

    @Test
    public void testHttpErrorResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "nonexistent-model", null, 30, 0, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }

    @Test
    public void testHttp401AuthenticationError() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", "invalid-key", 30, 3, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }

    @Test
    public void testHttp500ServerError() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 3, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
        assertEquals(4, mockWebServer.getRequestCount());
    }

    @Test
    public void testHttp404RetriesWhenRetryAttemptsConfigured() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "nonexistent-model", null, 30, 2, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    public void testEmptyResponseBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(""));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 0, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }

    @Test
    public void testDoneFalseResponse() throws Exception {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "incomplete");
        responsePayload.put("done", false);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl, "llama2", null, 30, 0, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }

    @Test
    public void testTrailingSlashHandling() throws Exception {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", "test");
        responsePayload.put("done", true);
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        
        OllamaRequestParams params = new OllamaRequestParams(
            baseUrl + "/", "llama2", null, 30, 0, new HashMap<>()
        );

        String result = client.compile(params, "test");
        assertEquals("test", result);
    }

    private OkHttpClient dnsPinnedClient() {
        return new OkHttpClient.Builder()
            .dns(hostname -> List.of(InetAddress.getByName("127.0.0.1")))
            .build();
    }
}
