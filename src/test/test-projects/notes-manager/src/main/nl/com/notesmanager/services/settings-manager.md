```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# SettingsManager

## Overview

A service component coordinating the application's configuration settings. It loads user preferences from persistence, validates modifications, and updates the active settings state.

## Dependencies

- [../domain/settings.md](../domain/settings.md): the underlying preferences entity.
- [../persistence/repository.md](../persistence/repository.md): persistence handler.
- [../event/event-system.md](../event/event-system.md): to broadcast configuration changes.

## Interface

- `currentSettings` — [Settings](../domain/settings.md)

- `SettingsManager(repository, eventSystem)` -> SettingsManager
- `getSettings()` -> [Settings](../domain/settings.md)
- `updateTheme(newTheme)` -> void
- `updateLanguage(newLanguage)` -> void
- `updateFontSize(newSize)` -> void
- `updateShortcut(action, keyStroke)` -> void

## Behavior

- Upon construction, requests settings from the repository via `repository.loadSettings()`.
- If no settings exist in the repository, instantiates a default [Settings](../domain/settings.md) object and saves it immediately.
- When any setting is updated (e.g. `updateTheme`):
  1. Calls the corresponding update method on the internal `currentSettings` object.
  2. Commits the changes to the persistence layer via `repository.saveSettings(currentSettings)`.
  3. Dispatches a SettingsChangedEvent through the [EventSystem](../event/event-system.md) so UI components can adjust immediately (e.g. re-paint active theme, adjust editor font size).
