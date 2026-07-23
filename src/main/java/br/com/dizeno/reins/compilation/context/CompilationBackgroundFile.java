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

package br.com.dizeno.reins.compilation.context;

import java.nio.file.Path;

/**
 * CompilationBackgroundFile is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
 * Acts as a component managing compilation background file.
 */
public class CompilationBackgroundFile {
    private final Path absolutePath;
    private final String displayPath;
    private final String content;

    /**
     * Constructs a new instance of {@link CompilationBackgroundFile}.
     *
     * @param absolutePath the absolute path
     * @param displayPath the display path
     * @param content the content
     */
    public CompilationBackgroundFile(Path absolutePath, String displayPath, String content) {
        this.absolutePath = absolutePath;
        this.displayPath = displayPath;
        this.content = content;
    }

    /**
     * Gets the absolute path.
     *
     * @return the resolved or constructed object
     */
    public Path getAbsolutePath() {
        return absolutePath;
    }

    /**
     * Gets the display path.
     *
     * @return the string result
     */
    public String getDisplayPath() {
        return displayPath;
    }

    /**
     * Gets the content.
     *
     * @return the string result
     */
    public String getContent() {
        return content;
    }
}
