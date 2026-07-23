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
import br.com.dizeno.reins.run.config.settings.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * ReinsConfigLoader is part of the general application functions in the reins architecture.
 * Acts as a component managing reins config loader.
 */
public class ReinsConfigLoader {

    /**
     * Loads the resource or context.
     *
     * @param cliArgs the cli args
     * @param sysProps the sys props
     * @param libConfig the lib config
     * @param baseDir the base dir
     * @return the resulting config
     */
    public static ReinsConfig load(String[] cliArgs, Properties sysProps, Map<String, Object> libConfig, File baseDir) {
        return load(cliArgs, sysProps, null, libConfig, baseDir);
    }

    /**
     * Loads the resource or context.
     *
     * @param cliArgs the cli args
     * @param sysProps the sys props
     * @param libProps the lib props
     * @param libConfig the lib config
     * @param baseDir the base dir
     * @return the resulting config
     */
    public static ReinsConfig load(String[] cliArgs, Properties sysProps, Properties libProps, Map<String, Object> libConfig, File baseDir) {
        ReinsConfig config = new ReinsConfig();

        
        String configPath = null;
        if (cliArgs != null) {
            for (int i = 0; i < cliArgs.length - 1; i++) {
                if ("--config".equals(cliArgs[i])) {
                    configPath = cliArgs[i + 1];
                    break;
                }
            }
        }

        File configFile = null;
        if (configPath != null) {
            configFile = resolveFile(configPath, baseDir);
            if (!configFile.exists()) {
                throw new IllegalArgumentException("Specified configuration file does not exist: " + configPath);
            }
        } else {
            
            String[] defaults = {"reins.yaml", "reins.yml", "reins.json", "reins.xml", "reins.properties"};
            for (String df : defaults) {
                File f = new File(baseDir, df);
                if (f.exists()) {
                    configFile = f;
                    break;
                }
            }
        }

        
        Map<String, String> fileProps = new HashMap<>();
        if (configFile != null) {
            loadFileProperties(configFile, fileProps, baseDir);
        }

        
        Map<String, String> sysPropsMap = new HashMap<>();
        if (sysProps != null) {
            for (String key : sysProps.stringPropertyNames()) {
                if (key.startsWith("reins.")) {
                    sysPropsMap.put(key, sysProps.getProperty(key));
                }
            }
        }

        
        Map<String, String> overlayProps = new HashMap<>();
        if (libProps != null) {
            for (String key : libProps.stringPropertyNames()) {
                overlayProps.put(key, libProps.getProperty(key));
            }
        }
        if (libConfig != null) {
            flattenMap(libConfig, overlayProps, "");
        }
        if (cliArgs != null) {
            parseCliArgs(cliArgs, overlayProps);
        }

        
        
        for (Map.Entry<String, String> e : fileProps.entrySet()) {
            bindProperty(config, e.getKey(), e.getValue(), baseDir);
        }
        
        for (Map.Entry<String, String> e : sysPropsMap.entrySet()) {
            bindProperty(config, e.getKey(), e.getValue(), baseDir);
        }
        
        for (Map.Entry<String, String> e : overlayProps.entrySet()) {
            bindProperty(config, e.getKey(), e.getValue(), baseDir);
        }

        if (config.getMainNlRoot() == null) {
            config.setMainNlRoot(new File(baseDir, "src/main/nl"));
        }
        if (config.getTestNlRoot() == null) {
            config.setTestNlRoot(new File(baseDir, "src/test/nl"));
        }
        if (config.getScanRoots() == null || config.getScanRoots().isEmpty()) {
            config.setDefaultScanRoots(true);
            List<File> defaultRoots = new ArrayList<>();
            defaultRoots.add(config.getMainNlRoot());
            defaultRoots.add(config.getTestNlRoot());
            config.setScanRoots(defaultRoots);
        }

        if (config.getProjectContextFile() == null) {
            config.setProjectContextFile(new File(baseDir, "project.md"));
        }
        if (config.getTarget() == null) {
            config.setTarget(new TargetSettings());
        }

        return config;
    }

    private static void parseCliArgs(String[] args, Map<String, String> target) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (key.equals("config")) {
                    i++; 
                    continue;
                }
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    target.put(key, args[i + 1]);
                    i++;
                } else {
                    target.put(key, "true");
                }
            }
        }
    }

    private static void flattenMap(Map<String, Object> source, Map<String, String> target, String prefix) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) val;
                flattenMap(subMap, target, key);
            } else if (val instanceof List) {
                List<?> list = (List<?>) val;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(list.get(i).toString());
                }
                target.put(key, sb.toString());
            } else if (val != null) {
                target.put(key, val.toString());
            }
        }
    }

    private static void loadFileProperties(File file, Map<String, String> target, File baseDir) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".properties")) {
                Properties p = new Properties();
                try (InputStream in = new FileInputStream(file)) {
                    p.load(in);
                }
                for (String key : p.stringPropertyNames()) {
                    target.put(key, p.getProperty(key));
                }
            } else if (name.endsWith(".json")) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(file, Map.class);
                flattenMap(map, target, "");
            } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                Yaml yaml = new Yaml();
                try (InputStream in = new FileInputStream(file)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = yaml.load(in);
                    if (map != null) {
                        flattenMap(map, target, "");
                    }
                }
            } else if (name.endsWith(".xml")) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(file);
                Element root = doc.getDocumentElement();
                String rootName = root.getTagName();
                if (rootName.equals("reins") || rootName.equals("configuration")) {
                    parseXmlElement(root, target, "");
                } else {
                    parseXmlElement(root, target, rootName);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load configuration file: " + file.getAbsolutePath(), ex);
        }
    }

    private static void parseXmlElement(Element element, Map<String, String> target, String prefix) {
        NodeList children = element.getChildNodes();
        boolean hasChildElements = false;
        List<String> listValues = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
                Element childEl = (Element) child;
                String childName = childEl.getTagName();
                if (childName.equals("scanRoot") || childName.equals("source")) {
                    listValues.add(childEl.getTextContent().trim());
                } else {
                    String nextPrefix = prefix.isEmpty() ? childName : prefix + "." + childName;
                    parseXmlElement(childEl, target, nextPrefix);
                }
            }
        }
        if (!listValues.isEmpty()) {
            target.put(prefix, String.join(",", listValues));
        } else if (!hasChildElements) {
            String text = element.getTextContent().trim();
            if (!text.isEmpty()) {
                target.put(prefix, text);
            }
        }
    }

    private static void bindProperty(ReinsConfig config, String key, String value, File baseDir) {
        String cleanKey = key.startsWith("reins.") ? key.substring(6) : key;
        switch (cleanKey) {
            case "provider":
                config.setProvider(value);
                break;
            case "scanRoots":
                config.setScanRoots(parseFileList(value, baseDir));
                break;
            case "includePattern":
                config.setIncludePattern(value);
                break;

            case "failOnError":
                config.setFailOnError(Boolean.parseBoolean(value));
                break;
            case "verbose":
                config.setVerbose(Boolean.parseBoolean(value));
                break;
            case "dryRun":
                config.setDryRun(Boolean.parseBoolean(value));
                break;
            case "validateAll":
                config.setValidateAll(Boolean.parseBoolean(value));
                break;
            case "projectContextFile":
                config.setProjectContextFile(resolveFile(value, baseDir));
                break;
            case "enableProjectInference":
                config.setEnableProjectInference(Boolean.parseBoolean(value));
                break;
            case "mainNlRoot":
                config.setMainNlRoot(resolveFile(value, baseDir));
                break;
            case "testNlRoot":
                config.setTestNlRoot(resolveFile(value, baseDir));
                break;
            case "source":
                config.setSource(value);
                config.setExplicitSourceMode(value != null && !value.trim().isEmpty());
                break;
            case "gemini.apiKey":
                config.getGemini().setApiKey(value);
                break;
            case "gemini.model":
                config.getGemini().setModel(value);
                break;
            case "gemini.endpoint":
                config.getGemini().setEndpoint(value);
                break;
            case "gemini.timeoutSeconds":
                config.getGemini().setTimeoutSeconds(Integer.parseInt(value));
                break;
            case "gemini.retryAttempts":
                config.getGemini().setRetryAttempts(Integer.parseInt(value));
                break;
            case "gemini.emptyResponseRetryDelayMs":
                config.getGemini().setEmptyResponseRetryDelayMs(Integer.parseInt(value));
                break;
            case "gemini.maximumTurns":
                config.getGemini().setMaximumTurns(Integer.parseInt(value));
                break;
            case "gemini.generation.temperature":
                getOrInitGeminiGen(config).setTemperature(Float.parseFloat(value));
                break;
            case "gemini.generation.topP":
                getOrInitGeminiGen(config).setTopP(Float.parseFloat(value));
                break;
            case "gemini.generation.topK":
                getOrInitGeminiGen(config).setTopK(Integer.parseInt(value));
                break;
            case "gemini.generation.presencePenalty":
                getOrInitGeminiGen(config).setPresencePenalty(Float.parseFloat(value));
                break;
            case "gemini.generation.frequencyPenalty":
                getOrInitGeminiGen(config).setFrequencyPenalty(Float.parseFloat(value));
                break;
            case "ollama.model":
                config.getOllama().setModel(value);
                break;
            case "ollama.endpoint":
                config.getOllama().setEndpoint(value);
                break;
            case "ollama.apiKey":
                config.getOllama().setApiKey(value);
                break;
            case "ollama.timeoutSeconds":
                config.getOllama().setTimeoutSeconds(Integer.parseInt(value));
                break;
            case "ollama.retryAttempts":
                config.getOllama().setRetryAttempts(Integer.parseInt(value));
                break;
            case "target.project":
                getOrInitTarget(config).setProject(resolveFile(value, baseDir));
                break;
            case "target.root":
                getOrInitTarget(config).setRoot(resolveFile(value, baseDir));
                break;
            case "target.main":
                getOrInitTarget(config).setMain(value);
                break;
            case "target.test":
                getOrInitTarget(config).setTest(value);
                break;
            case "reasoning.maxTurns":
                config.getReasoning().setMaxTurns(Integer.parseInt(value));
                break;
            case "reasoning.maxReferenceDepth":
                config.getReasoning().setMaxReferenceDepth(Integer.parseInt(value));
                break;
            case "reasoning.scriptsPath":
                config.getReasoning().setScriptsPath(value);
                break;
            case "reasoning.thinkingOutLoud":
                config.getReasoning().setThinkingOutLoud(Boolean.parseBoolean(value));
                break;
            case "reasoning.logSystemContext":
                config.getReasoning().setLogSystemContext(Boolean.parseBoolean(value));
                break;
            case "reasoning.logParseErrorRecovery":
                config.getReasoning().setLogParseErrorRecovery(Boolean.parseBoolean(value));
                break;
            case "reasoning.enableReasoningLog":
                config.getReasoning().setEnableReasoningLog(Boolean.parseBoolean(value));
                break;
            case "reasoning.turnCountNote":
                config.getReasoning().setTurnCountNote(Boolean.parseBoolean(value));
                break;
            case "recompileOn.markdownReferences":
                config.getRecompileOn().setMarkdownReferences(Boolean.parseBoolean(value));
                break;
            case "recompileOn.inspectedFiles":
                config.getRecompileOn().setInspectedFiles(Boolean.parseBoolean(value));
                break;
            case "recompileOn.compiledFiles":
                config.getRecompileOn().setCompiledFiles(Boolean.parseBoolean(value));
                break;
            case "eagerlyProvide.previouslyCompiledFiles":
                config.getEagerlyProvide().setPreviouslyCompiledFiles(Boolean.parseBoolean(value));
                break;
            case "eagerlyProvide.previouslyInspectedFiles":
                config.getEagerlyProvide().setPreviouslyInspectedFiles(Boolean.parseBoolean(value));
                break;
            case "eagerlyProvide.maxAttachmentSizeBytes":
                config.getEagerlyProvide().setMaxAttachmentSizeBytes(Long.parseLong(value));
                break;
            case "logging.scriptsEvents":
                config.getLogging().setScriptsEvents(Boolean.parseBoolean(value));
                break;
            case "logging.eagerlyProvided":
                config.getLogging().setEagerlyProvided(Boolean.parseBoolean(value));
                break;
            case "logging.fileListingAndReading":
                config.getLogging().setFileListingAndReading(Boolean.parseBoolean(value));
                break;
            case "logging.fileMutating":
                config.getLogging().setFileMutating(Boolean.parseBoolean(value));
                break;
            case "logging.scriptRun":
                config.getLogging().setScriptRun(Boolean.parseBoolean(value));
                break;
            case "logging.selectionReason":
                config.getLogging().setSelectionReason(Boolean.parseBoolean(value));
                break;
            case "logging.result":
                config.getLogging().setResult(Boolean.parseBoolean(value));
                break;
            case "logging.trackingFile":
                config.getLogging().setTrackingFile(Boolean.parseBoolean(value));
                break;
            case "logging.llmProvider":
                config.getLogging().setLlmProvider(Boolean.parseBoolean(value));
                break;
            case "context.includeProjectFiles":
                config.getContext().setIncludeProjectFiles(Boolean.parseBoolean(value));
                break;
            case "context.attachReferencedFiles":
                config.getContext().setAttachReferencedFiles(Boolean.parseBoolean(value));
                break;
            case "context.cachedContent":
                config.getContext().setCachedContent(Boolean.parseBoolean(value));
                break;
            case "context.allowScriptedMessageData":
                config.getContext().setAllowScriptedMessageData(Boolean.parseBoolean(value));
                break;
            case "context.allowScriptedAttachments":
                config.getContext().setAllowScriptedAttachments(Boolean.parseBoolean(value));
                break;
            case "context.sources":
                config.getContext().setSources(parseFileList(value, baseDir));
                break;
            case "context.referencesTreeDepth":
                config.getContext().setReferencesTreeDepth(value);
                break;
            case "tooling.main":
                config.getTooling().setMain(value);
                break;
            case "tooling.test":
                config.getTooling().setTest(value);
                break;
            case "tooling.target":
                config.getTooling().setTarget(value);
                break;
            case "tooling.scriptPath":
                config.getTooling().setScriptPath(value);
                break;
            case "tooling.addReasoningNotes":
                config.getTooling().setAddReasoningNotes(Boolean.parseBoolean(value));
                break;
            case "tracking.freezeState":
                config.getTracking().setFreezeState(Boolean.parseBoolean(value));
                break;
        }
    }

    private static GenerationSettings getOrInitGeminiGen(ReinsConfig config) {
        if (config.getGemini().getGeneration() == null) {
            config.getGemini().setGeneration(new GenerationSettings());
        }
        return config.getGemini().getGeneration();
    }

    private static TargetSettings getOrInitTarget(ReinsConfig config) {
        if (config.getTarget() == null) {
            config.setTarget(new TargetSettings());
        }
        return config.getTarget();
    }

    private static List<File> parseFileList(String commaSeparated, File baseDir) {
        List<File> list = new ArrayList<>();
        for (String s : commaSeparated.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                list.add(resolveFile(trimmed, baseDir));
            }
        }
        return list;
    }

    private static File resolveFile(String path, File baseDir) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(baseDir, path);
    }
}
