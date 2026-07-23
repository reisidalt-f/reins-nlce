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

package br.com.dizeno.reins.reasoning.tooling;

import br.com.dizeno.reins.source.domain.FileReference;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ToolExecutionRequest is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
 * Acts as a component managing tool execution request.
 */
public class ToolExecutionRequest {
    /**
     * Operation is part of the tool execution environments (like MCP tools and local file tools) exposed to LLMs in the reins architecture.
     * Acts as a component managing operation.
     */
    public enum Operation {
        LIST_FILES,
        READ_FILE,
        WRITE_FILE,
        PATCH_FILE,
        DELETE_FILE,
        LIST_COMPILED_FILES,
        RUN_SCRIPT,
        ADD_INFERENCE_NOTE,
        CLEAR_INFERENCE_NOTES
    }

    private Operation operation;
    private String base;
    private String path;
    private String script;
    private String intent;
    private boolean recursive;
    private String content;
    private Integer atLine;
    private Integer replacing;
    private boolean createParents = true;
    private List<String> args = List.of();
     
    private String source;
     
    private String compiled;
     
    private String note;

    /**
     * From Yaml.
     *
     * @param yamlBody the yaml body
     * @return the resolved or constructed object
     */
    public static ToolExecutionRequest fromYaml(String yamlBody) {
        List<ToolExecutionRequest> parsed = fromYamlAny(yamlBody);
        if (parsed.size() != 1) {
            throw new IllegalArgumentException("tool request must contain exactly one operation.");
        }
        return parsed.get(0);
    }

    private static final Pattern BLOCK_SCALAR_START = Pattern.compile(
            "^([ \\t]*)([a-zA-Z0-9_-]+):[ \\t]*([|>]\\d*[+-]?|\\d*[+-]?[|>])[ \\t]*$"
    );

    private static final Pattern BLOCK_SCALAR_TERMINATOR = Pattern.compile(
            "^[ \\t]*(?:-?[ \\t]*(?:operation|base|path|script|intent|recursive|content|atLine|replacing|createParents|args|source|compiled|note):|---|```|\"\"\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROOT_FENCE = Pattern.compile(
            "^[ \\t]*(?:```|\"\"\")[ \\t]*$"
    );

    private static int getLeadingWhitespaceLength(String line) {
        int count = 0;
        while (count < line.length() && (line.charAt(count) == ' ' || line.charAt(count) == '\t')) {
            count++;
        }
        return count;
    }

    /**
     * Preprocess Yaml Block Scalars.
     *
     * @param yamlBody the yaml body
     * @return the string result
     */
    public static String preprocessYamlBlockScalars(String yamlBody) {
        if (yamlBody == null) {
            return null;
        }
        String[] lines = yamlBody.split("\\r?\\n", -1);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            if (ROOT_FENCE.matcher(line).matches()) {
                i++;
                continue;
            }
            Matcher m = BLOCK_SCALAR_START.matcher(line);
            if (m.matches()) {
                sb.append(line);
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
                int keyIndent = m.group(1).length();
                int targetIndent = keyIndent + 2;
                List<String> blockLines = new ArrayList<>();
                i++;
                while (i < lines.length) {
                    String blockLine = lines[i];
                    int lineIndent = getLeadingWhitespaceLength(blockLine);
                    boolean hasContent = !blockLine.trim().isEmpty();
                    if (hasContent && lineIndent <= keyIndent && BLOCK_SCALAR_TERMINATOR.matcher(blockLine).find()) {
                        break;
                    }
                    blockLines.add(blockLine);
                    i++;
                }

                
                int start = -1;
                int end = -1;
                for (int j = 0; j < blockLines.size(); j++) {
                    String bl = blockLines.get(j);
                    if (!bl.trim().isEmpty()) {
                        int lineIndent = getLeadingWhitespaceLength(bl);
                        if (lineIndent < targetIndent) {
                            if (start == -1) {
                                start = j;
                            }
                            end = j;
                        }
                    }
                }

                if (start != -1) {
                    
                    int minIndent = Integer.MAX_VALUE;
                    for (int j = start; j <= end; j++) {
                        String bl = blockLines.get(j);
                        if (!bl.trim().isEmpty()) {
                            minIndent = Math.min(minIndent, getLeadingWhitespaceLength(bl));
                        }
                    }
                    if (minIndent != Integer.MAX_VALUE && minIndent < targetIndent) {
                        int shift = targetIndent - minIndent;
                        String padding = " ".repeat(shift);
                        for (int j = start; j <= end; j++) {
                            String bl = blockLines.get(j);
                            if (!bl.trim().isEmpty()) {
                                blockLines.set(j, padding + bl);
                            }
                        }
                    }
                }

                
                for (int j = 0; j < blockLines.size(); j++) {
                    sb.append(blockLines.get(j));
                    if (i < lines.length || j < blockLines.size() - 1) {
                        sb.append("\n");
                    }
                }
            } else {
                sb.append(line);
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
                i++;
            }
        }
        String result = sb.toString();
        if (!result.endsWith("\n")) {
            result += "\n";
        }
        return result;
    }

    /**
     * From Yaml Any.
     *
     * @param yamlBody the yaml body
     * @return the collection of elements
     */
    public static List<ToolExecutionRequest> fromYamlAny(String yamlBody) {
        String preprocessed = preprocessYamlBlockScalars(yamlBody);
        List<ToolExecutionRequest> requests = new ArrayList<>();
        try {
            for (Object loaded : new Yaml().loadAll(preprocessed)) {
                if (loaded instanceof Map<?, ?> map) {
                    requests.add(fromMap(map));
                } else if (loaded instanceof List<?> list) {
                    for (Object item : list) {
                        if (!(item instanceof Map<?, ?> mapItem)) {
                            throw new IllegalArgumentException("tool request list entries must be YAML objects.");
                        }
                        requests.add(fromMap(mapItem));
                    }
                } else if (loaded != null) {
                    throw new IllegalArgumentException("tool request must be a YAML object or list of objects.");
                }
            }
        } catch (YAMLException ex) {
            throw new IllegalArgumentException(
                    "Invalid tool YAML payload. Ensure string values are properly quoted (especially when containing ':'), use escaped newlines (\\n) inside quoted strings when needed, and rely on default YAML indentation logic for block scalars (content: |) where all lines are indented relative to the outer mapping (no unindented lines like closing braces `}` at column 1, and no closing quotes or symbols at the end). "
                            + ex.getMessage(),
                    ex);
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("tool request must be a YAML object or list of objects.");
        }
        return requests;
    }

    private static ToolExecutionRequest fromMap(Map<?, ?> map) {

        ToolExecutionRequest request = new ToolExecutionRequest();
        request.setOperation(parseOperation(stringValue(map.get("operation"))));
        request.setBase(stringValue(map.get("base")));
        request.setPath(stringValue(map.get("path")));
        request.setScript(stringValue(map.get("script")));
        request.setIntent(stringValue(map.get("intent")));
        request.setRecursive(booleanValue(map.get("recursive"), false));
        request.setContent(stringValue(map.get("content")));
        
        for (String deprecated : new String[]{"mode", "startLine", "startColumn", "endLine", "endColumn"}) {
            if (map.containsKey(deprecated)) {
                throw new IllegalArgumentException(
                    "patch_file parameter '" + deprecated + "' is no longer supported. "
                    + "Use atLine (1-based line number) and replacing (number of lines to remove, default 0) instead.");
            }
        }
        request.setAtLine(intValue(map.get("atLine")));
        request.setReplacing(intValue(map.get("replacing")));
        request.setCreateParents(booleanValue(map.get("createParents"), true));
        request.setArgs(argsValue(map.get("args"), request.getOperation()));
        request.setSource(stringValue(map.get("source")));
        request.setCompiled(stringValue(map.get("compiled")));
        request.setNote(stringValue(map.get("note")));

        validate(request);
        return request;
    }

    private static boolean isNoteOperation(Operation op) {
        return op == Operation.ADD_INFERENCE_NOTE || op == Operation.CLEAR_INFERENCE_NOTES;
    }

    private static boolean isQualifiedPath(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int colon = value.indexOf(':');
        return colon > 0 && colon < value.length() - 1;
    }

    private static void validate(ToolExecutionRequest request) {
        if (request.getOperation() == null) {
            throw new IllegalArgumentException("tool request operation is required.");
        }
        
        if (isNoteOperation(request.getOperation())) {
            switch (request.getOperation()) {
                case ADD_INFERENCE_NOTE -> {
                    boolean hasSource = request.getSource() != null && !request.getSource().isBlank();
                    boolean hasCompiled = request.getCompiled() != null && !request.getCompiled().isBlank();
                    if (hasSource == hasCompiled) {
                        throw new IllegalArgumentException(
                                "add_reasoning_note requires exactly one of 'source' or 'compiled'");
                    }
                    if (hasCompiled && !isQualifiedPath(request.getCompiled())) {
                        throw new IllegalArgumentException(
                                "add_reasoning_note 'compiled' must be a canonical qualified path (<base>:<path>), for example: target:com/example/MyFile.java");
                    }
                    if (request.getNote() == null || request.getNote().isBlank()) {
                        throw new IllegalArgumentException("add_reasoning_note requires non-blank 'note'");
                    }
                }
                case CLEAR_INFERENCE_NOTES -> {
                    if (request.getSource() == null || request.getSource().isBlank()) {
                        throw new IllegalArgumentException("clear_inference_notes requires 'source'");
                    }
                }
                default -> {   }
            }
            return;
        }
        if (request.getOperation() == Operation.RUN_SCRIPT) {
            validateRunScript(request);
            return;
        }
        if (request.getBase() == null || request.getBase().isBlank()) {
            throw new IllegalArgumentException("tool request base is required.");
        }
        try {
            FileReference.of(request.getBase(), request.getPath());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid tool request base/path: " + ex.getMessage(), ex);
        }
        
        
        if (request.getPath() == null) {
            throw new IllegalArgumentException("tool request path is required.");
        }
        boolean isMutationOp = request.getOperation() == Operation.WRITE_FILE
                || request.getOperation() == Operation.PATCH_FILE
                || request.getOperation() == Operation.DELETE_FILE;
        if (isMutationOp && request.getPath().isBlank()) {
            throw new IllegalArgumentException("tool request path is required for mutation operations.");
        }

        switch (request.getOperation()) {
            case WRITE_FILE -> {
                if (request.getContent() == null) {
                    throw new IllegalArgumentException("write_file requires content.");
                }
            }
            case PATCH_FILE -> {
                if (request.getAtLine() == null) {
                    throw new IllegalArgumentException("patch_file requires atLine.");
                }
                if (request.getAtLine() < 1) {
                    throw new IllegalArgumentException("patch_file atLine must be >= 1 (1-based).");
                }
                if (request.getReplacing() != null && request.getReplacing() < 0) {
                    throw new IllegalArgumentException("patch_file replacing must be >= 0.");
                }
                if (request.getContent() == null) {
                    throw new IllegalArgumentException("patch_file requires content.");
                }
            }
            default -> {
            }
        }
    }

    private static Operation parseOperation(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "list_files" -> Operation.LIST_FILES;
            case "read_file" -> Operation.READ_FILE;
            case "write_file" -> Operation.WRITE_FILE;
            case "patch_file" -> Operation.PATCH_FILE;
            case "delete_file" -> Operation.DELETE_FILE;
            case "list_compiled_files" -> Operation.LIST_COMPILED_FILES;
            case "run_script" -> Operation.RUN_SCRIPT;
            case "add_reasoning_note" -> Operation.ADD_INFERENCE_NOTE;
            case "clear_inference_notes" -> Operation.CLEAR_INFERENCE_NOTES;
            default -> throw new IllegalArgumentException("Unsupported tool operation: " + value);
        };
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static List<String> stringListValue(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item == null) {
                    out.add(null);
                } else {
                    out.add(String.valueOf(item));
                }
            }
            return Collections.unmodifiableList(out);
        }
        return List.of(String.valueOf(value));
    }

    private static List<String> argsValue(Object value, Operation operation) {
        if (operation != Operation.RUN_SCRIPT) {
            return stringListValue(value);
        }
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("run_script args must be an array.");
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String || item instanceof Number || item instanceof Boolean) {
                out.add(String.valueOf(item));
            } else {
                throw new IllegalArgumentException("run_script args entries must be string, number, or boolean.");
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static void validateRunScript(ToolExecutionRequest request) {
        if (request.getScript() == null || request.getScript().isBlank()) {
            throw new IllegalArgumentException("run_script requires script.");
        }
        for (String arg : request.getArgs()) {
            if (arg == null) {
                throw new IllegalArgumentException("run_script args entries must be string, number, or boolean.");
            }
        }
    }

    /**
     * Gets the operation.
     *
     * @return the resolved or constructed object
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Sets the operation.
     *
     * @param operation the operation
     */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /**
     * Gets the base.
     *
     * @return the string result
     */
    public String getBase() {
        return base;
    }

    /**
     * Sets the base.
     *
     * @param base the base
     */
    public void setBase(String base) {
        this.base = base;
    }

    /**
     * Gets the path.
     *
     * @return the string result
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the file or directory path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the script.
     *
     * @return the string result
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the script.
     *
     * @param script the script
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Gets the intent.
     *
     * @return the string result
     */
    public String getIntent() {
        return intent;
    }

    /**
     * Sets the intent.
     *
     * @param intent the reasoning intent
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    /**
     * Checks if the component is recursive.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Sets the recursive.
     *
     * @param recursive the recursive
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Gets the content.
     *
     * @return the string result
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the at line.
     *
     * @return the numeric value
     */
    public Integer getAtLine() {
        return atLine;
    }

    /**
     * Sets the at line.
     *
     * @param atLine the at line
     */
    public void setAtLine(Integer atLine) {
        this.atLine = atLine;
    }

    /**
     * Gets the replacing.
     *
     * @return the numeric value
     */
    public Integer getReplacing() {
        return replacing;
    }

    /**
     * Sets the replacing.
     *
     * @param replacing the replacing
     */
    public void setReplacing(Integer replacing) {
        this.replacing = replacing;
    }

    /**
     * Checks if the component is create parents.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isCreateParents() {
        return createParents;
    }

    /**
     * Sets the create parents.
     *
     * @param createParents the create parents
     */
    public void setCreateParents(boolean createParents) {
        this.createParents = createParents;
    }

    /**
     * Gets the args.
     *
     * @return the string result
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Sets the args.
     *
     * @param args the args
     */
    public void setArgs(List<String> args) {
        this.args = args == null ? List.of() : List.copyOf(args);
    }

    /**
     * Gets the source.
     *
     * @return the string result
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the source.
     *
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the compiled.
     *
     * @return the string result
     */
    public String getCompiled() {
        return compiled;
    }

    /**
     * Sets the compiled.
     *
     * @param compiled the compiled
     */
    public void setCompiled(String compiled) {
        this.compiled = compiled;
    }

    /**
     * Gets the note.
     *
     * @return the string result
     */
    public String getNote() {
        return note;
    }

    /**
     * Sets the note.
     *
     * @param note the note
     */
    public void setNote(String note) {
        this.note = note;
    }
}
