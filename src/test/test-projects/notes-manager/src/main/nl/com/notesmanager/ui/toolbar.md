```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Toolbar

## Overview

A UI component managing global action triggers. It provides access to operations such as creating notes, opening application settings, importing/exporting note data, and toggling the visual theme.

## Dependencies

- [../event/event-system.md](../event/event-system.md): to publish actions and settings requests.

## Interface

- `createAction` — UI action control for creating notes
- `settingsAction` — UI action control for opening settings
- `importAction` — UI action control for importing files
- `exportAction` — UI action control for exporting files
- `themeToggleControl` — UI toggle selector for switching themes

- `Toolbar(eventSystem)` -> Toolbar

## Behavior

- Renders the global options/commands section.
- Dispatches events via the [EventSystem](../event/event-system.md):
  - Triggering `createAction` fires a `CreateNoteEvent`.
  - Triggering `settingsAction` fires an `OpenSettingsDialogEvent`.
  - Triggering `importAction` prompts the user for a source file, processes it, and publishes an `ImportNoteEvent`.
  - Triggering `exportAction` prompts the user for a destination, and invokes the export process.
  - Toggling `themeToggleControl` fires a `ThemeToggleEvent` passing the target theme name.
