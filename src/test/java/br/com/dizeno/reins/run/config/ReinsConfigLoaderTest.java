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

package br.com.dizeno.reins.run.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ReinsConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    public void testMapLoading() {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", "ollama");
        
        Map<String, Object> ollamaMap = new HashMap<>();
        ollamaMap.put("model", "llama3");
        ollamaMap.put("timeoutSeconds", 60);
        map.put("ollama", ollamaMap);
        
        map.put("scanRoots", "src/main/nl,src/test/nl");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, null, map, baseDir);

        assertEquals("ollama", config.getProvider());
        assertEquals("llama3", config.getOllama().getModel());
        assertEquals(60, config.getOllama().getTimeoutSeconds());
        assertEquals(2, config.getScanRoots().size());
        assertEquals(new File(baseDir, "src/main/nl"), config.getScanRoots().get(0));
    }

    @Test
    public void testPropertiesLoading() {
        Properties props = new Properties();
        props.setProperty("reins.provider", "gemini");
        props.setProperty("reins.gemini.apiKey", "key-123");
        props.setProperty("reins.gemini.timeoutSeconds", "45");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertEquals("gemini", config.getProvider());
        assertEquals("key-123", config.getGemini().getApiKey());
        assertEquals(45, config.getGemini().getTimeoutSeconds());
    }

    @Test
    public void testCliArgsLoading() {
        String[] args = {
                "--provider", "gemini",
                "--gemini.apiKey", "cli-key",
                "--failOnError",
                "--verbose"
        };

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(args, null, null, baseDir);

        assertEquals("gemini", config.getProvider());
        assertEquals("cli-key", config.getGemini().getApiKey());
        assertTrue(config.isFailOnError());
        assertTrue(config.isVerbose());
    }

    @Test
    public void testPrecedence() throws IOException {
        File baseDir = tempDir.toFile();
        
        
        File propFile = new File(baseDir, "reins.properties");
        Properties fileProps = new Properties();
        fileProps.setProperty("provider", "ollama");
        fileProps.setProperty("ollama.model", "llama-default");
        fileProps.setProperty("failOnError", "true");
        try (FileOutputStream out = new FileOutputStream(propFile)) {
            fileProps.store(out, null);
        }

        
        Properties sysProps = new Properties();
        sysProps.setProperty("reins.ollama.model", "llama-sys");

        
        String[] args = {
                "--failOnError", "false"
        };

        ReinsConfig config = ReinsConfigLoader.load(args, sysProps, null, baseDir);

        
        assertEquals("ollama", config.getProvider()); 
        assertEquals("llama-sys", config.getOllama().getModel()); 
        assertFalse(config.isFailOnError()); 
    }

    @Test
    public void testConfigOverrideReplacesDefault() throws IOException {
        File baseDir = tempDir.toFile();

        
        File defaultFile = new File(baseDir, "reins.properties");
        Properties defaultProps = new Properties();
        defaultProps.setProperty("provider", "ollama");
        try (FileOutputStream out = new FileOutputStream(defaultFile)) {
            defaultProps.store(out, null);
        }

        
        File customFile = new File(baseDir, "custom.properties");
        Properties customProps = new Properties();
        customProps.setProperty("provider", "gemini");
        customProps.setProperty("gemini.apiKey", "custom-api-key");
        try (FileOutputStream out = new FileOutputStream(customFile)) {
            customProps.store(out, null);
        }

        String[] args = {
                "--config", "custom.properties"
        };

        ReinsConfig config = ReinsConfigLoader.load(args, null, null, baseDir);

        
        assertEquals("gemini", config.getProvider());
        assertEquals("custom-api-key", config.getGemini().getApiKey());
    }

    @Test
    public void testJsonConfigParsing() throws IOException {
        File baseDir = tempDir.toFile();
        File jsonFile = new File(baseDir, "reins.json");
        String jsonContent = "{\n" +
                "  \"provider\": \"ollama\",\n" +
                "  \"ollama\": {\n" +
                "    \"model\": \"json-model\",\n" +
                "    \"timeoutSeconds\": 35\n" +
                "  }\n" +
                "}";
        try (FileOutputStream out = new FileOutputStream(jsonFile)) {
            out.write(jsonContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("ollama", config.getProvider());
        assertEquals("json-model", config.getOllama().getModel());
        assertEquals(35, config.getOllama().getTimeoutSeconds());
    }

    @Test
    public void testYamlConfigParsing() throws IOException {
        File baseDir = tempDir.toFile();
        File yamlFile = new File(baseDir, "reins.yaml");
        String yamlContent = "provider: gemini\n" +
                "gemini:\n" +
                "  apiKey: yaml-key-456\n" +
                "  timeoutSeconds: 80\n";
        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write(yamlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("gemini", config.getProvider());
        assertEquals("yaml-key-456", config.getGemini().getApiKey());
        assertEquals(80, config.getGemini().getTimeoutSeconds());
    }

    @Test
    public void testXmlConfigParsing() throws IOException {
        File baseDir = tempDir.toFile();
        File xmlFile = new File(baseDir, "reins.xml");
        String xmlContent = "<configuration>\n" +
                "  <provider>gemini</provider>\n" +
                "  <gemini>\n" +
                "    <apiKey>xml-api-key-789</apiKey>\n" +
                "    <timeoutSeconds>95</timeoutSeconds>\n" +
                "  </gemini>\n" +
                "  <scanRoots>\n" +
                "    <scanRoot>src/main/nl</scanRoot>\n" +
                "    <scanRoot>src/test/nl</scanRoot>\n" +
                "  </scanRoots>\n" +
                "</configuration>";
        try (FileOutputStream out = new FileOutputStream(xmlFile)) {
            out.write(xmlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("gemini", config.getProvider());
        assertEquals("xml-api-key-789", config.getGemini().getApiKey());
        assertEquals(95, config.getGemini().getTimeoutSeconds());
        assertEquals(2, config.getScanRoots().size());
        assertEquals(new File(baseDir, "src/main/nl"), config.getScanRoots().get(0));
    }

    @Test
    public void testDefaultConfigScanningPrecedence() throws IOException {
        File baseDir = tempDir.toFile();

        
        File yamlFile = new File(baseDir, "reins.yaml");
        File ymlFile = new File(baseDir, "reins.yml");
        File jsonFile = new File(baseDir, "reins.json");
        File xmlFile = new File(baseDir, "reins.xml");
        File propertiesFile = new File(baseDir, "reins.properties");

        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write("provider: yaml-provider".getBytes());
        }
        try (FileOutputStream out = new FileOutputStream(ymlFile)) {
            out.write("provider: yml-provider".getBytes());
        }
        try (FileOutputStream out = new FileOutputStream(jsonFile)) {
            out.write("{\"provider\": \"json-provider\"}".getBytes());
        }
        try (FileOutputStream out = new FileOutputStream(xmlFile)) {
            out.write("<configuration><provider>xml-provider</provider></configuration>".getBytes());
        }
        try (FileOutputStream out = new FileOutputStream(propertiesFile)) {
            out.write("provider=properties-provider".getBytes());
        }

        
        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("yaml-provider", config.getProvider());

        yamlFile.delete();
        config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("yml-provider", config.getProvider());

        ymlFile.delete();
        config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("json-provider", config.getProvider());

        jsonFile.delete();
        config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("xml-provider", config.getProvider());

        xmlFile.delete();
        config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("properties-provider", config.getProvider());
    }

    @Test
    public void testLoggingResultLoading() {
        Properties props = new Properties();
        props.setProperty("reins.logging.result", "true");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertTrue(config.getLogging().isResult());
    }
}
