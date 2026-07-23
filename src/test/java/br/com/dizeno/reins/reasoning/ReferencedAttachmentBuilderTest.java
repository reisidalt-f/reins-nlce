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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferencedAttachmentBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsAttachmentsForNonRootNodesInTraversalOrder() throws Exception {
        Path root = write("src/main/nl/root.md", "[child-a.md]\n[child-b.md]\n");
        write("src/main/nl/child-a.md", "A\n");
        write("src/main/nl/child-b.md", "B\n");

        ReferenceTreeContextService treeService = new ReferenceTreeContextService();
        ReasoningRequest request = baseRequest(root);
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(request.getBaseMappings(), new PathValidator(tempDir));
        ReferenceTreeContextService.ReferenceTreeContext treeContext = treeService.build(request, new ReinsConfig(), resolver);

        ReferencedAttachmentBuilder builder = new ReferencedAttachmentBuilder();
        List<AttachedFilePayload> attachments = builder.build(treeContext, tempDir);

        assertEquals(2, attachments.size());
        assertEquals("main:child-a.md", attachments.get(0).getQualifiedPath());
        assertEquals("A\n", attachments.get(0).getContent());
        assertEquals("main:child-b.md", attachments.get(1).getQualifiedPath());
        assertEquals("B\n", attachments.get(1).getContent());
    }

    @Test
    void unreadableReferencedFileThrowsUnresolvedReferenceViolation() throws Exception {
        Path root = write("src/main/nl/root.md", "[child-a.md]\n");
        Path child = write("src/main/nl/child-a.md", "A\n");

        ReferenceTreeContextService treeService = new ReferenceTreeContextService();
        ReasoningRequest request = baseRequest(root);
        br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver resolver = new br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver(request.getBaseMappings(), new PathValidator(tempDir));
        ReferenceTreeContextService.ReferenceTreeContext treeContext = treeService.build(request, new ReinsConfig(), resolver);

        Files.delete(child);

        ReferencedAttachmentBuilder builder = new ReferencedAttachmentBuilder();
        GraphProcessingException ex = assertThrows(GraphProcessingException.class,
                () -> builder.build(treeContext, tempDir));

        assertEquals(GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE, ex.getViolationType());
    }

    private ReasoningRequest baseRequest(Path root) {
        ReasoningRequest request = new ReasoningRequest();
        request.setProjectRoot(tempDir);
        request.setSourceScope("main");
        request.setSourcePath(tempDir.relativize(root).toString().replace('\\', '/'));

        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(tempDir.resolve("src/main/nl"));
        mappings.setTestRoot(tempDir.resolve("src/test/nl"));
        mappings.setTargetRoot(tempDir.resolve("src"));
        request.setBaseMappings(mappings);
        return request;
    }

    private Path write(String relative, String content) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }
}
