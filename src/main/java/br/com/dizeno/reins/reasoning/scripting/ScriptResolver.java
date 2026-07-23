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

package br.com.dizeno.reins.reasoning.scripting;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

 
/**
 * ScriptResolver is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class ScriptResolver {

    static final String BUNDLED_CLASSPATH_PREFIX = "reasoning";
    private static final Pattern FORBIDDEN_BUILTINS = Pattern.compile("\\?(eval|interpret)\\b");

    private final Configuration configuration;
    private final Map<ScriptSource, Configuration> sourceConfigurations;
    private final File customScriptDir;
    private final String customClasspathPrefix;

     
    /**
     * Constructs a new instance of {@link ScriptResolver}.
     */
    public ScriptResolver() {
        this(null, null);
    }

     
    /**
     * Constructs a new instance of {@link ScriptResolver}.
     *
     * @param customScriptDir the custom script dir
     * @param customClasspathPrefix the custom classpath prefix
     */
    public ScriptResolver(File customScriptDir, String customClasspathPrefix) {
        this.customScriptDir = customScriptDir;
        this.customClasspathPrefix = customClasspathPrefix;
        this.configuration = buildConfiguration(customScriptDir, customClasspathPrefix);
        this.sourceConfigurations = buildSourceConfigurations(customScriptDir, customClasspathPrefix);
    }

     
    /**
     * Resolves the configured value or path.
     *
     * @param scriptName the script name
     * @param phaseType the phase type
     * @return the resolved or constructed object
     */
    public ScriptDescriptor resolve(String scriptName, PhaseType phaseType) throws IOException {
        
        ScriptSource source = determineSource(scriptName);
        String resolvedLocation = resolvedLocation(scriptName, source);
        validateForbiddenBuiltins(scriptName, source, resolvedLocation);

        Template template;
        try {
            template = configuration.getTemplate(scriptName);
        } catch (IOException e) {
            if (source == ScriptSource.CUSTOM_FILESYSTEM || source == ScriptSource.CUSTOM_CLASSPATH) {
                throw new IOException("Syntax error in custom script " + scriptName
                        + " at " + resolvedLocation + ": " + rootMessage(e), e);
            }
            throw new IOException("Failed to load script '" + scriptName + "' from " + resolvedLocation + ": " + e.getMessage(), e);
        }

        return new ScriptDescriptor(scriptName, phaseType, source, resolvedLocation, template);
    }

     
    /**
     * Resolves the configured value or path source order.
     *
     * @param scriptName the script name
     * @return the collection of elements
     */
    public List<ScriptSource> resolveSourceOrder(String scriptName) {
        List<ScriptSource> ordered = new ArrayList<>();
        if (hasSource(scriptName, ScriptSource.CUSTOM_FILESYSTEM)) {
            ordered.add(ScriptSource.CUSTOM_FILESYSTEM);
        }
        if (hasSource(scriptName, ScriptSource.CUSTOM_CLASSPATH)) {
            ordered.add(ScriptSource.CUSTOM_CLASSPATH);
        }
        if (hasSource(scriptName, ScriptSource.BUNDLED_DEFAULT)) {
            ordered.add(ScriptSource.BUNDLED_DEFAULT);
        }
        return ordered;
    }

     
    /**
     * Resolves the configured value or path invocation fallback order.
     *
     * @param scriptName the script name
     * @return the collection of elements
     */
    public List<ScriptSource> resolveInvocationFallbackOrder(String scriptName) {
        return resolveSourceOrder(scriptName);
    }

     
    /**
     * Resolves the configured value or path from source.
     *
     * @param scriptName the script name
     * @param phaseType the phase type
     * @param source the source
     * @return the resolved or constructed object
     */
    public ScriptDescriptor resolveFromSource(String scriptName,
                                              PhaseType phaseType,
                                              ScriptSource source) throws IOException {
        if (!hasSource(scriptName, source)) {
            throw new IOException("Script '" + scriptName + "' was not found in source " + source.name());
        }
        String resolvedLocation = resolvedLocation(scriptName, source);
        validateForbiddenBuiltins(scriptName, source, resolvedLocation);

        Configuration scopedConfiguration = sourceConfigurations.get(source);
        if (scopedConfiguration == null) {
            throw new IOException("No configuration available for script source " + source.name());
        }

        Template template;
        try {
            template = scopedConfiguration.getTemplate(scriptName);
        } catch (IOException e) {
            if (source == ScriptSource.CUSTOM_FILESYSTEM || source == ScriptSource.CUSTOM_CLASSPATH) {
                throw new IOException("Syntax error in custom script " + scriptName
                        + " at " + resolvedLocation + ": " + rootMessage(e), e);
            }
            throw new IOException("Failed to load script '" + scriptName + "' from " + resolvedLocation + ": " + e.getMessage(), e);
        }

        return new ScriptDescriptor(scriptName, phaseType, source, resolvedLocation, template);
    }

    private ScriptSource determineSource(String scriptName) {
        if (hasSource(scriptName, ScriptSource.CUSTOM_FILESYSTEM)) {
            return ScriptSource.CUSTOM_FILESYSTEM;
        }
        if (hasSource(scriptName, ScriptSource.CUSTOM_CLASSPATH)) {
            return ScriptSource.CUSTOM_CLASSPATH;
        }
        return ScriptSource.BUNDLED_DEFAULT;
    }

    private boolean hasSource(String scriptName, ScriptSource source) {
        return switch (source) {
            case CUSTOM_FILESYSTEM -> {
                if (customScriptDir == null || !customScriptDir.isDirectory()) {
                    yield false;
                }
                File candidate = new File(customScriptDir, scriptName);
                yield candidate.isFile();
            }
            case CUSTOM_CLASSPATH -> {
                if (customClasspathPrefix == null || customClasspathPrefix.isBlank()) {
                    yield false;
                }
                String resourcePath = customClasspathPrefix.replace('\\', '/')
                        + (customClasspathPrefix.endsWith("/") ? "" : "/")
                        + scriptName;
                yield getClass().getClassLoader().getResource(resourcePath) != null;
            }
            case BUNDLED_DEFAULT -> getClass().getClassLoader().getResource(BUNDLED_CLASSPATH_PREFIX + "/" + scriptName) != null;
        };
    }

    private String resolvedLocation(String scriptName, ScriptSource source) {
        return switch (source) {
            case CUSTOM_FILESYSTEM -> new File(customScriptDir, scriptName).getAbsolutePath();
            case CUSTOM_CLASSPATH -> (customClasspathPrefix != null
                    ? customClasspathPrefix + "/" : "") + scriptName;
            case BUNDLED_DEFAULT -> "classpath:" + BUNDLED_CLASSPATH_PREFIX + "/" + scriptName;
        };
    }

    private static Configuration buildConfiguration(File customScriptDir, String customClasspathPrefix) {
        Version incompatibleImprovements = Configuration.VERSION_2_3_34;
        Configuration cfg = new Configuration(incompatibleImprovements);

        
        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        cfg.setAPIBuiltinEnabled(false);
        cfg.setLogTemplateExceptions(false);

        
        DefaultObjectWrapper wrapper = new DefaultObjectWrapper(incompatibleImprovements);
        wrapper.setExposeFields(false);
        cfg.setObjectWrapper(wrapper);

        
        List<TemplateLoader> loaders = new ArrayList<>();

        if (customScriptDir != null && customScriptDir.isDirectory()) {
            try {
                loaders.add(new FileTemplateLoader(customScriptDir));
            } catch (IOException e) {
                
            }
        }

        if (customClasspathPrefix != null && !customClasspathPrefix.isBlank()) {
            String normalized = customClasspathPrefix.replace('\\', '/');
            if (!normalized.endsWith("/")) normalized += "/";
            loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), normalized));
        }

        
        loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(),
                BUNDLED_CLASSPATH_PREFIX));

        if (loaders.size() == 1) {
            cfg.setTemplateLoader(loaders.get(0));
        } else {
            cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setWhitespaceStripping(false);

        return cfg;
    }

    private static Map<ScriptSource, Configuration> buildSourceConfigurations(File customScriptDir,
                                                                               String customClasspathPrefix) {
        Map<ScriptSource, Configuration> map = new EnumMap<>(ScriptSource.class);
        map.put(ScriptSource.CUSTOM_FILESYSTEM,
                buildConfigurationForChainStart(customScriptDir, customClasspathPrefix, ScriptSource.CUSTOM_FILESYSTEM));
        map.put(ScriptSource.CUSTOM_CLASSPATH,
                buildConfigurationForChainStart(customScriptDir, customClasspathPrefix, ScriptSource.CUSTOM_CLASSPATH));
        map.put(ScriptSource.BUNDLED_DEFAULT,
                buildConfigurationForChainStart(customScriptDir, customClasspathPrefix, ScriptSource.BUNDLED_DEFAULT));
        return map;
    }

    private static Configuration buildConfigurationForChainStart(File customScriptDir,
                                                                 String customClasspathPrefix,
                                                                 ScriptSource startSource) {
        Version incompatibleImprovements = Configuration.VERSION_2_3_34;
        Configuration cfg = new Configuration(incompatibleImprovements);

        cfg.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        cfg.setAPIBuiltinEnabled(false);
        cfg.setLogTemplateExceptions(false);

        DefaultObjectWrapper wrapper = new DefaultObjectWrapper(incompatibleImprovements);
        wrapper.setExposeFields(false);
        cfg.setObjectWrapper(wrapper);

        List<TemplateLoader> loaders = new ArrayList<>();
        switch (startSource) {
            case CUSTOM_FILESYSTEM -> {
                if (customScriptDir != null && customScriptDir.isDirectory()) {
                    try {
                        loaders.add(new FileTemplateLoader(customScriptDir));
                    } catch (IOException ignored) {
                        
                    }
                }
                if (customClasspathPrefix != null && !customClasspathPrefix.isBlank()) {
                    String normalized = customClasspathPrefix.replace('\\', '/');
                    if (!normalized.endsWith("/")) {
                        normalized += "/";
                    }
                    loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), normalized));
                }
                loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), BUNDLED_CLASSPATH_PREFIX));
            }
            case CUSTOM_CLASSPATH -> {
                if (customClasspathPrefix != null && !customClasspathPrefix.isBlank()) {
                    String normalized = customClasspathPrefix.replace('\\', '/');
                    if (!normalized.endsWith("/")) {
                        normalized += "/";
                    }
                    loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), normalized));
                }
                loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), BUNDLED_CLASSPATH_PREFIX));
            }
            case BUNDLED_DEFAULT -> loaders.add(new ClassTemplateLoader(ScriptResolver.class.getClassLoader(), BUNDLED_CLASSPATH_PREFIX));
        }

        if (loaders.size() == 1) {
            cfg.setTemplateLoader(loaders.get(0));
        } else {
            cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0])));
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setWhitespaceStripping(false);
        return cfg;
    }

    Configuration getConfiguration() {
        return configuration;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? "unknown parsing error" : message;
    }

    private void validateForbiddenBuiltins(String scriptName,
                                           ScriptSource source,
                                           String resolvedLocation) throws IOException {
        String body = loadScriptBody(scriptName, source);
        if (body == null || body.isBlank()) {
            return;
        }
        if (FORBIDDEN_BUILTINS.matcher(body).find()) {
            throw new IOException("Forbidden built-in (?eval or ?interpret) in script "
                    + scriptName + " at " + resolvedLocation);
        }
    }

    private String loadScriptBody(String scriptName, ScriptSource source) throws IOException {
        return switch (source) {
            case CUSTOM_FILESYSTEM -> java.nio.file.Files.readString(new File(customScriptDir, scriptName).toPath());
            case CUSTOM_CLASSPATH -> readClasspathResource(resolveClasspathResourcePath(scriptName));
            case BUNDLED_DEFAULT -> readClasspathResource(BUNDLED_CLASSPATH_PREFIX + "/" + scriptName);
        };
    }

    private String resolveClasspathResourcePath(String scriptName) {
        String prefix = customClasspathPrefix == null ? "" : customClasspathPrefix.replace('\\', '/');
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + scriptName;
    }

    private String readClasspathResource(String resourcePath) throws IOException {
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(normalized)) {
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
