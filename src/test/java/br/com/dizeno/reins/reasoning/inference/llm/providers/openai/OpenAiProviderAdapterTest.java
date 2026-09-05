package br.com.dizeno.reins.reasoning.inference.llm.providers.openai;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.llm.adapters.OpenAiProviderAdapter;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmErrorMapper;
import br.com.dizeno.reins.reasoning.inference.llm.model.AdapterValidationResult;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import br.com.dizeno.reins.reasoning.inference.llm.model.ProviderCapabilityProfile;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OpenAiProviderAdapterTest {
    private OpenAiProviderAdapter adapter;
    private MockWebServer mockWebServer;
    private String baseUrl;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");

        OkHttpClient mockClient = new OkHttpClient();
        OpenAiClient client = new OpenAiClient(mockClient);
        adapter = new OpenAiProviderAdapter(client);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testProviderIdIsOpenAi() {
        assertEquals("openai", adapter.providerId());
    }

    @Test
    public void testValidateCompatibilitySuccess() {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 3);

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertTrue(result.isValid());
        assertEquals("openai", result.getProviderId());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    public void testValidateCompatibilityMissingConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setOpenai(null);

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("missing"));
    }

    @Test
    public void testValidateCompatibilityMissingModel() {
        ReinsConfig config = createOpenAiConfig(null, 30, 3);

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("model"));
    }

    @Test
    public void testValidateCompatibilityTimeoutZero() {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 0, 3);

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("timeoutSeconds"));
    }

    @Test
    public void testValidateCompatibilityRetryAttemptsNegative() {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, -1);

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("retryAttempts"));
    }

    @Test
    public void testValidateCompatibilityInvalidEndpoint() {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 3);
        config.getOpenai().setEndpoint("localhost:8080");

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("http"));
    }

    @Test
    public void testCapabilities() {
        ProviderCapabilityProfile capabilities = adapter.capabilities();
        assertTrue(capabilities.isSupportsTemperature());
        assertFalse(capabilities.isSupportsStructuredExtensions());
    }

    @Test
    public void testInvokeWithHistory() throws Exception {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 0);
        config.getOpenai().setEndpoint(baseUrl);

        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "Compiled content");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        LlmRequest request = new LlmRequest();
        request.setRequestId("test-1");
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "test prompt")));

        LlmResponse response = adapter.invoke(request, config);

        assertEquals("Compiled content", response.getContent());
        assertEquals(LlmResponse.Status.SUCCESS, response.getStatus());
        assertEquals("openai", response.getMetadata().get("provider"));
        assertEquals("gpt-4o", response.getMetadata().get("model"));
    }

    @Test
    public void testInvokeMultiTurn() throws Exception {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 0);
        config.getOpenai().setEndpoint(baseUrl);

        Map<String, Object> messageNode = new HashMap<>();
        messageNode.put("role", "assistant");
        messageNode.put("content", "Response based on history");

        Map<String, Object> choiceNode = new HashMap<>();
        choiceNode.put("message", messageNode);

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("choices", List.of(choiceNode));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        LlmRequest request = new LlmRequest();
        request.setRequestId("test-2");
        List<ConversationMessage> history = List.of(
            new ConversationMessage(ConversationMessage.Role.USER, "What is 2+2?")
        );
        request.setConversationHistory(history);

        LlmResponse response = adapter.invoke(request, config);

        assertEquals("Response based on history", response.getContent());
        assertEquals(LlmResponse.Status.SUCCESS, response.getStatus());
    }

    @Test
    public void testAuthenticationFailureMapsCorrectly() throws Exception {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 0);
        config.getOpenai().setEndpoint(baseUrl);
        config.getOpenai().setApiKey("secret-key-1234");

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"error\": {\"message\": \"Invalid API Key\", \"type\": \"invalid_request_error\"}}"));

        LlmRequest request = new LlmRequest();
        request.setRequestId("test-auth");
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "test prompt")));

        Exception error = assertThrows(Exception.class, () -> adapter.invoke(request, config));

        LlmError mapped = new LlmErrorMapper().map(error, "openai");
        assertEquals(LlmError.Category.AUTHENTICATION, mapped.getCategory());
        assertFalse(mapped.getMessage().contains("secret-key-1234"));
    }

    @Test
    public void testUnsupportedOperations() {
        ReinsConfig config = createOpenAiConfig("gpt-4o", 30, 0);
        assertThrows(UnsupportedOperationException.class, () -> adapter.createCachedContent(config, List.of()));
        assertThrows(UnsupportedOperationException.class, () -> adapter.deleteCachedContent(config, "cached-id"));
    }

    private ReinsConfig createOpenAiConfig(String model, int timeout, int retries) {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("openai");

        OpenAiSettings openai = new OpenAiSettings();
        openai.setModel(model);
        openai.setEndpoint("https://api.openai.com");
        openai.setTimeoutSeconds(timeout);
        openai.setRetryAttempts(retries);

        config.setOpenai(openai);
        return config;
    }
}
