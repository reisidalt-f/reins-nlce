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

package br.com.dizeno.reins.compilation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * MtimeSnapshotUtil is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing mtime snapshot util.
 */
public final class MtimeSnapshotUtil {

    private MtimeSnapshotUtil() {}

    /**
     * Snapshot.
     *
     * @param projectRoot the root path of the project
     * @param relativePaths the relative paths
     * @return the resulting result
     */
    public static SnapshotResult snapshot(Path projectRoot, Set<String> relativePaths) {
        return snapshot(projectRoot, relativePaths, null);
    }

    /**
     * Snapshot.
     *
     * @param projectRoot the root path of the project
     * @param relativePaths the relative paths
     * @param resolvedTargetRoot the resolved target root
     * @return the resulting result
     */
    public static SnapshotResult snapshot(Path projectRoot, Set<String> relativePaths, String resolvedTargetRoot) {
        Map<String, Long> mtimes = new LinkedHashMap<>();
        Set<String> missingOrUnreadable = new LinkedHashSet<>();
        for (String relativePath : relativePaths) {
            Path absolutePath;
            try {
                absolutePath = br.com.dizeno.reins.compilation.tracking.TrackedPathResolver.resolveTrackedPath(
                        projectRoot,
                        relativePath,
                        resolvedTargetRoot
                );
            } catch (Exception ex) {
                mtimes.put(relativePath, null);
                missingOrUnreadable.add(relativePath);
                continue;
            }
            try {
                if (!Files.exists(absolutePath) || Files.isDirectory(absolutePath)) {
                    mtimes.put(relativePath, null);
                    missingOrUnreadable.add(relativePath);
                    continue;
                }
                mtimes.put(relativePath, Files.getLastModifiedTime(absolutePath).toMillis());
            } catch (Exception ex) {
                mtimes.put(relativePath, null);
                missingOrUnreadable.add(relativePath);
            }
        }
        return new SnapshotResult(mtimes, missingOrUnreadable);
    }

    /**
     * SnapshotResult is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public record SnapshotResult(Map<String, Long> mtimes, Set<String> missingOrUnreadablePaths) {}
}
