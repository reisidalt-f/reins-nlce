# Advanced Customization Recipes

Reins offers deep customization capabilities allowing developers to tailor how prompts are structured, how reasoning loops behave, and how security tool boundaries are enforced.

---

## 1. Custom FreeMarker Inference Scripts

You can override or augment Reins's built-in inference execution phases by specifying a custom scripts directory in your configuration:

```xml
<configuration>
  <reasoning>
    <scriptsPath>scripts/reins</scriptsPath>
  </reasoning>
</configuration>
```

### Script Resolution Priority Hierarchy

When resolving Apache FreeMarker (`.ftl`) template scripts for compilation phases, Reins queries resources in the following order:

1. **Custom Filesystem Path:** Checked under `reasoning.scriptsPath` relative to the project root.
2. **Internal Classpath Overrides.**
3. **Default Packaged Templates:** Located inside the plugin JAR at `inference/*.ftl`.

### Customizing Prompt Templates

To customize prompt contexts or reasoning pipelines, create files inside your custom scripts directory matching the standard phase template names:

* `system-context.ftl`: Configures the base system prompt and system persona for the LLM.
* `user-prompt.ftl`: Controls how natural-language source texts, markdown references, and attached files are packaged into the user turn.
* `phase-list.ftl`: Outlines execution phase step instructions.
* `max-turn-grace-prompt.ftl`: Prompt appended when approaching maximum multi-turn conversation limits (`maxTurns`).
* `reasoning-pipeline.ftl`: Defines multi-turn iteration loops and step sequencing.

### Top-Level Context Aliases & Helpers

FreeMarker templates have direct access to top-level context shortcuts and computed helpers, eliminating boilerplate null-checks:

* **Top-Level Aliases**: `source`, `pipeline`, `policy`, `config`, `tracking`, `cycle`, `inference`, `currentToolResult`, `attachments` (alongside `project`).
* **Tool Group Checking (`policy`)**:
  * Individual checks: `policy.hasListFiles`, `policy.hasReadFiles`, `policy.hasWriteFiles`, `policy.hasAppendFiles`, `policy.hasPrependFiles`, `policy.hasMoveFiles`, `policy.hasCopyFiles`, `policy.hasPatchFiles`, `policy.hasDeleteFiles`, `policy.hasListCompiledFiles`, `policy.scriptRunnerEnabled`.
  * Group checks: `policy.hasAnyReadOrList`, `policy.hasAnyMutation`, `policy.hasAnyToolEnabled`.
* **Pipeline & Inference Helpers**:
  * `pipeline.firstPhase`: `true` if in initial execution phase (`currentPhaseOrdinal == 0`).
  * `pipeline.firstMessage`: `true` on the initial turn of a phase before tool results or directives.
  * `inference.firstTurn` / `inference.hasHistory`: check conversation history state.
* **Formatted List Helpers**:
  * `${tracking.notesOrNone}`, `${tracking.compiledPathsOrNone}`, `${inference.inspectedFilesOrNone}`: format lists as markdown bullet points or return `"- none"`.

### Project Context Object Reference (`project`)

The `project` context object (and its top-level aliases) exposes the complete runtime state to FreeMarker templates:

| Property / Alias | View Type | Key Properties & Helpers |
| :--- | :--- | :--- |
| `project.source` / `source` | `SourceView` | `path`, `qualifiedPath` (e.g. `main:src/spec.md`), `absolutePath`, `content`, `hash`, `scope`, `codegenBlocks` (`List<CodegenBlockView>`) |
| `project.fileBases` / `fileBases` | `FileBasesView` | Resolved filesystem paths: `main`, `test`, `target`, `projectRoot` |
| `project.pipeline` / `pipeline` | `PipelineView` | `currentPhase`, `currentPhaseOrdinal`, `totalPhases`, `phaseNames`, `processingStatus` (`COMPILE`/`VALIDATE`), `lastDirectiveIntent`, `lastDirectiveContentType`, `lastDirectiveBody`, `lastFailureClass`, `currentToolResultAvailable`, `firstMessage`, `firstPhase` |
| `project.policy` / `policy` | `PolicyView` | Permission bases (`listFilesBases`, `readFilesBases`, `writeFilesBases`, `appendFilesBases`, `prependFilesBases`, `moveFilesBases`, `copyFilesBases`, `patchFilesBases`, `deleteFilesBases`, `listCompiledFilesBases`), `scriptRunnerEnabled`, `toolOpsReference`, individual & group checks (`hasListFiles`, `hasReadFiles`, `hasWriteFiles`, `hasAppendFiles`, `hasPrependFiles`, `hasMoveFiles`, `hasPatchFiles`, `hasDeleteFiles`, `hasListCompiledFiles`, `hasAnyReadOrList`, `hasAnyMutation`, `hasAnyToolEnabled`) |
| `project.config` / `config` | `ConfigView` | `model`, `maxTurns`, `thinkingOutLoud`, `projectCycle`, `failOnError`, `verbose`, `dryRun`, `addReasoningNotes`, `scriptDir`, `scriptsPath` |
| `project.tracking` / `tracking` | `TrackingView` | `sourceHash`, `compiledPaths`, `compiledRelativePaths`, `lastStatus`, `lastCompiledAt`, `notes`, list helpers (`notesOrNone`, `compiledPathsOrNone`) |
| `project.inference` / `inference` | `InferenceStateView` | `context`, `conversationHistory` (`List<TurnView>`), `inspectedFiles`, `compiledFiles`, `toolOperations` (`List<ToolOpView>`), helpers (`inspectedFilesOrNone`, `hasHistory`, `firstTurn`) |
| `project.cycle` / `cycle` | `CycleView` | `cycleId`, `turnCount`, `maxTurns`, `status`, `completedAt` |
| `project.attachments` / `attachments` | `List<AttachmentView>` | Background attachments list (`path`, `content`, `mimeType`) |
| `project.referenceTree` | `String` | Rendered markdown reference graph string |
| `project.currentToolResult` / `currentToolResult` | `ToolResultView` | Result of last tool execution (`status`, `operation`, `qualifiedPath`, `resolvedBase`, `content`, `stdout`, `stderr`, `exitCode`, `failureReason`, `policyCode`) |

#### Top-Level Execution Variables

In addition to `project` and its aliases, templates receive specific execution variables depending on the phase:

* **`phase`**: Injected into `reasoning-pipeline.ftl` to identify the pipeline step (e.g. `list-phases`, `default-cycle`, `phase-entry`, `phase-transition`).
* **`step`**: Injected into `context-build.ftl` to evaluate context payload sub-steps (e.g. `list-message-type`, `system-context`, `previously-compiled-files`, `previously-inspected-files`, `references-tree`, `background-files`).

#### Nested View Types Reference

* **`CodegenBlockView`**: `index`, `language`, `instructions` (`List<String>`), `outputPath`, `fingerprint`
* **`TurnView`**: `sequence`, `role`, `content`, `attachedFilePaths` (`List<String>`)
* **`ToolOpView`**: `operation`, `base`, `path`, `status`, `failureReason`, `permittedToLlm`, `durationMs`
* **`AttachmentView`**: `path`, `content`, `mimeType`

### Reading Files in Templates (`readFile`, `hasFile`, `readFileOrDefault`)

FreeMarker prompt scripts can inspect and read file contents directly from any configured Reins base (`main`, `test`, `target`, or custom bases):

```ftl
<#-- Check if file exists -->
<#if hasFile("target:extra-instructions.txt")>
${readFile("target:extra-instructions.txt")}
</#if>

<#-- Read file with default string fallback -->
${readFileOrDefault("main:specs/schema.json", "{}")}

<#-- Read file using top-level source qualified path -->
<#assign companionPath = source.qualifiedPath?keep_before_last(".") + ".json">
${readFileOrDefault(companionPath, "{}")}
```

`readFile`, `hasFile`, and `readFileOrDefault` validate paths against Reins security boundaries (`PathValidator`). If the requested file is missing or unreadable, `readFile` safely returns `""` and `hasFile` returns `false`.

### Engineered Compilation Pipeline & Target Notes Scratchpad

`reasoning-pipeline.ftl` structures compilation into four engineered phases:

1. **`discovery-and-context`**: Inspects input source markdown, reference graph trees, and incoming tracking notes (`tracking.notes`). Creates and initializes the target scratchpad notes file (e.g. `target:compilation-notes/<source>.compilation-notes.md`).
2. **`architecture-and-contracts`**: Aligns component boundaries, API schemas, and data structures. Appends architectural designs and target file output coordinates to the scratchpad notes file.
3. **`code-synthesis`**: Synthesizes and writes target implementation code (`WRITE_FILE`, `PATCH_FILE`, `APPEND_FILE`, `PREPEND_FILE`) using the target scratchpad as source of truth.
4. **`verification-and-handoff`**: Audits output code against requirements, emits corrective handoff notes (`ADD_REASONING_NOTE`) for external source dependencies, and finishes execution.

#### Persistent On-Disk Scratchpad (`target:compilation-notes/...`)
The prompt computes `notesPath = "target:compilation-notes/" + sourceRelPath`. In every phase, if `hasFile(notesPath)` is true, Reins embeds `${readFile(notesPath)}` into the prompt context, preserving authoritative compilation state across turns and phases.

### Dynamic Phase Jumping (`GOTO_PHASE`)

In custom reasoning scripts or LLM response directives, you can dynamically cancel the current phase execution and start a target phase from the beginning using the `GOTO_PHASE` directive header:

```ftl
<#-- Shorthand direct header instruction -->
GOTO_PHASE: verification-phase
CONTENT_TYPE: user-progress

Canceling setup phase and jumping directly to verification-phase.
```

Or using canonical headers:

```ftl
INTENT: goto-phase
TARGET_PHASE: initial-context
CONTENT_TYPE: message-to-user

Restarting reasoning pipeline from initial-context.
```

---

## 2. Fine-Tuned Tooling Permission Profiles

Reins restricts file mutations through a sandboxed operations layer. You can tailor tool privileges for `main`, `test`, `target`, or any custom source base depending on your project security and flexibility needs.

### Profile A: Standard Least-Privilege Baseline (Recommended)

Blocks the model from modifying anything outside standard compilation target roots, allowing read-only access to existing source texts.

```xml
<tooling>
  <main>list,list_compiled,read</main>
  <test>list,list_compiled,read</test>
  <target>list,read,write,patch,delete</target>
</tooling>
```

### Profile B: Broad Compatibility Mode

Allows the model to clean up old files, write test code, or patch existing classes interactively during complex multi-turn reasoning cycles.

```xml
<tooling>
  <main>list,list_compiled,read,write,append,prepend,move,copy,patch,delete</main>
  <test>list,list_compiled,read,write,append,prepend,move,copy,patch,delete</test>
  <target>list,list_compiled,read,write,append,prepend,move,copy,patch,delete</target>
</tooling>
```

*Note: For backward compatibility, assigning the `write` permission token to a base implicitly grants `append`, `prepend`, `move`, and `copy` operations on that base unless explicitly restricted.*

---

## 3. Tool Operations Syntax & Reasoning Script Integration

In reasoning scripts (`.ftl` templates) and LLM multi-turn interactions, tool operations are requested using plain-text positional commands formatted inside `--reins-boundary` multipart sections.

### Automatic Tool Reference Injection (`${policy.toolOpsReference}`)

In system context templates (such as `system-context.ftl` or custom prompt scripts), embedding `${policy.toolOpsReference}` automatically generates the active tool capabilities and syntax guide tailored strictly to the configured permissions.

```ftl
<#-- Inject active tool instructions in custom prompt script -->
<#if policy.hasAnyToolEnabled>
${policy.toolOpsReference}
</#if>
```

### Tool Command Syntax Reference

Each tool operation part starts with a positional command line: `<OPERATION> <BASE> <PATH> [ARGS...]` followed by optional payload lines (for text/binary content or notes).

| Operation | Positional Command Syntax | Description | Example |
| :--- | :--- | :--- | :--- |
| `list_files` | `LIST_FILES <base> <path> [true\|false] [<intent>]` | List directory contents | `LIST_FILES target output/components true` |
| `read_file` | `READ_FILE <base> <path> [<intent>]` | Read file text content | `READ_FILE main domain/User.md` |
| `write_file` | `WRITE_FILE <base> <path> [base64] [<intent>]` | Create or overwrite a file | `WRITE_FILE target output/User.java` (+ content payload) |
| `append_file` | `APPEND_FILE <base> <path> [base64] [<intent>]` | Append text/binary content | `APPEND_FILE target output/Log.txt` (+ content payload) |
| `prepend_file` | `PREPEND_FILE <base> <path> [base64] [<intent>]` | Prepend text/binary content | `PREPEND_FILE target output/Header.txt` (+ content payload) |
| `move_file` | `MOVE_FILE <base> <path> <destination> [<intent>]` | Move or rename file within base | `MOVE_FILE target old/path.txt new/path.txt` |
| `copy_file` | `COPY_FILE <base> <path> <dest_base> <dest_path> [<intent>]` | Copy file across or within configured bases | `COPY_FILE main templates/Header.java target output/Header.java` |
| `patch_file` | `PATCH_FILE <base> <path> [<intent>]` | Apply Unified Diff patch | `PATCH_FILE target output/User.java` (+ diff payload) |
| `delete_file` | `DELETE_FILE <base> <path> [<intent>]` | Remove a file | `DELETE_FILE target obsolete/Old.java` |
| `list_compiled_files` | `LIST_COMPILED_FILES <base> <path> [<intent>]` | List files generated from source | `LIST_COMPILED_FILES main domain/User.md` |
| `run_script` | `RUN_SCRIPT <script_name> [args...] [--intent <intent>]` | Execute an allowed shell script | `RUN_SCRIPT build.sh target --intent compile` |
| `add_reasoning_note` | `ADD_REASONING_NOTE compiled <qualified_path>` or `source <path>` | Attach corrective tracking note | `ADD_REASONING_NOTE source domain/User.md` (+ note payload) |
| `clear_reasoning_notes` | `CLEAR_REASONING_NOTES <source> [<intent>]` | Clear tracking notes for source | `CLEAR_REASONING_NOTES main:domain/User.md` |

### Positional & Multipart Formatting Example

```text
--reins-boundary
APPEND_FILE target output/audit.log append access entry

[2026-08-13] Added security token check
--reins-boundary
MOVE_FILE target output/DraftClass.java output/FinalClass.java move completed draft
--reins-boundary--
```

### File Ownership Transfer (`grantFileOwnership`)

By default, `<grantFileOwnership>true</grantFileOwnership>` is enabled. When a source file modifies, appends, prepends, moves, patches, or deletes an output file previously tracked by another source file, Reins automatically transfers ownership of the output file to the active source file and removes it from the previous owner's tracking record. To enforce strict ownership isolation and prevent cross-source file mutations, set `<grantFileOwnership>false</grantFileOwnership>`.

