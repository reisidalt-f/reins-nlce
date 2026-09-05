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
import br.com.dizeno.reins.reasoning.inference.llm.adapters.OllamaProviderAdapter;
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

 
public class OllamaProviderAdapterTest {
    private OllamaProviderAdapter adapter;
    private MockWebServer mockWebServer;
    private String baseUrl;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        
        OkHttpClient mockClient = new OkHttpClient();
        OllamaClient client = new OllamaClient(mockClient);
        adapter = new OllamaProviderAdapter(client);
    }

    @AfterEach
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    

    @Test
    public void testProviderIdIsOllama() {
        assertEquals("ollama", adapter.providerId());
    }

    

    @Test
    public void testValidateCompatibilitySuccess() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 3);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertTrue(result.isValid());
        assertEquals("ollama", result.getProviderId());
        assertTrue(result.getViolations().isEmpty());
    }

    @Test
    public void testValidateCompatibilityMissingConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setOllama(null);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("missing"));
    }

    @Test
    public void testValidateCompatibilityMissingModel() {
        ReinsConfig config = createOllamaConfig(null, 30, 3);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("model"));
    }

    @Test
    public void testValidateCompatibilityBlankModel() {
        ReinsConfig config = createOllamaConfig("  ", 30, 3);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("model"));
    }

    @Test
    public void testValidateCompatibilityTimeoutZero() {
        ReinsConfig config = createOllamaConfig("llama2", 0, 3);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("timeoutSeconds"));
    }

    @Test
    public void testValidateCompatibilityTimeoutNegative() {
        ReinsConfig config = createOllamaConfig("llama2", -1, 3);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("timeoutSeconds"));
    }

    @Test
    public void testValidateCompatibilityRetryAttemptsNegative() {
        ReinsConfig config = createOllamaConfig("llama2", 30, -1);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("retryAttempts"));
    }

    @Test
    public void testValidateCompatibilityRetryAttemptsZero() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertTrue(result.isValid());
    }

    @Test
    public void testOllamaSettingsDefaultRetryAttemptsIsThree() {
        OllamaSettings settings = new OllamaSettings();

        assertEquals(3, settings.getRetryAttempts());
    }

    @Test
    public void testValidateCompatibilityInvalidEndpointMissingScheme() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 3);
        config.getOllama().setEndpoint("localhost:11434");
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("http"));
    }

    @Test
    public void testValidateCompatibilityHttpsEndpoint() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 3);
        config.getOllama().setEndpoint("https://ollama.example.com");
        
        AdapterValidationResult result = adapter.validateCompatibility(config);
        
        assertTrue(result.isValid());
    }

    

    @Test
    public void testCapabilitiesSupportsTemperature() {
        ProviderCapabilityProfile capabilities = adapter.capabilities();
        
        assertTrue(capabilities.isSupportsTemperature());
    }

    @Test
    public void testCapabilitiesNoStructuredExtensions() {
        ProviderCapabilityProfile capabilities = adapter.capabilities();
        
        assertFalse(capabilities.isSupportsStructuredExtensions());
    }

    

    @Test
    public void testInvokeWithHistory() throws Exception {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        config.getOllama().setEndpoint(baseUrl);

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("role", "assistant");
        messagePayload.put("content", "Compiled content");

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("message", messagePayload);
        responsePayload.put("done", true);

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(mapper.writeValueAsString(responsePayload)));

        LlmRequest request = buildRequest("test-1");

        LlmResponse response = adapter.invoke(request, config);

        assertEquals("Compiled content", response.getContent());
        assertEquals(LlmResponse.Status.SUCCESS, response.getStatus());
        assertEquals("ollama", response.getMetadata().get("provider"));
        assertEquals("llama2", response.getMetadata().get("model"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("localhost:" + mockWebServer.getPort(), recordedRequest.getHeader("Host"));
    }

    @Test
    public void testInvokeMultiTurn() throws Exception {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        config.getOllama().setEndpoint(baseUrl);

        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("role", "assistant");
        messagePayload.put("content", "Response based on history");

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("message", messagePayload);
        responsePayload.put("done", true);

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
    public void testInvokeRoutesToCloudLikeEndpoint() throws Exception {
        try (MockWebServer cloudServer = new MockWebServer()) {
            cloudServer.start();
            OkHttpClient cloudClient = new OkHttpClient.Builder()
                .dns(hostname -> List.of(InetAddress.getByName("127.0.0.1")))
                .build();
            OllamaProviderAdapter cloudAdapter = new OllamaProviderAdapter(new OllamaClient(cloudClient));

            Map<String, Object> messagePayload = new HashMap<>();
            messagePayload.put("role", "assistant");
            messagePayload.put("content", "cloud compiled content");

            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("message", messagePayload);
            responsePayload.put("done", true);

            cloudServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(responsePayload)));

            ReinsConfig config = createOllamaConfig("llama2", 30, 0);
            config.getOllama().setEndpoint("http://api.ollama.example.com:" + cloudServer.getPort());

            LlmRequest request = buildRequest("test-cloud");

            LlmResponse response = cloudAdapter.invoke(request, config);

            assertEquals("cloud compiled content", response.getContent());
            RecordedRequest recordedRequest = cloudServer.takeRequest();
            assertEquals("api.ollama.example.com:" + cloudServer.getPort(), recordedRequest.getHeader("Host"));
        }
    }

    @Test
    public void testAuthenticationFailureMapsToAuthenticationAndRedactsSecret() throws Exception {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        config.getOllama().setEndpoint(baseUrl);
        config.getOllama().setApiKey("secret-key-1234");

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"error\":\"Bearer secret-key-1234 rejected\"}"));

        Exception error = assertThrows(Exception.class, () -> adapter.invoke(buildRequest("auth-1"), config));

        LlmError mapped = new LlmErrorMapper().map(error, "ollama");
        assertEquals(LlmError.Category.AUTHENTICATION, mapped.getCategory());
        assertTrue(mapped.getMessage().contains("<redacted>"));
        assertFalse(mapped.getMessage().contains("secret-key-1234"));
    }

    @Test
    public void testValidationFailureDoesNotExposeApiKeyInViolations() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        config.getOllama().setEndpoint("not-a-valid-endpoint");
        config.getOllama().setApiKey("secret-key-1234");

        AdapterValidationResult result = adapter.validateCompatibility(config);

        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream().noneMatch(message -> message.contains("secret-key-1234")));
    }

    

    @Test
    public void testCreateCachedContentThrowsUnsupported() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        
        assertThrows(UnsupportedOperationException.class, 
            () -> adapter.createCachedContent(config, List.of()));
    }

    @Test
    public void testDeleteCachedContentThrowsUnsupported() {
        ReinsConfig config = createOllamaConfig("llama2", 30, 0);
        
        assertThrows(UnsupportedOperationException.class, 
            () -> adapter.deleteCachedContent(config, "cached-123"));
    }

    

    private ReinsConfig createOllamaConfig(String model, int timeout, int retries) {
        ReinsConfig config = new ReinsConfig();
        config.setProvider("ollama");
        
        OllamaSettings ollama = new OllamaSettings();
        ollama.setModel(model);
        ollama.setEndpoint("http://localhost:11434");
        ollama.setTimeoutSeconds(timeout);
        ollama.setRetryAttempts(retries);
        
        config.setOllama(ollama);
        return config;
    }

    private LlmRequest buildRequest(String requestId) {
        LlmRequest request = new LlmRequest();
        request.setRequestId(requestId);
        request.setConversationHistory(List.of(new ConversationMessage(ConversationMessage.Role.USER, "test prompt")));
        return request;
    }
}
