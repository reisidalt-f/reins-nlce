```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# LocalStorageRepository

## Overview

A concrete implementation of the [Repository](repository.md) interface that stores application data as JSON files in a local directory.

## Dependencies

- [Repository](repository.md): implements this interface.
- [../domain/note.md](../domain/note.md)
- [../domain/category.md](../domain/category.md)
- [../domain/tag.md](../domain/tag.md)
- [../domain/settings.md](../domain/settings.md)

## Interface

- `storageDirectory` — string (resolved from property `app.persistence.path`)

- `LocalStorageRepository(storageDirectory)` -> LocalStorageRepository

## Behavior

- Implements all methods declared in [Repository](repository.md).
- Saves notes in a `notes/` subdirectory as `<id>.json`.
- Saves categories, tags, and settings in single unified JSON files: `categories.json`, `tags.json`, and `settings.json` under the root `storageDirectory`.
- When writing files:
  1. Serializes the entity to JSON format.
  2. Writes the content atomically using a temporary file before renaming to prevent data corruption.
- When loading files:
  1. Checks if the target file exists; if not, returns empty collections or default settings.
  2. Deserializes JSON back into domain entities.
- Creates directory structures automatically if they do not exist.
