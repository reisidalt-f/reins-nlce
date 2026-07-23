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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FileTrackingDetails is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileTrackingDetails {
    private String category;
    private Long modificationTime;
    private String tracking;

    /**
     * Constructs a new instance of {@link FileTrackingDetails}.
     */
    public FileTrackingDetails() {
    }

    @JsonCreator
    public FileTrackingDetails(@JsonProperty("tracking") String tracking,
                                  @JsonProperty("category") String category,
                                  @JsonProperty("modificationTime") Long modificationTime) {
        this.tracking = tracking;
        this.category = category;
        this.modificationTime = modificationTime;
    }

    /**
     * Gets the category.
     *
     * @return the string result
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category.
     *
     * @param category the category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the modification time.
     *
     * @return the numeric value
     */
    public Long getModificationTime() {
        return modificationTime;
    }

    /**
     * Sets the modification time.
     *
     * @param modificationTime the modification time
     */
    public void setModificationTime(Long modificationTime) {
        this.modificationTime = modificationTime;
    }

    /**
     * Gets the tracking.
     *
     * @return the string result
     */
    public String getTracking() {
        return tracking;
    }

    /**
     * Sets the tracking.
     *
     * @param tracking the tracking
     */
    public void setTracking(String tracking) {
        this.tracking = tracking;
    }
}
