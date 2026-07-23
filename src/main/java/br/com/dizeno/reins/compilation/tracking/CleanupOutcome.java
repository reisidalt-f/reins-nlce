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

import java.time.Instant;

 
/**
 * CleanupOutcome is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup outcome.
 */
public class CleanupOutcome {
    
    /**
     * Status is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing status.
     */
    public enum Status {
        DELETED,
        ALREADY_MISSING,
        REJECTED,
        FAILED_DELETION
    }
    
    private final CleanupTarget target;
    private final Status status;
    private final String reason;
    private final Throwable errorCause;
    private final Instant attemptedAt;
    
     
    /**
     * Constructs a new instance of {@link CleanupOutcome}.
     *
     * @param target the target
     * @param status the status
     * @param reason the reason
     * @param errorCause the error cause
     * @param attemptedAt the attempted at
     */
    public CleanupOutcome(CleanupTarget target, Status status, String reason, Throwable errorCause, Instant attemptedAt) {
        this.target = target;
        this.status = status;
        this.reason = reason;
        this.errorCause = errorCause;
        this.attemptedAt = attemptedAt;
    }
    
    /**
     * Gets the target.
     *
     * @return the resolved or constructed object
     */
    public CleanupTarget getTarget() {
        return target;
    }
    
    /**
     * Gets the status.
     *
     * @return the resulting status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Gets the reason.
     *
     * @return the string result
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Gets the error cause.
     *
     * @return the resolved or constructed object
     */
    public Throwable getErrorCause() {
        return errorCause;
    }
    
    /**
     * Gets the attempted at.
     *
     * @return the resolved or constructed object
     */
    public Instant getAttemptedAt() {
        return attemptedAt;
    }
    
     
    /**
     * Checks if the component is failure.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFailure() {
        return status == Status.FAILED_DELETION;
    }
    
     
    /**
     * Checks if the component is deleted.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDeleted() {
        return status == Status.DELETED;
    }
    
     
    /**
     * Was Already Missing.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean wasAlreadyMissing() {
        return status == Status.ALREADY_MISSING;
    }
    
     
    /**
     * Was Rejected.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean wasRejected() {
        return status == Status.REJECTED;
    }
    
    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "CleanupOutcome{" +
                "target=" + target.getPath() +
                ", status=" + status +
                ", reason='" + reason + '\'' +
                '}';
    }
}
