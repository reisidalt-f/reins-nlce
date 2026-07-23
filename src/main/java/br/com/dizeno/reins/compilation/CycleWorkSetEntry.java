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

import br.com.dizeno.reins.compilation.tracking.ReprocessingDecision;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CycleWorkSetEntry is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class CycleWorkSetEntry {
    private final File sourceFile;
    private final String sourcePath;
    private SourceProcessingStatus status;
    private boolean processed;
    private int orderIndex = -1;
    private ReprocessingDecision.ReprocessingReason selectionReason;
    private final List<String> transitionCauses = new ArrayList<>();

    /**
     * Constructs a new instance of {@link CycleWorkSetEntry}.
     *
     * @param sourceFile the source file to process
     * @param sourcePath the path of the source file
     * @param status the status
     */
    public CycleWorkSetEntry(File sourceFile,
                             String sourcePath,
                             SourceProcessingStatus status) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * Gets the source file.
     *
     * @return the resolved or constructed object
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public SourceProcessingStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     */
    public void setStatus(SourceProcessingStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * Checks if the component is processed.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Mark Processed.
     *
     */
    public void markProcessed() {
        this.processed = true;
    }

    /**
     * Gets the order index.
     *
     * @return the numeric value
     */
    public int getOrderIndex() {
        return orderIndex;
    }

    /**
     * Sets the order index.
     *
     * @param orderIndex the order index
     */
    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Gets the transition causes.
     *
     * @return the string result
     */
    public List<String> getTransitionCauses() {
        return new ArrayList<>(transitionCauses);
    }

    public ReprocessingDecision.ReprocessingReason getSelectionReason() {
        return selectionReason;
    }

    /**
     * Sets the selection reason.
     *
     * @param selectionReason the selection reason
     */
    public void setSelectionReason(ReprocessingDecision.ReprocessingReason selectionReason) {
        this.selectionReason = selectionReason;
    }

    /**
     * Mark Validate If Skippable.
     *
     * @param cause the cause
     * @return true if successful or matching, false otherwise
     */
    public boolean markValidateIfSkippable(String cause) {
        if (status == SourceProcessingStatus.COMPILE) {
            return false;
        }
        if (status == SourceProcessingStatus.SKIP) {
            status = SourceProcessingStatus.VALIDATE;
        }
        if (cause != null && !cause.isBlank() && !transitionCauses.contains(cause)) {
            transitionCauses.add(cause);
        }
        return true;
    }

     
    /**
     * Mark As Note Triggered.
     *
     */
    public void markAsNoteTriggered() {
        if (status == SourceProcessingStatus.SKIP) {
            status = SourceProcessingStatus.COMPILE;
        }
        if (!transitionCauses.contains("note-triggered")) {
            transitionCauses.add("note-triggered");
        }
        this.selectionReason = ReprocessingDecision.ReprocessingReason.NOTES_BACKTRACK;
    }
}