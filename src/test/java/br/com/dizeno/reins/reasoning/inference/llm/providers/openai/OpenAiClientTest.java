package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

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

public class OpenAiClientTest {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private MockWebServer mockWebServer;
    private OpenAiClient client;
    private String baseUrl;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        client = new OpenAiClient();
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testCompileSuccess() throws Exception {
        String prompt = "Hello, world!";
        String expectedResponse = "Hello! How can I help you today?";

        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", expectedResponse);

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 3, new HashMap<>()
        );

        String result = client.compile(params, prompt);

        assertEquals(expectedResponse, result);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/v1/chat/completions"));
        assertEquals("application/json", request.getHeader("Content-Type"));
    }

    @Test
    public void testCompileSuccessAgainstV1Endpoint() throws Exception {
        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "v1 response");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl + "/v1", "gpt-4o", null, 30, 0, new HashMap<>()
        );

        String result = client.compile(params, "test prompt");

        assertEquals("v1 response", result);
        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().endsWith("/v1/chat/completions"));
    }

    @Test
    public void testCompileRequestEnvelope() throws Exception {
        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "test");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 3, new HashMap<>()
        );

        client.compile(params, "test prompt");

        RecordedRequest request = mockWebServer.takeRequest();
        String bodyString = request.getBody().readUtf8();
        Map<String, Object> requestBody = mapper.readValue(bodyString, MAP_TYPE);

        assertTrue(requestBody.containsKey("model"));
        assertTrue(requestBody.containsKey("messages"));
        List<?> msgList = (List<?>) requestBody.get("messages");
        assertEquals(1, msgList.size());
    }

    @Test
    public void testCompileWithOptions() throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);

        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "test");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 3, options
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        String bodyString = request.getBody().readUtf8();
        Map<String, Object> requestBody = mapper.readValue(bodyString, MAP_TYPE);

        assertTrue(requestBody.containsKey("temperature"));
        assertEquals(0.7, requestBody.get("temperature"));
    }

    @Test
    public void testCompileChatSuccess() throws Exception {
        List<ConversationMessage> messages = List.of(
            new ConversationMessage(ConversationMessage.Role.USER, "Hello")
        );

        String expectedResponse = "Hi there!";
        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", expectedResponse);

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 3, new HashMap<>()
        );

        String result = client.compileChat(params, messages);

        assertEquals(expectedResponse, result);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/v1/chat/completions"));
    }

    @Test
    public void testAuthorizationHeaderPresent() throws Exception {
        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "test");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", "test-api-key", 30, 3, new HashMap<>()
        );

        client.compile(params, "test");

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("Bearer test-api-key", request.getHeader("Authorization"));
    }

    @Test
    public void testHttpErrorResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 0, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }

    @Test
    public void testHttp500ServerErrorRetries() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 3, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
        assertEquals(4, mockWebServer.getRequestCount());
    }

    @Test
    public void testEmptyResponseBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(""));

        OpenAiRequestParams params = new OpenAiRequestParams(
            baseUrl, "gpt-4o", null, 30, 0, new HashMap<>()
        );

        assertThrows(IOException.class, () -> client.compile(params, "test"));
    }
}
