# Configuration Reference

The tables and XML schemas below apply uniformly across **CLI**, **Library**, and **Maven Plugin** configurations. When using JSON, YAML, or Properties files, configuration elements are mapped using standard dot-notation prefixes (e.g., the XML structure `<gemini><apiKey>...</apiKey></gemini>` maps to the property `gemini.apiKey`).

---

## Complete XML Configuration Schema

```xml
<configuration>
  <!-- Main Controls -->
  <provider>gemini</provider> <!-- Options: gemini, ollama -->
  <scanRoots>
    <scanRoot>src/main/nl</scanRoot>
    <scanRoot>src/test/nl</scanRoot>
  </scanRoots>
  <includePattern>**/*.md</includePattern>
  <mainNlRoot>${project.basedir}/src/main/nl</mainNlRoot>
  <testNlRoot>${project.basedir}/src/test/nl</testNlRoot>
  <source>domain/Customer.md</source>

  <projectContextFile>${project.basedir}/project.md</projectContextFile>
  <enableProjectInference>false</enableProjectInference>
  <failOnError>false</failOnError>
  <verbose>false</verbose>
  <dryRun>false</dryRun>
  <validateAll>false</validateAll>

  <!-- Target Paths Config (All parameters under <target> are optional) -->
  <target>
    <project>${project.basedir}</project> <!-- Base target directory (replaces deprecated root; defaults to project base dir) -->
    <main>src/main/java</main>            <!-- Main code output path (defaults to src/main/java; accepts absolute paths within project root) -->
    <test>src/test/java</test>            <!-- Test code output path (defaults to src/test/java; accepts absolute paths within project root) -->
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

  <!-- Ollama Local Config -->
  <ollama>
    <model>codegemma</model>
    <endpoint>http://localhost:11434</endpoint>
    <timeoutSeconds>60</timeoutSeconds>
    <retryAttempts>2</retryAttempts>
    <options>
      <temperature>0.1</temperature>
    </options>
  </ollama>

  <!-- Context & Ref Parsing -->
  <context>
    <includeProjectFiles>false</includeProjectFiles>
    <attachReferencedFiles>true</attachReferencedFiles>
    <cachedContent>true</cachedContent>
    <allowScriptedMessageData>true</allowScriptedMessageData>
    <allowScriptedAttachments>true</allowScriptedAttachments>
    <referencesTreeDepth>1</referencesTreeDepth> <!-- Options: 0, integer, * -->
    <sources>
      <sourceFile>docs/architecture.md</sourceFile>
    </sources>
  </context>

  <!-- Multi-Turn / Custom Orchestration Scripting -->
  <reasoning>
    <maxTurns>10</maxTurns>
    <maxReferenceDepth>8</maxReferenceDepth>
    <scriptsPath>scripts/reins</scriptsPath>
    <thinkingOutLoud>false</thinkingOutLoud>
    <logSystemContext>false</logSystemContext>
    <logParseErrorRecovery>false</logParseErrorRecovery>
    <enableReasoningLog>false</enableReasoningLog>
    <turnCountNote>true</turnCountNote>
  </reasoning>

  <!-- Tool Access permissions -->
  <tooling>
    <main>list,list_compiled,read</main>
    <test>list,list_compiled,read</test>
    <target>list,patch,delete</target>
    <scriptPath>scripts/reins</scriptPath>
    <addReasoningNotes>true</addReasoningNotes>
  </tooling>

  <!-- Recompilation Fine-Tuning -->
  <recompileOn>
    <markdownReferences>true</markdownReferences>
    <inspectedFiles>false</inspectedFiles>
    <compiledFiles>false</compiledFiles>
  </recompileOn>

  <eagerlyProvide>
    <previouslyCompiledFiles>true</previouslyCompiledFiles>
    <previouslyInspectedFiles>true</previouslyInspectedFiles>
    <maxAttachmentSizeBytes>0</maxAttachmentSizeBytes>
  </eagerlyProvide>

  <tracking>
    <freezeState>false</freezeState>
  </tracking>

  <!-- Diagnostics -->
  <logging>
    <scriptsEvents>false</scriptsEvents>
    <eagerlyProvided>false</eagerlyProvided>
    <fileListingAndReading>false</fileListingAndReading>
    <fileMutating>false</fileMutating>
    <scriptRun>false</scriptRun>
    <selectionReason>false</selectionReason>
  </logging>
</configuration>
```

---

## Detailed Parameter Index

### 1. Top-Level Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `provider` | `String` | `gemini` | Target LLM driver. Supported: `gemini`, `ollama`, `stub` (testing). |
| `scanRoots` | `List<File>` | `src/main/nl`, `src/test/nl` | Root directories scanned for Markdown files. |
| `includePattern` | `String` | `**/*.md` | Inclusion glob applied when scanning directories. |
| `mainNlRoot` | `File` | `src/main/nl` | Base root for resolving main source texts. |
| `testNlRoot` | `File` | `src/test/nl` | Base root for resolving test source texts. |
| `source` | `String` | unset | Relative path to activate **Explicit Source Mode** for direct compilation targets. |
| `projectContextFile` | `File` | `project.md` | Base Markdown file project-wide source text. |
| `enableProjectInference` | `boolean` | `false` | Enables project-level context evaluation during compilation runs. |
| `failOnError` | `boolean` | `false` | If true, compilation errors fail the execution immediately. |
| `verbose` | `boolean` | `false` | Enables diagnostic logging detailing execution pipelines. |
| `dryRun` | `boolean` | `false` | Runs validation and scanners without writing code to disk. |
| `validateAll` | `boolean` | `false` | If true, skipped and valid records are validated on disk. |

### 2. Gemini Configuration Settings (`<gemini>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `apiKey` | `String` | Required | Gemini cloud platform credential. |
| `model` | `String` | unset | Selected model version (e.g., `gemini-2.0-flash`, `gemini-2.5-pro`). |
| `endpoint` | `String` | unset | Base API host address (must be absolute HTTP/HTTPS URL). |
| `timeoutSeconds` | `int` | `30` | Network request timeout limits. |
| `retryAttempts` | `int` | `3` | Number of times to retry failed requests. Negative numbers clamp to 0. |
| `emptyResponseRetryDelayMs` | `int` | `1000` | Back-off delay when recovering from empty model response packets. |
| `maximumTurns` | `Integer` | `1` | Turn limits enforced for single execution sequences. |

#### Gemini Generation Configuration (`<gemini.generation>`)

| Parameter | Type | Range | Description |
|---|---|---|---|
| `temperature` | `Float` | `0.0` to `2.0` | Controls output creativity/determinism. Lower is more deterministic. |
| `topP` | `Float` | `0.0` to `1.0` | Nucleus sampling probability threshold. |
| `topK` | `Integer` | `>= 1` | Top-k tokens considered during generation selection. |
| `presencePenalty` | `Float` | `-2.0` to `2.0` | Penalty applied to tokens already appearing in generated text. |
| `frequencyPenalty` | `Float` | `-2.0` to `2.0` | Penalty applied based on token repetition rate. |

### 3. Ollama Configuration Settings (`<ollama>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `model` | `String` | unset | Model instance identifier loaded in local Ollama service. |
| `endpoint` | `String` | unset | Endpoint targeting the Ollama service. |
| `apiKey` | `String` | unset | Optional authorization key for secured local proxies. |
| `timeoutSeconds` | `int` | `60` | Request timeout limit. |
| `retryAttempts` | `int` | `3` | Request retry limit. |
| `options` | `Map<String, Object>`| empty | Map of custom configuration variables sent directly to Ollama. |

### 4. Context Configuration Settings (`<context>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `includeProjectFiles` | `boolean` | `false` | Attaches project files to the inference prompts. |
| `attachReferencedFiles` | `boolean` | `true` | Injects referenced source files directly into prompts. |
| `cachedContent` | `boolean` | `true` | Enables semantic client caching for repeat prompts. |
| `allowScriptedMessageData`| `boolean` | `true` | Permits inference scripts to structure custom prompt payloads. |
| `allowScriptedAttachments`| `boolean` | `true` | Allows inference scripts to append supplemental attachments. |
| `referencesTreeDepth` | `String` | `1` | Graph traversal limit. Valid values: `0` (disabled), positive integer, or `*` (infinite). |
| `sources` | `List<File>` | empty | List of supplementary source files (e.g., rules, target specs) attached as context. |

### 5. Reasoning Settings (`<reasoning>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `maxTurns` | `int` | `10` | Maximum multi-turn conversation rounds permitted before forced completion. |
| `maxReferenceDepth` | `int` | `8` | Deepest source reference level resolved when building graphs. |
| `scriptsPath` | `String` | unset | Path to custom FreeMarker scripts directory, overriding defaults. |
| `thinkingOutLoud` | `boolean` | `false` | Enables verbose logging of LLM reasoning traces. |
| `logSystemContext` | `boolean` | `false` | Logs resolved FreeMarker system prompts. |
| `logParseErrorRecovery` | `boolean` | `false` | Prints parsing diagnostics when recovering from malformed model responses. |
| `enableReasoningLog` | `boolean` | `false` | Creates dedicated session conversation logs inside `.reins/logs`. |
| `turnCountNote` | `boolean` | `true` | Appends turn-warning signals to the prompt as maxTurns approaches. |

### 6. Tooling Configuration Settings (`<tooling>`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `main` | `String` | unset | Comma-separated list of permitted tool operations on `src/main`. |
| `test` | `String` | unset | Comma-separated list of permitted tool operations on `src/test`. |
| `target` | `String` | unset | Comma-separated list of permitted tool operations on compiled outputs. |
| `scriptPath` | `String` | unset | Directory path containing executable scripts for the `run_script` tool. |
| `addReasoningNotes` | `boolean` | `false` | Enables automated notes injection through inference scripts. |

*Permitted tool tokens for `main`, `test`, `target` include:* `list`, `list_compiled`, `read`, `write`, `patch`, `delete`.

### 7. Target Paths Configuration (`<target>`)

All parameters inside the `<target>` configuration block are **optional**.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `project` | `File` | project base dir | Optional. Base directory for compilation output resolution (replaces deprecated `root`). Resolves relative to the project base directory unless an absolute path is provided. |
| `root` | `File` | project base dir | Optional (Deprecated). Fallback base directory for compilation output resolution. |
| `main` | `String` | `src/main/java` | Optional. Sub-path/directory for generated main sources. Accepts absolute paths, but they must resolve to a location inside the project's base directory. If a relative path is provided, it resolves relative to the project's base directory. |
| `test` | `String` | `src/test/java` | Optional. Sub-path/directory for generated test sources. Accepts absolute paths, but they must resolve to a location inside the project's base directory. If a relative path is provided, it resolves relative to the project's base directory. |

### 8. Fine-Tuned Tracking & Logging

* **`<recompileOn>`**: Fine-tune compilation invalidation triggers.
  * `markdownReferences` (default: `true`): Re-trigger compilation if referenced source files are modified.
  * `inspectedFiles` (default: `false`): Re-trigger compilation if files inspected during prior runs are modified.
  * `compiledFiles` (default: `false`): Re-trigger compilation if compiled output files are modified.
* **`<eagerlyProvide>`**: Control which files are attached during the compilation cycle.
  * `previouslyCompiledFiles` (default: `true`): Eagerly attaches previously generated outputs.
  * `previouslyInspectedFiles` (default: `true`): Eagerly attaches previously read codebase context.
  * `maxAttachmentSizeBytes` (default: `0`): Global file attachment size limits (0 indicates unlimited).
* **`<tracking>`**:
  * `freezeState` (default: `false`): Evaluates existing compilation manifests, but skips writing new manifests to disk.
* **`<logging>`**: Debugging outputs.
  * `scriptsEvents` (default: `false`): Logs FreeMarker phase load/unload events.
  * `eagerlyProvided` (default: `false`): Logs details of files eagerly attached to prompts.
  * `fileListingAndReading` (default: `false`): Logs model-driven files reads.
  * `fileMutating` (default: `false`): Logs model-driven writes, patches, and deletions.
  * `scriptRun` (default: `false`): Logs custom tool script executions.
  * `selectionReason` (default: `false`): Logs compilation selection reason banners for every processed target.

---

## Troubleshooting

### "Invalid plugin configuration: Gemini apiKey is required"
Reins cannot locate your LLM authorization key. Double-check that `GEMINI_API_KEY` is exported in the shell running Maven or the CLI, or hardcode it in your configuration.

### "Unrecognized tool operation token 'X' in tooling.Y"
You specified an invalid permission in your tooling config. Supported operations are restricted to `list`, `list_compiled`, `read`, `write`, `patch`, and `delete`.

### "enableProjectInference is true but project file not found: project.md"
You activated project-wide inference context processing, but forgot to supply a high-level summary at your project root. Disable `<enableProjectInference>` or create a basic `project.md` file detailing your tech stack.

### "Explicit source is ambiguous"
When compiling using explicit sources (e.g., `-Dsource=file.md` or `--source file.md`), the filename matches multiple files inside your scan roots. Provide a relative path prefix (e.g., `-Dsource=domain/file.md`) to resolve ambiguity.
