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

/**
 * CompilationSummary is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing compilation summary.
 */
public class CompilationSummary {
    private int discovered;
    private int processed;
    private int compiled;
    private int skipped;
    private int failed;
    private int dependencyLinks;
    private int reprocessedDueToStaleness;
    private int reprocessedDueToChildChange;
    private int validateAllPromoted;

    /**
     * Gets the discovered.
     *
     * @return the numeric value
     */
    public int getDiscovered() {
        return discovered;
    }

    /**
     * Sets the discovered.
     *
     * @param discovered the discovered
     */
    public void setDiscovered(int discovered) {
        this.discovered = discovered;
    }

    /**
     * Gets the processed.
     *
     * @return the numeric value
     */
    public int getProcessed() {
        return processed;
    }

    /**
     * Increment Processed.
     *
     */
    public void incrementProcessed() {
        this.processed++;
    }

    /**
     * Gets the compiled.
     *
     * @return the numeric value
     */
    public int getCompiled() {
        return compiled;
    }

    /**
     * Increment Compiled.
     *
     */
    public void incrementCompiled() {
        this.compiled++;
    }

    /**
     * Gets the skipped.
     *
     * @return the numeric value
     */
    public int getSkipped() {
        return skipped;
    }

    /**
     * Increment Skipped.
     *
     */
    public void incrementSkipped() {
        this.skipped++;
    }

    /**
     * Gets the failed.
     *
     * @return the numeric value
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Increment Failed.
     *
     */
    public void incrementFailed() {
        this.failed++;
    }

    /**
     * Gets the dependency links.
     *
     * @return the numeric value
     */
    public int getDependencyLinks() {
        return dependencyLinks;
    }

    /**
     * Sets the dependency links.
     *
     * @param dependencyLinks the dependency links
     */
    public void setDependencyLinks(int dependencyLinks) {
        this.dependencyLinks = dependencyLinks;
    }

    /**
     * Gets the reprocessed due to staleness.
     *
     * @return the numeric value
     */
    public int getReprocessedDueToStaleness() {
        return reprocessedDueToStaleness;
    }

    /**
     * Increment Reprocessed Due To Staleness.
     *
     */
    public void incrementReprocessedDueToStaleness() {
        this.reprocessedDueToStaleness++;
    }

    /**
     * Gets the reprocessed due to child change.
     *
     * @return the numeric value
     */
    public int getReprocessedDueToChildChange() {
        return reprocessedDueToChildChange;
    }

    /**
     * Increment Reprocessed Due To Child Change.
     *
     */
    public void incrementReprocessedDueToChildChange() {
        this.reprocessedDueToChildChange++;
    }

    /**
     * Gets the validate all promoted.
     *
     * @return the numeric value
     */
    public int getValidateAllPromoted() {
        return validateAllPromoted;
    }

    /**
     * Add Validate All Promoted.
     *
     * @param count the count
     */
    public void addValidateAllPromoted(int count) {
        if (count > 0) {
            this.validateAllPromoted += count;
        }
    }
}
