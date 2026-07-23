```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# FiltersPanel

## Overview

A UI component containing interactive filter settings. It allows users to select, toggle, and combine filters (such as date boundaries, categories, archived/favorited flags) to focus the visible list of notes.

## Dependencies

- [../event/event-system.md](../event/event-system.md): to dispatch combined filter updates.

## Interface

- `categorySelector` — UI selection control for categories
- `favoriteToggle` — UI binary toggle control for favorite status
- `archivedToggle` — UI binary toggle control for archived status
- `creationDateStartInput` — UI date input control for the start boundary
- `creationDateEndInput` — UI date input control for the end boundary
- `resetTrigger` — UI action control to clear filters

- `FiltersPanel(eventSystem)` -> FiltersPanel

## Behavior

- Displays options to filter notes by category, tag requirements, favorited/archived properties, and date ranges.
- Whenever any selection changes:
  1. Queries the active states of all selectors and toggles.
  2. Constructs a filter criteria payload mapping these choices.
  3. Broadcasts a `FilterCriteriaChangedEvent` with the criteria payload via the [EventSystem](../event/event-system.md).
- Triggering `resetTrigger` resets all inputs and toggles to default values, firing a blank filter event to restore full listings.
