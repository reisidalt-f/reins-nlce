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
import java.util.List;

/**
 * ReinsConfig is part of the general application functions in the reins
 * architecture.
 * Stores configuration settings for reins, mapped from Maven properties or YAML
 * configurations.
 */
public class ReinsConfig {
    private GeminiSettings gemini = new GeminiSettings();
    private OllamaSettings ollama = new OllamaSettings();
    private String provider = "gemini";
    private List<File> scanRoots = new ArrayList<>();
    private String includePattern = "**/*.md";
    private boolean defaultScanRoots;
    private boolean failOnError;
    private boolean verbose;
    private boolean dryRun;
    private boolean validateAll;
    private File mainNlRoot;
    private File testNlRoot;
    private TargetSettings target;
    private ReasoningSettings reasoning = new ReasoningSettings();
    private RecompileOnSettings recompileOn = new RecompileOnSettings();
    private EagerlyProvideSettings eagerlyProvide = new EagerlyProvideSettings();
    private LogSettings log = new LogSettings();
    private LoggingSettings logging = new LoggingSettings();

    private File projectContextFile;
    private ToolingSettings tooling = new ToolingSettings();
    private McpFileBaseOpsSettings mcp = new McpFileBaseOpsSettings();
    private boolean enableProjectInference = false;
    private ContextSettings context = new ContextSettings();
    private TrackingSettings tracking = new TrackingSettings();
    private String source;
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
        if (provider == null) {
            return gemini != null ? gemini.getModel() : null;
        }
        String normalizedProvider = provider.trim().toLowerCase();
        if ("ollama".equals(normalizedProvider)) {
            return ollama != null ? ollama.getModel() : null;
        } else if ("stub".equals(normalizedProvider)) {
            return "stub";
        } else {
            return gemini != null ? gemini.getModel() : null;
        }
    }


    /**
     * Gets the scan roots.
     *
     * @return the collection of elements
     */
    public List<File> getScanRoots() {
        return scanRoots;
    }

    /**
     * Sets the scan roots.
     *
     * @param scanRoots the scan roots
     */
    public void setScanRoots(List<File> scanRoots) {
        this.scanRoots = scanRoots;
    }

    /**
     * Gets the include pattern.
     *
     * @return the string result
     */
    public String getIncludePattern() {
        return includePattern;
    }

    /**
     * Sets the include pattern.
     *
     * @param includePattern the include pattern
     */
    public void setIncludePattern(String includePattern) {
        this.includePattern = includePattern;
    }

    /**
     * Checks if the component is default scan roots.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDefaultScanRoots() {
        return defaultScanRoots;
    }

    /**
     * Sets the default scan roots.
     *
     * @param defaultScanRoots the default scan roots
     */
    public void setDefaultScanRoots(boolean defaultScanRoots) {
        this.defaultScanRoots = defaultScanRoots;
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

    /**
     * Gets the main nl root.
     *
     * @return the resolved or constructed object
     */
    public File getMainNlRoot() {
        return mainNlRoot;
    }

    /**
     * Sets the main nl root.
     *
     * @param mainNlRoot the main nl root
     */
    public void setMainNlRoot(File mainNlRoot) {
        this.mainNlRoot = mainNlRoot;
    }

    /**
     * Gets the test nl root.
     *
     * @return the resolved or constructed object
     */
    public File getTestNlRoot() {
        return testNlRoot;
    }

    /**
     * Sets the test nl root.
     *
     * @param testNlRoot the test nl root
     */
    public void setTestNlRoot(File testNlRoot) {
        this.testNlRoot = testNlRoot;
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
     * Gets the project context file.
     *
     * @return the resolved or constructed object
     */
    public File getProjectContextFile() {
        return projectContextFile;
    }

    /**
     * Sets the project context file.
     *
     * @param projectContextFile the project context file
     */
    public void setProjectContextFile(File projectContextFile) {
        this.projectContextFile = projectContextFile;
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
     * Gets the mcp.
     *
     * @return the collection of elements
     */
    public McpFileBaseOpsSettings getMcp() {
        return mcp;
    }

    /**
     * Sets the mcp.
     *
     * @param mcp the mcp
     */
    public void setMcp(McpFileBaseOpsSettings mcp) {
        this.mcp = mcp != null ? mcp : new McpFileBaseOpsSettings();
    }

    /**
     * Checks if the component is enable project inference.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEnableProjectInference() {
        return enableProjectInference;
    }

    /**
     * Sets the enable project inference.
     *
     * @param enableProjectInference the enable project inference
     */
    public void setEnableProjectInference(boolean enableProjectInference) {
        this.enableProjectInference = enableProjectInference;
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
     * Sets the source.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
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
