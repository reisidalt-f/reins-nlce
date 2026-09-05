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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

 
class ReferenceTreeContextServiceScanRootFilterTest {

    @TempDir
    Path projectDir;

    

    @Test
    void throwsGraphProcessingException_whenProjectFileReferencesFileInMainNlRoot() throws Exception {
        
        Path projectMd = write("project.md", "# Project\n\n[src/main/nl/service.md]\n");
        write("src/main/nl/service.md", "# Service\n");

        ReferenceTreeContextService service = new ReferenceTreeContextService();
        ReasoningRequest request = projectRequest(projectMd);
        ReinsConfig config = new ReinsConfig();
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = resolverForProjectInference();

        Set<Path> excludedRoots = Set.of(
                projectDir.resolve("src/main/nl").normalize(),
                projectDir.resolve("src/test/nl").normalize()
        );

        GraphProcessingException ex = assertThrows(GraphProcessingException.class,
                () -> service.build(request, config, resolver, excludedRoots));

        assertTrue(ex.getMessage().contains("scan root"),
                "Exception message should mention 'scan root'. Got: " + ex.getMessage());
    }

    @Test
    void throwsGraphProcessingException_whenProjectFileReferencesFileInTestNlRoot() throws Exception {
        Path projectMd = write("project.md", "# Project\n\n[src/test/nl/domain-test.md]\n");
        write("src/test/nl/domain-test.md", "# Test spec\n");

        ReferenceTreeContextService service = new ReferenceTreeContextService();
        ReasoningRequest request = projectRequest(projectMd);
        ReinsConfig config = new ReinsConfig();
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = resolverForProjectInference();

        Set<Path> excludedRoots = Set.of(
                projectDir.resolve("src/main/nl").normalize(),
                projectDir.resolve("src/test/nl").normalize()
        );

        assertThrows(GraphProcessingException.class,
                () -> service.build(request, config, resolver, excludedRoots));
    }

    

    @Test
    void doesNotThrow_whenProjectFileOnlyReferencesFilesOutsideScanRoots() throws Exception {
        
        Path projectMd = write("project.md", "# Project\n\n[docs/conventions.md]\n");
        write("docs/conventions.md", "# Conventions\n");

        ReferenceTreeContextService service = new ReferenceTreeContextService();
        ReasoningRequest request = projectRequest(projectMd);
        ReinsConfig config = new ReinsConfig();
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = resolverForProjectInference();

        Set<Path> excludedRoots = Set.of(
                projectDir.resolve("src/main/nl").normalize(),
                projectDir.resolve("src/test/nl").normalize()
        );

        assertDoesNotThrow(() -> service.build(request, config, resolver, excludedRoots));
    }

    

    @Test
    void doesNotThrow_whenExcludedRootsIsEmpty_evenIfRefIsInsideScanRoot() throws Exception {
        Path projectMd = write("project.md", "# Project\n\n[src/main/nl/domain.md]\n");
        write("src/main/nl/domain.md", "# Domain\n");

        ReferenceTreeContextService service = new ReferenceTreeContextService();
        ReasoningRequest request = projectRequest(projectMd);
        ReinsConfig config = new ReinsConfig();
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = resolverForProjectInference();

        
        assertDoesNotThrow(() -> service.build(request, config, resolver, Set.of()));
    }

    

    private ReasoningRequest projectRequest(Path projectFile) {
        ReasoningRequest request = new ReasoningRequest();
        request.setMessage("Use attached source markdown as the primary context.");
        request.setProjectRoot(projectDir);
        request.setSourceScope("main");
        request.setSourcePath(projectDir.relativize(projectFile).toString().replace('\\', '/'));
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectDir.toAbsolutePath().normalize());
        mappings.setTargetRoot(projectDir.resolve("target").toAbsolutePath().normalize());
        request.setBaseMappings(mappings);
        return request;
    }

    private br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolverForProjectInference() {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectDir.toAbsolutePath().normalize());
        mappings.setTargetRoot(projectDir.resolve("target").toAbsolutePath().normalize());
        return new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(
                mappings,
                new PathValidator(projectDir)
        );
    }

    private Path write(String relative, String content) throws Exception {
        Path file = projectDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
