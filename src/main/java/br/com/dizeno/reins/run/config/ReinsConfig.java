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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReinsConfig is part of the general application functions in the reins
 * architecture.
 * Stores configuration settings for reins, mapped from Maven properties or YAML
 * configurations.
 */
public class ReinsConfig {
    private GeminiSettings gemini = new GeminiSettings();
    private OllamaSettings ollama = new OllamaSettings();
    private OpenAiSettings openai = new OpenAiSettings();
    private String provider;
    private Map<String, File> sourceBases = new LinkedHashMap<>();
    private boolean skipTest = false;
    private String includePattern = "**/*.md";
    private boolean failOnError;
    private boolean verbose;
    private boolean dryRun;
    private boolean validateAll;
    private int compilationThreads = 1;
    private boolean freshCompilation = false;
    private TargetSettings target;
    private ReasoningSettings reasoning = new ReasoningSettings();
    private RecompileOnSettings recompileOn = new RecompileOnSettings();
    private EagerlyProvideSettings eagerlyProvide = new EagerlyProvideSettings();
    private LogSettings log = new LogSettings();
    private LoggingSettings logging = new LoggingSettings();
    private ModelSettings model = new ModelSettings();
    private BuildSettings build = new BuildSettings();

    private ToolingSettings tooling = new ToolingSettings();
    private FileToolsSettings fileTools = new FileToolsSettings();
    private ContextSettings context = new ContextSettings();
    private TrackingSettings tracking = new TrackingSettings();
    private String source;
    private String note;
    private boolean explicitSourceMode = false;

    /**
     * Gets the gemini.
     *
     * @return the collection of elements
     */
    public GeminiSettings getGemini() {
        return gemini;
    }

    /**
     * Sets the gemini.
     *
     * @param gemini the gemini
     */
    public void setGemini(GeminiSettings gemini) {
        this.gemini = gemini;
    }

    /**
     * Gets the ollama.
     *
     * @return the collection of elements
     */
    public OllamaSettings getOllama() {
        return ollama;
    }

    /**
     * Sets the ollama.
     *
     * @param ollama the ollama
     */
    public void setOllama(OllamaSettings ollama) {
        this.ollama = ollama;
    }

    /**
     * Gets the openai.
     *
     * @return the openai settings
     */
    public OpenAiSettings getOpenai() {
        return openai;
    }

    /**
     * Sets the openai.
     *
     * @param openai the openai settings
     */
    public void setOpenai(OpenAiSettings openai) {
        this.openai = openai;
    }

    /**
     * Gets the provider.
     *
     * @return the string result
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Sets the provider.
     *
     * @param provider the provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Resolves the active model name based on the selected provider.
     *
     * @return the resolved model name, or null if not configured
     */
    public String resolveModel() {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        String normalizedProvider = provider.trim().toLowerCase();
        if ("ollama".equals(normalizedProvider)) {
            return ollama != null ? ollama.getModel() : null;
        } else if ("openai".equals(normalizedProvider)) {
            return openai != null ? openai.getModel() : null;
        } else if ("stub".equals(normalizedProvider)) {
            return "stub";
        } else if ("gemini".equals(normalizedProvider)) {
            return gemini != null ? gemini.getModel() : null;
        } else {
            return null;
        }
    }

    /**
     * Resolves the active provider settings based on the selected provider.
     *
     * @return the resolved ModelProviderSetting instance, or null if not configured
     */
    public ModelProviderSetting resolveActiveModelSettings() {
        if (provider == null || provider.isBlank()) {
            return gemini;
        }
        String normalizedProvider = provider.trim().toLowerCase();
        if ("ollama".equals(normalizedProvider)) {
            return ollama;
        } else if ("openai".equals(normalizedProvider)) {
            return openai;
        } else if ("gemini".equals(normalizedProvider)) {
            return gemini;
        } else {
            return gemini;
        }
    }



    /**
     * Gets the scan roots derived dynamically from configured source bases.
     *
     * @return the collection of elements
     */
    public List<File> getScanRoots() {
        List<File> roots = new ArrayList<>();
        if (sourceBases != null) {
            for (Map.Entry<String, File> entry : sourceBases.entrySet()) {
                String baseName = entry.getKey();
                if (entry.getValue() == null) {
                    continue;
                }
                // Only scan main and test bases implicitly.
                // Custom bases (e.g. doc) are included only when an explicit source is provided.
                if (!"main".equalsIgnoreCase(baseName) && !"test".equalsIgnoreCase(baseName)) {
                    continue;
                }
                if (skipTest && "test".equalsIgnoreCase(baseName)) {
                    continue;
                }
                roots.add(entry.getValue());
            }
        }
        return roots;
    }

    public Map<String, File> getSourceBases() {
        return sourceBases;
    }

    public void setSourceBases(Map<String, File> sourceBases) {
        this.sourceBases = sourceBases != null ? new LinkedHashMap<>(sourceBases) : new LinkedHashMap<>();
    }

    public File getSourceBase(String name) {
        return name == null ? null : sourceBases.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public void setSourceBase(String name, File file) {
        if (name != null && file != null) {
            this.sourceBases.put(name.toLowerCase(java.util.Locale.ROOT), file);
        }
    }

    public boolean hasSourceBase(String name) {
        return name != null && sourceBases.containsKey(name.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isSkipTest() {
        return skipTest;
    }

    public void setSkipTest(boolean skipTest) {
        this.skipTest = skipTest;
    }

    public String getIncludePattern() {
        return includePattern;
    }

    public void setIncludePattern(String includePattern) {
        this.includePattern = includePattern;
    }

    /**
     * Checks if the component is fail on error.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFailOnError() {
        return failOnError;
    }

    /**
     * Sets the fail on error.
     *
     * @param failOnError the fail on error
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Checks if the component is verbose.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Sets the verbose.
     *
     * @param verbose the verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Checks if the component is dry run.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Sets the dry run.
     *
     * @param dryRun the dry run
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Checks if the component is validate all.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValidateAll() {
        return validateAll;
    }

    /**
     * Sets the validate all.
     *
     * @param validateAll the validate all
     */
    public void setValidateAll(boolean validateAll) {
        this.validateAll = validateAll;
    }

    public int getCompilationThreads() {
        return build != null ? build.getCompilationThreads() : compilationThreads;
    }

    public void setCompilationThreads(int compilationThreads) {
        this.compilationThreads = compilationThreads;
        if (this.build == null) {
            this.build = new BuildSettings();
        }
        this.build.setCompilationThreads(compilationThreads);
    }

    /**
     * Checks if freshCompilation is enabled.
     *
     * @return true if freshCompilation is enabled, false otherwise
     */
    public boolean isFreshCompilation() {
        return build != null ? build.isFreshCompilation() : freshCompilation;
    }

    /**
     * Sets the freshCompilation parameter.
     *
     * @param freshCompilation whether to perform a fresh compilation
     */
    public void setFreshCompilation(boolean freshCompilation) {
        this.freshCompilation = freshCompilation;
        if (this.build == null) {
            this.build = new BuildSettings();
        }
        this.build.setFreshCompilation(freshCompilation);
    }



    /**
     * Gets the reasoning.
     *
     * @return the collection of elements
     */
    public ReasoningSettings getReasoning() {
        return reasoning;
    }

    /**
     * Sets the reasoning.
     *
     * @param reasoning the reasoning
     */
    public void setReasoning(ReasoningSettings reasoning) {
        this.reasoning = reasoning;
    }

    /**
     * Gets the recompile on.
     *
     * @return the collection of elements
     */
    public RecompileOnSettings getRecompileOn() {
        return recompileOn;
    }

    /**
     * Sets the recompile on.
     *
     * @param recompileOn the recompile on
     */
    public void setRecompileOn(RecompileOnSettings recompileOn) {
        this.recompileOn = recompileOn;
    }



    /**
     * Gets the target.
     *
     * @return the collection of elements
     */
    public TargetSettings getTarget() {
        return target;
    }

    /**
     * Sets the target.
     *
     * @param target the target
     */
    public void setTarget(TargetSettings target) {
        this.target = target;
    }



    /**
     * Gets the eagerly provide.
     *
     * @return the collection of elements
     */
    public EagerlyProvideSettings getEagerlyProvide() {
        return eagerlyProvide;
    }

    /**
     * Sets the eagerly provide.
     *
     * @param eagerlyProvide the eagerly provide
     */
    public void setEagerlyProvide(EagerlyProvideSettings eagerlyProvide) {
        this.eagerlyProvide = eagerlyProvide;
    }

    /**
     * Gets the log.
     *
     * @return the collection of elements
     */
    public LogSettings getLog() {
        return log;
    }

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(LogSettings log) {
        this.log = log != null ? log : new LogSettings();
    }

    /**
     * Gets the logging.
     *
     * @return the collection of elements
     */
    public LoggingSettings getLogging() {
        return logging;
    }

    /**
     * Sets the logging.
     *
     * @param logging the logging
     */
    public void setLogging(LoggingSettings logging) {
        this.logging = logging != null ? logging : new LoggingSettings();
    }

    /**
     * Gets the model.
     *
     * @return the model settings
     */
    public ModelSettings getModel() {
        return model;
    }

    /**
     * Sets the model.
     *
     * @param model the model settings
     */
    public void setModel(ModelSettings model) {
        this.model = model != null ? model : new ModelSettings();
    }

    /**
     * Gets the build settings.
     *
     * @return the build settings
     */
    public BuildSettings getBuild() {
        return build;
    }

    /**
     * Sets the build settings.
     *
     * @param build the build settings
     */
    public void setBuild(BuildSettings build) {
        this.build = build != null ? build : new BuildSettings();
        this.compilationThreads = this.build.getCompilationThreads();
        this.freshCompilation = this.build.isFreshCompilation();
    }

    /**
     * Gets the tooling.
     *
     * @return the collection of elements
     */
    public ToolingSettings getTooling() {
        return tooling;
    }

    /**
     * Sets the tooling.
     *
     * @param tooling the tooling
     */
    public void setTooling(ToolingSettings tooling) {
        this.tooling = tooling != null ? tooling : new ToolingSettings();
    }

    /**
     * Gets the file tools.
     *
     * @return the collection of elements
     */
    public FileToolsSettings getFileTools() {
        return fileTools;
    }

    /**
     * Sets the file tools.
     *
     * @param fileTools the file tools
     */
    public void setFileTools(FileToolsSettings fileTools) {
        this.fileTools = fileTools != null ? fileTools : new FileToolsSettings();
    }



    /**
     * Gets the context.
     *
     * @return the collection of elements
     */
    public ContextSettings getContext() {
        return context;
    }

    /**
     * Sets the context.
     *
     * @param context the context
     */
    public void setContext(ContextSettings context) {
        this.context = context != null ? context : new ContextSettings();
    }

    /**
     * Gets the tracking.
     *
     * @return the collection of elements
     */
    public TrackingSettings getTracking() {
        return tracking;
    }

    /**
     * Sets the tracking.
     *
     * @param tracking the tracking
     */
    public void setTracking(TrackingSettings tracking) {
        this.tracking = tracking != null ? tracking : new TrackingSettings();
    }

    /**
     * Gets the source.
     *
     * @return the string result
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the parsed list of explicit source entries.
     *
     * @return list of source paths
     */
    public List<String> getSources() {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (String s : source.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    /**
     * Sets the source.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Sets sources from a list.
     *
     * @param sources list of source paths
     */
    public void setSources(List<String> sources) {
        this.source = (sources != null && !sources.isEmpty()) ? String.join(",", sources) : null;
    }

    /**
     * Gets the note.
     *
     * @return the note
     */
    public String getNote() {
        return note;
    }

    /**
     * Sets the note.
     *
     * @param note the note
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Checks if the component is explicit source mode.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isExplicitSourceMode() {
        return explicitSourceMode;
    }

    /**
     * Sets the explicit source mode.
     *
     * @param explicitSourceMode the explicit source mode
     */
    public void setExplicitSourceMode(boolean explicitSourceMode) {
        this.explicitSourceMode = explicitSourceMode;
    }

    /**
     * Gets the script dir.
     *
     * @return the string result
     */
    @Deprecated
    public String getScriptDir() {
        return reasoning == null ? null : reasoning.getScriptsPath();
    }

    /**
     * Sets the script dir.
     *
     * @param scriptDir the script dir
     */
    @Deprecated
    public void setScriptDir(String scriptDir) {
        if (this.reasoning == null) {
            this.reasoning = new ReasoningSettings();
        }
        this.reasoning.setScriptsPath(scriptDir);
    }

    /**
     * Gets the scripts path.
     *
     * @return the string result
     */
    public String getScriptsPath() {
        return reasoning == null ? null : reasoning.getScriptsPath();
    }

    /**
     * Sets the scripts path.
     *
     * @param scriptsPath the scripts path
     */
    public void setScriptsPath(String scriptsPath) {
        if (this.reasoning == null) {
            this.reasoning = new ReasoningSettings();
        }
        this.reasoning.setScriptsPath(scriptsPath);
    }
}
