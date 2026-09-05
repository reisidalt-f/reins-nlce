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
        
        map.put("source.main", "src/main/nl");
        map.put("source.test", "src/test/nl");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, null, map, baseDir);

        assertEquals("ollama", config.getProvider());
        assertEquals("llama3", config.getOllama().getModel());
        assertEquals(60, config.getOllama().getTimeoutSeconds());
        assertEquals(new File(baseDir, "src/main/nl"), config.getSourceBase("main"));
        assertEquals(new File(baseDir, "src/test/nl"), config.getSourceBase("test"));
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
                "  <sources>\n" +
                "    <main>src/main/nl</main>\n" +
                "    <test>src/test/nl</test>\n" +
                "  </sources>\n" +
                "</configuration>";
        try (FileOutputStream out = new FileOutputStream(xmlFile)) {
            out.write(xmlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals("gemini", config.getProvider());
        assertEquals("xml-api-key-789", config.getGemini().getApiKey());
        assertEquals(95, config.getGemini().getTimeoutSeconds());
        assertEquals(new File(baseDir, "src/main/nl"), config.getSourceBase("main"));
        assertEquals(new File(baseDir, "src/test/nl"), config.getSourceBase("test"));
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

    @Test
    public void testModelRequestResponseLogLoading() {
        Properties props = new Properties();
        props.setProperty("reins.model.requestResponseLog", "true");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertTrue(config.getModel().isRequestResponseLog());
    }

    @Test
    public void testTrackingCleanupStaleCompiledFilesLoading() {
        Properties props = new Properties();
        props.setProperty("reins.tracking.cleanupStaleCompiledFiles", "true");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertTrue(config.getTracking().isCleanupStaleCompiledFiles());
    }

    @Test
    public void testYamlContextSourcesObjectListParsing() throws IOException {
        File baseDir = tempDir.toFile();
        File yamlFile = new File(baseDir, "reins.yaml");
        String yamlContent = "context:\n" +
                "  sources:\n" +
                "    - file: compilation/java/java-aplicacao-raiz.md\n" +
                "      pattern: \"**/aplicacao.md\"\n" +
                "      phase: \"*\"\n" +
                "    - file: compilation/java/java-dominio.md\n" +
                "      pattern: \"**/dominio/*.md\"\n" +
                "      phase: \"initial-context\"\n";
        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write(yamlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertNotNull(config.getContext());
        assertEquals(2, config.getContext().getSources().size());

        assertEquals(new File(baseDir, "compilation/java/java-aplicacao-raiz.md"), config.getContext().getSources().get(0).getFile());
        assertEquals("**/aplicacao.md", config.getContext().getSources().get(0).getPattern());
        assertEquals(java.util.List.of("*"), config.getContext().getSources().get(0).getPhases());

        assertEquals(new File(baseDir, "compilation/java/java-dominio.md"), config.getContext().getSources().get(1).getFile());
        assertEquals("**/dominio/*.md", config.getContext().getSources().get(1).getPattern());
        assertEquals(java.util.List.of("initial-context"), config.getContext().getSources().get(1).getPhases());
    }

    @Test
    public void testMultiSourceCliArgsLoading() {
        String[] args = {
                "--source", "domain/Customer.md",
                "--source", "domain/Order.md"
        };

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(args, null, null, baseDir);

        assertEquals("domain/Customer.md,domain/Order.md", config.getSource());
        assertEquals(java.util.List.of("domain/Customer.md", "domain/Order.md"), config.getSources());
        assertTrue(config.isExplicitSourceMode());
    }

    @Test
    public void testCommaSeparatedSourceString() {
        Properties props = new Properties();
        props.setProperty("reins.source", "domain/Customer.md, domain/Order.md");

        File baseDir = tempDir.toFile();
        ReinsConfig config = ReinsConfigLoader.load(null, props, null, baseDir);

        assertEquals(java.util.List.of("domain/Customer.md", "domain/Order.md"), config.getSources());
        assertTrue(config.isExplicitSourceMode());
    }

    @Test
    public void testSmallRyeConfigOrdinalPrecedence() throws IOException {
        File baseDir = tempDir.toFile();
        File yamlFile = new File(baseDir, "reins.yaml");
        String yamlContent = "provider: ollama\n" +
                "ollama:\n" +
                "  model: yaml-model\n";
        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write(yamlContent.getBytes());
        }

        Properties sysProps = new Properties();
        sysProps.setProperty("reins.ollama.model", "sys-model");

        String[] args = {"--ollama.model", "cli-model"};

        ReinsConfig config = ReinsConfigLoader.load(args, sysProps, null, baseDir);
        assertEquals("cli-model", config.getOllama().getModel());
    }

    @Test
    public void testSmallRyeConfigEnvVarInterpolation() throws IOException {
        File baseDir = tempDir.toFile();
        File yamlFile = new File(baseDir, "reins.yaml");
        String envKey = System.getenv().keySet().iterator().next();
        String expectedVal = System.getenv(envKey);

        String yamlContent = "provider: gemini\n" +
                "gemini:\n" +
                "  apiKey: \"${env." + envKey + "}\"\n";
        try (FileOutputStream out = new FileOutputStream(yamlFile)) {
            out.write(yamlContent.getBytes());
        }

        ReinsConfig config = ReinsConfigLoader.load(null, null, null, baseDir);
        assertEquals(expectedVal, config.getGemini().getApiKey());
    }

    @Test
    public void testSmallRyeConfigSourceOrdinalAndProperties() {
        Map<String, String> map = Map.of("key1", "val1");
        ReinsConfigLoader.ReinsConfigSource source = new ReinsConfigLoader.ReinsConfigSource("test", map, 250);

        assertEquals("test", source.getName());
        assertEquals(250, source.getOrdinal());
        assertEquals("val1", source.getValue("key1"));
        assertEquals(map, source.getProperties());
        assertTrue(source.getPropertyNames().contains("key1"));
    }
}

