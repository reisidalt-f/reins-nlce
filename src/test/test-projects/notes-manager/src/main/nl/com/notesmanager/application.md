```text
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at https://mozilla.org/MPL/2.0/.

MPL-2.0-ADDENDUM.md
-------------------
This project includes additional terms and clarifications that apply
to this file. See MPL-2.0-ADDENDUM.md for details.
```

# Application

## Overview

Application bootstrap component. It initializes the dependency context and hands control to the primary user interface shell.

## Dependencies

- [ui/user-interface-shell.md](ui/user-interface-shell.md): provides the main user interface shell launched at startup.

## Behavior

- Initializes the dependency context.
- Once the context is initialized, retrieves the [UserInterfaceShell](ui/user-interface-shell.md) component and commands it to initialize and display.
