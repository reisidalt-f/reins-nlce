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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Map<String, String> rawFileProps = new HashMap<>();
            loadFileProperties(configFile, rawFileProps, baseDir);
            for (Map.Entry<String, String> entry : rawFileProps.entrySet()) {
                fileProps.put(cleanKey(entry.getKey()), entry.getValue());
            }
        }

        Map<String, String> sysPropsMap = new HashMap<>();
        if (sysProps != null) {
            for (String key : sysProps.stringPropertyNames()) {
                if (key.startsWith("reins.")) {
                    sysPropsMap.put(cleanKey(key), sysProps.getProperty(key));
                }
            }
        }

        Map<String, String> overlayProps = new HashMap<>();
        if (libProps != null) {
            for (String key : libProps.stringPropertyNames()) {
                overlayProps.put(cleanKey(key), libProps.getProperty(key));
            }
        }
        if (libConfig != null) {
            Map<String, String> rawLibConfig = new HashMap<>();
            flattenMap(libConfig, rawLibConfig, "");
            for (Map.Entry<String, String> entry : rawLibConfig.entrySet()) {
                overlayProps.put(cleanKey(entry.getKey()), entry.getValue());
            }
        }
        if (cliArgs != null) {
            Map<String, String> rawCliArgs = new HashMap<>();
            parseCliArgs(cliArgs, rawCliArgs);
            for (Map.Entry<String, String> entry : rawCliArgs.entrySet()) {
                overlayProps.put(cleanKey(entry.getKey()), entry.getValue());
            }
        }

        java.util.Set<String> allKeys = new java.util.LinkedHashSet<>();
        allKeys.addAll(fileProps.keySet());
        allKeys.addAll(sysPropsMap.keySet());
        allKeys.addAll(overlayProps.keySet());

        io.smallrye.config.SmallRyeConfigBuilder builder = new io.smallrye.config.SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(new EnvWithPrefixConfigSource())
                .withSources(new ReinsConfigSource("ReinsFileConfig", fileProps, 200))
                .withSources(new ReinsConfigSource("ReinsSysConfig", sysPropsMap, 300))
                .withSources(new ReinsConfigSource("ReinsOverlayConfig", overlayProps, 500));

        io.smallrye.config.SmallRyeConfig smallRyeConfig = builder.build();

        for (String key : allKeys) {
            String value;
            try {
                value = smallRyeConfig.getValue(key, String.class);
            } catch (Exception ex) {
                value = smallRyeConfig.getRawValue(key);
            }
            if (value != null) {
                bindProperty(config, key, value, baseDir);
            }
        }

        if (config.getTarget() == null) {
            config.setTarget(new TargetSettings());
        }

        return config;
    }

    private static String cleanKey(String key) {
        return key.startsWith("reins.") ? key.substring(6) : key;
    }

    public static class EnvWithPrefixConfigSource implements org.eclipse.microprofile.config.spi.ConfigSource {
        private final Map<String, String> envMap = new HashMap<>();

        public EnvWithPrefixConfigSource() {
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                envMap.put("env." + entry.getKey(), entry.getValue());
                envMap.put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Map<String, String> getProperties() {
            return envMap;
        }

        @Override
        public java.util.Set<String> getPropertyNames() {
            return envMap.keySet();
        }

        @Override
        public int getOrdinal() {
            return 300;
        }

        @Override
        public String getValue(String propertyName) {
            if (propertyName.startsWith("env.")) {
                return System.getenv(propertyName.substring(4));
            }
            return System.getenv(propertyName);
        }

        @Override
        public String getName() {
            return "EnvWithPrefixConfigSource";
        }
    }

    public static class ReinsConfigSource implements org.eclipse.microprofile.config.spi.ConfigSource {
        private final String name;
        private final Map<String, String> properties;
        private final int ordinal;

        public ReinsConfigSource(String name, Map<String, String> properties, int ordinal) {
            this.name = name;
            this.properties = properties;
            this.ordinal = ordinal;
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }

        @Override
        public java.util.Set<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            String clean = propertyName.startsWith("reins.") ? propertyName.substring(6) : propertyName;
            return properties.get(clean);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static void parseCliArgs(String[] args, Map<String, String> target) {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-D")) {
                String prop = arg.substring(2);
                int eqIdx = prop.indexOf('=');
                if (eqIdx > 0) {
                    target.put(prop.substring(0, eqIdx), prop.substring(eqIdx + 1));
                } else {
                    target.put(prop, "true");
                }
            } else if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (key.equals("config")) {
                    i++; 
                    continue;
                }
                if (i + 1 < args.length && !args[i + 1].startsWith("--") && !args[i + 1].startsWith("-D")) {
                    if ("source".equals(key) && target.containsKey(key)) {
                        target.put(key, target.get(key) + "," + args[i + 1]);
                    } else {
                        target.put(key, args[i + 1]);
                    }
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
                boolean isListOfMaps = false;
                for (Object item : list) {
                    if (item instanceof Map) {
                        isListOfMaps = true;
                        break;
                    }
                }
                if (isListOfMaps) {
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        String itemKey = key + "." + i;
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> subMap = (Map<String, Object>) item;
                            flattenMap(subMap, target, itemKey);
                        } else if (item != null) {
                            target.put(itemKey, item.toString());
                        }
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append(list.get(i).toString());
                    }
                    target.put(key, sb.toString());
                }
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

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{env\\.([^}]+)\\}");

    public static String interpolateEnvVars(String value) {
        if (value == null || !value.contains("${env.")) {
            return value;
        }
        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String envVal = System.getenv(varName);
            if (envVal == null) {
                envVal = "";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(envVal));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void bindProperty(ReinsConfig config, String key, String rawValue, File baseDir) {
        String value = interpolateEnvVars(rawValue);
        String cleanKey = key.startsWith("reins.") ? key.substring(6) : key;
        if (cleanKey.startsWith("context.sources.")) {
            String subKey = cleanKey.substring("context.sources.".length());
            bindContextSourceProperty(config, subKey, value, baseDir);
            return;
        }
        if (cleanKey.startsWith("source.")) {
            String baseName = cleanKey.substring("source.".length());
            if (!baseName.isBlank()) {
                config.setSourceBase(baseName, resolveFile(value, baseDir));
            }
            return;
        }
        if (cleanKey.startsWith("sources.")) {
            String baseName = cleanKey.substring("sources.".length());
            if (!baseName.isBlank()) {
                config.setSourceBase(baseName, resolveFile(value, baseDir));
            }
            return;
        }
        if (cleanKey.startsWith("target.")) {
            String baseName = cleanKey.substring("target.".length());
            if (!baseName.isBlank()) {
                getOrInitTarget(config).setTargetBase(baseName, value);
            }
            return;
        }
        if (cleanKey.startsWith("tooling.")) {
            String baseName = cleanKey.substring("tooling.".length());
            if ("scriptPath".equals(baseName)) {
                config.getTooling().setScriptPath(value);
            } else if ("addReasoningNotes".equals(baseName)) {
                config.getTooling().setAddReasoningNotes(Boolean.parseBoolean(value));
            } else if ("grantFileOwnership".equalsIgnoreCase(baseName)) {
                config.getTooling().setGrantFileOwnership(Boolean.parseBoolean(value));
            } else if (!baseName.isBlank()) {
                config.getTooling().setToolingBase(baseName, value);
            }
            return;
        }
        if (cleanKey.startsWith("tracking.")) {
            String baseName = cleanKey.substring("tracking.".length());
            if ("freezeState".equalsIgnoreCase(baseName)) {
                config.getTracking().setFreezeState(Boolean.parseBoolean(value));
            } else if ("cleanupStaleCompiledFiles".equalsIgnoreCase(baseName)) {
                config.getTracking().setCleanupStaleCompiledFiles(Boolean.parseBoolean(value));
            }
            return;
        }
        if (cleanKey.startsWith("model.")) {
            String baseName = cleanKey.substring("model.".length());
            if ("requestResponseLog".equalsIgnoreCase(baseName)) {
                config.getModel().setRequestResponseLog(Boolean.parseBoolean(value));
            }
            return;
        }
        if (cleanKey.startsWith("build.")) {
            String baseName = cleanKey.substring("build.".length());
            if ("compilationThreads".equalsIgnoreCase(baseName)) {
                config.setCompilationThreads(Integer.parseInt(value));
            } else if ("freshCompilation".equalsIgnoreCase(baseName)) {
                boolean val = Boolean.parseBoolean(value);
                config.setFreshCompilation(val);
            }
            return;
        }
        if ("skipTest".equalsIgnoreCase(cleanKey) || "skipTests".equalsIgnoreCase(cleanKey)) {
            config.setSkipTest(Boolean.parseBoolean(value));
            return;
        }

        switch (cleanKey) {
            case "provider":
                config.setProvider(value);
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
            case "compilationThreads":
            case "compilationthreads":
                config.setCompilationThreads(Integer.parseInt(value));
                break;
            case "freshCompilation":
            case "freshcompilation":
                config.setFreshCompilation(Boolean.parseBoolean(value));
                break;
            case "source":
            case "sources":
                if (config.getSource() != null && !config.getSource().isBlank()) {
                    config.setSource(config.getSource() + "," + value);
                } else {
                    config.setSource(value);
                }
                config.setExplicitSourceMode(config.getSource() != null && !config.getSource().trim().isEmpty());
                break;
            case "note":
                config.setNote(value);
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
            case "openai.model":
                config.getOpenai().setModel(value);
                break;
            case "openai.endpoint":
                config.getOpenai().setEndpoint(value);
                break;
            case "openai.apiKey":
                config.getOpenai().setApiKey(value);
                break;
            case "openai.timeoutSeconds":
                config.getOpenai().setTimeoutSeconds(Integer.parseInt(value));
                break;
            case "openai.retryAttempts":
                config.getOpenai().setRetryAttempts(Integer.parseInt(value));
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
            case "reasoning.summarizeCycleTurns":
            case "reasoning.summarizeTurnInterval":
                config.getReasoning().setSummarizeCycleTurns(Integer.parseInt(value));
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
            case "logging.sourceTag":
            case "logging.sourcetag":
                config.getLogging().setSourceTag(Boolean.parseBoolean(value));
                break;
            case "context.referencesTree.attachFiles":
                config.getContext().getReferencesTree().setAttachFiles(Boolean.parseBoolean(value));
                break;
            case "context.referencesTree.depth":
                config.getContext().getReferencesTree().setDepth(value);
                break;
            case "context.referencesTree.maxDepth":
                config.getContext().getReferencesTree().setMaxDepth(Integer.parseInt(value));
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
            case "context.compiledFiles":
                config.getContext().setCompiledFiles(Boolean.parseBoolean(value));
                break;
            case "context.inspectedFiles":
                config.getContext().setInspectedFiles(Boolean.parseBoolean(value));
                break;
            case "context.sources":
                bindContextSourceProperty(config, "", value, baseDir);
                break;
            case "context.plainAttachmentExtensions":
                config.getContext().setPlainAttachmentExtensions(parseStringList(value));
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
            case "tooling.grantFileOwnership":
            case "tooling.grantfileownership":
                config.getTooling().setGrantFileOwnership(Boolean.parseBoolean(value));
                break;
            case "tracking.freezeState":
            case "tracking.freezestate":
                config.getTracking().setFreezeState(Boolean.parseBoolean(value));
                break;
            case "tracking.cleanupStaleCompiledFiles":
            case "tracking.cleanupstalecompiledfiles":
                config.getTracking().setCleanupStaleCompiledFiles(Boolean.parseBoolean(value));
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

    private static List<String> parseStringList(String commaSeparated) {
        List<String> list = new ArrayList<>();
        if (commaSeparated != null) {
            for (String s : commaSeparated.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
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

    private static void bindContextSourceProperty(ReinsConfig config, String subKey, String value, File baseDir) {
        if (value == null || value.isBlank()) {
            return;
        }
        List<ContextSourceSpec> specs = config.getContext().getSources();
        if (subKey != null && !subKey.isBlank()) {
            int firstDot = subKey.indexOf('.');
            if (firstDot > 0) {
                String indexStr = subKey.substring(0, firstDot);
                String property = subKey.substring(firstDot + 1);
                try {
                    int idx = Integer.parseInt(indexStr);
                    while (specs.size() <= idx) {
                        specs.add(new ContextSourceSpec());
                    }
                    ContextSourceSpec spec = specs.get(idx);
                    if ("file".equalsIgnoreCase(property) || "path".equalsIgnoreCase(property)) {
                        spec.setFile(resolveFile(value, baseDir));
                    } else if ("pattern".equalsIgnoreCase(property)) {
                        spec.setPattern(value);
                    } else if ("phase".equalsIgnoreCase(property) || "phases".equalsIgnoreCase(property)) {
                        spec.setPhaseString(value);
                    }
                    return;
                } catch (NumberFormatException ignored) {
                }
            }

            try {
                int idx = Integer.parseInt(subKey);
                while (specs.size() <= idx) {
                    specs.add(new ContextSourceSpec());
                }
                specs.get(idx).setFile(resolveFile(value, baseDir));
                return;
            } catch (NumberFormatException ignored) {
            }
        }

        for (String fileStr : parseStringList(value)) {
            specs.add(new ContextSourceSpec(resolveFile(fileStr, baseDir)));
        }
    }

    private static File resolveFile(String path, File baseDir) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(baseDir, path);
    }
}
