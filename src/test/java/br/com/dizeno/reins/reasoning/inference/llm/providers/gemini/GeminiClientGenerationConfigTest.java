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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientGenerationConfigTest {

    private MockWebServer server;
    private GeminiClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        mapper = new ObjectMapper();
        client = new GeminiClient(mapper, new RetryPolicy(), new OkHttpClient());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private GeminiRequestParams buildParams(GenerationSettings gc) {
        return new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                5,
                0,
                1000,
                false,
                gc
        );
    }

    private GeminiRequestParams buildNullConfigParams() {
        return new GeminiRequestParams(
                server.url("/").toString().replaceAll("/$", ""),
                "test-api-key",
                "gemini-model",
                5,
                0,
                1000,
                false,
                null
        );
    }

    private static String successBody() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}";
    }

    private JsonNode sendAndCaptureGeneration(GeminiRequestParams params) throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(successBody()));
        client.compile(params, "prompt");
        RecordedRequest recorded = server.takeRequest();
        JsonNode body = mapper.readTree(recorded.getBody().readUtf8());
        return body.get("generation");
    }

    

    @Test
    void allFieldsSet_allIncludedInPayload() throws Exception {
        GenerationSettings gc = new GenerationSettings();
        gc.setTemperature(0.5f);
        gc.setTopP(0.9f);
        gc.setTopK(40);
        gc.setPresencePenalty(0.1f);
        gc.setFrequencyPenalty(0.2f);

        JsonNode genConfig = sendAndCaptureGeneration(buildParams(gc));

        assertNotNull(genConfig);
        assertEquals(0.5, genConfig.get("temperature").asDouble(), 0.01);
        assertEquals(0.9, genConfig.get("topP").asDouble(), 0.01);
        assertEquals(40, genConfig.get("topK").asInt());
        assertEquals(0.1, genConfig.get("presencePenalty").asDouble(), 0.01);
        assertEquals(0.2, genConfig.get("frequencyPenalty").asDouble(), 0.01);
        
        assertNull(genConfig.get("candidateCount"));
    }

    

    @Test
    void subsetFields_onlyThoseIncludedInPayload() throws Exception {
        GenerationSettings gc = new GenerationSettings();
        gc.setTemperature(1.2f);
        gc.setTopK(10);

        JsonNode genConfig = sendAndCaptureGeneration(buildParams(gc));

        assertNotNull(genConfig);
        assertEquals(1.2, genConfig.get("temperature").asDouble(), 0.01);
        assertEquals(10, genConfig.get("topK").asInt());
        assertNull(genConfig.get("topP"));
        assertNull(genConfig.get("presencePenalty"));
        assertNull(genConfig.get("frequencyPenalty"));
    }

    

    @Test
    void onlyTemperatureSet_othersOmitted() throws Exception {
        GenerationSettings gc = new GenerationSettings();
        gc.setTemperature(0.7f);

        JsonNode genConfig = sendAndCaptureGeneration(buildParams(gc));

        assertNotNull(genConfig);
        assertEquals(0.7, genConfig.get("temperature").asDouble(), 0.01);
        assertNull(genConfig.get("topP"));
        assertNull(genConfig.get("topK"));
        assertNull(genConfig.get("presencePenalty"));
        assertNull(genConfig.get("frequencyPenalty"));
    }

    

    @Test
    void allFieldsNull_generationEmpty() throws Exception {
        GenerationSettings gc = new GenerationSettings();
        

        JsonNode genConfig = sendAndCaptureGeneration(buildParams(gc));

        assertNotNull(genConfig);
        assertEquals(0, genConfig.size(), "generation should be empty when no fields are set");
    }

    

    @Test
    void nullGeneration_emptyObjectSent() throws Exception {
        JsonNode genConfig = sendAndCaptureGeneration(buildNullConfigParams());

        assertNotNull(genConfig);
        assertEquals(0, genConfig.size(), "generation should be empty when null");
    }
}
