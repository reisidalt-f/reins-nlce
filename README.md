# Reins: AI-Native Inference-First Natural Language Compilation Engine

Reins is an AI-native, inference-first natural language compilation engine (Reins NLCE). It treats a Markdown text base as the primary implementation artifact and the preferred form for editing and changing a software. The executable form—including code written in programming languages—is generated(compiled) from these sources and can be regenerated whenever needed, much like object code.

If you want greater control over code generation while keeping yourself in the role of engineer, Reins offers the right approach. For an overview of the ideas behind this project, including how it differs from software engineering agents, see the article [Natural Language Compiler](https://objectcode.hashnode.dev/natural-language-compiler?utm_source=chatgpt.com).

> [!WARNING]
> Reins is currently a proof of concept and an ongoing work-in-progress. It should not be used in production environments, and it may change at any time with no guarantee of backward compatibility.

---

## How Reins Works as a Compilation Engine

Reins compiles natural-language software components source texts directly into executable or compile-ready target code. Traditional compilers (e.g., `gcc`, `javac`, `rustc`) translate high-level programming source code into low-level machine code or bytecode. Reins operates at a layer above, translating structured Markdown files (the source text) into target-language code files (the compiled object code), which then feed directly into traditional compiler toolchains.

### Core Architecture & Compilation Phases

The compilation process mirrors traditional compiler phases, powered by a flexible and secure orchestration architecture:

1. **Source Discovery & Scanning:**
   * **Source Discovery:** Reins scans configured root directories (`scanRoots`) and filters files using configurable include/exclude glob patterns to discover valid natural-language `.md` source files.

2. **Parsing & Reference Resolution (Markdown Source to Graph):**
   * **Natural-Language Sources:** Treat `.md` files directly as your compilation sources.
   * **Markdown Reference Graphs:** Reins parses Markdown sources for references (e.g., `[shared/util.md]` or `(shared/util.md)`) to build a directed acyclic reference graph (comparable to a compiler AST or dependency graph) that determines compilation order and contextual attachments.

3. **Semantic & Architectural Analysis:**
   * **Target Architecture:** The engine evaluates dependencies, constraints, and target platform rules (e.g., Assembly, Java, TypeScript, Python conventions) against the source text to ensure the generated code satisfies target specs.

4. **Code Generation & Tool Execution:**
   * **Provider-Agnostic LLM Abstraction:** The inference pipeline runs on a vendor-neutral LLM client interface with adapters for **Google Gemini** and **Ollama** (with more to come).
   * **Scriptable Inference Orchestration:** Compilation prompts and pipeline phases are executed using customizable **Apache FreeMarker templates** (`.ftl`), letting you override prompt patterns, system contexts, and reasoning loops.
   * **Sandboxed File Operations Tooling:** The engine writes fully realized, compile-ready target files through a highly constrained file operations tool layer (`list`, `read`, `write`, `patch`, `delete`, `list_compiled`, `run_script`), restricting execution exclusively to authorized project paths.

5. **Incremental Recompilation (Incremental Builds):**
   * **Compilation Manifest Tracking:** Reins tracks source file SHA-256 hashes and mapped output artifacts in `.reins/compilation-tracking/`. Unchanged source files are skipped during builds, ensuring rapid incremental compiles.

---

## Documentation & Guides

Comprehensive documentation for Reins is split into the following guides:

* 📖 **[Usage Modes & Quick Start](docs/usage-modes.md):** Detailed guide on running Reins as a **Maven Plugin**, **Command Line Tool (CLI)**, or **Reusable Java Library**. Includes command flags, CLI auto-discovery rules, and programmatic Java code examples.
* ⚙️ **[Configuration Reference](docs/configuration.md):** Complete XML schema, property dot-notation mapping, and exhaustive parameter reference index (Top-Level, Gemini, Ollama, Context, Reasoning, Tooling, Target Paths, Logging). Includes the troubleshooting guide.
* 🛠️ **[Advanced Customization Recipes](docs/customization.md):** How to override Apache FreeMarker inference prompt scripts (`scriptsPath`), template priority hierarchy, and fine-tune tooling security permission profiles.

---

## Demonstration Project: Notes Manager

To see Reins in action, explore the **Notes Manager** sample project located in the repository under [`src/test/test-projects/notes-manager`](src/test/test-projects/notes-manager).

This sample project demonstrates how a natural-language Markdown source text base is structured and translated by Reins into a fully working Java application.

### Structure of the Demonstration Project

The project layout separates natural language source text base from target specifications and build tooling:

```
src/test/test-projects/notes-manager/
├── pom.xml                            # Maven build file configuring reins-nlce
├── compilation/                       # Technical compilation guidance & rules
│   ├── java-console.md                  - Target rules for Java 17 Console UI 
│   └── java-swing.md                    - Target rules for Java Swing UI generation
└── src/main/nl/com/notesmanager/      # Natural-Language Source Text (.md)
    ├── application.md                   - Application bootstrap & entry component
    ├── domain/                          # Domain models and entities
    │   ├── note.md                        - Note entity attributes & validation rules
    │   ├── category.md                    - Category
    │   ├── tag.md                         - Tag entity
    │   ├── history.md                     - Edit history tracking
    │   ├── settings.md                    - App settings entity
    │   └── export-document.md             - Export document representation
    ├── services/                        # Business logic & services
    │   ├── note-service.md                - CRUD & note manipulation logic
    │   ├── search-service.md              - Full-text and title search
    │   ├── filter-service.md              - Note filtering rules 
    │   ├── sorting-service.md             - Multi-field note sorting
    │   ├── import-service.md              - Data import
    │   ├── export-service.md              - Data export
    │   └── settings-manager.md            - User settings persistence & management
    ├── persistence/                     # Data access abstractions & repositories
    │   ├── repository.md                  - Storage interface contract
    │   └── local-storage-repository.md    - File-based local JSON repository 
    ├── event/                           # Event handling abstractions
    │   └── event-system.md                - Application event bus & event listener
    └── ui/                              # User interface components
        ├── user-interface-shell.md        - Main UI container & orchestration
        ├── notes-list.md                  - Notes list view
        ├── editor.md                      - Note editor view
        ├── sidebar.md                     - Navigation sidebar
        ├── toolbar.md                     - Action toolbar
        ├── search-bar.md                  - Search input 
        ├── filters-panel.md               - Category/tag filter 
        └── note-creation.md               - New note creation modal/dialog
```

1. **Source & Guidance Layout:**
   * **Source Files (`src/main/nl/`):** Markdown source texts (the compilation inputs), structured into domains, services, events, and UI components.
   * **Target Architecture (`compilation/`):** Technology target rules defining language (Java 17), coding standards, and user interfaces. For example, `java-console.md` compiles the code into a command-line interface, while `java-swing.md` compiles it into a graphical Java Swing interface.

2. **Inference & Compiler Configuration (`pom.xml`):**
   * **Source Discovery (`<scanRoots>` & `<includePattern>`):** Configured to automatically scan `src/main/nl` for all `.md` files matching `**/*.md`.
   * **Target Outputs (`<target>`):** Compiled `.java` artifacts are written directly to `target/generated-sources/reins/` to align package names with target folder structures.
   * **Tooling Constraints (`<tooling>`):** Sandboxes file mutations by granting the generation model strict access only to `list`, `read`, `write`, and `delete` tools.
   * **Incremental Tracking (`<tracking>`):** control of the compilation and changes tracking system.
   * **Build Integration (`build-helper-maven-plugin`):** Registers the output directory (`target/generated-sources/reins`) as a Java source root, integrating the generated codebase seamlessly into standard compilation phases (like `mvn compile`).

3. **Source Base Structure:** The Markdown sources under `src/main/nl/com/notesmanager/` are organized into clean layers:
   * **Application bootstrap** (`application.md`)
   * **Entities and rules** (`domain/`)
   * **Business logic** (`services/`)
   * **Persistence layers** (`persistence/`)
   * **Event orchestration** (`event/`)
   * **Views** (`ui/`)
   * *Note: Reins compiles these source texts sequentially into their matching target packages.*

---

### Model & API Key Configuration

By default, the Notes Manager sample project is configured in its [`pom.xml`](src/test/test-projects/notes-manager/pom.xml) to use the **Ollama** provider, targeting a Llama-derived model (configured via the `glm-5.2` model identifier).

#### API Key Requirement
Although local Ollama instances running on `http://localhost:11434` do not require credentials, the sample default targets a remote Ollama endpoint (`https://ollama.com`). As a result, the plugin configuration includes:
```xml
<apiKey>${env.OLLAMA_API_KEY}</apiKey>
```
To run the compilation under this configuration, export `OLLAMA_API_KEY`:
```bash
export OLLAMA_API_KEY="your-actual-api-key"
```

#### Switching to Gemini or Local Ollama
* **To use Google Gemini:** Change `<provider>gemini</provider>` in `pom.xml`, configure `<gemini><apiKey>${env.GEMINI_API_KEY}</apiKey><model>gemini-2.0-flash</model></gemini>`, and export `GEMINI_API_KEY`.
* **To use local Ollama:** Update `<endpoint>` to `http://localhost:11434` and set `<model>` to your local model (e.g., `llama3` or `codegemma`). Local instances do not require an API key.

---

### How to Run the Sample

1. Export your API credentials:
   ```bash
   export OLLAMA_API_KEY="your-actual-api-key"
   ```

2. Navigate to the sample directory and trigger compilation:
   ```bash
   cd src/test/test-projects/notes-manager
   mvn reins:compile
   ```

   *Note: The generated Java source files are written to the `target/generated-sources/reins/` directory.*

3. To build the generated Java classes:
   ```bash
   mvn compile
   ```

