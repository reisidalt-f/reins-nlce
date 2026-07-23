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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

 
/**
 * CompiledFileListing is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class CompiledFileListing {

    @JsonProperty("existingPaths")
    private List<String> existingPaths = new ArrayList<>();

    @JsonProperty("staledPathsRemoved")
    private int staledPathsRemoved;

    @JsonProperty("cleanupStatus")
    private String cleanupStatus = "no-stale-paths";

    @JsonProperty("cleanupDiagnostic")
    private String cleanupDiagnostic;

    @JsonProperty("excludedPaths")
    private List<String> excludedPaths = new ArrayList<>();

    @JsonProperty("exclusionReason")
    private String exclusionReason;

    
    /**
     * Constructs a new instance of {@link CompiledFileListing}.
     */
    public CompiledFileListing() {
    }

    /**
     * Constructs a new instance of {@link CompiledFileListing}.
     *
     * @param existingPaths the existing paths
     */
    public CompiledFileListing(List<String> existingPaths) {
        this.existingPaths = existingPaths != null ? existingPaths : new ArrayList<>();
    }

    
    /**
     * Gets the existing paths.
     *
     * @return the string result
     */
    public List<String> getExistingPaths() {
        return existingPaths;
    }

    /**
     * Sets the existing paths.
     *
     * @param existingPaths the existing paths
     */
    public void setExistingPaths(List<String> existingPaths) {
        this.existingPaths = existingPaths != null ? existingPaths : new ArrayList<>();
    }

    /**
     * Add Existing Path.
     *
     * @param path the file or directory path
     */
    public void addExistingPath(String path) {
        this.existingPaths.add(path);
    }

    /**
     * Gets the staled paths removed.
     *
     * @return the numeric value
     */
    public int getStaledPathsRemoved() {
        return staledPathsRemoved;
    }

    /**
     * Sets the staled paths removed.
     *
     * @param staledPathsRemoved the staled paths removed
     */
    public void setStaledPathsRemoved(int staledPathsRemoved) {
        this.staledPathsRemoved = staledPathsRemoved;
    }

    /**
     * Gets the cleanup status.
     *
     * @return the string result
     */
    public String getCleanupStatus() {
        return cleanupStatus;
    }

    /**
     * Sets the cleanup status.
     *
     * @param cleanupStatus the cleanup status
     */
    public void setCleanupStatus(String cleanupStatus) {
        this.cleanupStatus = cleanupStatus;
    }

    /**
     * Gets the cleanup diagnostic.
     *
     * @return the string result
     */
    public String getCleanupDiagnostic() {
        return cleanupDiagnostic;
    }

    /**
     * Sets the cleanup diagnostic.
     *
     * @param cleanupDiagnostic the cleanup diagnostic
     */
    public void setCleanupDiagnostic(String cleanupDiagnostic) {
        this.cleanupDiagnostic = cleanupDiagnostic;
    }

    /**
     * Gets the excluded paths.
     *
     * @return the string result
     */
    public List<String> getExcludedPaths() {
        return excludedPaths;
    }

    /**
     * Sets the excluded paths.
     *
     * @param excludedPaths the excluded paths
     */
    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludedPaths = excludedPaths != null ? excludedPaths : new ArrayList<>();
    }

    /**
     * Add Excluded Path.
     *
     * @param path the file or directory path
     */
    public void addExcludedPath(String path) {
        this.excludedPaths.add(path);
    }

    /**
     * Gets the exclusion reason.
     *
     * @return the string result
     */
    public String getExclusionReason() {
        return exclusionReason;
    }

    /**
     * Sets the exclusion reason.
     *
     * @param exclusionReason the exclusion reason
     */
    public void setExclusionReason(String exclusionReason) {
        this.exclusionReason = exclusionReason;
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "CompiledFileListing{" +
                "existingPaths=" + existingPaths.size() +
                ", staledPathsRemoved=" + staledPathsRemoved +
                ", excludedPaths=" + excludedPaths.size() +
                ", cleanupStatus='" + cleanupStatus + '\'' +
                '}';
    }
}
