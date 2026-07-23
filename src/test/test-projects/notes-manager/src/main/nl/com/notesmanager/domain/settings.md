```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Settings

## Overview

A domain model entity representing the user's application preferences. Settings are independent of specific notes.

## Interface

- `theme` — string (e.g. `"LIGHT"`, `"DARK"`)
- `language` — string (ISO 639-1 language code, e.g. `"en"`)
- `fontSize` — integer (point size, default `14`)
- `editorBehavior` — string (behavior flags, e.g. `"AUTOSAVE"`)
- `keyboardShortcuts` — Map of string shortcuts (action name -> key combination)

- `Settings()` -> Settings
- `update(theme, language, fontSize)` -> void

## Behavior

- Instantiated with default values: theme = `"DARK"`, language = `"en"`, fontSize = `14`, and standard keyboard shortcut bindings.
- Validation: Font size must be positive and restricted between `8` and `72` points.
- Modifying settings triggers preferences storage updates.
