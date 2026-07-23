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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

 
/**
 * SourceProcessingQueue is part of the core compilation lifecycle management, orchestrating file discovery, dependency resolution, and pipeline execution in the reins architecture.
 * Acts as a component managing source processing queue.
 */
public class SourceProcessingQueue {

    private final List<CycleWorkSetEntry> entries;
     
    private int pointer = 0;
     
    private final Set<String> newlyNotedSinceLastPoll = new LinkedHashSet<>();
     
    private int totalNewlyNoted = 0;

    /**
     * Constructs a new instance of {@link SourceProcessingQueue}.
     *
     * @param entries the entries
     */
    public SourceProcessingQueue(List<CycleWorkSetEntry> entries) {
        this.entries = new ArrayList<>(Objects.requireNonNull(entries, "entries"));
    }

     
    /**
     * Next Executable.
     *
     * @return the collection of elements
     */
    public CycleWorkSetEntry nextExecutable() {
        newlyNotedSinceLastPoll.clear();
        while (pointer < entries.size()) {
            CycleWorkSetEntry entry = entries.get(pointer);
            pointer++;
            if (entry.getStatus().shouldExecute()) {
                return entry;
            }
        }
        return null;
    }

     
    /**
     * Mark Note.
     *
     * @param sourcePath the path of the source file
     * @return true if successful or matching, false otherwise
     */
    public boolean markNote(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return false;
        }
        int entryIndex = resolveEntryIndex(sourcePath);
        if (entryIndex < 0) {
            return false;
        }
        CycleWorkSetEntry entry = entries.get(entryIndex);
        entry.markAsNoteTriggered();
        newlyNotedSinceLastPoll.add(sourcePath);
        totalNewlyNoted++;
        
        if (entryIndex < pointer) {
            pointer = entryIndex;
        }
        return true;
    }

    private int resolveEntryIndex(String sourcePath) {
        String normalizedNote = normalizePath(sourcePath);
        if (normalizedNote.isEmpty()) {
            return -1;
        }

        
        for (int i = 0; i < entries.size(); i++) {
            if (normalizePath(entries.get(i).getSourcePath()).equals(normalizedNote)) {
                return i;
            }
        }

        
        Map<Integer, Set<String>> entryAliases = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            entryAliases.put(i, buildAliases(entries.get(i).getSourcePath()));
        }
        for (Map.Entry<Integer, Set<String>> entry : entryAliases.entrySet()) {
            if (entry.getValue().contains(normalizedNote)) {
                return entry.getKey();
            }
        }

        
        if (!normalizedNote.startsWith("main:") && !normalizedNote.startsWith("test:")) {
            int match = -1;
            for (Map.Entry<Integer, Set<String>> entry : entryAliases.entrySet()) {
                if (entry.getValue().contains("main:" + normalizedNote)
                        || entry.getValue().contains("test:" + normalizedNote)) {
                    if (match >= 0) {
                        return -1;
                    }
                    match = entry.getKey();
                }
            }
            return match;
        }

        return -1;
    }

    private Set<String> buildAliases(String rawPath) {
        String normalized = normalizePath(rawPath);
        Set<String> aliases = new LinkedHashSet<>();
        if (normalized.isEmpty()) {
            return aliases;
        }
        aliases.add(normalized);

        if (normalized.startsWith("src/main/nl/")) {
            String relative = normalized.substring("src/main/nl/".length());
            aliases.add("main:" + relative);
            aliases.add(relative);
        } else if (normalized.startsWith("src/test/nl/")) {
            String relative = normalized.substring("src/test/nl/".length());
            aliases.add("test:" + relative);
            aliases.add(relative);
        } else if (normalized.startsWith("main:")) {
            String relative = normalized.substring("main:".length());
            aliases.add("src/main/nl/" + relative);
            aliases.add(relative);
        } else if (normalized.startsWith("test:")) {
            String relative = normalized.substring("test:".length());
            aliases.add("src/test/nl/" + relative);
            aliases.add(relative);
        }
        return aliases;
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

     
    /**
     * Gets the newly noted since last poll.
     *
     * @return the string result
     */
    public Set<String> getNewlyNotedSinceLastPoll() {
        return new LinkedHashSet<>(newlyNotedSinceLastPoll);
    }

     
    /**
     * Checks if the component has remaining.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean hasRemaining() {
        return pointer < entries.size();
    }

     
    /**
     * Gets the pointer state.
     *
     * @return the resulting state
     */
    public QueuePointerState getPointerState() {
        return new QueuePointerState(pointer, totalNewlyNoted);
    }
}
