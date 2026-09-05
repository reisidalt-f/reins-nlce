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
        sb.append("Use this guide to construct your tool requests using plain-text positional commands and `--reins-boundary` multipart formatting.\n\n");
        sb.append("### Multipart & Positional Command Format\n");
        sb.append("- Every tool request MUST be formatted as a multipart body delimited by `--reins-boundary` and terminating with `--reins-boundary--`.\n");
        sb.append("- Line 1 of each part: Positional command without field labels: `<OPERATION> <BASE> <PATH> [ARGS...]`.\n");
        sb.append("- Optional payload lines: Raw content for write_file/append_file/prepend_file/patch_file or note text for add_reasoning_note follow after the command line.\n");
        sb.append("- For binary files in write_file, append_file, or prepend_file, append `base64` to the command line and provide the Base64 encoded payload.\n\n");

        Set<String> listBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.LIST_FILES);
        Set<String> readBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.READ_FILE);
        Set<String> writeBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.WRITE_FILE);
        Set<String> patchBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.PATCH_FILE);
        Set<String> deleteBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.DELETE_FILE);
        Set<String> appendBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.APPEND_FILE);
        Set<String> prependBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.PREPEND_FILE);
        Set<String> moveBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.MOVE_FILE);
        Set<String> copyBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.COPY_FILE);
        Set<String> listGenBases = policy.getEnabledBaseNamesForOperation(ToolExecutionType.LIST_COMPILED_FILES);

        appendToolPermissionsSection(sb,
                fileListingAndReadingEnabled, fileMutatingEnabled,
                scriptRunnerEnabled, scriptRunEnabled,
                addReasoningNotesEnabled,
                listBases, readBases, writeBases, patchBases, deleteBases, appendBases, prependBases, moveBases, copyBases, listGenBases);

        sb.append("### Operation Details\n");

        if (fileListingAndReadingEnabled && !listBases.isEmpty()) {
            sb.append("- **list_files**: List files in a directory\n");
            sb.append("  - syntax: `LIST_FILES <base> <path> [true|false] [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(listBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nLIST_FILES target output/components true list component files\\n--reins-boundary--`\n\n");
        }

        if (fileListingAndReadingEnabled && !readBases.isEmpty()) {
            sb.append("- **read_file**: Read file content\n");
            sb.append("  - syntax: `READ_FILE <base> <path> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(readBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nREAD_FILE main domain/entities.md inspect domain model entities\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !writeBases.isEmpty()) {
            sb.append("- **write_file**: Create or overwrite a file\n");
            sb.append("  - syntax: `WRITE_FILE <base> <path> [base64] [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(writeBases)).append(" (target is always available)\n");
            sb.append("  - text example: `--reins-boundary\\nWRITE_FILE target output/Component.java create component class\\n\\npackage output;\\npublic class Component {}\\n--reins-boundary--`\n");
            sb.append("  - binary example: `--reins-boundary\\nWRITE_FILE target assets/logo.png base64 upload logo image\\n\\niVBORw0KGgoAAA...\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !appendBases.isEmpty()) {
            sb.append("- **append_file**: Append content to an existing or new file\n");
            sb.append("  - syntax: `APPEND_FILE <base> <path> [base64] [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(appendBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nAPPEND_FILE target output/Log.txt append log line\\n\\nNew log entry\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !prependBases.isEmpty()) {
            sb.append("- **prepend_file**: Prepend content to an existing or new file\n");
            sb.append("  - syntax: `PREPEND_FILE <base> <path> [base64] [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(prependBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nPREPEND_FILE target output/Header.txt prepend header line\\n\\nHeader text\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !moveBases.isEmpty()) {
            sb.append("- **move_file**: Move or rename a file within target base\n");
            sb.append("  - syntax: `MOVE_FILE <base> <path> <destination> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(moveBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nMOVE_FILE target old/path.txt new/path.txt move file\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !copyBases.isEmpty()) {
            sb.append("- **copy_file**: Copy a file from any origin base to target base\n");
            sb.append("  - syntax: `COPY_FILE <base> <path> <destination> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(copyBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nCOPY_FILE main templates/Header.java output/Header.java copy header template\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !patchBases.isEmpty()) {
            sb.append("- **patch_file**: Apply targeted changes using standard Unified Diff format\n");
            sb.append("  - syntax: `PATCH_FILE <base> <path> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(patchBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nPATCH_FILE target output/TargetFile.java apply updates\\n\\n@@ -10,3 +10,4 @@\\n context\\n-old\\n+new\\n--reins-boundary--`\n\n");
        }

        if (fileMutatingEnabled && !deleteBases.isEmpty()) {
            sb.append("- **delete_file**: Remove a file\n");
            sb.append("  - syntax: `DELETE_FILE <base> <path> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(deleteBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nDELETE_FILE test obsolete/OldFile.ext remove obsolete file\\n--reins-boundary--`\n\n");
        }

        if (fileListingAndReadingEnabled && !listGenBases.isEmpty()) {
            sb.append("- **list_compiled_files**: List files compiled from source markdown\n");
            sb.append("  - syntax: `LIST_COMPILED_FILES <base> <path> [<intent>]`\n");
            sb.append("  - available on bases: ").append(baseNamesDescription(listGenBases)).append("\n");
            sb.append("  - example: `--reins-boundary\\nLIST_COMPILED_FILES main domain/entities.md list compiled files for domain\\n--reins-boundary--`\n\n");
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
            sb.append("  - syntax: `ADD_REASONING_NOTE compiled <qualified_path> [<intent>]` or `ADD_REASONING_NOTE source <path> [<intent>]`\n");
            sb.append("  - example: `--reins-boundary\\nADD_REASONING_NOTE compiled target:path/to/TargetFile.ext add type note\\n\\nAdd missing definition for type\\n--reins-boundary--`\n\n");
            sb.append("- **list_reasoning_notes**: List reasoning notes for a specific source or all sources\n");
            sb.append("  - syntax: `LIST_REASONING_NOTES [<source>] [<intent>]`\n");
            sb.append("  - example: `--reins-boundary\\nLIST_REASONING_NOTES main:domain/User.md list notes\\n--reins-boundary--`\n\n");
            sb.append("- **clear_reasoning_notes**: Remove all notes from a source file's tracking record or all sources\n");
            sb.append("  - syntax: `CLEAR_REASONING_NOTES [<source>] [<intent>]`\n");
            sb.append("  - example: `--reins-boundary\\nCLEAR_REASONING_NOTES main:domain/User.md clear notes\\n--reins-boundary--`\n\n");
        } else {
            sb.append("- **fallback when add_reasoning_note is unavailable**: finish the pipeline with a `finish-error` user message\\n\\n");
        }
    }

    private static void appendRunScriptSection(StringBuilder sb) {
        sb.append("- **run_script**: Execute an enabled script\n");
        sb.append("  - syntax: `RUN_SCRIPT <script_name> [args...] [--intent <intent>]`\n");
        sb.append("  - note: Script arguments are passed directly to the shell script. To provide an intent without passing it as script arguments, append `--intent <intent_text>` at the end of the line.\n");
        sb.append("  - example: `--reins-boundary\\nRUN_SCRIPT run-build.sh path/to/file.ext --intent build target component\\n--reins-boundary--`\n\n");
    }

    private static String baseNamesDescription(Set<String> bases) {
        return new TreeSet<>(bases).stream()
                .collect(Collectors.joining(", "));
    }

    private static void appendToolPermissionsSection(
            StringBuilder sb,
            boolean fileListingAndReadingEnabled,
            boolean fileMutatingEnabled,
            boolean scriptRunnerEnabled,
            boolean scriptRunEnabled,
            boolean addReasoningNotesEnabled,
            Set<String> listBases,
            Set<String> readBases,
            Set<String> writeBases,
            Set<String> patchBases,
            Set<String> deleteBases,
            Set<String> appendBases,
            Set<String> prependBases,
            Set<String> moveBases,
            Set<String> copyBases,
            Set<String> listGenBases) {
        sb.append("### Tool Permissions\n");
        if (fileListingAndReadingEnabled && !listBases.isEmpty())
            sb.append("- list_files: ").append(baseNamesDescription(listBases)).append("\n");
        if (fileListingAndReadingEnabled && !readBases.isEmpty())
            sb.append("- read_file: ").append(baseNamesDescription(readBases)).append("\n");
        if (fileMutatingEnabled && !writeBases.isEmpty())
            sb.append("- write_file: ").append(baseNamesDescription(writeBases)).append("\n");
        if (fileMutatingEnabled && !appendBases.isEmpty())
            sb.append("- append_file: ").append(baseNamesDescription(appendBases)).append("\n");
        if (fileMutatingEnabled && !prependBases.isEmpty())
            sb.append("- prepend_file: ").append(baseNamesDescription(prependBases)).append("\n");
        if (fileMutatingEnabled && !moveBases.isEmpty())
            sb.append("- move_file: ").append(baseNamesDescription(moveBases)).append("\n");
        if (fileMutatingEnabled && !copyBases.isEmpty())
            sb.append("- copy_file: ").append(baseNamesDescription(copyBases)).append("\n");
        if (fileMutatingEnabled && !patchBases.isEmpty())
            sb.append("- patch_file: ").append(baseNamesDescription(patchBases)).append("\n");
        if (fileMutatingEnabled && !deleteBases.isEmpty())
            sb.append("- delete_file: ").append(baseNamesDescription(deleteBases)).append("\n");
        if (fileListingAndReadingEnabled && !listGenBases.isEmpty())
            sb.append("- list_compiled_files: ").append(baseNamesDescription(listGenBases)).append("\n");
        if (scriptRunnerEnabled && scriptRunEnabled)
            sb.append("- run_script: always available\n");
        if (addReasoningNotesEnabled) {
            sb.append("- add_reasoning_note: always available\n");
            sb.append("- clear_reasoning_notes: always available\n");
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

        sb.append("### Error Handling & Additional Guidance\n");
        sb.append("If a tool operation fails, the system returns a detailed error message without terminating.\n");
        sb.append("Stay within allowed scope and use `--reins-boundary` multipart syntax for all tool requests.\n\n");
    }
}
