```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Tag

## Overview

A domain model entity representing a tag. Tags can be applied to notes to facilitate searching, grouping, and filtering across categories.

## Interface

- `id` — UUID string (unique identifier)
- `name` — string
- `color` — string (hex color code representation, e.g., `#FF5733`)

- `Tag(name)` -> Tag
- `Tag(id, name, color)` -> Tag

## Behavior

- The `id` is generated as a random UUID if not provided.
- `name` must be unique across the application and cannot be blank. Trims leading and trailing whitespace.
- If no color is specified, default is `#888888`.
