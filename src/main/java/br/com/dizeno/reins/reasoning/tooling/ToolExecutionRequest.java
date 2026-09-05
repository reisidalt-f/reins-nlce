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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ToolExecutionRequest is part of the tool execution environments (like tools and local file tools) exposed to LLMs in the reins architecture.
 * Acts as a component managing tool execution request.
 */
public class ToolExecutionRequest {
    public enum Operation {
        LIST_FILES,
        READ_FILE,
        WRITE_FILE,
        PATCH_FILE,
        DELETE_FILE,
        APPEND_FILE,
        PREPEND_FILE,
        MOVE_FILE,
        COPY_FILE,
        LIST_COMPILED_FILES,
        RUN_SCRIPT,
        ADD_REASONING_NOTE,
        CLEAR_REASONING_NOTES,
        LIST_REASONING_NOTES
    }

    private Operation operation;
    private String base;
    private String path;
    private String destination;
    private String script;
    private String intent;
    private boolean recursive;
    private String content;
    private boolean base64;
    private boolean createParents = true;
    private List<String> args = List.of();
    private String source;
    private String compiled;
    private String note;

    public static ToolExecutionRequest fromText(String body) {
        List<ToolExecutionRequest> parsed = fromTextAny(body);
        if (parsed.size() != 1) {
            throw new IllegalArgumentException("tool request must contain exactly one operation.");
        }
        return parsed.get(0);
    }

    public static ToolExecutionRequest fromYaml(String body) {
        return fromText(body);
    }

    public static List<ToolExecutionRequest> fromYamlAny(String body) {
        return fromTextAny(body);
    }

    public static List<ToolExecutionRequest> fromTextAny(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("tool request body must not be null or blank.");
        }

        List<ToolExecutionRequest> requests = new ArrayList<>();

        if (body.contains("--reins-boundary")) {
            String[] parts = body.split("(?m)^--reins-boundary(?:--)?\\s*$");
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    ToolExecutionRequest req = fromPart(trimmedPart);
                    if (req != null) {
                        requests.add(req);
                    }
                }
            }
        } else {
            List<ToolExecutionRequest> legacyBlocks = fromConsecutiveOperationBlocks(body);
            if (!legacyBlocks.isEmpty()) {
                requests.addAll(legacyBlocks);
            } else {
                ToolExecutionRequest single = fromPart(body.trim());
                if (single != null) {
                    requests.add(single);
                }
            }
        }

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Invalid tool request payload. Could not parse any valid operation.");
        }
        return requests;
    }

    private static List<ToolExecutionRequest> fromConsecutiveOperationBlocks(String body) {
        Pattern OPERATION_START = Pattern.compile("(?m)^-?[ \\t]*operation:\\s*");
        Matcher matcher = OPERATION_START.matcher(body);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.size() <= 1) {
            return List.of();
        }

        List<ToolExecutionRequest> requests = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : body.length();
            String chunk = body.substring(start, end).trim();
            if (!chunk.isBlank()) {
                ToolExecutionRequest req = fromPart(chunk);
                if (req != null) {
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    public static ToolExecutionRequest fromPart(String partBody) {
        if (partBody == null || partBody.isBlank()) {
            return null;
        }

        String[] lines = partBody.split("\\r?\\n", -1);

        int cmdLineIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) {
                cmdLineIdx = i;
                break;
            }
        }
        if (cmdLineIdx == -1) {
            return null;
        }

        String cmdLine = lines[cmdLineIdx].trim();
        String lowerCmd = cmdLine.toLowerCase(Locale.ROOT);

        if (lowerCmd.startsWith("operation:") || lowerCmd.startsWith("- operation:") || lowerCmd.startsWith("-operation:")) {
            return fromKeyValueLines(lines, cmdLineIdx);
        }

        List<String> tokens = tokenizeCommandLine(cmdLine);
        if (tokens.isEmpty()) {
            return null;
        }

        ToolExecutionRequest req = new ToolExecutionRequest();
        Operation op = parseOperation(tokens.get(0));
        req.setOperation(op);

        boolean isBase64Header = false;
        int payloadStartIdx = cmdLineIdx + 1;

        while (payloadStartIdx < lines.length) {
            String line = lines[payloadStartIdx].trim();
            if (line.isEmpty()) {
                payloadStartIdx++;
                break;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("content-transfer-encoding:")
                    && line.toLowerCase(Locale.ROOT).contains("base64")) {
                isBase64Header = true;
                payloadStartIdx++;
            } else {
                break;
            }
        }

        switch (op) {
            case READ_FILE, LIST_COMPILED_FILES, DELETE_FILE -> {
                if (tokens.size() >= 2) req.setBase(tokens.get(1));
                if (tokens.size() >= 3) req.setPath(tokens.get(2));
                if (tokens.size() >= 4) req.setIntent(joinTokens(tokens, 3));
            }
            case LIST_FILES -> {
                if (tokens.size() >= 2) req.setBase(tokens.get(1));
                if (tokens.size() >= 3) req.setPath(tokens.get(2));
                int intentStart = 3;
                if (tokens.size() >= 4) {
                    String arg3 = tokens.get(3);
                    if ("true".equalsIgnoreCase(arg3) || "false".equalsIgnoreCase(arg3) || "recursive".equalsIgnoreCase(arg3)) {
                        req.setRecursive("true".equalsIgnoreCase(arg3) || "recursive".equalsIgnoreCase(arg3));
                        intentStart = 4;
                    }
                }
                if (tokens.size() > intentStart) req.setIntent(joinTokens(tokens, intentStart));
            }
            case WRITE_FILE, APPEND_FILE, PREPEND_FILE -> {
                if (tokens.size() >= 2) req.setBase(tokens.get(1));
                if (tokens.size() >= 3) req.setPath(tokens.get(2));
                int intentStart = 3;
                if (tokens.size() >= 4 && "base64".equalsIgnoreCase(tokens.get(3))) {
                    req.setBase64(true);
                    intentStart = 4;
                }
                if (isBase64Header) {
                    req.setBase64(true);
                }
                if (tokens.size() > intentStart) req.setIntent(joinTokens(tokens, intentStart));
                req.setContent(extractPayload(lines, payloadStartIdx));
            }
            case MOVE_FILE, COPY_FILE -> {
                if (tokens.size() >= 2) req.setBase(tokens.get(1));
                if (tokens.size() >= 3) req.setPath(tokens.get(2));
                if (tokens.size() >= 4) req.setDestination(tokens.get(3));
                if (tokens.size() >= 5) req.setIntent(joinTokens(tokens, 4));
            }
            case PATCH_FILE -> {
                if (tokens.size() >= 2) req.setBase(tokens.get(1));
                if (tokens.size() >= 3) req.setPath(tokens.get(2));
                if (tokens.size() >= 4) req.setIntent(joinTokens(tokens, 3));
                req.setContent(extractPayload(lines, payloadStartIdx));
            }
            case RUN_SCRIPT -> {
                if (tokens.size() >= 2) req.setScript(tokens.get(1));
                if (tokens.size() >= 3) {
                    int intentIdx = -1;
                    for (int i = 2; i < tokens.size(); i++) {
                        if ("--intent".equalsIgnoreCase(tokens.get(i))) {
                            intentIdx = i;
                            break;
                        }
                    }
                    if (intentIdx != -1) {
                        req.setArgs(tokens.subList(2, intentIdx));
                        if (intentIdx + 1 < tokens.size()) {
                            req.setIntent(joinTokens(tokens, intentIdx + 1));
                        }
                    } else {
                        req.setArgs(tokens.subList(2, tokens.size()));
                    }
                }
                if (req.getIntent() == null && payloadStartIdx < lines.length) {
                    String nextLine = lines[payloadStartIdx].trim();
                    if (nextLine.toLowerCase(Locale.ROOT).startsWith("intent:")) {
                        req.setIntent(nextLine.substring(7).trim());
                    }
                }
            }
            case ADD_REASONING_NOTE -> {
                if (tokens.size() >= 3) {
                    String targetType = tokens.get(1).toLowerCase(Locale.ROOT);
                    if ("compiled".equals(targetType)) {
                        req.setCompiled(tokens.get(2));
                    } else {
                        req.setSource(tokens.get(2));
                    }
                }
                if (tokens.size() >= 4) {
                    req.setIntent(joinTokens(tokens, 3));
                }
                String noteText = extractPayload(lines, payloadStartIdx);
                if (noteText != null && !noteText.isBlank()) {
                    req.setNote(noteText.trim());
                }
            }
            case CLEAR_REASONING_NOTES -> {
                if (tokens.size() >= 2) req.setSource(tokens.get(1));
                if (tokens.size() >= 3) req.setIntent(joinTokens(tokens, 2));
            }
            case LIST_REASONING_NOTES -> {
                if (tokens.size() >= 2) req.setSource(tokens.get(1));
                if (tokens.size() >= 3) req.setIntent(joinTokens(tokens, 2));
            }
        }

        validate(req);
        return req;
    }

    private static ToolExecutionRequest fromKeyValueLines(String[] lines, int startIdx) {
        String opVal = null;
        String baseVal = null;
        String pathVal = null;
        String destinationVal = null;
        String scriptVal = null;
        String intentVal = null;
        String contentVal = null;
        String noteVal = null;
        String sourceVal = null;
        String compiledVal = null;
        boolean recVal = false;
        boolean b64Val = false;

        StringBuilder contentBuf = new StringBuilder();
        boolean inContent = false;

        for (int i = startIdx; i < lines.length; i++) {
            String line = lines[i];
            if (inContent) {
                contentBuf.append(line).append("\n");
                continue;
            }
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String rawKey = line.substring(0, colonIdx).trim().replaceFirst("^-\\s*", "");
                String key = rawKey.toLowerCase(Locale.ROOT);
                String val = line.substring(colonIdx + 1).trim();

                boolean isQuoted = (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2)
                        || (val.startsWith("'") && val.endsWith("'") && val.length() >= 2);

                if (!isQuoted && !val.startsWith("|")) {
                    if (val.contains(": ") || ("content".equals(key) || "patch".equals(key)) && val.contains(":")) {
                        throw new IllegalArgumentException("Invalid tool YAML payload. Unquoted colon in value.");
                    }
                }

                if (isQuoted) {
                    val = val.substring(1, val.length() - 1);
                }
                switch (key) {
                    case "mode", "startline", "startcolumn", "endline", "endcolumn", "atline", "replacing" ->
                        throw new IllegalArgumentException("'" + rawKey + "' is deprecated and no longer supported.");
                    case "operation" -> opVal = val;
                    case "base" -> baseVal = val;
                    case "path" -> pathVal = val;
                    case "destination", "dest", "destinationpath", "targetpath" -> destinationVal = val;
                    case "script" -> scriptVal = val;
                    case "intent" -> intentVal = val;
                    case "recursive" -> recVal = Boolean.parseBoolean(val);
                    case "base64" -> b64Val = Boolean.parseBoolean(val);
                    case "source" -> sourceVal = val;
                    case "compiled" -> compiledVal = val;
                    case "note" -> noteVal = val;
                    case "content", "patch" -> {
                        if (val.startsWith("|") || val.isEmpty()) {
                            inContent = true;
                        } else {
                            contentVal = val;
                        }
                    }
                }
            }
        }
        if (inContent) {
            contentVal = contentBuf.toString();
        }

        ToolExecutionRequest req = new ToolExecutionRequest();
        req.setOperation(parseOperation(opVal));
        req.setBase(baseVal);
        req.setPath(pathVal);
        req.setDestination(destinationVal);
        req.setScript(scriptVal);
        req.setIntent(intentVal);
        req.setRecursive(recVal);
        req.setContent(contentVal);
        req.setBase64(b64Val);
        req.setSource(sourceVal);
        req.setCompiled(compiledVal);
        req.setNote(noteVal);
        validate(req);
        return req;
    }

    public static List<String> tokenizeCommandLine(String line) {
        List<String> tokens = new ArrayList<>();
        if (line == null || line.isBlank()) {
            return tokens;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static String joinTokens(List<String> tokens, int startIndex) {
        if (startIndex >= tokens.size()) {
            return null;
        }
        return String.join(" ", tokens.subList(startIndex, tokens.size()));
    }

    private static String extractPayload(String[] lines, int startIdx) {
        if (startIdx >= lines.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static boolean isNoteOperation(Operation op) {
        return op == Operation.ADD_REASONING_NOTE || op == Operation.CLEAR_REASONING_NOTES || op == Operation.LIST_REASONING_NOTES;
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
                case ADD_REASONING_NOTE -> {
                    boolean hasSource = request.getSource() != null && !request.getSource().isBlank();
                    boolean hasCompiled = request.getCompiled() != null && !request.getCompiled().isBlank();
                    if (hasSource == hasCompiled) {
                        throw new IllegalArgumentException(
                                "add_reasoning_note requires exactly one of 'source' or 'compiled'");
                    }
                    if (hasCompiled && !isQualifiedPath(request.getCompiled())) {
                        throw new IllegalArgumentException(
                                "add_reasoning_note 'compiled' must be a canonical qualified path (<base>:<path>), for example: target:path/to/file.ext");
                    }
                    if (request.getNote() == null || request.getNote().isBlank()) {
                        throw new IllegalArgumentException("add_reasoning_note requires non-blank 'note'");
                    }
                }
                case CLEAR_REASONING_NOTES -> {
                    if (request.getSource() == null || request.getSource().isBlank()) {
                        throw new IllegalArgumentException("clear_reasoning_notes requires 'source'");
                    }
                }
                default -> { }
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
                || request.getOperation() == Operation.DELETE_FILE
                || request.getOperation() == Operation.APPEND_FILE
                || request.getOperation() == Operation.PREPEND_FILE
                || request.getOperation() == Operation.MOVE_FILE
                || request.getOperation() == Operation.COPY_FILE;
        if (isMutationOp && request.getPath().isBlank()) {
            throw new IllegalArgumentException("tool request path is required for mutation operations.");
        }

        switch (request.getOperation()) {
            case WRITE_FILE -> {
                if (request.getContent() == null) {
                    throw new IllegalArgumentException("write_file requires content.");
                }
            }
            case APPEND_FILE -> {
                if (request.getContent() == null) {
                    throw new IllegalArgumentException("append_file requires content.");
                }
            }
            case PREPEND_FILE -> {
                if (request.getContent() == null) {
                    throw new IllegalArgumentException("prepend_file requires content.");
                }
            }
            case MOVE_FILE -> {
                if (request.getDestination() == null || request.getDestination().isBlank()) {
                    throw new IllegalArgumentException("move_file requires destination.");
                }
            }
            case COPY_FILE -> {
                if (request.getDestination() == null || request.getDestination().isBlank()) {
                    throw new IllegalArgumentException("copy_file requires destination.");
                }
            }
            case PATCH_FILE -> {
                if (request.getContent() == null || request.getContent().isBlank()) {
                    throw new IllegalArgumentException("patch_file requires content or patch (unified diff string).");
                }
            }
            default -> { }
        }
    }

    private static Operation parseOperation(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "list_files" -> Operation.LIST_FILES;
            case "read_file" -> Operation.READ_FILE;
            case "write_file" -> Operation.WRITE_FILE;
            case "patch_file" -> Operation.PATCH_FILE;
            case "delete_file" -> Operation.DELETE_FILE;
            case "append_file" -> Operation.APPEND_FILE;
            case "prepend_file" -> Operation.PREPEND_FILE;
            case "move_file" -> Operation.MOVE_FILE;
            case "copy_file" -> Operation.COPY_FILE;
            case "list_compiled_files" -> Operation.LIST_COMPILED_FILES;
            case "run_script" -> Operation.RUN_SCRIPT;
            case "add_reasoning_note" -> Operation.ADD_REASONING_NOTE;
            case "clear_reasoning_notes" -> Operation.CLEAR_REASONING_NOTES;
            case "list_reasoning_notes" -> Operation.LIST_REASONING_NOTES;
            default -> throw new IllegalArgumentException("Unsupported tool operation: " + value);
        };
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

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isBase64() {
        return base64;
    }

    public void setBase64(boolean base64) {
        this.base64 = base64;
    }

    public boolean isCreateParents() {
        return createParents;
    }

    public void setCreateParents(boolean createParents) {
        this.createParents = createParents;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args == null ? List.of() : List.copyOf(args);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCompiled() {
        return compiled;
    }

    public void setCompiled(String compiled) {
        this.compiled = compiled;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
