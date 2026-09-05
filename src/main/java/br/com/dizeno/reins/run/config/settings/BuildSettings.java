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

package br.com.dizeno.reins.run.config.settings;

/**
 * BuildSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class BuildSettings {
    private int compilationThreads = 1;
    private boolean freshCompilation = false;

    /**
     * Gets the compilationThreads.
     *
     * @return the maximum number of compilation threads
     */
    public int getCompilationThreads() {
        return compilationThreads;
    }

    /**
     * Sets the compilationThreads.
     *
     * @param compilationThreads the maximum number of compilation threads
     */
    public void setCompilationThreads(int compilationThreads) {
        this.compilationThreads = compilationThreads;
    }

    /**
     * Checks if freshCompilation is enabled.
     *
     * @return true if freshCompilation is enabled, false otherwise
     */
    public boolean isFreshCompilation() {
        return freshCompilation;
    }

    /**
     * Sets the freshCompilation parameter.
     *
     * @param freshCompilation whether to perform a fresh compilation
     */
    public void setFreshCompilation(boolean freshCompilation) {
        this.freshCompilation = freshCompilation;
    }
}
