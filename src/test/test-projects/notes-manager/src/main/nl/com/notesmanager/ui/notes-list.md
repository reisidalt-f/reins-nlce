```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# NotesList

## Overview

A UI component displaying summaries of a collection of notes, including title, modification timestamp, tag indicators, and favorited status.

## Dependencies

- [../domain/note.md](../domain/note.md): items displayed.
- [../event/event-system.md](../event/event-system.md): to notify when a note is selected or to receive filtered note listings.

## Interface

- `listContainer` — Generic container component displaying note items.
- `displayedNotes` — list of [Note](../domain/note.md) currently visible.

- `NotesList(eventSystem)` -> NotesList
- `setNotes(notes)` -> void

## Behavior

- Renders the list of notes supplied to `setNotes(notes)`.
- Each note summary item displays:
  - Note title (styled according to note color).
  - Date and time details.
  - A text preview (e.g. first 100 characters of note content).
  - Tags and category badges.
  - A favorite indicator toggle control.
- Interactivity:
  - Allow ther user to choose a note.
      - Selecting a note summary item publishes a `NoteSelectedEvent` containing the [Note](../domain/note.md) UUID via the [EventSystem](../event/event-system.md).
  - Triggering the favorite indicator toggle updates the note's favorite state and fires a `NoteUpdatedEvent`.
- Subscribes to events:
  - `FilteredNotesListEvent`: Updates the rendered list by calling `setNotes` with the new matching notes list.
