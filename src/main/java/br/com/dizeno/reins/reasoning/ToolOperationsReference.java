/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionType;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

 
/**
 * ToolOperationsReference is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a model representation of a source file reference.
 */
public class ToolOperationsReference {

    private ToolOperationsReference() {}

     
    /**
     * Builds the configured target.
     *
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param fileListingAndReadingEnabled the file listing and reading enabled
     * @param fileMutatingEnabled the file mutating enabled
     * @param scriptRunEnabled the script run enabled
     * @return the string result
     */
    public static String build(FilePolicy policy,
                               boolean scriptRunnerEnabled,
                               boolean fileListingAndReadingEnabled,
                               boolean fileMutatingEnabled,
                               boolean scriptRunEnabled) {
        java.util.Objects.requireNonNull(policy, "policy must not be null");
        return build(
                policy,
                scriptRunnerEnabled,
                fileListingAndReadingEnabled,
                fileMutatingEnabled,
                scriptRunEnabled,
                true);
    }

     
    /**
     * Builds the configured target.
     *
     * @param policy the policy
     * @param scriptRunnerEnabled the script runner enabled
     * @param fileListingAndReadingEnabled the file listing and reading enabled
     * @param fileMutatingEnabled the file mutating enabled
     * @param scriptRunEnabled the script run enabled
     * @param addReasoningNotesEnabled the add inference notes enabled
     * @return the string result
     */
    public static String build(FilePolicy policy,
                               boolean scriptRunnerEnabled,
                               boolean fileListingAndReadingEnabled,
                               boolean fileMutatingEnabled,
                               boolean scriptRunEnabled,
                               boolean addReasoningNotesEnabled) {
        java.util.Objects.requireNonNull(policy, "policy must not be null");
        return buildPolicyAware(
                policy,
                scriptRunnerEnabled,
                fileListingAndReadingEnabled,
                fileMutatingEnabled,
                scriptRunEnabled,
                addReasoningNotesEnabled);
    }

    private static String buildPolicyAware(FilePolicy policy,
                                           boolean scriptRunnerEnabled,
                                           boolean fileListingAndReadingEnabled,
                                           boolean fileMutatingEnabled,
                                           boolean scriptRunEnabled,
                                           boolean addReasoningNotesEnabled) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Tool Operations Reference\n");
        sb.append("Use this guide to construct your tool requests.\n\n");
        sb.append("### YAML String Safety\n");
        sb.append("- Quote free-form text scalar fields when YAML could misread them\n");
        sb.append("- Use double quotes for `intent`, `note`, and any other text value containing `:`, `#`, `[`, `]`, `{`, `}`, `,`, or leading/trailing spaces\n");
        sb.append("- Prefer quoted strings for short explanatory text instead of unquoted plain scalars\n");
        sb.append("- CRITICAL: Do NOT use or append any closing characters, closing quotes, or closing symbols (such as triple quotes `\"\"\"` or `'''`) at the end of any YAML block scalar, or at the end of the `content` field for `write_file` or `patch_file` operations.\n\n");
        Set<FilePolicy.Base> listBases = policy.getEnabledBasesForOperation(ToolExecutionType.LIST_FILES);
        Set<FilePolicy.Base> readBases = policy.getEnabledBasesForOperation(ToolExecutionType.READ_FILE);
        Set<FilePolicy.Base> writeBases = policy.getEnabledBasesForOperation(ToolExecutionType.WRITE_FILE);
        Set<FilePolicy.Base> patchBases = policy.getEnabledBasesForOperation(ToolExecutionType.PATCH_FILE);
        Set<FilePolicy.Base> deleteBases = policy.getEnabledBasesForOperation(ToolExecutionType.DELETE_FILE);
        Set<FilePolicy.Base> listGenBases = policy.getEnabledBasesForOperation(ToolExecutionType.LIST_COMPILED_FILES);
        appendToolPermissionsSection(sb,
                fileListingAndReadingEnabled, fileMutatingEnabled,
                scriptRunnerEnabled, scriptRunEnabled,
                addReasoningNotesEnabled,
                listBases, readBases, writeBases, patchBases, deleteBases, listGenBases);
        sb.append("### Operation Details\n");

        if (fileListingAndReadingEnabled && !listBases.isEmpty()) {
            sb.append("- **list_files**: List files in a directory\n");
            sb.append("  - required: base, path\n");
            sb.append("  - optional: recursive (boolean), intent (describe why you need this operation)\n");
            sb.append("  - behavior: recursive=false lists only direct children; recursive=true lists the full tree under the path\n");
            sb.append("  - available on bases: ").append(basesDescription(listBases)).append("\n");
            sb.append("  - example (output structure inspection): operation: list_files | base: target | path: com/example | recursive: true | intent: inspect existing compiled class structure\n\n");
        }

        if (fileListingAndReadingEnabled && !readBases.isEmpty()) {
            sb.append("- **read_file**: Read file content\n");
            sb.append("  - required: base, path\n");
            sb.append("  - optional: intent (describe why you need to read this file)\n");
            sb.append("  - available on bases: ").append(basesDescription(readBases)).append("\n");
            sb.append("  - example: operation: read_file | base: main | path: domain/entities.md | intent: understand the entity model to compile correct services\n\n");
        }

        
        if (fileMutatingEnabled && !writeBases.isEmpty()) {
            sb.append("- **write_file**: Create or overwrite a file\n");
            sb.append("  - required: base, path, content\n");
            sb.append("  - available on bases: ").append(basesDescription(writeBases)).append(" (target is always available)\n");
            sb.append("  - policy: write operations require destination to stay under the configured base root\n");
            sb.append("  - optional: intent (describe why you are creating/overwriting this file)\n");
            sb.append("  - YAML block scalar safety for content: If you use block scalar syntax (`content: |`), you MUST place the `intent:` key (and any other optional keys) BEFORE the `content:` key in the YAML mapping. Placing keys after a block scalar is PROHIBITED because YAML will parse them as part of the file content string. Every line of the file content must be indented by at least 2 spaces relative to the outer mapping (e.g. `content: |` followed by lines starting with at least 2 spaces of indentation). CRITICAL: Do NOT use or append any closing characters, closing quotes, or closing symbols (such as triple quotes `\"\"\"` or `'''`) at the end of the block scalar content or at the end of the `content` field.\n");
            sb.append("  - example: operation: write_file | base: target | path: compiled/NewType.java | intent: \"compile new entity class based on requirements\" | content: (file text)\n\n");
        }

        if (fileMutatingEnabled && !patchBases.isEmpty()) {
            sb.append("- **patch_file**: Apply targeted changes to an existing file\n");
            sb.append("  - required: base, path, atLine, content\n");
            sb.append("  - optional: replacing (default 0), intent\n");
            sb.append("  - available on bases: ").append(basesDescription(patchBases)).append("\n");
            sb.append("  - top-level fields:\n");
            sb.append("    - atLine: integer (1-based); the line at which insertion or replacement begins\n");
            sb.append("    - replacing: integer (0 or omitted = pure insert; N > 0 = remove N existing lines starting at atLine, then insert content)\n");
            sb.append("    - content: string (text to insert; may span multiple lines)\n");
            sb.append("  - validation:\n");
            sb.append("    - atLine must be >= 1 (1-based; first line of the file is 1)\n");
            sb.append("    - replacing must be >= 0 (omitting it defaults to 0)\n");
            sb.append("    - atLine, replacing, content are top-level fields — do NOT nest them under a 'changes' key\n");
            sb.append("    - for patch_file content, ALWAYS use a quoted string with escaped newlines (\\n); never use YAML block scalar syntax (content: |). CRITICAL: Do NOT use or append any closing characters, closing quotes, or closing symbols (such as triple quotes `\"\"\"` or `'''`) at the end of the content string (only use the normal closing double quote to close the YAML string scalar value).\n");
            sb.append("    - the previous coordinate-based patch contract is no longer supported; any request using the old fields is rejected\n");
            sb.append("  - example insert (no lines removed): operation: patch_file | base: target | path: domain/Task.java | atLine: 12 | content: \"@Deprecated\\n\" | intent: add deprecation annotation\n\n");
        }

        if (fileMutatingEnabled && !deleteBases.isEmpty()) {
            sb.append("- **delete_file**: Remove a file\n");
            sb.append("  - required: base, path\n");
            sb.append("  - optional: intent (describe why you are removing this file)\n");
            sb.append("  - available on bases: ").append(basesDescription(deleteBases)).append("\n");
            sb.append("  - example: operation: delete_file | base: test | path: obsolete/OldTest.java | intent: remove deprecated test class\n\n");
        }

        if (fileListingAndReadingEnabled && !listGenBases.isEmpty()) {
            sb.append("- **list_compiled_files**: purpose: list files compiled from one source markdown file\n");
            sb.append("  - parameter: provide source file via base + path\n");
            sb.append("  - available on bases: ").append(basesDescription(listGenBases)).append("\n");
            sb.append("  - behavior: returned files are restricted to paths under the configured target base root\n");
            sb.append("  - behavior: associated files outside target base scope are excluded and reported as exclusions\n");
            sb.append("  - optional: intent (describe why you need to list compiled files)\n");
            sb.append("  - example: operation: list_compiled_files | base: main | path: domain/entities.md | intent: check all files compiled by entity model\n\n");
        }

        if (scriptRunnerEnabled && scriptRunEnabled) {
            appendRunScriptSection(sb);
        }

        appendReasoningNoteSection(sb, addReasoningNotesEnabled);
        appendErrorHandlingAndGuidance(sb, policy);
        return sb.toString();
    }

    private static void appendReasoningNoteSection(StringBuilder sb, boolean addReasoningNotesEnabled) {
        if (addReasoningNotesEnabled) {
            sb.append("- **add_reasoning_note**: Attach a corrective note to a source file's tracking record\n");
            sb.append("  - purpose: signal to the next compilation cycle that a specific source needs a targeted fix\n");
            sb.append("  - required: exactly one of `source` (canonical source path) or `compiled` (canonical compiled file path)\n");
            sb.append("  - compiled path format: MUST be canonical qualified path with base prefix, e.g. `target:com/example/Foo.java`\n");
            sb.append("  - if `compiled` is provided, the owning source is resolved automatically from tracking trackings\n");
            sb.append("  - required: `note` — non-blank corrective guidance text explaining what needs to change\n");
            sb.append("  - note format (YAML-safe): ALWAYS provide `note` as a quoted string\n");
            sb.append("    - use double quotes when note contains colons, commas, brackets, or symbols\n");
            sb.append("    - for multi-line guidance, keep it in one quoted string and escape newlines as `\\n`\n");
            sb.append("    - do NOT use unquoted plain text for note values\n");
            sb.append("  - optional: `intent` (describe why you are adding this note)\n");
            sb.append("  - intent format (YAML-safe): quote it when it contains punctuation or multiple clauses; double quotes are preferred\n");
            sb.append("  - behavior: note is de-duplicated by normalized text; duplicate notes are silently ignored\n");
            sb.append("  - behavior: source status is set to recompilation-required; the queue rewinds to process it next\n");
            sb.append("  - when to use: when fixing a compilation error requires recompiling files owned by ANOTHER source\n");
            sb.append("    that you cannot modify in this cycle — attach a corrective note to that source and continue\n");
            sb.append("  - example: operation: add_reasoning_note | compiled: target:com/example/UserRepository.java | note: \"Add import for com.example.dto.UserDto, referenced by UserService\" | intent: fix compilation dependency\n\n");
            sb.append("- **clear_inference_notes**: Remove all notes from a source file's tracking record\n");
            sb.append("  - required: `source` (canonical source path)\n");
            sb.append("  - optional: `intent` (describe why you are clearing notes)\n");
            sb.append("  - behavior: clears all notes on the record; safe to call even if no notes are present\n");
            sb.append("  - when to use: when notes are no longer applicable after a successful fix\n");
            sb.append("  - example: operation: clear_inference_notes | source: src/main/nl/domain/User.md | intent: notes resolved after successful recompilation\n\n");
        } else {
            sb.append("- **fallback when add_reasoning_note is unavailable**: finish the pipeline with a `finish-error` user message\n");
            sb.append("  - behavior: surface all pending notes in the final user-facing message\n");
            sb.append("  - grouping: use canonical recipient keys in the form `base:path`\n");
            sb.append("  - ordering: preserve deterministic grouping and note order\n\n");
        }
    }

    private static void appendRunScriptSection(StringBuilder sb) {
        sb.append("- **run_script**: Execute an enabled script and return stdout, stderr, and exit code\n");
        sb.append("  - required: script\n");
        sb.append("  - optional: args (array of string/number/boolean), intent (describe why you need this script)\n");
        sb.append("  - script format: path-style identifier resolved under configured script root\n");
        sb.append("  - args format: YAML list, e.g. args: [\"com/example/Foo.java\", true, 3]\n");
        sb.append("  - behavior: base and path fields are non-operative for run_script and are ignored\n");
        sb.append("  - behavior: script must resolve as a canonical path under configured script root, execute as an executable file, and run with project root as working directory\n");
        sb.append("  - behavior: stdout, stderr, and exit code are always returned as the operation result\n");
        sb.append("  - behavior: script failures do not terminate compilation; they are returned for analysis\n\n");
    }

    private static String basesDescription(Set<FilePolicy.Base> bases) {
        return new TreeSet<>(bases).stream()
                .map(b -> b.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }

    private static void appendToolPermissionsSection(
            StringBuilder sb,
            boolean fileListingAndReadingEnabled,
            boolean fileMutatingEnabled,
            boolean scriptRunnerEnabled,
            boolean scriptRunEnabled,
            boolean addReasoningNotesEnabled,
            Set<FilePolicy.Base> listBases,
            Set<FilePolicy.Base> readBases,
            Set<FilePolicy.Base> writeBases,
            Set<FilePolicy.Base> patchBases,
            Set<FilePolicy.Base> deleteBases,
            Set<FilePolicy.Base> listGenBases) {
        sb.append("### Tool Permissions\n");
        if (fileListingAndReadingEnabled && !listBases.isEmpty())
            sb.append("- list_files: ").append(basesDescription(listBases)).append("\n");
        if (fileListingAndReadingEnabled && !readBases.isEmpty())
            sb.append("- read_file: ").append(basesDescription(readBases)).append("\n");
        if (fileMutatingEnabled && !writeBases.isEmpty())
            sb.append("- write_file: ").append(basesDescription(writeBases)).append("\n");
        if (fileMutatingEnabled && !patchBases.isEmpty())
            sb.append("- patch_file: ").append(basesDescription(patchBases)).append("\n");
        if (fileMutatingEnabled && !deleteBases.isEmpty())
            sb.append("- delete_file: ").append(basesDescription(deleteBases)).append("\n");
        if (fileListingAndReadingEnabled && !listGenBases.isEmpty())
            sb.append("- list_compiled_files: ").append(basesDescription(listGenBases)).append("\n");
        if (scriptRunnerEnabled && scriptRunEnabled)
            sb.append("- run_script: always available\n");
        if (addReasoningNotesEnabled) {
            sb.append("- add_reasoning_note: always available\n");
            sb.append("- clear_inference_notes: always available\n");
        }
        sb.append("\n");
    }

    private static void appendErrorHandlingAndGuidance(StringBuilder sb, FilePolicy policy) {
        boolean readAllowed = policy == null
            || !policy.getEnabledBasesForOperation(ToolExecutionType.READ_FILE).isEmpty();
        boolean listAllowed = policy == null
            || !policy.getEnabledBasesForOperation(ToolExecutionType.LIST_FILES).isEmpty();
        boolean listCompiledAllowed = policy == null
            || !policy.getEnabledBasesForOperation(ToolExecutionType.LIST_COMPILED_FILES).isEmpty();

        sb.append("### Error Handling During Tool Operations\n");
        sb.append("If a tool operation fails (e.g., file not found, invalid path, permission error):\n");
        sb.append("- You WILL NOT be terminated or forced to finish\n");
        sb.append("- The system will send you a detailed error message describing the failure\n");
        sb.append("- You can analyze the error and request a different operation or alternative approach\n");
        sb.append("- Continue iterating: read a different file, try an alternate path, or adjust your strategy\n");
        sb.append("- Errors are learning opportunities to understand the actual file structure\n\n");
        sb.append("### Cross-Source Correction Handoff\n");
        sb.append("If you encounter a compilation error that requires changing files compiled by a DIFFERENT source:\n");
        sb.append("- You can use `add_reasoning_note` targeting that source (by its compiled file or source path) with a clear corrective note\n");
        sb.append("- IMPORTANT ownership rule: do NOT add notes to the main source currently being processed\n");
        sb.append("- IMPORTANT ownership rule: do NOT add notes to any file compiled by the main source currently being processed\n");
        sb.append("- If the issue is in the current source or in files compiled by the current source, fix it in the current pipeline\n");
        sb.append("- Add notes only for other sources (or files compiled by other sources) when cross-source guidance is helpful\n");
        sb.append("- Include all necessary context in the note: what import/declaration/change is needed and why\n");
        sb.append("- Notes survive between cycles; the recompilation of the noted source will receive your note as guidance\n\n");
        sb.append("### Additional Guidance\n");
        sb.append("Referenced markdown guidance:\n");
        sb.append("- Strict scope: stay within the root markdown, its transitive referenced markdown graph, and compiled files linked to that graph\n");
        sb.append("- Never inspect unrelated markdown files that are outside the reference trees provided at the start of this session\n");
        sb.append("- Never use list_files to explore or discover markdown files: only read markdown files already listed in the provided reference trees\n");
        sb.append("- Traverse references recursively when needed to fully understand dependencies\n");
        sb.append("- Respect target-base write policy: use target base for write/patch/delete operations and keep all destinations under configured target root\n");
        sb.append("- Existing plugin base parameters (target/main/test) remain unchanged; choose target for mutations and main/test for markdown inspection\n");
        sb.append("- Immutability rule: do NOT modify referenced markdown files or files compiled from referenced markdown\n");
        sb.append("  - Always check file content before creating patches to ensure line numbers are accurate\n");
        sb.append("  - Use the optional `intent` field to describe your reasoning for each tool operation\n");
        sb.append("- When `intent` or another free-form text field contains punctuation, especially `:`, wrap it in double quotes\n");
        sb.append("- You can include multiple tool requests in one message; they will be executed in order and returned together\n");
        sb.append("- When including multiple tool requests, encode them as one YAML list and never separate them with ---\n\n");

        if (listCompiledAllowed) {
            sb.append("- Use list_compiled_files for each relevant markdown node in the reference graph before patching affected compiled files\n");
        }
        if (listAllowed) {
            sb.append("- Before implementing code for a path, inspect its structure with list_files using recursive: true\n");
        }
        if (readAllowed || listCompiledAllowed) {
            sb.append("- Inspect referenced markdown files and their compiled outputs before patching\n");
        }
        if (!readAllowed && !listAllowed && !listCompiledAllowed) {
            sb.append("- Tool read/list inspection is not available in this session; rely on attached files and provided reference trees for analysis\n");
            sb.append("- Do not emit read/list tool operations that are not advertised as available\n");
        }
        sb.append("\n");
    }


}
