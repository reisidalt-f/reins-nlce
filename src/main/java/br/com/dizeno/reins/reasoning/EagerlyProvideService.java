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

import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.source.domain.FileReference;
import br.com.dizeno.reins.security.PathValidator;
import org.apache.maven.plugin.logging.Log;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;
import br.com.dizeno.reins.util.PathLogFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * EagerlyProvideService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Pre-loads and merges attachment payloads for first-turn LLM reasoning context.
 */
public class EagerlyProvideService {

    /**
     * Builds the configured target.
     *
     * @param priorRecord the prior record
     * @param settings the eagerly provide settings
     * @param contextSettings the context settings
     * @param eagerlyProvidedLoggingEnabled the eagerly provided logging enabled
     * @param projectRoot the root path of the project
     * @param validator the path validator for security boundary checks
     * @param log the logger instance
     * @return the resulting result
     */
    public EagerlyProvideResult build(SourceTrackingRecord priorRecord,
                                      EagerlyProvideSettings settings,
                                      ContextSettings contextSettings,
                                      boolean eagerlyProvidedLoggingEnabled,
                                      Path projectRoot,
                                      PathValidator validator,
                                      Log log) {
        boolean compiledFiles = contextSettings != null && contextSettings.isCompiledFiles();
        boolean inspectedFiles = contextSettings != null && contextSettings.isInspectedFiles();
        if (!compiledFiles && !inspectedFiles) {
            if (eagerlyProvidedLoggingEnabled) {
                log.info("Eagerly provide disabled for both categories — skipping all prior-cycle file attachments");
            }
            return EagerlyProvideResult.empty();
        }
        if (priorRecord == null) {
            return EagerlyProvideResult.empty();
        }
        List<AttachedFilePayload> compiled = Collections.emptyList();
        List<AttachedFilePayload> inspected = Collections.emptyList();
        List<CompiledSourceGroup> groups = Collections.emptyList();
        Path resolvedTargetRoot = resolveTargetRoot(priorRecord, projectRoot);
        String sourceCanonicalPath = priorRecord.getSourcePath();
        String sourceSimpleName = resolveSimpleName(sourceCanonicalPath);
        if (compiledFiles) {
            compiled = buildPayloads(priorRecord.getCompiledFiles(), "previously-compiled",
                settings, eagerlyProvidedLoggingEnabled, projectRoot, resolvedTargetRoot, validator, log,
                sourceCanonicalPath, sourceSimpleName);
            groups = List.of(new CompiledSourceGroup(sourceCanonicalPath, sourceSimpleName, compiled));
        }
        if (inspectedFiles) {
            inspected = buildPayloads(priorRecord.getInspectedFiles(), "previously-inspected",
                settings, eagerlyProvidedLoggingEnabled, projectRoot, resolvedTargetRoot, validator, log,
                sourceCanonicalPath, sourceSimpleName);
        }
        return new EagerlyProvideResult(compiled, inspected, groups);
    }

    private List<AttachedFilePayload> buildPayloads(Map<String, ?> fileMap, String base,
                                                    EagerlyProvideSettings settings,
                                                    boolean eagerlyProvidedLoggingEnabled,
                                                    Path projectRoot, Path targetRoot,
                                                    PathValidator validator, Log log,
                                                    String sourceCanonicalPath,
                                                    String sourceSimpleName) {
        if (fileMap == null || fileMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sorted = new ArrayList<>(fileMap.keySet());
        Collections.sort(sorted);
        List<AttachedFilePayload> result = new ArrayList<>();
        for (String storedReference : sorted) {
            FileReference reference;
            try {
                reference = FileReference.fromCanonical(storedReference);
            } catch (IllegalArgumentException ex) {
                log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot) + " — " + ex.getMessage());
                continue;
            }
            if ("script".equals(reference.getBaseName())) {
                log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot) + " — script base is not attachable");
                continue;
            }
            Path resolvedPath = reference.toAbsolutePathByBaseName(resolvedBase -> switch (resolvedBase) {
                case "main" -> projectRoot.resolve("src/main/nl").normalize();
                case "test" -> projectRoot.resolve("src/test/nl").normalize();
                case "target" -> targetRoot;
                case "script" -> throw new IllegalArgumentException("Script base is not supported for eager attachments.");
                default -> projectRoot.resolve(resolvedBase).normalize();
            });
            try {
                validator.validateInProject(resolvedPath);
            } catch (SecurityException e) {
                log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot) + " — " + e.getMessage());
                continue;
            }
            if (!Files.isRegularFile(resolvedPath)) {
                log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot) + " — not a regular file");
                continue;
            }
            long sizeLimit = settings.getMaxAttachmentSizeBytes();
            if (sizeLimit > 0) {
                long size;
                try {
                    size = Files.size(resolvedPath);
                } catch (IOException e) {
                    log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot)
                            + " — could not read size: " + e.getMessage());
                    continue;
                }
                if (size > sizeLimit) {
                    throw new EagerlyProvideTooLargeException(storedReference, size, sizeLimit);
                }
            }
            String content;
            try {
                content = Files.readString(resolvedPath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Eagerly provide: skipping " + PathLogFormatter.formatPath(storedReference, projectRoot)
                        + " — could not read content: " + e.getMessage());
                continue;
            }
            AttachedFilePayload payload = new AttachedFilePayload();
            payload.setBase(reference.getBase().value());
            payload.setRelativePath(reference.getPath());
            payload.setQualifiedPath(reference.toCanonicalString());
            payload.setContent(content);
            payload.setSourceCanonicalPath(sourceCanonicalPath);
            payload.setSourceSimpleName(sourceSimpleName);
            if (eagerlyProvidedLoggingEnabled) {
                log.info("Eagerly provide: attaching [" + reference.getBase().value() + "] " + PathLogFormatter.formatPath(resolvedPath, projectRoot));
            }
            result.add(payload);
        }
        return result;
    }

    private String resolveSimpleName(String canonicalPath) {
        if (canonicalPath == null || canonicalPath.isBlank()) {
            return "unknown.md";
        }
        String pathPart = canonicalPath;
        int baseSeparator = canonicalPath.indexOf(':');
        if (baseSeparator >= 0 && baseSeparator + 1 < canonicalPath.length()) {
            pathPart = canonicalPath.substring(baseSeparator + 1);
        }
        String normalized = pathPart.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            return normalized.substring(slash + 1);
        }
        return normalized;
    }

    private Path resolveTargetRoot(SourceTrackingRecord priorRecord, Path projectRoot) {
        if (priorRecord == null || priorRecord.getResolvedTargetRoot() == null || priorRecord.getResolvedTargetRoot().isBlank()) {
            return projectRoot;
        }
        return projectRoot.resolve(priorRecord.getResolvedTargetRoot()).normalize();
    }
}
