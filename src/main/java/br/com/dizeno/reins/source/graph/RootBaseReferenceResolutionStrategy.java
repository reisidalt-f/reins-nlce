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
 * RootBaseReferenceResolutionStrategy handles references starting with '/' by resolving them relative to the referer's source base root directory.
 */
public class RootBaseReferenceResolutionStrategy implements ResolutionStrategy {

    @Override
    public String strategyId() {
        return "root-base-relative";
    }

    @Override
    public boolean applies(ReferenceRequest request, ResolverContext context) {
        return request != null && request.rawReference() != null && request.rawReference().startsWith("/");
    }

    @Override
    public StrategyAttemptResult attempt(ReferenceRequest request, ResolverContext context) {
        if (!applies(request, context)) {
            return StrategyAttemptResult.noMatch("strategy does not apply");
        }
        String subPathStr = request.rawReference().substring(1);
        Path refererDir = request.refererDir();
        Path refererBaseRoot = findRefererBaseRoot(refererDir, context);
        if (refererBaseRoot == null) {
            refererBaseRoot = context.getProjectRoot();
        }

        Path targetPath = context.getPathValidator().validateInProject(refererBaseRoot.resolve(subPathStr));
        if (Files.isRegularFile(targetPath)) {
            return StrategyAttemptResult.fromCandidates(List.of(targetPath.toAbsolutePath().normalize()), "resolved root base relative reference");
        }
        return StrategyAttemptResult.noMatch("root base relative file not found");
    }

    private Path findRefererBaseRoot(Path refererDir, ResolverContext context) {
        if (refererDir == null || context == null || context.getSourceBases() == null) {
            return null;
        }
        Path normalizedReferer = refererDir.toAbsolutePath().normalize();
        for (Path baseRoot : context.getSourceBases().values()) {
            if (baseRoot != null && normalizedReferer.startsWith(baseRoot.toAbsolutePath().normalize())) {
                return baseRoot.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
