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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceProcessingQueueTest {

    @Test
    void markNote_acceptsBareRelativePathAndRewindsPointer() {
        CycleWorkSetEntry target = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/shared.md"),
                "src/main/nl/com/acme/shared.md",
                SourceProcessingStatus.SKIP);
        CycleWorkSetEntry current = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/current.md"),
                "src/main/nl/com/acme/current.md",
                SourceProcessingStatus.COMPILE);

        SourceProcessingQueue queue = new SourceProcessingQueue(List.of(target, current));

        CycleWorkSetEntry first = queue.nextExecutable();
        assertNotNull(first);
        assertEquals("src/main/nl/com/acme/current.md", first.getSourcePath());

        boolean queued = queue.markNote("com/acme/shared.md");
        assertTrue(queued);

        CycleWorkSetEntry next = queue.nextExecutable();
        assertNotNull(next);
        assertEquals("src/main/nl/com/acme/shared.md", next.getSourcePath());
    }

    @Test
    void markNote_acceptsCanonicalMainPath() {
        CycleWorkSetEntry target = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/target.md"),
                "src/main/nl/com/acme/target.md",
                SourceProcessingStatus.SKIP);

        SourceProcessingQueue queue = new SourceProcessingQueue(List.of(target));

        boolean queued = queue.markNote("main:com/acme/target.md");
        assertTrue(queued);

        CycleWorkSetEntry next = queue.nextExecutable();
        assertNotNull(next);
        assertEquals("src/main/nl/com/acme/target.md", next.getSourcePath());
    }

    @Test
    void validateEntryIsExecutableAndPreservesQueueOrder() {
        CycleWorkSetEntry validateEntry = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/validate.md"),
                "src/main/nl/com/acme/validate.md",
                SourceProcessingStatus.SKIP);
        assertTrue(validateEntry.markValidateIfSkippable("validate-all"));

        CycleWorkSetEntry compileEntry = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/compile.md"),
                "src/main/nl/com/acme/compile.md",
                SourceProcessingStatus.COMPILE);

        SourceProcessingQueue queue = new SourceProcessingQueue(List.of(validateEntry, compileEntry));

        CycleWorkSetEntry first = queue.nextExecutable();
        assertNotNull(first);
        assertEquals(SourceProcessingStatus.VALIDATE, first.getStatus());
        assertEquals("src/main/nl/com/acme/validate.md", first.getSourcePath());

        CycleWorkSetEntry second = queue.nextExecutable();
        assertNotNull(second);
        assertEquals(SourceProcessingStatus.COMPILE, second.getStatus());
        assertEquals("src/main/nl/com/acme/compile.md", second.getSourcePath());
    }

    @Test
    void markAsNoteTriggered_setsSelectionReasonToNotesBacktrack() {
        CycleWorkSetEntry entry = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/noted.md"),
                "src/main/nl/com/acme/noted.md",
                SourceProcessingStatus.SKIP);

        entry.markAsNoteTriggered();

        assertEquals(ReprocessingDecision.ReprocessingReason.NOTES_BACKTRACK, entry.getSelectionReason());
    }

    @Test
    void markAsNoteTriggered_alreadyCompile_stillSetsSelectionReasonToNotesBacktrack() {
        CycleWorkSetEntry entry = new CycleWorkSetEntry(
                new File("src/main/nl/com/acme/noted.md"),
                "src/main/nl/com/acme/noted.md",
                SourceProcessingStatus.COMPILE);

        entry.markAsNoteTriggered();

        assertEquals(ReprocessingDecision.ReprocessingReason.NOTES_BACKTRACK, entry.getSelectionReason());
        assertEquals(SourceProcessingStatus.COMPILE, entry.getStatus(),
                "Status must remain COMPILE when already COMPILE");
    }
}
