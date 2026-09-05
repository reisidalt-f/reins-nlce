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

import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadFileMethodTest {

    @TempDir
    Path tempDir;

    @Test
    void readsFileFromMainAndTargetBases() throws Exception {
        Path mainDir = Files.createDirectories(tempDir.resolve("src/main/markdown"));
        Path targetDir = Files.createDirectories(tempDir.resolve("target/output"));

        Files.writeString(mainDir.resolve("doc.md"), "Hello Main Source");
        Files.writeString(targetDir.resolve("extra.txt"), "Hello Target Extra");

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainDir);
        mappings.setTargetRoot(targetDir);

        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(tempDir));
        ReadFileMethod method = new ReadFileMethod(resolver);

        assertEquals("Hello Main Source", method.exec(List.of("main:doc.md")));
        assertEquals("Hello Target Extra", method.exec(List.of("target:/extra.txt")));
        assertEquals("Hello Target Extra", method.exec(List.of("target:extra.txt")));
    }

    @Test
    void readsFileFromCustomSourceBase() throws Exception {
        Path customDir = Files.createDirectories(tempDir.resolve("custom/specs"));
        Files.writeString(customDir.resolve("api.json"), "{\"status\": \"ok\"}");

        ReinsConfig config = new ReinsConfig();
        config.getSourceBases().put("specs", customDir.toFile());

        BasePathMappingSet mappings = BasePathMappingSet.fromConfig(config, tempDir);
        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(tempDir));
        ReadFileMethod method = new ReadFileMethod(resolver);

        assertEquals("{\"status\": \"ok\"}", method.exec(List.of("specs:api.json")));
    }

    @Test
    void returnsEmptyStringForMissingOrInvalidInput() throws Exception {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir);
        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(tempDir));
        ReadFileMethod method = new ReadFileMethod(resolver);

        assertEquals("", method.exec(List.of()));
        assertEquals("", method.exec(List.of("")));
        assertEquals("", method.exec(List.of("main:nonexistent.txt")));
    }

    @Test
    void blocksPathTraversalOutsideProjectRoot() throws Exception {
        Path mainDir = Files.createDirectories(tempDir.resolve("src/main/markdown"));
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainDir);
        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(tempDir));
        ReadFileMethod method = new ReadFileMethod(resolver);

        assertEquals("", method.exec(List.of("main:../../secret.txt")));
    }

    @Test
    void fallbackResolutionUsingScriptContextFileBases() throws Exception {
        Path mainDir = Files.createDirectories(tempDir.resolve("main"));
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));
        Files.writeString(mainDir.resolve("source.md"), "Context Main Content");
        Files.writeString(targetDir.resolve("out.txt"), "Context Target Content");

        ReasoningScriptContext context = ReasoningScriptContext.builder()
                .fileBases(new ReasoningScriptViews.FileBasesView(
                        mainDir.toString(),
                        tempDir.toString(),
                        targetDir.toString(),
                        tempDir.toString()))
                .build();

        ReadFileMethod fallbackMethod = new ReadFileMethod(null, context);

        assertEquals("Context Main Content", fallbackMethod.exec(List.of("main:source.md")));
        assertEquals("Context Target Content", fallbackMethod.exec(List.of("target:/out.txt")));
    }

    @Test
    void integratesWithScriptEvaluatorFreeMarkerRendering() throws Exception {
        Path mainDir = Files.createDirectories(tempDir.resolve("main"));
        Files.writeString(mainDir.resolve("spec.md"), "SPEC_CONTENT");

        Path scriptDir = Files.createDirectories(tempDir.resolve("scripts"));
        Files.writeString(scriptDir.resolve("user-prompt.ftl"),
                "<#assign spec = readFile(\"main:spec.md\")>\nContent: ${spec}");

        ScriptRegistry registry = ScriptRegistry.build(new ScriptResolver(scriptDir.toFile(), null), scriptDir.toFile());
        registry.validateAll();

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(mainDir);
        BasePathResolver resolver = new BasePathResolver(mappings, new PathValidator(tempDir));

        ScriptEvaluator evaluator = new ScriptEvaluator(registry, null, false, resolver);

        ReasoningScriptContext context = ReasoningScriptContext.builder()
                .fileBases(new ReasoningScriptViews.FileBasesView(
                        mainDir.toString(),
                        tempDir.toString(),
                        tempDir.toString(),
                        tempDir.toString()))
                .build();

        String rendered = evaluator.evaluate("user-prompt.ftl", context);
        assertTrue(rendered.contains("Content: SPEC_CONTENT"));
    }
}
