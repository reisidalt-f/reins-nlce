```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# SearchBar

## Overview

A simple UI input component that captures user search queries and broadcasts query changes.

## Dependencies

- [../event/event-system.md](../event/event-system.md): to dispatch search query update events.

## Interface

- `queryInput` — UI text input control for entering search text.

- `SearchBar(eventSystem)` -> SearchBar

## Behavior

- Renders the query input control with placeholder text `"Search notes..."`.
- Interactivity:
  - Listens to changes in `queryInput`.
  - When the user modifies the text, dispatches a `SearchQueryChangedEvent` containing the updated query string via the [EventSystem](../event/event-system.md).
  - Implements input debouncing (e.g. 150ms) before triggering the event to prevent excessive updates.
