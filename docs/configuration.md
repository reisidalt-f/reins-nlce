# Configuration Reference

The tables and XML schemas below apply uniformly across **CLI**, **Library**, and **Maven Plugin** configurations. When using JSON, YAML, or Properties files, configuration elements are mapped using standard dot-notation prefixes (e.g., the XML structure `<gemini><apiKey>...</apiKey></gemini>` maps to the property `gemini.apiKey`).

---

## Complete XML Configuration Schema

```xml
<configuration>
  <!-- Main Controls -->
  <provider>gemini</provider> <!-- Options: gemini, openai, ollama, stub -->
  
  <!-- Named Source Base Group (Required: source.main) -->
  <source>
    <main>${project.basedir}/src/main/nl</main>
    <test>${project.basedir}/src/test/nl</test>
    <lib>${project.basedir}/src/lib/nl</lib>
  </source>
  
  <skipTest>false</skipTest> <!-- CLI: -DskipTest / --skipTest -->
  <includePattern>**/*.md</includePattern>
  <source>domain/Customer.md</source>
  <note>Fix API signatures for customer module</note> <!-- CLI: -Dnote / --note -->

  <failOnError>false</failOnError>
  <verbose>false</verbose>
  <dryRun>false</dryRun>
  <validateAll>false</validateAll>
  <compilationThreads>1</compilationThreads> <!-- Number of simultaneous compilation threads (default: 1) -->
  <freshCompilation>false</freshCompilation>   <!-- Force full recompilation ignoring prior tracking hashes -->

  <!-- Mandatory Target Paths Config (Every active source.<name> must have a matching target.<name>) -->
  <target>
    <main>src/main/java</main>            <!-- Target code output path for source.main -->
    <test>src/test/java</test>            <!-- Target code output path for source.test -->
    <lib>src/lib/java</lib>               <!-- Target code output path for source.lib -->
  </target>

  <!-- Gemini LLM Config -->
  <gemini>
    <apiKey>${env.GEMINI_API_KEY}</apiKey>
    <model>gemini-2.0-flash</model>
    <endpoint>https://generativelanguage.googleapis.com</endpoint>
    <timeoutSeconds>30</timeoutSeconds>
    <retryAttempts>3</retryAttempts>
    <emptyResponseRetryDelayMs>1000</emptyResponseRetryDelayMs>
    <maximumTurns>1</maximumTurns>
    <generation>
      <temperature>0.2</temperature>
      <topP>0.9</topP>
      <topK>40</topK>
      <presencePenalty>0.0</presencePenalty>
      <frequencyPenalty>0.0</frequencyPenalty>
    </generation>
  </gemini>

  <!-- OpenAI Config -->
  <openai>
    <apiKey>${env.OPENAI_API_KEY}</apiKey>
    <model>gpt-4o</model>
    <endpoint>https://api.openai.com</endpoint>
    <timeoutSeconds>60</timeoutSeconds>
    <retryAttempts>3</retryAttempts>
  </openai>

  <!-- Ollama Local Config -->
  <ollama>
    <model>codegemma</model>
    <endpoint>http://localhost:11434</endpoint>
    <apiKey></apiKey>
    <timeoutSeconds>60</timeoutSeconds>
    <retryAttempts>2</retryAttempts>
    <options>
      <temperature>0.1</temperature>
    </options>
  </ollama>

  <!-- Context & Ref Parsing -->
  <context>
    <cachedContent>true</cachedContent>
    <allowScriptedMessageData>true</allowScriptedMessageData>
    <allowScriptedAttachments>true</allowScriptedAttachments>
    <compiledFiles>true</compiledFiles>
    <inspectedFiles>true</inspectedFiles>
    <plainAttachmentExtensions>json,py,rs</plainAttachmentExtensions> <!-- Additional text file extensions formatted as plain text -->
    <referencesTree>
      <attachFiles>false</attachFiles> <!-- Toggle attaching referenced files to LLM context (default: false) -->
      <depth>3</depth> <!-- Reference tree depth: 0, integer, * (default: 3) -->
      <maxDepth>8</maxDepth> <!-- Max safety cap limit for traversal depth (default: 8) -->
    </referencesTree>
    <sources>
      <source pattern="**/domain/*.md" phase="initial-context, verification">
        <file>docs/architecture.md</file>
      </source>
    </sources>
  </context>

  <!-- Multi-Turn / Custom Orchestration Scripting -->
  <reasoning>
    <enabled>true</enabled>
    <maxTurns>10</maxTurns>
    <maxReferenceDepth>8</maxReferenceDepth>
    <scriptsPath>scripts/reins</scriptsPath>
    <thinkingOutLoud>false</thinkingOutLoud>
    <logSystemContext>false</logSystemContext>
    <logParseErrorRecovery>false</logParseErrorRecovery>
    <enableReasoningLog>false</enableReasoningLog>
    <turnCountNote>true</turnCountNote>
    <summarizeCycleTurns>0</summarizeCycleTurns> <!-- Turn interval for conversation summarization (default: 0 = disabled) -->
  </reasoning>

  <!-- Tool Access permissions -->
  <tooling>
    <main>list,list_compiled,read</main>
    <test>list,list_compiled,read</test>
    <target>list,read,write,patch,delete</target>
    <scriptPath>scripts/reins</scriptPath>
    <addReasoningNotes>true</addReasoningNotes>
    <grantFileOwnership>true</grantFileOwnership>
  </tooling>

  <!-- Recompilation Fine-Tuning -->
  <recompileOn>
    <markdownReferences>true</markdownReferences>
    <inspectedFiles>false</inspectedFiles>
    <compiledFiles>false</compiledFiles>
  </recompileOn>

  <eagerlyProvide>
    <maxAttachmentSizeBytes>0</maxAttachmentSizeBytes>
  </eagerlyProvide>

  <tracking>
    <freezeState>false</freezeState>
    <cleanupStaleCompiledFiles>false</cleanupStaleCompiledFiles>
  </tracking>

  <!-- Diagnostics -->
  <logging>
    <scriptsEvents>false</scriptsEvents>
    <eagerlyProvided>false</eagerlyProvided>
    <fileListingAndReading>false</fileListingAndReading>
    <fileMutating>false</fileMutating>
    <scriptRun>false</scriptRun>
    <selectionReason>false</selectionReason>
    <result>false</result>
    <trackingFile>false</trackingFile>
    <llmProvider>false</llmProvider>
    <sourceTag>false</sourceTag>
  </logging>

  <log>
    <skipped>false</skipped>
    <processingOrder>false</processingOrder>
    <eagerlyProvided>false</eagerlyProvided>
  </log>

  <!-- Model Request/Response Logging -->
  <model>
    <requestResponseLog>false</requestResponseLog>
  </model>
</configuration>
```

---

## Detailed Parameter Index

### 1. Top-Level & Source Base Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `provider` | `String` | **None** (Required) | Target LLM driver. Mandatory. Supported: `gemini`, `openai`, `ollama`, `stub` (testing). |
| `source.main` | `Path` | **None** (Required) | Base root directory for main natural language sources. Mandatory. |
| `source.test` | `Path` | **None** (Optional) | Base root directory for test natural language sources. |
| `source.<name>` | `Path` | **None** (Optional) | Custom named source base directories (e.g., `source.lib`, `source.include`). |
| `skipTest` | `boolean` | `false` | CLI flag (`-DskipTest` or `--skipTest`) to disable scanning and compilation of `test` base. |
| `includePattern` | `String` | `**/*.md` | Inclusion glob applied when scanning source base directories. |
| `source` | `String` | unset | Explicit source target selection. Supports scheme prefixes (e.g. `lib:domain/Customer.md`) and base-root paths (`/domain/Customer.md`). Used for explicit mode during `compile` or `clean`, and to designate the target for `add-note`. |
| `note` | `String` | unset | User/CLI guidance note attached to explicit source compilation or `add-note` executions. |
| `failOnError` | `boolean` | `false` | If true, compilation errors fail the execution immediately. |
| `verbose` | `boolean` | `false` | Enables diagnostic logging detailing execution pipelines. |
| `dryRun` | `boolean` | `false` | Runs validation and scanners without writing code to disk. |
| `validateAll` | `boolean` | `false` | If true, skipped and valid records are validated on disk. |
| `compilationThreads` | `int` | `1` | Number of simultaneous file compilation threads executed in parallel while respecting the directed dependency graph (`-DcompilationThreads` / `--compilationThreads`). Must be `>= 1`. |
| `freshCompilation` | `boolean` | `false` | Force fresh compilation ignoring existing tracking checksums (`-DfreshCompilation` / `--freshCompilation`). |

---

### 2. Target Paths Configuration (`<target>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `target.main` | `String` | **None** (Required) | Target code output directory relative to project root for `source.main`. |
| `target.test` | `String` | **None** (Required if `source.test` present) | Target code output directory relative to project root for `source.test`. |
| `target.<name>` | `String` | **None** (Required for `source.<name>`) | Target code output directory relative to project root for custom `source.<name>`. |

*Note: Reins enforces that **every registered `source.<name>` must have a corresponding `target.<name>` path configured**. Hardcoded defaults have been completely removed.*

---

### 4. LLM Provider Configurations

#### Gemini Configuration Settings (`<gemini>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `gemini.apiKey` | `String` | unset | Gemini platform API key. |
| `gemini.model` | `String` | unset | Gemini model identifier (e.g. `gemini-2.0-flash`). |
| `gemini.endpoint` | `String` | `https://generativelanguage.googleapis.com` | Base endpoint URL for Gemini API requests. |
| `gemini.timeoutSeconds` | `int` | `30` | Network request timeout limit in seconds. |
| `gemini.retryAttempts` | `int` | `3` | Number of retry attempts on network or HTTP errors. |
| `gemini.emptyResponseRetryDelayMs` | `int` | `1000` | Delay in milliseconds before retrying after an empty response. |
| `gemini.maximumTurns` | `Integer` | `1` | Maximum turns override for Gemini multi-turn reasoning cycles. |
| `gemini.generation.temperature` | `Float` | unset | Sampling temperature. |
| `gemini.generation.topP` | `Float` | unset | Nucleus sampling parameter. |
| `gemini.generation.topK` | `int` | unset | Top-K sampling parameter. |
| `gemini.generation.presencePenalty` | `Float` | unset | Presence penalty parameter. |
| `gemini.generation.frequencyPenalty` | `Float` | unset | Frequency penalty parameter. |

#### OpenAI Configuration Settings (`<openai>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `openai.model` | `String` | unset | OpenAI model version (e.g., `gpt-4o`, `gpt-4-turbo`). |
| `openai.endpoint` | `String` | `https://api.openai.com` | Base API endpoint for OpenAI. |
| `openai.apiKey` | `String` | unset | OpenAI platform credential. |
| `openai.timeoutSeconds` | `int` | `60` | Network request timeout limits. |
| `openai.retryAttempts` | `int` | `3` | Number of times to retry failed requests. |
| `openai.options` | `Map` | empty | Map of custom configuration variables sent directly to OpenAI. |

#### Ollama Configuration Settings (`<ollama>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `ollama.model` | `String` | unset | Local model identifier (e.g., `codegemma`, `llama3`). |
| `ollama.endpoint` | `String` | unset | Local Ollama instance URL (e.g., `http://localhost:11434`). |
| `ollama.apiKey` | `String` | unset | Optional authorization key for secured Ollama proxies. |
| `ollama.timeoutSeconds` | `int` | `60` | Request timeout limit in seconds. |
| `ollama.retryAttempts` | `int` | `3` | Retry attempts on connection failures. |
| `ollama.options` | `Map` | empty | Additional custom option parameters sent to Ollama API. |

---

### 5. Context Settings (`<context>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `cachedContent` | `boolean` | `true` | Enables server-side LLM context caching (`cachedContents`). |
| `allowScriptedMessageData` | `boolean` | `true` | Permits custom prompt scripts to modify message data payloads. |
| `allowScriptedAttachments` | `boolean` | `true` | Permits custom prompt scripts to filter or reorder background attachments. |
| `compiledFiles` | `boolean` | `true` | Controls whether previously compiled files from prior cycles are eagerly provided in context attachments. |
| `inspectedFiles` | `boolean` | `true` | Controls whether previously inspected files from prior cycles are eagerly provided in context attachments. |
| `plainAttachmentExtensions` | `List` | empty | Additional custom file extensions attached using plain Markdown formatting instead of Base64. Merged with built-in text/code extensions (`md`, `java`, `py`, `json`, `yaml`, `xml`, `txt`, `css`, `sh`, `rs`, `go`, etc.). |
| `referencesTree.attachFiles` | `boolean` | `false` | Toggles whether files discovered in reference tree traversal are attached as context payloads. |
| `referencesTree.depth` | `String` | `"3"` | Reference tree traversal depth. Options: `0` (disabled), `1`, `2`, ..., `*` (unlimited). |
| `referencesTree.maxDepth` | `int` | `8` | Safety cap for maximum recursion depth when processing reference graphs. |
| `sources` | `List` | empty | Background context files with target processed file pattern filtering (`pattern`, default `**/*.md`) and multiple reasoning phase pattern filtering (`phase`, default `*`). Configurable in XML (`<source pattern="..." phase="...">`), YAML, JSON, or properties (`context.sources.N.file`, `context.sources.N.pattern`, `context.sources.N.phase`). |

---

### 6. Reasoning Settings (`<reasoning>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `enabled` | `Boolean` | unset | Explicit toggle to enable or disable reasoning execution cycles. |
| `maxTurns` | `int` | `10` | Maximum multi-turn reasoning limits for a compilation cycle. |
| `maxReferenceDepth` | `int` | `8` | Safety cap for maximum recursion depth when processing reference graphs. |
| `scriptsPath` | `String` | unset | Custom filesystem path for FreeMarker (`.ftl`) prompt scripts. |
| `thinkingOutLoud` | `boolean` | `false` | Enables incremental planning messages to user during turn evaluation. |
| `logSystemContext` | `boolean` | `false` | Enables diagnostic logging of rendered system context prompt. |
| `logParseErrorRecovery` | `boolean` | `false` | Enables diagnostic logging of response parse error recovery attempts. |
| `enableReasoningLog` | `boolean` | `false` | Enables writing structured execution log files (`.log`). |
| `turnCountNote` | `boolean` | `true` | Appends turn budget notes to outgoing prompts. |
| `summarizeCycleTurns` | `int` | `0` | Turn interval for LLM conversation history summarization. Set to `0` to disable summarization (default), `1` to request summarization every turn, or `N` to summarize every `N` turns (`reasoning.summarizeTurnInterval` is supported as a property alias). |

---

### 7. Model Settings (`<model>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `requestResponseLog` | `boolean` | `false` | Toggles writing effective language model requests and raw responses to `model-log/<timestamp>-<name-of-file-being-processed>.log`. |

---

### 8. Tooling Settings (`<tooling>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `tooling.main` | `String` | unset | Granted tool permissions for `main` source base (`list`, `list_compiled`, `read`, `write`, `append`, `prepend`, `move`, `copy`, `patch`, `delete`). |
| `tooling.test` | `String` | unset | Granted tool permissions for `test` source base (`list`, `list_compiled`, `read`, `write`, `append`, `prepend`, `move`, `copy`, `patch`, `delete`). |
| `tooling.target` | `String` | unset | Granted tool permissions for `target` output base (`list`, `list_compiled`, `read`, `write`, `append`, `prepend`, `move`, `copy`, `patch`, `delete`). |
| `tooling.<name>` | `String` | unset | Granted tool permissions for custom named base `<name>`. |
| `tooling.scriptPath` | `String` | unset | Directory path containing allowed external scripts for `RUN_SCRIPT`. |
| `tooling.addReasoningNotes` | `boolean` | `false` | Permits adding reasoning notes via `ADD_REASONING_NOTE`. |
| `tooling.grantFileOwnership` | `boolean` | `true` | Controls whether mutating a file owned by another source automatically grants file ownership to the current source and disowns the previous owner. If set to `false`, mutating files owned by another source is blocked. |

---

### 9. Tracking Settings (`<tracking>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `tracking.freezeState` | `boolean` | `false` | Disables writing and updating source tracking records on disk when set to `true`. |
| `tracking.cleanupStaleCompiledFiles` | `boolean` | `false` | Controls whether stale compiled files are automatically deleted upon compilation cycle completion (applies to both full batch runs and explicit source compilation runs). |

---

### 10. Logging Settings (`<logging>` & `<log>`)

#### Fine-Grained Diagnostics (`<logging>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `logging.scriptsEvents` | `boolean` | `false` | Logs script engine events and phase transitions. |
| `logging.eagerlyProvided` | `boolean` | `false` | Logs details of files eagerly provided as context attachments. |
| `logging.fileListingAndReading` | `boolean` | `false` | Logs tool file reading and directory listing operations. |
| `logging.fileMutating` | `boolean` | `false` | Logs file writing, patching, appending, and deleting operations. |
| `logging.scriptRun` | `boolean` | `false` | Logs script execution operations (`RUN_SCRIPT`). |
| `logging.selectionReason` | `boolean` | `false` | Logs reasons why individual source files were selected or skipped. |
| `logging.result` | `boolean` | `false` | Logs compilation cycle summary results. |
| `logging.trackingFile` | `boolean` | `false` | Logs tracking record file reads and writes. |
| `logging.llmProvider` | `boolean` | `false` | Logs LLM provider initialization and configuration resolution. |
| `logging.sourceTag` | `boolean` | `false` | Prefixes log messages emitted during a file's reasoning cycle with `[<simple-file-name>]` (e.g. `[Customer.md]`). |

#### Legacy Build Log Flags (`<log>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `log.skipped` | `boolean` | `false` | Logs skipped files during batch compilation scanning. |
| `log.processingOrder` | `boolean` | `false` | Logs dependency graph processing order. |
| `log.eagerlyProvided` | `boolean` | `false` | Logs eagerly provided attachment details. |

---

## Troubleshooting

### "Invalid plugin configuration: source.main is required and must be explicitly configured."
Reins requires an explicit `source.main` entry. Configure `<source><main>src/main/nl</main></source>` or set property `reins.source.main`.

### "Source base 'X' is defined but has no corresponding target path defined in target.X"
Every configured source base requires an output path in target. Add `<target><X>path</X></target>` or set property `reins.target.X=path`.

### "Explicit source is ambiguous"
When compiling using explicit sources (e.g., `-Dsource=file.md` or `--source file.md`), the filename matches multiple files. Provide a base scheme (e.g. `-Dsource=main:file.md`) or sub-path prefix (`-Dsource=domain/file.md`) to resolve ambiguity.

