```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# ExportService

## Overview

A service component responsible for serializing notes into external file formats. It creates an `ExportDocument` containing the byte payload and filename metadata, which can be saved to the local file system.

## Dependencies

- [../domain/note.md](../domain/note.md): source notes to export.
- [../domain/export-document.md](../domain/export-document.md): output representation.

## Interface

- `exportNote(note, format)` -> [ExportDocument](../domain/export-document.md)
  - `note`: The [Note](../domain/note.md) entity to serialize.
  - `format`: Target export format (e.g. `"TXT"`, `"MD"`, `"JSON"`).

- `exportNotes(notes, format)` -> [ExportDocument](../domain/export-document.md)
  - `notes`: List of [Note](../domain/note.md) entities to package together.
  - `format`: Target export format.

## Behavior

- Maps the note properties into the selected `format`:
  - `"TXT"`: Combines title and raw text content separated by blank lines.
  - `"MD"`: Prefixes the title with `# ` and serializes tags as metadata headers or trailing hashtags, followed by markdown content.
  - `"JSON"`: Serializes the note or note collection properties directly to a JSON string.
- Packages the serialized text into a byte array.
- Constructs and returns an [ExportDocument](../domain/export-document.md) containing the binary payload, file format extensions, and a suggested filename (e.g. note title slug).
