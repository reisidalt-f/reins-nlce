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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * TestToMainFallbackResolutionStrategy is part of the general application functions in the reins architecture.
 * Acts as a component managing test to main fallback resolution strategy.
 */
public class TestToMainFallbackResolutionStrategy implements ResolutionStrategy {
    /**
     * Strategy Id.
     *
     * @return the string result
     */
    @Override
    public String strategyId() {
        return "test-to-main-fallback";
    }

    /**
     * Applies.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean applies(ReferenceRequest request, ResolverContext context) {
        return request.refererNlScope() == ReferenceRequest.NlScope.TEST;
    }

    /**
     * Attempt.
     *
     * @param request the request containing path and scope metadata
     * @param context the context
     * @return the resulting result
     */
    @Override
    public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) throws Exception {
        Path mainRoot = context.getMainNlRoot();
        if (!Files.exists(mainRoot)) {
            return StrategyAttemptResult.noMatch("main NL root missing");
        }

        String normalizedRaw = normalizeRef(request.rawReference());

        Path mirrored = resolveMirroredMainPath(request.refererPath(), normalizedRaw, context.getProjectRoot(), mainRoot);
        if (mirrored != null && Files.exists(mirrored) && Files.isRegularFile(mirrored)) {
            return StrategyAttemptResult.fromCandidates(List.of(mirrored), "resolved mirrored fallback candidate");
        }

        String fileName = Path.of(normalizedRaw).getFileName().toString().toLowerCase(Locale.ROOT);
        String normalizedTail = stripParentSegments(normalizedRaw).toLowerCase(Locale.ROOT);
        String slashTail = "/" + normalizedTail;

        Set<Path> candidates = new LinkedHashSet<>();
        try (Stream<Path> stream = Files.walk(mainRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
                    .forEach(path -> {
                        String relative = normalizeRef(mainRoot.relativize(path).toString()).toLowerCase(Locale.ROOT);
                        String candidateFileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        boolean matches = candidateFileName.equals(fileName)
                            || relative.equals(normalizedRaw.toLowerCase(Locale.ROOT))
                            || relative.equals(normalizedTail)
                            || relative.endsWith(slashTail);
                        if (matches) {
                            candidates.add(path);
                        }
                    });
        }

        if (candidates.isEmpty()) {
            return StrategyAttemptResult.noMatch("no fallback candidates under src/main/nl");
        }

        if (candidates.size() == 1) {
            return StrategyAttemptResult.fromCandidates(new ArrayList<>(candidates), "resolved fallback candidate");
        }

        String subtree = refererSubtree(request.refererPath());
        List<Path> preferred = candidates.stream()
                .filter(path -> candidateSubtree(path, mainRoot).equals(subtree))
                .toList();

        if (preferred.size() == 1) {
            return StrategyAttemptResult.fromCandidates(preferred, "resolved subtree-preferred fallback candidate");
        }

        List<Path> ambiguousPool = preferred.isEmpty() ? new ArrayList<>(candidates) : preferred;
        return StrategyAttemptResult.fromCandidates(ambiguousPool, "ambiguous fallback candidates");
    }

    private String normalizeRef(String value) {
        return value.replace('\\', '/');
    }

    private String stripParentSegments(String value) {
        String stripped = value;
        while (stripped.startsWith("../")) {
            stripped = stripped.substring(3);
        }
        return stripped;
    }

    private String refererSubtree(String refererPath) {
        String normalized = normalizeRef(refererPath);
        String prefix = "src/test/nl/";
        if (!normalized.startsWith(prefix)) {
            return "";
        }
        String afterRoot = normalized.substring(prefix.length());
        int lastSlash = afterRoot.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return afterRoot.substring(0, lastSlash);
    }

    private String candidateSubtree(Path candidatePath, Path mainRoot) {
        Path relative = mainRoot.relativize(candidatePath);
        Path parent = relative.getParent();
        return parent == null ? "" : normalizeRef(parent.toString());
    }

    private Path resolveMirroredMainPath(String refererPath,
                                         String rawReference,
                                         Path projectRoot,
                                         Path mainRoot) {
        String normalizedReferer = normalizeRef(refererPath);
        String prefix = "src/test/nl/";
        if (!normalizedReferer.startsWith(prefix)) {
            return null;
        }

        String refererRelative = normalizedReferer.substring(prefix.length());
        Path refererRelativePath = Path.of(refererRelative).getParent();
        Path effectiveBase = refererRelativePath == null ? Path.of("") : refererRelativePath;

        Path candidateRelative = effectiveBase.resolve(rawReference).normalize();
        Path mirrored = mainRoot.resolve(candidateRelative).normalize();
        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();
        if (!mirrored.startsWith(normalizedProjectRoot)) {
            return null;
        }
        return mirrored;
    }
}
