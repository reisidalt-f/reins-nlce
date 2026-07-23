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
 * CleanupTarget is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup target.
 */
public class CleanupTarget {
    
    /**
     * Type is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing type.
     */
    public enum Type {
        FILE,
        DIRECTORY
    }
    
    private final String path;
    private final Type type;
    private final String sourceReferenceId;
    private final long size;
    private final Instant lastModified;
    
     
    /**
     * Constructs a new instance of {@link CleanupTarget}.
     *
     * @param path the file or directory path
     * @param type the type
     * @param sourceReferenceId the source reference id
     * @param size the size
     * @param lastModified the last modified
     */
    public CleanupTarget(String path, Type type, String sourceReferenceId, long size, Instant lastModified) {
        this.path = path;
        this.type = type;
        this.sourceReferenceId = sourceReferenceId;
        this.size = size;
        this.lastModified = lastModified;
    }
    
    /**
     * Gets the path.
     *
     * @return the string result
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Gets the type.
     *
     * @return the resolved or constructed object
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Gets the source reference id.
     *
     * @return the string result
     */
    public String getSourceReferenceId() {
        return sourceReferenceId;
    }
    
    /**
     * Gets the size.
     *
     * @return the numeric value
     */
    public long getSize() {
        return size;
    }
    
    /**
     * Gets the last modified.
     *
     * @return the resolved or constructed object
     */
    public Instant getLastModified() {
        return lastModified;
    }
    
    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "CleanupTarget{" +
                "path='" + path + '\'' +
                ", type=" + type +
                ", size=" + size +
                ", lastModified=" + lastModified +
                '}';
    }
}
