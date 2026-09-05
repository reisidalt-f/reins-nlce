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

package br.com.dizeno.reins.compilation;

import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.reasoning.inference.llm.error.LlmEmptyResponseException;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceResponse;
import br.com.dizeno.reins.reasoning.ReasoningRequest;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.reasoning.ReasoningService;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompilationServiceTest {
    @TempDir
    Path projectDir;

    private ReinsConfig config(boolean failOnError) {
        ReinsConfig config = new ReinsConfig();
        GeminiSettings gemini = new GeminiSettings();
        gemini.setApiKey("test-key");
        gemini.setModel("gemini-2.0-flash");
        gemini.setEndpoint("https://generativelanguage.googleapis.com");
        gemini.setTimeoutSeconds(30);
        gemini.setRetryAttempts(1);

        config.setGemini(gemini);
        config.setDryRun(false);
        config.setFailOnError(failOnError);
        config.setIncludePattern("**/*.md");
        config.setSourceBase("main", projectDir.resolve("src/main/nl").toFile());
        config.setSourceBase("test", projectDir.resolve("src/test/nl").toFile());

        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        target.setTargetBase("test", "src/test/java");
        config.setTarget(target);
        return config;
    }

    private Path writeMarkdown(Path path, String response) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path,
                "# fixture\n\nDescribes component behavior.\n\n<!-- reins-fixture-response\n" + response + "\n-->\n");
        return path;
    }

    private String mainFixtureResponse() {
        return String.join("\n",
                "```java path=\"fixture/currentfeatures/main/CurrentFeatureData.java\"",
                "package fixture.currentfeatures.main;",
                "public record CurrentFeatureData(String title, boolean enabled) {}",
                "```",
                "```java path=\"fixture/currentfeatures/main/CurrentFeatureEntity.java\"",
                "package fixture.currentfeatures.main;",
                "public final class CurrentFeatureEntity {}",
                "```",
                "```java path=\"fixture/currentfeatures/main/CurrentFeatureService.java\"",
                "package fixture.currentfeatures.main;",
                "public final class CurrentFeatureService {}",
                "```",
                "```java path=\"fixture/currentfeatures/main/CurrentFeatureController.java\"",
                "package fixture.currentfeatures.main;",
                "public final class CurrentFeatureController {}",
                "```",
                "```java path=\"fixture/currentfeatures/main/CurrentFeatureGui.java\"",
                "package fixture.currentfeatures.main;",
                "public final class CurrentFeatureGui {}",
                "```");
    }

    private String testFixtureResponse() {
        return String.join("\n",
                "```java path=\"fixture/currentfeatures/test/CurrentFeatureTestData.java\"",
                "package fixture.currentfeatures.test;",
                "public record CurrentFeatureTestData(String scenario) {}",
                "```",
                "```java path=\"fixture/currentfeatures/test/CurrentFeatureTestEntity.java\"",
                "package fixture.currentfeatures.test;",
                "public final class CurrentFeatureTestEntity {}",
                "```",
                "```java path=\"fixture/currentfeatures/test/CurrentFeatureTestService.java\"",
                "package fixture.currentfeatures.test;",
                "public final class CurrentFeatureTestService {}",
                "```",
                "```java path=\"fixture/currentfeatures/test/CurrentFeatureTestController.java\"",
                "package fixture.currentfeatures.test;",
                "public final class CurrentFeatureTestController {}",
                "```",
                "```java path=\"fixture/currentfeatures/test/CurrentFeatureTestGui.java\"",
                "package fixture.currentfeatures.test;",
                "public final class CurrentFeatureTestGui {}",
                "```");
    }

    private String testResourceResponse() {
        return String.join("\n",
                "```java path=\"fixture/currentfeatures/resources/TestResourceBackedService.java\"",
                "package fixture.currentfeatures.resources;",
                "public final class TestResourceBackedService {}",
                "```",
                "```yaml path=\"config/test-resource.yml\"",
                "scope: test",
                "mode: yaml",
                "```",
                "```java path=\"fixture/currentfeatures/resources/ExplicitCompiled.java\" output=\"target/compiled-sources/explicit/ExplicitCompiled.java\"",
                "package fixture.currentfeatures.resources;",
                "public final class ExplicitCompiled {}",
                "```");
    }

    private String stressResponse() {
        return String.join("\n",
                "```java path=\"fixture/currentfeatures/stress/StressDashboard.java\"",
                "package fixture.currentfeatures.stress;",
                "public final class StressDashboard {}",
                "```",
                "```json path=\"stress/stress-metadata.json\"",
                "{\"scenario\":\"stress\",\"valid\":true}",
                "```",
                "```java",
                "package fixture.currentfeatures.stress;",
                "public final class MissingPathBlock {}",
                "```",
                "```java path=\"fixture/currentfeatures/stress/StressDashboard.java\"",
                "package fixture.currentfeatures.stress;",
                "public final class DuplicatePathBlock {}",
                "```");
    }

    static final class RecordingLog implements Log {
        final List<String> infoMessages = new ArrayList<>();
        final List<String> errorMessages = new ArrayList<>();

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(CharSequence content) {
        }

        @Override
        public void debug(CharSequence content, Throwable error) {
        }

        @Override
        public void debug(Throwable error) {
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(CharSequence content) {
            infoMessages.add(String.valueOf(content));
        }

        @Override
        public void info(CharSequence content, Throwable error) {
            infoMessages.add(String.valueOf(content));
        }

        @Override
        public void info(Throwable error) {
            infoMessages.add(String.valueOf(error.getMessage()));
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public void warn(CharSequence content) {
            infoMessages.add(String.valueOf(content));
        }

        @Override
        public void warn(CharSequence content, Throwable error) {
            infoMessages.add(String.valueOf(content));
        }

        @Override
        public void warn(Throwable error) {
            infoMessages.add(String.valueOf(error.getMessage()));
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void error(CharSequence content) {
            errorMessages.add(String.valueOf(content));
        }

        @Override
        public void error(CharSequence content, Throwable error) {
            errorMessages.add(String.valueOf(content));
        }

        @Override
        public void error(Throwable error) {
            errorMessages.add(String.valueOf(error.getMessage()));
        }
    }

    @Test
    void exhaustedEmptyResponseFailsImmediatelyEvenWhenFailOnErrorIsFalse() throws Exception {
        InferenceService inferenceService = mock(InferenceService.class);
        ReasoningService reasoningService = mock(ReasoningService.class);
        when(reasoningService.runCycle(any(ReasoningRequest.class), any(ReinsConfig.class)))
                .thenThrow(new LlmEmptyResponseException("LLM returned empty candidates", 2, 2));

        CompilationService service = new CompilationService(
                inferenceService,
                new OutputWriter(),
                new ResultPrinter(),
                new br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore(),
                new br.com.dizeno.reins.compilation.tracking.SourceFingerprintService(),
                new br.com.dizeno.reins.compilation.tracking.RecompilationDecider(),
                new br.com.dizeno.reins.source.graph.MarkdownDependencyGraphBuilder(),
                new br.com.dizeno.reins.source.graph.ProcessingOrderResolver(),
                reasoningService);

        ReinsConfig cfg = config(false);
        Path source = projectDir.resolve("src/main/nl/FailFastEmpty.md");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "# Empty response test\n");
        RecordingLog log = new RecordingLog();

        assertThrows(LlmEmptyResponseException.class,
                () -> service.processFiles(List.of(source.toFile()), cfg, projectDir, log));
        assertTrue(
                log.infoMessages.stream().anyMatch(msg -> msg.contains("retrySummary=attempts=2,noUsableContent=2")));
    }
}
