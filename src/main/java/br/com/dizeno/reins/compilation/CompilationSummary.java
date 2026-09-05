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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompilationSummary is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing compilation summary.
 */
public class CompilationSummary {
    private final AtomicInteger discovered = new AtomicInteger(0);
    private final AtomicInteger processed = new AtomicInteger(0);
    private final AtomicInteger compiled = new AtomicInteger(0);
    private final AtomicInteger noChange = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger dependencyLinks = new AtomicInteger(0);
    private final AtomicInteger reprocessedDueToStaleness = new AtomicInteger(0);
    private final AtomicInteger reprocessedDueToChildChange = new AtomicInteger(0);
    private final AtomicInteger validateAllPromoted = new AtomicInteger(0);

    /**
     * Gets the discovered.
     *
     * @return the numeric value
     */
    public int getDiscovered() {
        return discovered.get();
    }

    /**
     * Sets the discovered.
     *
     * @param discovered the discovered
     */
    public void setDiscovered(int discovered) {
        this.discovered.set(discovered);
    }

    /**
     * Gets the processed.
     *
     * @return the numeric value
     */
    public int getProcessed() {
        return processed.get();
    }

    /**
     * Increment Processed.
     *
     */
    public void incrementProcessed() {
        this.processed.incrementAndGet();
    }

    /**
     * Gets the compiled.
     *
     * @return the numeric value
     */
    public int getCompiled() {
        return compiled.get();
    }

    /**
     * Increment Compiled.
     *
     */
    public void incrementCompiled() {
        this.compiled.incrementAndGet();
    }

    /**
     * Gets the noChange count.
     *
     * @return the numeric value
     */
    public int getNoChange() {
        return noChange.get();
    }

    /**
     * Increment NoChange.
     *
     */
    public void incrementNoChange() {
        this.noChange.incrementAndGet();
    }

    /**
     * Gets the skipped.
     *
     * @return the numeric value
     */
    public int getSkipped() {
        return skipped.get();
    }

    /**
     * Increment Skipped.
     *
     */
    public void incrementSkipped() {
        this.skipped.incrementAndGet();
    }

    /**
     * Gets the failed.
     *
     * @return the numeric value
     */
    public int getFailed() {
        return failed.get();
    }

    /**
     * Increment Failed.
     *
     */
    public void incrementFailed() {
        this.failed.incrementAndGet();
    }

    /**
     * Gets the dependency links.
     *
     * @return the numeric value
     */
    public int getDependencyLinks() {
        return dependencyLinks.get();
    }

    /**
     * Sets the dependency links.
     *
     * @param dependencyLinks the dependency links
     */
    public void setDependencyLinks(int dependencyLinks) {
        this.dependencyLinks.set(dependencyLinks);
    }

    /**
     * Gets the reprocessed due to staleness.
     *
     * @return the numeric value
     */
    public int getReprocessedDueToStaleness() {
        return reprocessedDueToStaleness.get();
    }

    /**
     * Increment Reprocessed Due To Staleness.
     *
     */
    public void incrementReprocessedDueToStaleness() {
        this.reprocessedDueToStaleness.incrementAndGet();
    }

    /**
     * Gets the reprocessed due to child change.
     *
     * @return the numeric value
     */
    public int getReprocessedDueToChildChange() {
        return reprocessedDueToChildChange.get();
    }

    /**
     * Increment Reprocessed Due To Child Change.
     *
     */
    public void incrementReprocessedDueToChildChange() {
        this.reprocessedDueToChildChange.incrementAndGet();
    }

    /**
     * Gets the validate all promoted.
     *
     * @return the numeric value
     */
    public int getValidateAllPromoted() {
        return validateAllPromoted.get();
    }

    /**
     * Add Validate All Promoted.
     *
     * @param count the count
     */
    public void addValidateAllPromoted(int count) {
        if (count > 0) {
            this.validateAllPromoted.addAndGet(count);
        }
    }
}
