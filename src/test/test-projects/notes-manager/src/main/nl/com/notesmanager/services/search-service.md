```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# SearchService

## Overview

A service component responsible for indexing and locating notes in real-time. It coordinates search queries across multiple note properties (title, content, tags, category).

## Dependencies

- [../domain/note.md](../domain/note.md): target entity of search operations.

## Interface

- `search(notes, query)` -> list of [Note](../domain/note.md)
  - `notes`: Collection of [Note](../domain/note.md) elements to search within.
  - `query`: String containing search terms.

## Behavior

- Implements a case-insensitive, tokenized text search.
- When `search` is called:
  1. If the `query` is blank or null, returns the original list of notes.
  2. Splits the `query` into individual search terms/tokens.
  3. Filters the `notes` list, keeping notes where all search tokens match at least one of the following:
     - Note title containing the token.
     - Note content containing the token.
     - Any applied tag name matching the token.
     - Category name matching the token.
  4. Returns the filtered list of matching notes.
- Designed to allow additional search criteria (e.g. searching by dates or custom attributes) in future iterations without changing the interface.
