```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# ImportService

## Overview

A service component responsible for reading and parsing external files (e.g. text files, JSON files, Markdown) to instantiate Note entities. Designed to be extensible to support multiple file formats.

## Dependencies

- [../domain/note.md](../domain/note.md): instantiates notes from import content.

## Interface

- `importNote(filePath, format)` -> [Note](../domain/note.md)
  - `filePath`: Path to the external file.
  - `format`: Format descriptor (e.g., `"TXT"`, `"MD"`, `"JSON"`).

## Behavior

- Reads the raw bytes or text from the specified `filePath`.
- Selects the appropriate parser based on the `format` argument.
- Parser details:
  - `"TXT"`: Sets the file name (without extension) as note `title` and reads all lines as plain text `content`.
  - `"MD"`: Sets the first H1 header found in the markdown text as the `title`, and maps the remaining markdown to the note `content`.
  - `"JSON"`: Deserializes the JSON structure directly matching [Note](../domain/note.md) fields.
- Instantiates a new `Note` with the parsed details, generating a new unique identifier, and setting the creation date to the current timestamp.
- Returns the imported note, ready to be saved by the repository.
