```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# NoteService

## Overview

Application layer service that coordinates note management use cases. It serves as the primary controller orchestrating operations such as note creation, editing, deletion, duplication, archiving, trashing, and restoring.

## Dependencies

- [../domain/note.md](../domain/note.md): domain model entity.
- [../persistence/repository.md](../persistence/repository.md): persistence operations.
- [../event/event-system.md](../event/event-system.md): to publish state change events.

## Interface

- `repository` — [Repository](../persistence/repository.md)
- `eventSystem` — [EventSystem](../event/event-system.md)

- `NoteService(repository, eventSystem)` -> NoteService
- `createNote(title, content)` -> [Note](../domain/note.md)
- `updateNote(id, title, content)` -> [Note](../domain/note.md)
- `duplicateNote(id)` -> [Note](../domain/note.md)
- `archiveNote(id)` -> void
- `restoreNoteFromArchive(id)` -> void
- `trashNote(id)` -> void
- `restoreNoteFromTrash(id)` -> void
- `permanentlyDeleteNote(id)` -> void
- `getAllNotes()` -> list of [Note](../domain/note.md)
- `getNoteById(id)` -> [Note](../domain/note.md)

## Behavior

- Upon construction, subscribes to `NoteTextChangedEvent` via the `eventSystem`.
- Listens to `NoteTextChangedEvent` via the `eventSystem` and invokes `updateNote` with the updated title, content, and ID to persist changes.
- `createNote` instantiates a new [Note](../domain/note.md), saves it to the `repository`, and publishes a `NoteCreatedEvent` via the `eventSystem`.
- `updateNote` fetches the note by its `id`, updates its properties, saves it to the `repository`, and publishes a `NoteUpdatedEvent`.
- `duplicateNote` retrieves a note, creates a new note instance containing the same title (prefixed with `"Copy of "`), text, tags, and category, saves it, and publishes a `NoteCreatedEvent`.
- `archiveNote` toggles `isArchived` to `true` on the note, saves it, and publishes a `NoteArchivedEvent`.
- `restoreNoteFromArchive` toggles `isArchived` to `false`, saves, and publishes a `NoteRestoredEvent`.
- `trashNote` sets `isTrashed` to `true`, saves, and publishes a `NoteTrashedEvent`.
- `restoreNoteFromTrash` sets `isTrashed` to `false`, saves, and publishes a `NoteRestoredEvent`.
- `permanentlyDeleteNote` removes the note records completely from the repository and publishes a `NoteDeletedEvent`.
- `getAllNotes` retrieves all notes from the `repository`.
- `getNoteById` retrieves a note by its `id` from the `repository`.
