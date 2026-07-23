```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# EventSystem

## Overview

A centralized event bus coordinating publisher-subscriber communications. It enables loose coupling between the presentation layer, the application services, and the persistence engine by routing event messages without direct class dependencies.

## Interface

- `listeners` — Map of event types to list of consumer callbacks.

- `EventSystem()` -> EventSystem
- `subscribe(eventType, listener)` -> void
  - `eventType`: Type of event class or topic string.
  - `listener`: Callback consumer/observer function.
- `unsubscribe(eventType, listener)` -> void
- `publish(event)` -> void
  - `event`: Event payload instance containing source and metadata.

## Behavior

- Maintains a registry of active listeners mapped to event types.
- Thread-safe invocation: event dispatching should safely handle situations where listeners modify the subscription list during traversal by duplicating the listener list before calling callbacks.
- Typical events routed:
  - NoteSelectedEvent (emitted by sidebar/notes-list, consumed by editor)
  - NoteUpdatedEvent (emitted by editor/repository, consumed by notes-list)
  - SettingsChangedEvent (emitted by settings manager, consumed by UI components for themes/fonts)
  - SearchQueryChangedEvent (emitted by search bar, consumed by notes list)
