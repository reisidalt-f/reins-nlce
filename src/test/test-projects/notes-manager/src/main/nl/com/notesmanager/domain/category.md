```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Category

## Overview

A domain model entity representing a category. Used for hierarchical organization of notes. A note can belong to at most one category.

## Interface

- `id` — UUID string (unique identifier)
- `name` — string
- `description` — string

- `Category(name)` -> Category
- `Category(id, name, description)` -> Category

## Behavior

- The `id` is generated as a random UUID if not provided.
- `name` is required and must be non-empty. Leading and trailing whitespaces are trimmed.
- Validates that `name` is not blank. Throws an IllegalArgumentException if validation fails.
