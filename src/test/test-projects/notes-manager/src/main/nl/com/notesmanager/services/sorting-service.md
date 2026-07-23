```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# SortingService

## Overview

A service component responsible for sorting note collections. It supports sorting by various attributes in ascending or descending order.

## Dependencies

- [../domain/note.md](../domain/note.md): target entity of sorting operations.

## Interface

- `SortField` — enum containing `TITLE`, `CREATION_DATE`, `MODIFICATION_DATE`, `CUSTOM_ORDER`
- `SortDirection` — enum containing `ASCENDING`, `DESCENDING`
- `sort(notes, field, direction)` -> list of [Note](../domain/note.md)
  - `notes`: Collection of [Note](../domain/note.md) elements.
  - `field`: `SortField` indicating the sort attribute.
  - `direction`: `SortDirection` indicating sort order.

## Behavior

- Sorts the list of notes based on the chosen sort attribute.
- Sort logic mapping:
  - `TITLE`: alphabetical sorting of the note's `title` (case-insensitive).
  - `CREATION_DATE`: chronological ordering of `creationDate`.
  - `MODIFICATION_DATE`: chronological ordering of `lastModifiedDate`.
  - `CUSTOM_ORDER`: keeps user-defined order (if custom ordering indices are introduced later, defaults to note creation date order currently).
- Reverses sorting order if the `SortDirection` is `DESCENDING`.
- Modifies the list order and returns it.
