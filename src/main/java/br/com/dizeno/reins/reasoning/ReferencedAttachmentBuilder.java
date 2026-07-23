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

import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;
import br.com.dizeno.reins.source.domain.SourceScope;
import br.com.dizeno.reins.source.graph.GraphProcessingException;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

 
/**
 * ReferencedAttachmentBuilder is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a helper utility for building its prefix objects.
 */
public class ReferencedAttachmentBuilder {

    /**
     * Builds the configured target.
     *
     * @param treeContext the tree context
     * @param projectRoot the root path of the project
     * @return the collection of elements
     */
    public List<AttachedFilePayload> build(ReferenceTreeContextService.ReferenceTreeContext treeContext,
                                           Path projectRoot) {
        if (treeContext == null || treeContext.getNodesByCanonical() == null || treeContext.getNodesByCanonical().isEmpty()) {
            return List.of();
        }

        String rootCanonicalPath = treeContext.getRootCanonicalPath();
        PathValidator validator = new PathValidator(projectRoot);
        List<AttachedFilePayload> attachments = new ArrayList<>();
        List<String> orderedCanonicalPaths = collectOrderedCanonicalPaths(treeContext, rootCanonicalPath);

        for (String canonicalPath : orderedCanonicalPaths) {
            Path absolutePath;
            try {
                absolutePath = validator.validateInProject(projectRoot.resolve(canonicalPath).normalize());
            } catch (SecurityException ex) {
                throw new GraphProcessingException(
                        GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                        List.of(rootCanonicalPath, canonicalPath),
                        "Referenced file is outside project root: " + projectRoot.resolve(canonicalPath).normalize());
            }

            String content;
            try {
                content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new GraphProcessingException(
                        GraphProcessingException.ViolationType.UNRESOLVED_REFERENCE,
                        List.of(rootCanonicalPath, canonicalPath),
                        "Failed to read referenced file for attachment: " + canonicalPath + ": " + ex.getMessage());
            }

            AttachedFilePayload attachment = new AttachedFilePayload();
            SourceScope scope = SourceScope.fromPath(canonicalPath);
            String base = scope == SourceScope.TEST ? "test" : "main";
            String relativePath = relativizeToScope(canonicalPath, scope);
            attachment.setBase(base);
            attachment.setRelativePath(relativePath);
            attachment.setQualifiedPath(base + ":" + relativePath);
            attachment.setContent(content);
            attachments.add(attachment);
        }

        return attachments;
    }

    private List<String> collectOrderedCanonicalPaths(ReferenceTreeContextService.ReferenceTreeContext treeContext,
                                                      String rootCanonicalPath) {
        List<String> ordered = new ArrayList<>();

        for (Map.Entry<String, ReferenceTreeContextService.ReferenceNode> entry : treeContext.getNodesByCanonical().entrySet()) {
            String canonicalPath = entry.getKey();
            if (canonicalPath != null && !canonicalPath.equals(rootCanonicalPath) && !ordered.contains(canonicalPath)) {
                ordered.add(canonicalPath);
            }
        }

        for (ReferenceTreeContextService.ReferenceNode node : treeContext.getNodesByCanonical().values()) {
            for (ReferenceTreeContextService.ReferenceEdge edge : node.getEdges()) {
                String target = edge.targetCanonicalPath();
                if (target != null && !target.equals(rootCanonicalPath) && !ordered.contains(target)) {
                    ordered.add(target);
                }
            }
        }

        return ordered;
    }

    private String relativizeToScope(String canonicalPath, SourceScope scope) {
        if (canonicalPath == null || canonicalPath.isBlank()) {
            return "";
        }
        if (scope == SourceScope.TEST && canonicalPath.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return canonicalPath.substring((ProjectDirectoryPaths.TEST_NL_ROOT + "/").length());
        }
        if (canonicalPath.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return canonicalPath.substring((ProjectDirectoryPaths.MAIN_NL_ROOT + "/").length());
        }
        return canonicalPath;
    }
}
