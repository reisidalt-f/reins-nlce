```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# ExportDocument

## Overview

A domain model entity representing a structured document ready for export to the file system. It encapsulates file metadata and content bytes.

## Interface

- `fileName` — string (target name of the file)
- `contentType` — string (MIME type or extension descriptor)
- `contentBytes` — byte array of the serialized notes data

- `ExportDocument(fileName, contentType, contentBytes)` -> ExportDocument

## Behavior

- Acts as a container for data passing between the `ExportService` and the system's file export handler.
- Prevents coupling between serialization formats and the presentation layer.
