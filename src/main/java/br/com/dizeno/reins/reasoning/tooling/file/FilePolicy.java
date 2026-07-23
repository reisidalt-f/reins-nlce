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
        DELETE("delete");

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
            String normalizedToken = token.trim().toLowerCase();
            for (OperationToken t : OperationToken.values()) {
                if (t.value.equals(normalizedToken)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final Map<Base, Set<OperationToken>> permissionsByBase;

     
    /**
     * Constructs a new instance of {@link FilePolicy}.
     *
     * @param settings the settings
     */
    public FilePolicy(ToolingSettings settings) {
        this.permissionsByBase = new HashMap<>();
        permissionsByBase.put(Base.MAIN, parseTokens(settings.getMain()));
        permissionsByBase.put(Base.TEST, parseTokens(settings.getTest()));
        permissionsByBase.put(Base.TARGET, parseTokens(settings.getTarget()));

        
        permissionsByBase.get(Base.TARGET).add(OperationToken.WRITE);
    }

     
    /**
     * Constructs a new instance of {@link FilePolicy}.
     *
     * @param permissionsByBase the permissions by base
     */
    public FilePolicy(Map<Base, Set<OperationToken>> permissionsByBase) {
        this.permissionsByBase = new HashMap<>(permissionsByBase);
    }

     
    /**
     * All Permissive.
     *
     * @return the resolved or constructed object
     */
    public static FilePolicy allPermissive() {
        Map<Base, Set<OperationToken>> map = new HashMap<>();
        Set<OperationToken> all = new HashSet<>(Set.of(OperationToken.values()));
        for (Base base : Base.values()) {
            map.put(base, new HashSet<>(all));
        }
        return new FilePolicy(map);
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
                    "Recognized tokens: list, list_compiled, read, write, patch, delete"
                );
            }
            result.add(token);
        }

        return result;
    }

     
    /**
     * Checks if the component is operation allowed.
     *
     * @param operation the operation
     * @param base the base
     * @return true if successful or matching, false otherwise
     */
    public boolean isOperationAllowed(ToolExecutionType operation, Base base) {
        if (operation == null || base == null) {
            return false;
        }

        
        if (operation == ToolExecutionType.WRITE_FILE && base == Base.TARGET) {
            return true;
        }

        
        OperationToken token = operation.getRequiredToken();
        if (token == null) {
            return false;
        }

        
        return permissionsByBase.get(base).contains(token);
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
     * Gets the permissions for base.
     *
     * @param base the base
     * @return the collection of elements
     */
    public Set<OperationToken> getPermissionsForBase(Base base) {
        return permissionsByBase.getOrDefault(base, Set.of());
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append("{");
        for (Base base : Base.values()) {
            sb.append(base.name().toLowerCase()).append("=[");
            Set<OperationToken> tokens = permissionsByBase.get(base);
            String tokenStr = tokens.stream()
                .map(OperationToken::getValue)
                .sorted()
                .collect(Collectors.joining(", "));
            sb.append(tokenStr).append("]");
            if (!base.equals(Base.TARGET)) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
