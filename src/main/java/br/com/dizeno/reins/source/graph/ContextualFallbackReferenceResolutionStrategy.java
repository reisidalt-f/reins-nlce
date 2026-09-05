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
import java.util.List;
import java.util.Map;

/**
 * ContextualFallbackReferenceResolutionStrategy searches fallback source bases according to the referer's scope:
 * - From main: fallback to custom bases excluding test
 * - From test: fallback to main base first, then other custom bases
 * - Other custom bases: fallback to other custom bases excluding main and test
 */
public class ContextualFallbackReferenceResolutionStrategy implements ResolutionStrategy {

    @Override
    public String strategyId() {
        return "contextual-fallback";
    }

    @Override
    public boolean applies(ReferenceRequest request, ResolverContext context) {
        return request != null && request.rawReference() != null && !request.rawReference().contains("://");
    }

    @Override
    public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) {
        if (!applies(request, context)) {
            return StrategyAttemptResult.noMatch("strategy does not apply");
        }

        String rawRef = request.rawReference();
        if (rawRef.startsWith("/")) {
            rawRef = rawRef.substring(1);
        }
        int colonIdx = rawRef.indexOf(':');
        if (colonIdx > 0) {
            rawRef = rawRef.substring(colonIdx + 1);
            if (rawRef.startsWith("/")) {
                rawRef = rawRef.substring(1);
            }
        }

        String refererCategory = determineRefererCategory(request.refererDir(), context);
        List<Path> fallbackBases = buildFallbackBases(refererCategory, context);

        Path refererSubPath = null;
        if (context.getSourceBases() != null && context.getSourceBases().containsKey(refererCategory)) {
            Path categoryBase = context.getSourceBases().get(refererCategory).toAbsolutePath().normalize();
            Path normalizedReferer = request.refererDir().toAbsolutePath().normalize();
            if (normalizedReferer.startsWith(categoryBase)) {
                refererSubPath = categoryBase.relativize(normalizedReferer);
            }
        }

        for (Path baseRoot : fallbackBases) {
            if (baseRoot == null || !Files.exists(baseRoot)) {
                continue;
            }
            if (refererSubPath != null) {
                Path mirroredCandidate = context.getPathValidator().validateInProject(baseRoot.resolve(refererSubPath).resolve(rawRef));
                if (Files.isRegularFile(mirroredCandidate)) {
                    return StrategyAttemptResult.fromCandidates(List.of(mirroredCandidate.toAbsolutePath().normalize()), "resolved contextual fallback reference");
                }
            }
            Path candidate = context.getPathValidator().validateInProject(baseRoot.resolve(rawRef));
            if (Files.isRegularFile(candidate)) {
                return StrategyAttemptResult.fromCandidates(List.of(candidate.toAbsolutePath().normalize()), "resolved contextual fallback reference");
            }
        }

        return StrategyAttemptResult.noMatch("contextual fallback file not found");
    }

    private String determineRefererCategory(Path refererDir, ResolverContext context) {
        if (refererDir == null || context == null || context.getSourceBases() == null) {
            return "main";
        }
        Path normalizedReferer = refererDir.toAbsolutePath().normalize();
        for (Map.Entry<String, Path> entry : context.getSourceBases().entrySet()) {
            if (entry.getValue() != null && normalizedReferer.startsWith(entry.getValue().toAbsolutePath().normalize())) {
                return entry.getKey().toLowerCase(java.util.Locale.ROOT);
            }
        }
        return "main";
    }

    private List<Path> buildFallbackBases(String refererCategory, ResolverContext context) {
        List<Path> bases = new ArrayList<>();
        if (context == null || context.getSourceBases() == null) {
            return bases;
        }

        Map<String, Path> sourceBases = context.getSourceBases();

        if ("main".equalsIgnoreCase(refererCategory)) {
            // Main referer: fallback to custom bases excluding main and test
            for (Map.Entry<String, Path> entry : sourceBases.entrySet()) {
                String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                if (!"main".equals(key) && !"test".equals(key)) {
                    bases.add(entry.getValue());
                }
            }
        } else if ("test".equalsIgnoreCase(refererCategory)) {
            // Test referer: look into test first, fallback to main, then other custom bases
            if (sourceBases.containsKey("test")) {
                bases.add(sourceBases.get("test"));
            }
            if (sourceBases.containsKey("main")) {
                bases.add(sourceBases.get("main"));
            }
            for (Map.Entry<String, Path> entry : sourceBases.entrySet()) {
                String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                if (!"main".equals(key) && !"test".equals(key)) {
                    bases.add(entry.getValue());
                }
            }
        } else {
            // Other custom base referer: referer's base first, then other custom bases EXCLUDING main and test
            if (sourceBases.containsKey(refererCategory)) {
                bases.add(sourceBases.get(refererCategory));
            }
            for (Map.Entry<String, Path> entry : sourceBases.entrySet()) {
                String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
                if (!"main".equals(key) && !"test".equals(key) && !refererCategory.equalsIgnoreCase(key)) {
                    bases.add(entry.getValue());
                }
            }
        }

        return bases;
    }
}
