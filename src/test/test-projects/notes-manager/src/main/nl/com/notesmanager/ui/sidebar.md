```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Sidebar

## Overview

A navigation UI component that allows filtering the notes view by folders (All, Favorites, Archived, Trash), categories, or tags.

## Dependencies

- [../event/event-system.md](../event/event-system.md): to publish selected navigation paths.
- [../domain/category.md](../domain/category.md): displays category items.
- [../domain/tag.md](../domain/tag.md): displays tag items.

## Interface

- `navigationSelector` — Abstract component mapping navigation points (All, Favorites, Archived, Trash, Categories, and Tags).

- `Sidebar(eventSystem)` -> Sidebar
- `refreshCategories(categories)` -> void
- `refreshTags(tags)` -> void

## Behavior

- Arranges folders, categories, and tags in a structured menu or list.
- When the user selects a item in `navigationSelector`:
  1. Identifies the classification of the chosen item (folder, category, or tag).
  2. Broadcasts a `NavigationSelectedEvent` containing the filter constraints (e.g., `isFavorite=true`, or `category=Work`) via the [EventSystem](../event/event-system.md).
- The `refreshCategories` and `refreshTags` methods update the visual layout to display any new or modified categories/tags.
