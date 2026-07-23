```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Editor

## Overview

A dedicated UI component responsible for display and editing of the currently selected note. It handles only rendering and input updates, remaining decoupled from backend storage and business logic.

## Dependencies

- [../domain/note.md](../domain/note.md): note entity being edited.
- [../event/event-system.md](../event/event-system.md): to notify that text has changed.

## Interface

- `titleInput` — UI text input control for the note title
- `contentInput` — UI multi-line text input control for the note content
- `activeNote` — [Note](../domain/note.md) (nullable)

- `Editor(eventSystem)` -> Editor
- `loadNote(note)` -> void
- `clear()` -> void

## Behavior

- `loadNote(note)` targets a note for editing:
  1. Sets `activeNote` to `note`.
  2. Populates `titleInput` with the value of `note.title`.
  3. Populates `contentInput` with the value of `note.content`.
  4. Enables the input controls.
- `clear()` unbinds the active note, clears the fields, and disables the editing controls.
- Input changes:
  - User modifications to `titleInput` or `contentInput` trigger content changed notifications.
  - Fires a `NoteTextChangedEvent` containing the updated text values and the target note ID via the [EventSystem](../event/event-system.md).
- Extensibility: The visual implementation of `contentInput` can be upgraded from plain text to rich formatting or a Markdown viewer without impacting the rest of the application.
