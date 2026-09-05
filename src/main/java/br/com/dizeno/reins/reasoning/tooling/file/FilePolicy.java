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

package br.com.dizeno.reins.reasoning.tooling.file;

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionType;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * FilePolicy is part of the general application functions in the reins architecture.
 * Acts as a component managing file policy.
 */
public class FilePolicy {

    /**
     * Base is part of the general application functions in the reins architecture.
     * Acts as a component managing base.
     */
    public enum Base {
        MAIN, TEST, TARGET
    }

    /**
     * OperationToken is part of the general application functions in the reins architecture.
     * Acts as a component managing operation token.
     */
    public enum OperationToken {
        LIST("list"),
        LIST_COMPILED("list_compiled"),
        READ("read"),
        WRITE("write"),
        PATCH("patch"),
        DELETE("delete"),
        APPEND("append"),
        PREPEND("prepend"),
        MOVE("move"),
        COPY("copy");

        private final String value;

        OperationToken(String value) {
            this.value = value;
        }

        /**
         * Gets the value.
         *
         * @return the string result
         */
        public String getValue() {
            return value;
        }

        /**
         * From String.
         *
         * @param token the token
         * @return the resolved or constructed object
         */
        public static OperationToken fromString(String token) {
            if (token == null || token.isBlank()) {
                return null;
            }
            String normalizedToken = token.trim().toLowerCase(Locale.ROOT);
            for (OperationToken t : OperationToken.values()) {
                if (t.value.equals(normalizedToken)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final Map<String, Set<OperationToken>> permissionsByBase;
    private boolean addReasoningNotes;

    /**
     * Constructs a new instance of {@link FilePolicy}.
     *
     * @param settings the settings
     */
    public FilePolicy(ToolingSettings settings) {
        this.permissionsByBase = new HashMap<>();
        this.addReasoningNotes = settings != null && settings.isAddReasoningNotes();
        if (settings != null && settings.getBases() != null) {
            for (Map.Entry<String, String> entry : settings.getBases().entrySet()) {
                if (entry.getKey() != null) {
                    permissionsByBase.put(entry.getKey().toLowerCase(Locale.ROOT), parseTokens(entry.getValue()));
                }
            }
        }
        if (!permissionsByBase.containsKey("main")) {
            permissionsByBase.put("main", parseTokens(settings != null ? settings.getMain() : null));
        }
        if (!permissionsByBase.containsKey("test")) {
            permissionsByBase.put("test", parseTokens(settings != null ? settings.getTest() : null));
        }
        if (!permissionsByBase.containsKey("target")) {
            permissionsByBase.put("target", parseTokens(settings != null ? settings.getTarget() : null));
        }
    }

    /**
     * Constructs a new instance of {@link FilePolicy}.
     *
     * @param permissionsByBase the permissions by base
     */
    public FilePolicy(Map<Base, Set<OperationToken>> permissionsByBase) {
        this(permissionsByBase, true);
    }

    /**
     * Constructs a new instance of {@link FilePolicy}.
     *
     * @param permissionsByBase the permissions by base
     * @param addReasoningNotes whether reasoning notes operations are permitted
     */
    public FilePolicy(Map<Base, Set<OperationToken>> permissionsByBase, boolean addReasoningNotes) {
        this.permissionsByBase = new HashMap<>();
        this.addReasoningNotes = addReasoningNotes;
        if (permissionsByBase != null) {
            for (Map.Entry<Base, Set<OperationToken>> entry : permissionsByBase.entrySet()) {
                if (entry.getKey() != null) {
                    this.permissionsByBase.put(entry.getKey().name().toLowerCase(Locale.ROOT), new HashSet<>(entry.getValue()));
                }
            }
        }
    }

    /**
     * All Permissive.
     *
     * @return the resolved or constructed object
     */
    public static FilePolicy allPermissive() {
        Map<String, Set<OperationToken>> map = new HashMap<>();
        Set<OperationToken> all = new HashSet<>(Set.of(OperationToken.values()));
        for (Base base : Base.values()) {
            map.put(base.name().toLowerCase(Locale.ROOT), new HashSet<>(all));
        }
        FilePolicy policy = new FilePolicy(ToolingSettings.class.cast(null));
        policy.addReasoningNotes = true;
        for (Map.Entry<String, Set<OperationToken>> entry : map.entrySet()) {
            policy.permissionsByBase.put(entry.getKey(), entry.getValue());
        }
        return policy;
    }

    /**
     * Checks if reasoning notes operations are allowed.
     *
     * @return true if reasoning notes operations are enabled, false otherwise
     */
    public boolean isAddReasoningNotes() {
        return addReasoningNotes;
    }

    private static Set<OperationToken> parseTokens(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            return new HashSet<>();
        }

        Set<OperationToken> result = new HashSet<>();
        String[] parts = tokenString.split("\\s*,\\s*");

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            OperationToken token = OperationToken.fromString(part);
            if (token == null) {
                throw new IllegalArgumentException(
                    "Unrecognized tool operation token: '" + part + "'. " +
                    "Recognized tokens: list, list_compiled, read, write, patch, delete, append, prepend, move, copy"
                );
            }
            result.add(token);
        }

        return result;
    }

    /**
     * Checks if the component is operation allowed for enum Base.
     *
     * @param operation the operation
     * @param base the base
     * @return true if successful or matching, false otherwise
     */
    public boolean isOperationAllowed(ToolExecutionType operation, Base base) {
        if (base == null) {
            return false;
        }
        return isOperationAllowed(operation, base.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if the component is operation allowed for a base name.
     *
     * @param operation the operation
     * @param base base name
     * @return true if successful or matching, false otherwise
     */
    public boolean isOperationAllowed(ToolExecutionType operation, String base) {
        if (operation == null || base == null || base.isBlank()) {
            return false;
        }
        String normalized = base.trim().toLowerCase(Locale.ROOT);

        OperationToken token = operation.getRequiredToken();
        if (token == null) {
            return false;
        }

        Set<OperationToken> tokens = permissionsByBase.get(normalized);
        if (tokens == null) {
            return false;
        }
        if (tokens.contains(token)) {
            return true;
        }
        return tokens.contains(OperationToken.WRITE) &&
                (token == OperationToken.APPEND || token == OperationToken.PREPEND || token == OperationToken.MOVE || token == OperationToken.COPY);
    }

    /**
     * Gets the enabled bases for operation.
     *
     * @param operation the operation
     * @return the collection of elements
     */
    public Set<Base> getEnabledBasesForOperation(ToolExecutionType operation) {
        if (operation == null) {
            return Set.of();
        }

        Set<Base> enabled = new HashSet<>();
        for (Base base : Base.values()) {
            if (isOperationAllowed(operation, base)) {
                enabled.add(base);
            }
        }
        return enabled;
    }

    /**
     * Gets all enabled base names for operation.
     *
     * @param operation the operation
     * @return set of base names
     */
    public Set<String> getEnabledBaseNamesForOperation(ToolExecutionType operation) {
        if (operation == null) {
            return Set.of();
        }
        Set<String> enabled = new HashSet<>();
        for (String base : permissionsByBase.keySet()) {
            if (isOperationAllowed(operation, base)) {
                enabled.add(base);
            }
        }
        return enabled;
    }

    /**
     * Gets the permissions for enum Base.
     *
     * @param base the base
     * @return the collection of elements
     */
    public Set<OperationToken> getPermissionsForBase(Base base) {
        if (base == null) return Set.of();
        return getPermissionsForBase(base.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Gets the permissions for base name.
     *
     * @param base base name
     * @return set of tokens
     */
    public Set<OperationToken> getPermissionsForBase(String base) {
        if (base == null || base.isBlank()) return Set.of();
        return permissionsByBase.getOrDefault(base.trim().toLowerCase(Locale.ROOT), Set.of());
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append("{");
        boolean first = true;
        for (Map.Entry<String, Set<OperationToken>> entry : permissionsByBase.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append("=[");
            String tokenStr = entry.getValue().stream()
                .map(OperationToken::getValue)
                .sorted()
                .collect(Collectors.joining(", "));
            sb.append(tokenStr).append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}
