```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# NoteCreation

## Overview

A dedicated UI component responsible for gathering the necessary information to create a new note. It coordinates prompts or input forms, validates requirements, and utilizes the note management service to persist the new note.

## Dependencies

- [../services/note-service.md](../services/note-service.md): note management service.
- [../event/event-system.md](../event/event-system.md): to receive create triggers or publish completion notifications.

## Interface

- `noteService` — [NoteService](../services/note-service.md)
- `eventSystem` — [EventSystem](../event/event-system.md)
- `titleInput` — UI text input control for the new note's title
- `contentInput` — UI multi-line text input control for the new note's content

- `NoteCreation(noteService, eventSystem)` -> NoteCreation
- `show()` -> void
- `clear()` -> void

## Behavior

- **Data Requests**:
  - The component requests the following data inputs from the user:
    - **Title**: A text string representing the name/title of the note.
    - **Content**: A multi-line text block representing the body/content of the note.
- **Requirements**:
  - **Title**: Must be provided and cannot be empty or blank.
  - **Content**: Optional (can be blank or empty). Input gathering must support multi-line text blocks.
- **Grouping**:
  - All input fields for note creation (title and content) are grouped together as a single interactive flow (such as a modal input dialog, form panel, or consecutive console input prompts).
- **Note Creation Flow**:
  - When `show()` is invoked:
    1. Clears any existing values in `titleInput` and `contentInput` by calling `clear()`.
    2. Displays the grouped input prompts/fields to the user to capture the **Title** and **Content**.
    3. Validates the entered data against requirements:
       - If the title is blank, displays an error message: `"Note title cannot be empty."` and prompts the user again or cancels the operation.
    4. Invokes `noteService.createNote(title, content)`.
    5. Displays a confirmation message to the user: `"Note created successfully."`.
- `clear()` resets the input fields and internal states of the component.
