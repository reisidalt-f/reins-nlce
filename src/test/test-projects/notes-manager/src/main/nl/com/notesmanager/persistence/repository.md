```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Repository

## Overview

Interface defining the persistence operations. It abstracts access to notes, categories, tags, and settings, ensuring that the application layer is independent of the actual storage mechanism.

## Dependencies

- [../domain/note.md](../domain/note.md): persistence target.
- [../domain/category.md](../domain/category.md): persistence target.
- [../domain/tag.md](../domain/tag.md): persistence target.
- [../domain/settings.md](../domain/settings.md): persistence target.

## Interface

- `saveNote(note)` -> [Note](../domain/note.md)
- `findNoteById(id)` -> [Note](../domain/note.md)
- `findAllNotes()` -> list of [Note](../domain/note.md)
- `deleteNote(id)` -> void

- `saveCategory(category)` -> [Category](../domain/category.md)
- `findCategoryById(id)` -> [Category](../domain/category.md)
- `findAllCategories()` -> list of [Category](../domain/category.md)
- `deleteCategory(id)` -> void

- `saveTag(tag)` -> [Tag](../domain/tag.md)
- `findTagById(id)` -> [Tag](../domain/tag.md)
- `findAllTags()` -> list of [Tag](../domain/tag.md)
- `deleteTag(id)` -> void

- `saveSettings(settings)` -> [Settings](../domain/settings.md)
- `loadSettings()` -> [Settings](../domain/settings.md)

## Behavior

- Defines standard contract for reading and writing domain entities.
- Must throw specialized persistence exceptions on read/write failures.
- Concrete implementations are responsible for actual persistence mechanism (e.g. Local Storage, SQLite, Cloud).
