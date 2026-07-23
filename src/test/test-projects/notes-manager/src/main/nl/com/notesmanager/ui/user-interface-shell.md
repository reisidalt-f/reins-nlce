```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# UserInterfaceShell

## Overview

The primary, top-level user interface shell for the Notes Manager. It acts as the coordinate container hosting and positioning the independent UI panels (global controls, navigation sidebar, notes summary list, editor panel, search input, and filters).

## Dependencies

- [toolbar.md](toolbar.md): Global actions and controls.
- [sidebar.md](sidebar.md): Navigation listing folders/categories.
- [notes-list.md](notes-list.md): Summaries of notes.
- [editor.md](editor.md): Text editing workspace.
- [search-bar.md](search-bar.md): Search query input component.
- [filters-panel.md](filters-panel.md): Toggle filters panel.
- [note-creation.md](note-creation.md): component responsible to create a note.
- [../event/event-system.md](../event/event-system.md): to receive preference and size updates.
- [../services/note-service.md](../services/note-service.md): note management service.
- [../services/search-service.md](../services/search-service.md): text search index.
- [../services/filter-service.md](../services/filter-service.md): metadata filter handler.
- [../services/sorting-service.md](../services/sorting-service.md): collection sorting.

## Interface

- `toolbar` — [Toolbar](toolbar.md)
- `sidebar` — [Sidebar](sidebar.md)
- `notesList` — [NotesList](notes-list.md)
- `editor` — [Editor](editor.md)
- `searchBar` — [SearchBar](search-bar.md)
- `filtersPanel` — [FiltersPanel](filters-panel.md)
- `noteCreation` — [NoteCreation](note-creation.md)
- `noteService` — [NoteService](../services/note-service.md)
- `searchService` — [SearchService](../services/search-service.md)
- `filterService` — [FilterService](../services/filter-service.md)
- `sortingService` — [SortingService](../services/sorting-service.md)

- `UserInterfaceShell(toolbar, sidebar, notesList, editor, searchBar, filtersPanel, eventSystem, noteService, searchService, filterService, sortingService, noteCreation)` -> UserInterfaceShell
- `initialize()` -> void

## Behavior

- Orchestrates startup assembly of the user interface.
- Applies the application title configured in properties to the UI shell's primary header/display area.
- Structural Layout:
  - Places the `Toolbar` at the top or global header section.
  - Places the `Sidebar` in the navigation/side drawer section.
  - Positions the `NotesList` and `Editor` adjacent to each other in the main viewport.
  - Positions the `SearchBar` and `FiltersPanel` prominent to or embedded above the `NotesList` for query control.
- Event subscriptions configured on `initialize()`:
  - Listens to `SettingsChangedEvent` via the [EventSystem](../event/event-system.md) to apply theme settings (e.g. font size, color palette) dynamically across all subcomponents.
  - Listens to `CreateNoteEvent` to launch the note creation flow.
  - Listens to `NoteCreatedEvent`, `NoteUpdatedEvent`, `NoteDeletedEvent`, `SearchQueryChangedEvent`, and `FilterCriteriaChangedEvent` to trigger a notes collection sync.
- Interaction loops:
  - **CreateNoteEvent handling**:
    1. Delegates the note creation flow to `noteCreation` by calling `noteCreation.show()`.
  - **Delete Note selection**:
    - Prompts the user for action, and delegates directly to the corresponding service method (`archiveNote`, `trashNote`, or `permanentlyDeleteNote` in `NoteService`) based on selection, rather than modifying properties in-place.
  - **Note Collection Synchronization**:
    - Tracks active filters, current search query, and current sorting state.
    - Whenever synchronization is triggered:
      1. Pulls all notes from `noteService.getAllNotes()`.
      2. Passes them through `filterService.applyFilters()` and `searchService.search()`.
      3. Passes the results through `sortingService.sort()`.
      4. Publishes a `FilteredNotesListEvent` containing the final sorted list of notes via the [EventSystem](../event/event-system.md) to update the `NotesList` display.
    - Triggers this synchronization during startup `initialize()` to display the initial notes list.
