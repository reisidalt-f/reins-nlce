```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# FilterService

## Overview

A service component responsible for composing and applying filters on notes. Filters can be combined to select specific subsets of notes based on criteria like tags, categories, favorited, archived, and date ranges.

## Dependencies

- [../domain/note.md](../domain/note.md): target entity of filtering.
- [../domain/category.md](../domain/category.md)
- [../domain/tag.md](../domain/tag.md)

## Interface

- `FilterCriteria` — internal helper class containing filter constraints (category, tags list, favorite flag, archived flag, start/end creation dates, start/end modified dates).
- `applyFilters(notes, criteria)` -> list of [Note](../domain/note.md)
  - `notes`: Collection of [Note](../domain/note.md) elements.
  - `criteria`: `FilterCriteria` instance specifying active filter configurations.

## Behavior

- Filters are combinable (logical AND between active constraints).
- When filtering:
  1. Checks `criteria.archived`: If set, matches notes with the same `isArchived` status (defaulting to filtering out archived notes if not explicitly requested).
  2. Checks `criteria.favorite`: If true, matches only notes where `isFavorite` is true.
  3. Checks `criteria.category`: If specified, matches only notes in that [Category](../domain/category.md).
  4. Checks `criteria.tags`: If specified, matches only notes that contain all specified [Tag](../domain/tag.md) instances.
  5. Checks creation/modification date ranges: If dates are set, matches notes whose corresponding timestamp falls within the boundaries (inclusive).
- Returns the list of notes that satisfy all specified conditions.
