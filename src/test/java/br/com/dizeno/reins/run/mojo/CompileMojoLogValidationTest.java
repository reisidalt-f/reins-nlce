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
import br.com.dizeno.reins.compilation.CompilationService;
import br.com.dizeno.reins.compilation.ReprocessingPreFilterService;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompileMojoLogValidationTest {

    @TempDir
    Path projectDir;

    private CompileMojo mojo;
    private CompilationService compilationService;
    private ReprocessingPreFilterService preFilterService;
    private Log log;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new CompileMojo();
        compilationService = mock(CompilationService.class);
        preFilterService = mock(ReprocessingPreFilterService.class);
        log = mock(Log.class);

        mojo.setCompilationService(compilationService);
        mojo.setPreFilterService(preFilterService);
        mojo.setLog(log);

        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        gemini.setTimeoutSeconds(30);
        gemini.setRetryAttempts(1);
        setField(mojo, "gemini", gemini);

        TargetSettings targetSettings = new TargetSettings();
        targetSettings.setTargetBase("main", "src/main/java");
        setField(mojo, "target", targetSettings);

        setField(mojo, "includePattern", "**/*.md");

        Files.createDirectories(projectDir.resolve("src/main/nl"));
        java.util.Map<String, File> sourceBases = new java.util.LinkedHashMap<>();
        sourceBases.put("main", projectDir.resolve("src/main/nl").toFile());
        setField(mojo, "sources", sourceBases);
        setField(mojo, "provider", "gemini");
    }

    @Test
    void acceptsKnownNestedLoggingKeys() throws Exception {
        MavenProject project = createProjectWithPluginLoggingConfig(loggingConfig(true, true, true, true, true, true, false));
        setField(mojo, "project", project);

        assertDoesNotThrow(() -> mojo.execute());
    }

    @Test
    void failsFastOnUnknownNestedLoggingKey() throws Exception {
        MavenProject project = createProjectWithPluginLoggingConfig(loggingConfig(true, true, true, true, true, true, true));
        setField(mojo, "project", project);

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(ex.getMessage().contains("Unknown logging configuration key: logging.unknownToggle"));
    }

    @Test
    void rejectsNonBooleanNestedLoggingValues() throws Exception {
        MavenProject project = createProjectWithPluginLoggingConfig(loggingConfigWithInvalidValue());
        setField(mojo, "project", project);

        MojoExecutionException ex = assertThrows(MojoExecutionException.class, () -> mojo.execute());
        assertTrue(ex.getMessage().contains("Logging configuration value for logging.scriptsEvents must be boolean."));
    }

    private MavenProject createProjectWithPluginLoggingConfig(FakeConfigNode loggingNode) {
        Plugin plugin = new Plugin();
        plugin.setGroupId("br.com.dizeno");
        plugin.setArtifactId("reins-maven-plugin");

        FakeConfigNode configuration = new FakeConfigNode("configuration");
        configuration.addChild(loggingNode);
        plugin.setConfiguration(configuration);

        return new MavenProject() {
            @Override
            public File getBasedir() {
                return projectDir.toFile();
            }

            @Override
            public List<Plugin> getBuildPlugins() {
                return List.of(plugin);
            }
        };
    }

    private FakeConfigNode loggingConfig(boolean scriptsEvents,
                                         boolean eagerlyProvided,
                                         boolean fileListingAndReading,
                                         boolean fileMutating,
                                         boolean scriptRun,
                                         boolean selectionReason,
                                         boolean addUnknown) {
        FakeConfigNode logging = new FakeConfigNode("logging");

        FakeConfigNode inferenceNode = new FakeConfigNode("scriptsEvents");
        inferenceNode.setValue(Boolean.toString(scriptsEvents));
        logging.addChild(inferenceNode);

        FakeConfigNode eagerlyNode = new FakeConfigNode("eagerlyProvided");
        eagerlyNode.setValue(Boolean.toString(eagerlyProvided));
        logging.addChild(eagerlyNode);

        FakeConfigNode listingNode = new FakeConfigNode("fileListingAndReading");
        listingNode.setValue(Boolean.toString(fileListingAndReading));
        logging.addChild(listingNode);

        FakeConfigNode mutatingNode = new FakeConfigNode("fileMutating");
        mutatingNode.setValue(Boolean.toString(fileMutating));
        logging.addChild(mutatingNode);

        FakeConfigNode scriptRunNode = new FakeConfigNode("scriptRun");
        scriptRunNode.setValue(Boolean.toString(scriptRun));
        logging.addChild(scriptRunNode);

        FakeConfigNode selectionReasonNode = new FakeConfigNode("selectionReason");
        selectionReasonNode.setValue(Boolean.toString(selectionReason));
        logging.addChild(selectionReasonNode);

        FakeConfigNode sourceTagNode = new FakeConfigNode("sourceTag");
        sourceTagNode.setValue("true");
        logging.addChild(sourceTagNode);

        if (addUnknown) {
            FakeConfigNode unknownNode = new FakeConfigNode("unknownToggle");
            unknownNode.setValue("true");
            logging.addChild(unknownNode);
        }

        return logging;
    }

    private FakeConfigNode loggingConfigWithInvalidValue() {
        FakeConfigNode logging = new FakeConfigNode("logging");

        FakeConfigNode inferenceNode = new FakeConfigNode("scriptsEvents");
        inferenceNode.setValue("maybe");
        logging.addChild(inferenceNode);

        FakeConfigNode eagerlyNode = new FakeConfigNode("eagerlyProvided");
        eagerlyNode.setValue("false");
        logging.addChild(eagerlyNode);

        FakeConfigNode listingNode = new FakeConfigNode("fileListingAndReading");
        listingNode.setValue("false");
        logging.addChild(listingNode);

        FakeConfigNode mutatingNode = new FakeConfigNode("fileMutating");
        mutatingNode.setValue("false");
        logging.addChild(mutatingNode);

        FakeConfigNode scriptRunNode = new FakeConfigNode("scriptRun");
        scriptRunNode.setValue("false");
        logging.addChild(scriptRunNode);

        return logging;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                throw ignored;
            }
            return findField(superclass, name);
        }
    }

    static final class FakeConfigNode {
        private final String name;
        private String value;
        private final List<FakeConfigNode> children = new ArrayList<>();

        FakeConfigNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void addChild(FakeConfigNode child) {
            children.add(child);
        }

        public FakeConfigNode getChild(String childName) {
            return children.stream().filter(c -> c.name.equals(childName)).findFirst().orElse(null);
        }

        public FakeConfigNode[] getChildren() {
            return children.toArray(new FakeConfigNode[0]);
        }
    }
}
