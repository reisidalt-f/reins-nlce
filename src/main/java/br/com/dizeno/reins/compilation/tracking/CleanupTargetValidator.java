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

package br.com.dizeno.reins.compilation.tracking;

import br.com.dizeno.reins.security.PathValidator;
import java.nio.file.Files;
import java.nio.file.Path;

 
/**
 * CleanupTargetValidator is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a helper utility for validating its prefix constraints.
 */
public class CleanupTargetValidator {
    
    private final PathValidator pathValidator;
    
     
    /**
     * ValidationResult is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        
        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }
        
        /**
         * Valid.
         *
         * @return the resulting result
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        /**
         * Rejected.
         *
         * @param reason the reason
         * @return the resulting result
         */
        public static ValidationResult rejected(String reason) {
            return new ValidationResult(false, reason);
        }
        
        /**
         * Checks if the component is valid.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Gets the reason.
         *
         * @return the string result
         */
        public String getReason() {
            return reason;
        }
    }
    
     
    /**
     * Constructs a new instance of {@link CleanupTargetValidator}.
     *
     * @param pathValidator the path validator
     */
    public CleanupTargetValidator(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }
    
     
    /**
     * Validates the inputs or files target.
     *
     * @param target the target
     * @param projectRoot the root path of the project
     * @return the resulting result
     */
    public ValidationResult validateTarget(CleanupTarget target, Path projectRoot) {
        String path = target.getPath();
        
        
        if (path == null || path.isEmpty()) {
            return ValidationResult.rejected("Target path cannot be empty");
        }
        
        if (!path.contains(":")) {
            return ValidationResult.rejected(
                String.format("Path is not in canonical format (expected '<base>:<relative path>'): %s", path)
            );
        }
        
        String[] parts = path.split(":", 2);
        if (parts.length != 2 || parts[1].isEmpty()) {
            return ValidationResult.rejected(
                String.format("Canonical path format invalid: %s", path)
            );
        }
        
        String relativePart = parts[1];
        
        
        if (relativePart.contains("..")) {
            return ValidationResult.rejected(
                String.format("Path attempts to escape project root via '..': %s", path)
            );
        }
        
        if (relativePart.startsWith("/")) {
            return ValidationResult.rejected(
                String.format("Relative path must not start with '/': %s", path)
            );
        }
        
        
        try {
            Path resolvedPath = projectRoot.resolve(relativePart).normalize();

            
            pathValidator.validateInProject(resolvedPath);

            
            if (!isWithinProjectRoot(resolvedPath, projectRoot)) {
                return ValidationResult.rejected(
                    String.format("Path resolves outside project root boundary: %s", path)
                );
            }
        } catch (Exception e) {
            return ValidationResult.rejected(
                String.format("Path validation error: %s", e.getMessage())
            );
        }
        
        
        try {
            Path resolvedPath = projectRoot.resolve(relativePart).normalize();
            
            if (Files.exists(resolvedPath)) {
                boolean isDirectory = Files.isDirectory(resolvedPath);
                boolean isFile = !isDirectory;
                
                if (target.getType() == CleanupTarget.Type.FILE && isDirectory) {
                    return ValidationResult.rejected(
                        String.format("Target type is FILE but path is a directory: %s", path)
                    );
                }
                
                if (target.getType() == CleanupTarget.Type.DIRECTORY && isFile) {
                    return ValidationResult.rejected(
                        String.format("Target type is DIRECTORY but path is a file: %s", path)
                    );
                }
            }
        } catch (Exception e) {
            return ValidationResult.rejected(
                String.format("Type validation error: %s", e.getMessage())
            );
        }
        
        return ValidationResult.valid();
    }
    
     
    private boolean isWithinProjectRoot(Path resolvedPath, Path projectRoot) {
        try {
            Path normalizedRoot = projectRoot.normalize();
            Path normalizedResolved = resolvedPath.normalize();
            
            
            return normalizedResolved.startsWith(normalizedRoot);
        } catch (Exception e) {
            return false;
        }
    }
}
