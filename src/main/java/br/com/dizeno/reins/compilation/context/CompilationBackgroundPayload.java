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

import java.util.List;

/**
 * CompilationBackgroundPayload is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class CompilationBackgroundPayload {
    private final List<CompilationBackgroundFile> files;

    /**
     * Constructs a new instance of {@link CompilationBackgroundPayload}.
     *
     * @param files the list of files
     */
    public CompilationBackgroundPayload(List<CompilationBackgroundFile> files) {
        this.files = files == null ? List.of() : List.copyOf(files);
    }

    /**
     * Empty.
     *
     * @return the resulting payload
     */
    public static CompilationBackgroundPayload empty() {
        return new CompilationBackgroundPayload(List.of());
    }

    /**
     * Gets the files.
     *
     * @return the collection of elements
     */
    public List<CompilationBackgroundFile> getFiles() {
        return files;
    }

    /**
     * Checks if the component is empty.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isEmpty() {
        return files.isEmpty();
    }
}
