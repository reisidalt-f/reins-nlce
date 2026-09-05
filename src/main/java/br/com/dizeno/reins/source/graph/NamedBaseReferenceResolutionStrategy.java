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
import java.util.List;

/**
 * NamedBaseReferenceResolutionStrategy handles references formatted as baseScheme:/subpath or baseScheme:subpath.
 */
public class NamedBaseReferenceResolutionStrategy implements ResolutionStrategy {

    @Override
    public String strategyId() {
        return "named-base-scheme";
    }

    @Override
    public boolean applies(ReferenceRequest request, ResolverContext context) {
        if (request == null || request.rawReference() == null || context == null) {
            return false;
        }
        String raw = request.rawReference();
        int colonIdx = raw.indexOf(':');
        if (colonIdx <= 0 || raw.contains("://")) {
            return false;
        }
        String scheme = raw.substring(0, colonIdx);
        return context.getSourceBase(scheme) != null;
    }

    @Override
    public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) {
        if (!applies(request, context)) {
            return StrategyAttemptResult.noMatch("strategy does not apply");
        }
        String raw = request.rawReference();
        int colonIdx = raw.indexOf(':');
        String scheme = raw.substring(0, colonIdx);
        String subPathStr = raw.substring(colonIdx + 1);
        if (subPathStr.startsWith("/")) {
            subPathStr = subPathStr.substring(1);
        }

        Path baseRoot = context.getSourceBase(scheme);
        Path targetPath = context.getPathValidator().validateInProject(baseRoot.resolve(subPathStr));
        if (Files.isRegularFile(targetPath)) {
            return StrategyAttemptResult.fromCandidates(List.of(targetPath.toAbsolutePath().normalize()), "resolved named base reference");
        }
        return StrategyAttemptResult.noMatch("named base file not found");
    }
}
