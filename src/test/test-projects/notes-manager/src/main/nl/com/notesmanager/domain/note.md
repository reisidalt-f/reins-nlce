```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Note

## Overview

A domain model entity representing a single user note. It contains the text contents, metadata, categorization tags, and visual styling properties.

## Dependencies

- [category.md](category.md): Reference to the note's parent category.
- [tag.md](tag.md): References to tags associated with the note.

## Interface

- `id` — UUID string (unique identifier)
- `title` — string
- `content` — string
- `creationDate` — LocalDateTime
- `lastModifiedDate` — LocalDateTime
- `tags` — list of [Tag](tag.md)
- `category` — [Category](category.md) (nullable)
- `color` — string (hex representation, default `#FFFFFF`)
- `isFavorite` — boolean
- `isArchived` — boolean
- `isTrashed` — boolean

- `Note(id, title, content)` -> Note
- `addTag(tag)` -> void
- `removeTag(tag)` -> void
- `updateContent(newContent)` -> void
- `deepCopy()` -> Note

## Behavior

- The UUID is generated automatically upon instantiation if not supplied.
- When `updateContent` is called:
  1. Updates the `content` property with `newContent`.
  2. Updates `lastModifiedDate` to the current system date and time.
- `deepCopy` creates a completely detached instance of the note with copies of tags and category lists, ensuring snapshot safety for history and undo states.
- By default, `isFavorite`, `isArchived`, and `isTrashed` are set to `false`.
