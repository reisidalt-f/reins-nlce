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

package br.com.dizeno.reins.source.scanner;

import br.com.dizeno.reins.security.PathValidator;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * CoderMdScanner is part of the general application functions in the reins architecture.
 * Acts as a component managing coder md scanner.
 */
public class CoderMdScanner {
    /**
     * Scan.
     *
     * @param roots the roots
     * @param includePattern the include pattern
     * @param pathValidator the path validator
     * @return the collection of elements
     */
    public List<File> scan(List<File> roots, String includePattern, PathValidator pathValidator) {
        return scan(roots, includePattern, pathValidator, SourceDiscoveryMode.FULL_SCAN);
    }

    /**
     * Scan.
     *
     * @param roots the roots
     * @param includePattern the include pattern
     * @param pathValidator the path validator
     * @param discoveryMode the discovery mode
     * @return the collection of elements
     */
    public List<File> scan(List<File> roots,
                           String includePattern,
                           PathValidator pathValidator,
                           SourceDiscoveryMode discoveryMode) {
        if (discoveryMode == SourceDiscoveryMode.EXPLICIT_SOURCE) {
            return List.of();
        }
        List<File> files = new ArrayList<>();
        for (File root : roots) {
            if (root == null || !root.exists() || !root.isDirectory()) {
                continue;
            }
            File normalizedRoot = pathValidator.validateInProject(root.toPath()).toFile();
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir(normalizedRoot);
            scanner.setIncludes(new String[]{includePattern});
            scanner.setExcludes(new String[]{"**/target/**", "**/.git/**"});
            scanner.scan();
            for (String item : scanner.getIncludedFiles()) {
                File found = new File(normalizedRoot, item);
                pathValidator.validateInProject(found.toPath());
                files.add(found);
            }
        }
        files.sort((a, b) -> a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath()));
        return files;
    }
}
