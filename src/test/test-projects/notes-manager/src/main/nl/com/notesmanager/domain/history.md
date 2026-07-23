```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# History

## Overview

A domain model entity representing a single historical revision snapshot of a Note. This supports note version history features.

## Interface

- `id` — UUID string (unique entry identifier)
- `noteId` — UUID string (id of the target note)
- `timestamp` — LocalDateTime (when this snapshot was recorded)
- `versionNumber` — integer (incremental version indicator)
- `titleSnapshot` — string
- `contentSnapshot` — string
- `tagsSnapshot` — list of string tag names

- `History(noteId, versionNumber, title, content, tags)` -> History

## Behavior

- Created automatically whenever a note version is frozen.
- Keeps immutable snapshots of the note's fields.
- Allows reconstructing a past version of a note by mapping properties back to a Note entity.
