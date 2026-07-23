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

package br.com.dizeno.reins.reasoning;

/**
 * ReferenceMutationDecision is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class ReferenceMutationDecision {

    /**
     * ReasonCode is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing reason code.
     */
    public enum ReasonCode {
        ALLOW_NO_CONFLICT,
        BLOCK_REFERENCED_SOURCE,
        BLOCK_REFERENCED_COMPILED,
        BLOCK_FOREIGN_OWNED,
        BLOCK_TEST_MAIN_EXCLUSION,
        ERROR_FALLBACK
    }

    private final boolean blocked;
    private final ReasonCode reasonCode;
    private final int consideredReferences;
    private final int ignoredSelfReferences;

    private ReferenceMutationDecision(boolean blocked,
                                      ReasonCode reasonCode,
                                      int consideredReferences,
                                      int ignoredSelfReferences) {
        this.blocked = blocked;
        this.reasonCode = reasonCode;
        this.consideredReferences = consideredReferences;
        this.ignoredSelfReferences = ignoredSelfReferences;
    }

    /**
     * Allow No Conflict.
     *
     * @param consideredReferences the considered references
     * @param ignoredSelfReferences the ignored self references
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision allowNoConflict(int consideredReferences, int ignoredSelfReferences) {
        return new ReferenceMutationDecision(false, ReasonCode.ALLOW_NO_CONFLICT, consideredReferences, ignoredSelfReferences);
    }

    /**
     * Block Referenced Source.
     *
     * @param consideredReferences the considered references
     * @param ignoredSelfReferences the ignored self references
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision blockReferencedSource(int consideredReferences, int ignoredSelfReferences) {
        return new ReferenceMutationDecision(true, ReasonCode.BLOCK_REFERENCED_SOURCE, consideredReferences, ignoredSelfReferences);
    }

    /**
     * Block Referenced Compiled.
     *
     * @param consideredReferences the considered references
     * @param ignoredSelfReferences the ignored self references
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision blockReferencedCompiled(int consideredReferences, int ignoredSelfReferences) {
        return new ReferenceMutationDecision(true, ReasonCode.BLOCK_REFERENCED_COMPILED, consideredReferences, ignoredSelfReferences);
    }

    /**
     * Block Foreign Owned.
     *
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision blockForeignOwned() {
        return new ReferenceMutationDecision(true, ReasonCode.BLOCK_FOREIGN_OWNED, 0, 0);
    }

    /**
     * Block Test Main Exclusion.
     *
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision blockTestMainExclusion() {
        return new ReferenceMutationDecision(true, ReasonCode.BLOCK_TEST_MAIN_EXCLUSION, 0, 0);
    }

    /**
     * Error Fallback.
     *
     * @param consideredReferences the considered references
     * @param ignoredSelfReferences the ignored self references
     * @return the resolved or constructed object
     */
    public static ReferenceMutationDecision errorFallback(int consideredReferences, int ignoredSelfReferences) {
        return new ReferenceMutationDecision(false, ReasonCode.ERROR_FALLBACK, consideredReferences, ignoredSelfReferences);
    }

    /**
     * Checks if the component is blocked.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Gets the reason code.
     *
     * @return the resolved or constructed object
     */
    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    /**
     * Gets the considered references.
     *
     * @return the numeric value
     */
    public int getConsideredReferences() {
        return consideredReferences;
    }

    /**
     * Gets the ignored self references.
     *
     * @return the numeric value
     */
    public int getIgnoredSelfReferences() {
        return ignoredSelfReferences;
    }
}
