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

package br.com.dizeno.reins.run.mojo;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.scripting.ScriptRegistry;
import br.com.dizeno.reins.reasoning.scripting.ScriptResolver;
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.compilation.DefaultReprocessingPreFilterService;
import br.com.dizeno.reins.compilation.CompilationService;
import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.PreFilterResult;
import br.com.dizeno.reins.compilation.ProcessingResult;
import br.com.dizeno.reins.compilation.ReprocessingPreFilterService;
import br.com.dizeno.reins.compilation.CycleWorkSetEntry;
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.compilation.SourceFileProcessor;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.source.scanner.CoderMdScanner;
import br.com.dizeno.reins.source.scanner.SourceDiscoveryMode;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.util.LogSanitizer;
import br.com.dizeno.reins.util.PathNormalizer;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CompileMojo is part of the general application functions in the reins
 * architecture.
 * Maven goal that executes the compilation and inference process on source
 * files.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CompileMojo extends AbstractMojo {
    private static final Set<String> KNOWN_LOGGING_KEYS = Set.of(
            "scriptsEvents",
            "eagerlyProvided",
            "fileListingAndReading",
            "fileMutating",
            "scriptRun",
            "selectionReason",
            "result",
            "trackingFile",
            "llmProvider",
            "sourceTag");

    @Parameter
    private GeminiSettings gemini;

    @Parameter
    private OllamaSettings ollama;

    @Parameter
    private OpenAiSettings openai;

    @Parameter
    private String provider;

    @Parameter(defaultValue = "**/*.md")
    private String includePattern;

    @Parameter
    private TargetSettings target;

    @Parameter
    private Map<String, File> sources;

    @Parameter(property = "source")
    private String source;

    @Parameter(property = "note")
    private String note;

    @Parameter(property = "skipTest", defaultValue = "false")
    private boolean skipTest;

    @Parameter
    private ReasoningSettings reasoning;

    @Parameter
    private RecompileOnSettings recompileOn;

    @Parameter
    private EagerlyProvideSettings eagerlyProvide;

    @Parameter
    private LoggingSettings logging;

    @Parameter
    private ModelSettings model;

    @Parameter
    private BuildSettings build;



    @Parameter(defaultValue = "false")
    private boolean failOnError;

    @Parameter(defaultValue = "false")
    private boolean verbose;

    @Parameter(defaultValue = "false")
    private boolean dryRun;

    @Parameter(property = "validateAll", defaultValue = "false")
    private boolean validateAll;

    @Parameter(property = "compilationThreads", defaultValue = "1")
    private int compilationThreads = 1;

    @Parameter(property = "freshCompilation", defaultValue = "false")
    private boolean freshCompilation;

    @Parameter(property = "skipReins")
    private String skipReins;

    @Parameter
    private ContextSettings context;

    @Parameter
    private ToolingSettings tooling;

    @Parameter
    private FileToolsSettings fileTools;

    @Parameter
    private TrackingSettings tracking;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin.groupId}", readonly = true)
    private String pluginGroupId;

    @Parameter(defaultValue = "${plugin.artifactId}", readonly = true)
    private String pluginArtifactId;

    CompilationService compilationService = new CompilationService();

    void setCompilationService(CompilationService gs) {
        this.compilationService = gs;
    }

    ReprocessingPreFilterService preFilterService = new DefaultReprocessingPreFilterService();

    void setPreFilterService(ReprocessingPreFilterService svc) {
        this.preFilterService = svc;
    }

    /**
     * Executes the operation.
     *
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            SkipDecision skipDecision = new SkipReinsResolver().resolve(skipReins);
            if (skipDecision.shouldFail()) {
                throw new MojoExecutionException(SkipReinsMessages.invalidValueMessage("reins:compile", skipDecision));
            }
            if (skipDecision.shouldSkip()) {
                getLog().info(SkipReinsMessages.skipMessage("reins:compile", skipDecision));
                return;
            }

            validateUnknownLoggingKeys();
            ReinsConfig config = buildConfig();

            br.com.dizeno.reins.run.ReinsRunner runner = new br.com.dizeno.reins.run.ReinsRunner();
            runner.setCompilationService(compilationService);
            runner.setPreFilterService(preFilterService);
            runner.compile(config, project.getBasedir(), getLog());

        } catch (ConfigValidationException ex) {
            throw new MojoExecutionException("Invalid plugin configuration: " + ex.getMessage(), ex);
        } catch (GraphProcessingException ex) {
            if (ex.getViolationType() == GraphProcessingException.ViolationType.CYCLE) {
                getLog().error(ex.toStructuredDiagnostic());
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
            throw new MojoExecutionException("Failed to execute Reins plugin", ex);
        } catch (MojoExecutionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to execute Reins plugin", ex);
        }
    }

    private List<CycleWorkSetEntry> buildExplicitWorkSet(List<File> files, Path projectRoot) {
        List<CycleWorkSetEntry> entries = new ArrayList<>();
        for (File file : files) {
            String sourcePath = PathNormalizer.toForwardSlashes(
                    projectRoot.toAbsolutePath().normalize()
                            .relativize(file.toPath().toAbsolutePath().normalize()).toString());
            entries.add(new CycleWorkSetEntry(file, sourcePath, SourceProcessingStatus.COMPILE));
        }
        return entries;
    }

    private ReinsConfig buildConfig() {
        ReinsConfig config = new ReinsConfig();
        config.setGemini(gemini == null ? new GeminiSettings() : gemini);
        config.setOllama(ollama == null ? new OllamaSettings() : ollama);
        config.setOpenai(openai == null ? new OpenAiSettings() : openai);
        config.setProvider(provider);
        config.setIncludePattern(
                includePattern == null ? ProjectDirectoryPaths.DEFAULT_INCLUDE_PATTERN : includePattern);

        config.setTarget(target == null ? new TargetSettings() : target);
        config.setSkipTest(skipTest);
        config.setReasoning(reasoning == null ? new ReasoningSettings() : reasoning);
        if (recompileOn != null) {
            config.setRecompileOn(recompileOn);
        }
        config.setEagerlyProvide(eagerlyProvide == null ? new EagerlyProvideSettings() : eagerlyProvide);
        config.setLogging(logging == null ? new LoggingSettings() : logging);
        config.setModel(model == null ? new ModelSettings() : model);
        config.setBuild(build == null ? new BuildSettings() : build);
        config.setFailOnError(failOnError);
        config.setVerbose(verbose);
        config.setDryRun(dryRun);
        config.setValidateAll(validateAll);
        int effectiveThreads = (build != null && build.getCompilationThreads() > 1) ? build.getCompilationThreads() : (compilationThreads > 0 ? compilationThreads : 1);
        config.setCompilationThreads(effectiveThreads);
        config.setFreshCompilation(freshCompilation);
        config.setContext(context == null ? new ContextSettings() : context);
        config.setTooling(tooling == null ? new ToolingSettings() : tooling);

        config.setFileTools(fileTools == null ? new FileToolsSettings() : fileTools);
        config.setTracking(tracking == null ? new TrackingSettings() : tracking);
        if (sources != null) {
            sources.forEach(config::setSourceBase);
        }
        config.setSource(source);
        config.setNote(note);
        config.setExplicitSourceMode(source != null && !source.trim().isEmpty());
        return config;
    }

    private File resolveScriptDir(String scriptDirParam, File basedir) {
        if (scriptDirParam == null || scriptDirParam.isBlank()) {
            return null;
        }
        File f = new File(scriptDirParam);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(basedir, scriptDirParam);
    }

    private void validateUnknownLoggingKeys() {
        Object legacyLogNode = resolveRawLegacyLogNode();
        if (legacyLogNode != null) {
            throw new ConfigValidationException("Legacy log configuration key 'log' is not supported; use 'logging'.");
        }

        Object loggingNode = resolveRawLoggingNode();
        if (loggingNode == null) {
            return;
        }
        for (Object child : children(loggingNode)) {
            String childName = nodeName(child);
            if (!KNOWN_LOGGING_KEYS.contains(childName)) {
                throw new ConfigValidationException("Unknown logging configuration key: logging." + childName);
            }
            String value = nodeValue(child);
            if (!isBooleanValue(value)) {
                throw new ConfigValidationException(
                        "Logging configuration value for logging." + childName + " must be boolean.");
            }
        }
    }

    private Object resolveRawLoggingNode() {
        if (project == null || project.getBuildPlugins() == null) {
            return null;
        }
        for (Plugin plugin : project.getBuildPlugins()) {
            if (!isCurrentPlugin(plugin)) {
                continue;
            }
            Object pluginLevel = plugin.getConfiguration();
            Object pluginLogging = child(pluginLevel, "logging");
            if (pluginLogging != null) {
                return pluginLogging;
            }
            if (plugin.getExecutions() == null) {
                continue;
            }
            for (PluginExecution execution : plugin.getExecutions()) {
                Object executionLevel = execution.getConfiguration();
                Object executionLogging = child(executionLevel, "logging");
                if (executionLogging != null) {
                    return executionLogging;
                }
            }
        }
        return null;
    }

    private Object resolveRawLegacyLogNode() {
        if (project == null || project.getBuildPlugins() == null) {
            return null;
        }
        for (Plugin plugin : project.getBuildPlugins()) {
            if (!isCurrentPlugin(plugin)) {
                continue;
            }
            Object pluginLevel = plugin.getConfiguration();
            Object pluginLog = child(pluginLevel, "log");
            if (pluginLog != null) {
                return pluginLog;
            }
            if (plugin.getExecutions() == null) {
                continue;
            }
            for (PluginExecution execution : plugin.getExecutions()) {
                Object executionLevel = execution.getConfiguration();
                Object executionLog = child(executionLevel, "log");
                if (executionLog != null) {
                    return executionLog;
                }
            }
        }
        return null;
    }

    private boolean isCurrentPlugin(Plugin plugin) {
        if (plugin == null) {
            return false;
        }
        if (pluginGroupId != null && pluginArtifactId != null) {
            return Objects.equals(pluginGroupId, plugin.getGroupId())
                    && Objects.equals(pluginArtifactId, plugin.getArtifactId());
        }
        return "br.com.dizeno".equals(plugin.getGroupId())
                && "reins-maven-plugin".equals(plugin.getArtifactId());
    }

    private Object child(Object root, String name) {
        if (root == null) {
            return null;
        }
        try {
            Method method = root.getClass().getMethod("getChild", String.class);
            return method.invoke(root, name);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Object> children(Object root) {
        if (root == null) {
            return List.of();
        }
        try {
            Method method = root.getClass().getMethod("getChildren");
            Object value = method.invoke(root);
            if (value instanceof Object[] array) {
                return Arrays.asList(array);
            }
            if (value instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            return List.of();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String nodeValue(Object node) {
        if (node == null) {
            return null;
        }
        try {
            Method method = node.getClass().getMethod("getValue");
            Object value = method.invoke(node);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBooleanValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "false".equals(normalized);
    }

    private String nodeName(Object node) {
        if (node == null) {
            return "";
        }
        try {
            Method method = node.getClass().getMethod("getName");
            Object value = method.invoke(node);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ignored) {
            return "";
        }
    }



    private String formatDisplayPath(Path absolutePath, Path mainBase, Path testBase) {
        Path normalized = absolutePath.toAbsolutePath().normalize();
        Path mainNormalized = mainBase.toAbsolutePath().normalize();
        Path testNormalized = testBase.toAbsolutePath().normalize();

        if (normalized.startsWith(mainNormalized)) {
            Path relPath = mainNormalized.relativize(normalized);
            return "main:" + relPath;
        } else if (normalized.startsWith(testNormalized)) {
            Path relPath = testNormalized.relativize(normalized);
            return "test:" + relPath;
        } else {
            return "unknown:" + absolutePath;
        }
    }
}
