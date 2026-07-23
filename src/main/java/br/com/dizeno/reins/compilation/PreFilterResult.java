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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PreFilterResult is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public final class PreFilterResult {

    private final List<File> sourceFiles;
    private final List<File> filesToProcess;
    private final boolean runProjectInference;
    private final List<PreFilterSkipDecision> skipDecisions;
    private final List<CycleWorkSetEntry> workSetEntries;
    private final int validateAllPromotedCount;

    /**
     * Constructs a new instance of {@link PreFilterResult}.
     *
     * @param sourceFiles the list of source files to process
     * @param runProjectInference the run project inference
     * @param skipDecisions the skip decisions
     */
    public PreFilterResult(List<File> sourceFiles,
                           boolean runProjectInference,
                           List<PreFilterSkipDecision> skipDecisions) {
        this(sourceFiles,
                sourceFiles.stream()
                        .map(file -> new CycleWorkSetEntry(file, file.getPath(), SourceProcessingStatus.COMPILE))
                        .toList(),
                runProjectInference,
                skipDecisions,
                0);
    }

    /**
     * Constructs a new instance of {@link PreFilterResult}.
     *
     * @param sourceFiles the list of source files to process
     * @param workSetEntries the work set entries
     * @param runProjectInference the run project inference
     * @param skipDecisions the skip decisions
     */
    public PreFilterResult(List<File> sourceFiles,
                           List<CycleWorkSetEntry> workSetEntries,
                           boolean runProjectInference,
                           List<PreFilterSkipDecision> skipDecisions) {
            this(sourceFiles, workSetEntries, runProjectInference, skipDecisions, 0);
            }

            /**
             * Constructs a new instance of {@link PreFilterResult}.
             *
             * @param sourceFiles the list of source files to process
             * @param workSetEntries the work set entries
             * @param runProjectInference the run project inference
             * @param skipDecisions the skip decisions
             * @param validateAllPromotedCount the validate all promoted count
             */
            public PreFilterResult(List<File> sourceFiles,
                       List<CycleWorkSetEntry> workSetEntries,
                       boolean runProjectInference,
                       List<PreFilterSkipDecision> skipDecisions,
                       int validateAllPromotedCount) {
        this.sourceFiles = new ArrayList<>(sourceFiles);
        this.filesToProcess = workSetEntries.stream()
                .filter(entry -> entry.getStatus().shouldExecute())
                .map(CycleWorkSetEntry::getSourceFile)
                .toList();
        this.runProjectInference = runProjectInference;
        this.skipDecisions = new ArrayList<>(skipDecisions);
        this.workSetEntries = new ArrayList<>(workSetEntries);
            this.validateAllPromotedCount = Math.max(0, validateAllPromotedCount);
    }

    /**
     * Gets the source files.
     *
     * @return the collection of elements
     */
    public List<File> getSourceFiles() {
        return new ArrayList<>(sourceFiles);
    }

    /**
     * Gets the files to process.
     *
     * @return the collection of elements
     */
    public List<File> getFilesToProcess() {
        return new ArrayList<>(filesToProcess);
    }

    /**
     * Checks if the component is run project inference.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isRunProjectInference() {
        return runProjectInference;
    }

    /**
     * Gets the skip decisions.
     *
     * @return the collection of elements
     */
    public List<PreFilterSkipDecision> getSkipDecisions() {
        return new ArrayList<>(skipDecisions);
    }

    /**
     * Gets the work set entries.
     *
     * @return the collection of elements
     */
    public List<CycleWorkSetEntry> getWorkSetEntries() {
        return new ArrayList<>(workSetEntries);
    }

    /**
     * Gets the validate all promoted count.
     *
     * @return the numeric value
     */
    public int getValidateAllPromotedCount() {
        return validateAllPromotedCount;
    }

     
    /**
     * To Processing Queue.
     *
     * @return the resolved or constructed object
     */
    public SourceProcessingQueue toProcessingQueue() {
        return new SourceProcessingQueue(new ArrayList<>(workSetEntries));
    }

     
    /**
     * Fail Open.
     *
     * @param sourceFiles the list of source files to process
     * @return the resulting result
     */
    public static PreFilterResult failOpen(List<File> sourceFiles) {
        return new PreFilterResult(sourceFiles, true, Collections.emptyList());
    }
}
