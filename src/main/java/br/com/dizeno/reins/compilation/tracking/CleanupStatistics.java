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

package br.com.dizeno.reins.compilation.tracking;

 
/**
 * CleanupStatistics is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup statistics.
 */
public class CleanupStatistics {
    
    private final int targetedForDeletion;
    private final int successfullyDeleted;
    private final int alreadyMissing;
    private final int rejected;
    private final int failedDeletion;
    private final int trackingRecordsUpdated;
    
     
    /**
     * Constructs a new instance of {@link CleanupStatistics}.
     *
     * @param targetedForDeletion the targeted for deletion
     * @param successfullyDeleted the successfully deleted
     * @param alreadyMissing the already missing
     * @param rejected the rejected
     * @param failedDeletion the failed deletion
     * @param trackingRecordsUpdated the tracking records updated
     */
    public CleanupStatistics(int targetedForDeletion, int successfullyDeleted, int alreadyMissing, 
                             int rejected, int failedDeletion, int trackingRecordsUpdated) {
        int sum = successfullyDeleted + alreadyMissing + rejected + failedDeletion;
        if (sum != targetedForDeletion) {
            throw new IllegalArgumentException(
                String.format("Statistics invariant violated: sum of categories (%d) != targetedForDeletion (%d). " +
                    "Deleted: %d, AlreadyMissing: %d, Rejected: %d, FailedDeletion: %d",
                    sum, targetedForDeletion, successfullyDeleted, alreadyMissing, rejected, failedDeletion)
            );
        }
        if (trackingRecordsUpdated > successfullyDeleted) {
            throw new IllegalArgumentException(
                String.format("Statistics invariant violated: trackingRecordsUpdated (%d) > successfullyDeleted (%d)",
                    trackingRecordsUpdated, successfullyDeleted)
            );
        }
        
        this.targetedForDeletion = targetedForDeletion;
        this.successfullyDeleted = successfullyDeleted;
        this.alreadyMissing = alreadyMissing;
        this.rejected = rejected;
        this.failedDeletion = failedDeletion;
        this.trackingRecordsUpdated = trackingRecordsUpdated;
    }
    
    /**
     * Gets the targeted for deletion.
     *
     * @return the numeric value
     */
    public int getTargetedForDeletion() {
        return targetedForDeletion;
    }
    
    /**
     * Gets the successfully deleted.
     *
     * @return the numeric value
     */
    public int getSuccessfullyDeleted() {
        return successfullyDeleted;
    }
    
    /**
     * Gets the already missing.
     *
     * @return the numeric value
     */
    public int getAlreadyMissing() {
        return alreadyMissing;
    }
    
    /**
     * Gets the rejected.
     *
     * @return the numeric value
     */
    public int getRejected() {
        return rejected;
    }
    
    /**
     * Gets the failed deletion.
     *
     * @return the numeric value
     */
    public int getFailedDeletion() {
        return failedDeletion;
    }
    
    /**
     * Gets the tracking records updated.
     *
     * @return the numeric value
     */
    public int getTrackingRecordsUpdated() {
        return trackingRecordsUpdated;
    }
    
     
    /**
     * Checks if the component is successful.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSuccessful() {
        return failedDeletion == 0;
    }
    
     
    /**
     * Checks if the component has failures.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean hasFailures() {
        return failedDeletion > 0;
    }
    
    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "CleanupStatistics{" +
                "targetedForDeletion=" + targetedForDeletion +
                ", successfullyDeleted=" + successfullyDeleted +
                ", alreadyMissing=" + alreadyMissing +
                ", rejected=" + rejected +
                ", failedDeletion=" + failedDeletion +
                ", trackingRecordsUpdated=" + trackingRecordsUpdated +
                '}';
    }
}
