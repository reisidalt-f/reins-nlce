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

package br.com.dizeno.reins.source.graph;

import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.run.mojo.ExplicitSourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NamedBaseAndStrategyTest {

    @TempDir
    Path projectRoot;

    private Path mainDir;
    private Path testDir;
    private Path libDir;
    private Map<String, Path> sourceBases;
    private Map<String, File> sourceBasesFile;
    private PathValidator validator;

    @BeforeEach
    void setUp() throws IOException {
        mainDir = Files.createDirectories(projectRoot.resolve("src/main/nl"));
        testDir = Files.createDirectories(projectRoot.resolve("src/test/nl"));
        libDir = Files.createDirectories(projectRoot.resolve("src/lib/nl"));

        sourceBases = new LinkedHashMap<>();
        sourceBases.put("main", mainDir);
        sourceBases.put("test", testDir);
        sourceBases.put("lib", libDir);

        sourceBasesFile = new LinkedHashMap<>();
        sourceBasesFile.put("main", mainDir.toFile());
        sourceBasesFile.put("test", testDir.toFile());
        sourceBasesFile.put("lib", libDir.toFile());

        validator = new PathValidator(projectRoot);
    }

    @Test
    void testNamedBaseReferenceResolutionStrategy() throws IOException {
        Path targetFile = Files.createFile(libDir.resolve("util.md"));
        ResolverContext context = new ResolverContext(projectRoot, sourceBases, List.of("named-base-scheme"), validator, Map.of());

        NamedBaseReferenceResolutionStrategy strategy = new NamedBaseReferenceResolutionStrategy();
        ReferenceRequest request = new ReferenceRequest("src/main/nl/app.md", ReferenceRequest.NlScope.MAIN, mainDir, "lib:/util.md");

        assertTrue(strategy.applies(request, context));
        StrategyAttemptResult result = strategy.attempt(request, context);
        assertFalse(result.getCandidates().isEmpty());
        assertEquals(List.of(targetFile.toAbsolutePath().normalize()), result.getCandidates());
    }

    @Test
    void testRootBaseReferenceResolutionStrategy() throws IOException {
        Files.createDirectories(mainDir.resolve("domain"));
        Path targetFile = Files.createFile(mainDir.resolve("domain/Customer.md"));
        ResolverContext context = new ResolverContext(projectRoot, sourceBases, List.of("root-base-relative"), validator, Map.of());

        RootBaseReferenceResolutionStrategy strategy = new RootBaseReferenceResolutionStrategy();
        ReferenceRequest request = new ReferenceRequest("src/main/nl/service/OrderService.md", ReferenceRequest.NlScope.MAIN, mainDir.resolve("service"), "/domain/Customer.md");

        assertTrue(strategy.applies(request, context));
        StrategyAttemptResult result = strategy.attempt(request, context);
        assertFalse(result.getCandidates().isEmpty());
        assertEquals(List.of(targetFile.toAbsolutePath().normalize()), result.getCandidates());
    }

    @Test
    void testContextualFallbackReferenceResolutionStrategy_testToMain() throws IOException {
        Path targetFile = Files.createFile(mainDir.resolve("Customer.md"));
        ResolverContext context = new ResolverContext(projectRoot, sourceBases, List.of("contextual-fallback"), validator, Map.of());

        ContextualFallbackReferenceResolutionStrategy strategy = new ContextualFallbackReferenceResolutionStrategy();
        ReferenceRequest request = new ReferenceRequest("src/test/nl/CustomerTest.md", ReferenceRequest.NlScope.TEST, testDir, "Customer.md");

        assertTrue(strategy.applies(request, context));
        StrategyAttemptResult result = strategy.attempt(request, context);
        assertFalse(result.getCandidates().isEmpty());
        assertEquals(List.of(targetFile.toAbsolutePath().normalize()), result.getCandidates());
    }

    @Test
    void testExplicitSourceResolver_namedSchemePrefix() throws IOException {
        Path file = Files.createFile(libDir.resolve("helper.md"));

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "lib:helper.md",
                projectRoot,
                sourceBasesFile,
                "**/*.md",
                validator
        );

        assertNotNull(result);
        assertFalse(result.directory());
        assertEquals(1, result.files().size());
        assertEquals(file.toAbsolutePath().normalize(), result.files().get(0).toPath().toAbsolutePath().normalize());
    }

    @Test
    void testExplicitSourceResolver_leadingSlashImplicitBases() throws IOException {
        Files.createDirectories(mainDir.resolve("domain"));
        Path file = Files.createFile(mainDir.resolve("domain/Item.md"));

        ExplicitSourceResolver resolver = new ExplicitSourceResolver();
        ExplicitSourceResolver.ResolutionResult result = resolver.resolve(
                "/domain/Item.md",
                projectRoot,
                sourceBasesFile,
                "**/*.md",
                validator
        );

        assertNotNull(result);
        assertFalse(result.directory());
        assertEquals(1, result.files().size());
        assertEquals(file.toAbsolutePath().normalize(), result.files().get(0).toPath().toAbsolutePath().normalize());
    }
}
