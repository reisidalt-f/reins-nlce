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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SourceTrackingManager is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Orchestrates the persistence and retrieval of source file tracking details to manage incremental compilations.
 */
public class SourceTrackingManager {

    private final TrackingCommitStrategy trackingCommitStrategy;

    
    
    private static final Map<String, Object> PATH_LOCKS = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructs a new instance of {@link SourceTrackingManager}.
     */
    public SourceTrackingManager() {
        this(new TrackingCommitStrategy());
    }

    /**
     * Constructs a new instance of {@link SourceTrackingManager}.
     *
     * @param trackingCommitStrategy the tracking commit strategy
     */
    public SourceTrackingManager(TrackingCommitStrategy trackingCommitStrategy) {
        this.trackingCommitStrategy = trackingCommitStrategy;
    }

     
    private static Object getLockForPath(Path trackingFile) {
        String key = trackingFile.toAbsolutePath().normalize().toString();
        return PATH_LOCKS.computeIfAbsent(key, k -> new Object());
    }


    /**
     * Commits the changes.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @param record the tracking record
     * @param trackingStore the persistence store for file tracking records
     */
    public void commit(Path projectRoot,
                       String sourcePath,
                       SourceTrackingRecord record,
                       CompilationTrackingStore trackingStore) throws IOException {
        trackingCommitStrategy.commit(projectRoot, sourcePath, record, trackingStore);
    }

     
    /**
     * Append Note.
     *
     * @param projectRoot the root path of the project
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param noteText the note text
     * @param origin the origin
     * @param trackingStore the persistence store for file tracking records
     * @return the numeric value
     */
    public int appendNote(Path projectRoot,
                          String canonicalSourcePath,
                          String noteText,
                          ReasoningNote.Origin origin,
                          CompilationTrackingStore trackingStore) throws IOException {
        if (noteText == null || noteText.isBlank()) {
            throw new IllegalArgumentException("appendNote: noteText must be non-blank");
        }
        String normalizedText = noteText.strip();

        
        
        Path trackingFile = trackingStore.trackingFilePath(projectRoot, canonicalSourcePath);
        synchronized (getLockForPath(trackingFile)) {
            
            SourceTrackingRecord record = trackingStore.load(projectRoot, canonicalSourcePath)
                    .orElseGet(SourceTrackingRecord::new);

            if (record.getSourcePath() == null) {
                record.setSourcePath(canonicalSourcePath);
            }

            List<ReasoningNote> notes = new ArrayList<>(record.getNotes());
            boolean alreadyPresent = notes.stream()
                    .anyMatch(n -> normalizedText.equals(n.getText() == null ? null : n.getText().strip()));
            if (!alreadyPresent) {
                ReasoningNote note = new ReasoningNote(normalizedText, Instant.now().toString(), origin);
                notes.add(note);
                record.setNotes(notes);
            }

            record.setLastStatus("note-pending");
            trackingCommitStrategy.commit(projectRoot, canonicalSourcePath, record, trackingStore);

            return record.getNotes().size();
        }
    }

     
    /**
     * Clears the cache or state notes.
     *
     * @param projectRoot the root path of the project
     * @param canonicalSourcePath the canonicalized path of the source file
     * @param trackingStore the persistence store for file tracking records
     * @return the numeric value
     */
    public int clearNotes(Path projectRoot,
                          String canonicalSourcePath,
                          CompilationTrackingStore trackingStore) throws IOException {
        
        Path trackingFile = trackingStore.trackingFilePath(projectRoot, canonicalSourcePath);
        synchronized (getLockForPath(trackingFile)) {
            
            return trackingStore.load(projectRoot, canonicalSourcePath)
                    .map(record -> {
                        int count = record.getNotes().size();
                        if (count > 0) {
                            record.setNotes(new ArrayList<>());
                            try {
                                trackingCommitStrategy.commit(projectRoot, canonicalSourcePath, record, trackingStore);
                            } catch (IOException e) {
                                throw new RuntimeException("clearNotes: failed to persist tracking record for " + canonicalSourcePath, e);
                            }
                        }
                        return count;
                    })
                    .orElse(0);
        }
    }
}

