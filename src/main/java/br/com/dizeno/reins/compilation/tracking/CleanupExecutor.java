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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

 
/**
 * CleanupExecutor is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup executor.
 */
public class CleanupExecutor {
    
    private final CleanupTargetValidator validator;
    private final Path projectRoot;
    
     
    /**
     * Constructs a new instance of {@link CleanupExecutor}.
     *
     * @param validator the path validator for security boundary checks
     * @param projectRoot the root path of the project
     */
    public CleanupExecutor(CleanupTargetValidator validator, Path projectRoot) {
        this.validator = validator;
        this.projectRoot = projectRoot;
    }
    
     
    /**
     * Deletes the target target.
     *
     * @param target the target
     * @return the resolved or constructed object
     */
    public CleanupOutcome deleteTarget(CleanupTarget target) {
        Instant attemptedAt = Instant.now();
        
        
        CleanupTargetValidator.ValidationResult validationResult = 
            validator.validateTarget(target, projectRoot);
        
        if (!validationResult.isValid()) {
            return new CleanupOutcome(
                target,
                CleanupOutcome.Status.REJECTED,
                validationResult.getReason(),
                null,
                attemptedAt
            );
        }
        
        
        Path targetPath = parseCanonicalPath(target.getPath(), projectRoot);
        if (targetPath == null) {
            return new CleanupOutcome(
                target,
                CleanupOutcome.Status.REJECTED,
                "Invalid canonical path format",
                null,
                attemptedAt
            );
        }
        
        
        if (!Files.exists(targetPath)) {
            return new CleanupOutcome(
                target,
                CleanupOutcome.Status.ALREADY_MISSING,
                "File was already missing",
                null,
                attemptedAt
            );
        }
        
        
        try {
            if (Files.isDirectory(targetPath)) {
                
                if (!isEmpty(targetPath)) {
                    return new CleanupOutcome(
                        target,
                        CleanupOutcome.Status.FAILED_DELETION,
                        "Directory is not empty",
                        null,
                        attemptedAt
                    );
                }
            }
            
            Files.delete(targetPath);
            
            return new CleanupOutcome(
                target,
                CleanupOutcome.Status.DELETED,
                "File deleted successfully",
                null,
                attemptedAt
            );
        } catch (Exception e) {
            
            String reason = categorizeError(e);
            
            return new CleanupOutcome(
                target,
                CleanupOutcome.Status.FAILED_DELETION,
                reason,
                e,
                attemptedAt
            );
        }
    }
    
     
    private Path parseCanonicalPath(String canonicalPath, Path projectRoot) {
        try {
            if (!canonicalPath.contains(":")) {
                return null;
            }
            String[] parts = canonicalPath.split(":", 2);
            String relativePart = parts[1];
            
            
            
            Path resolved = projectRoot.resolve(relativePart);
            return resolved.normalize();
        } catch (Exception e) {
            return null;
        }
    }
    
     
    private boolean isEmpty(Path directory) {
        try {
            return Files.list(directory).count() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
     
    private String categorizeError(Exception e) {
        String exceptionClassName = e.getClass().getSimpleName();
        
        if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
            return "Permission denied (read-only file or directory)";
        }
        if (exceptionClassName.contains("AccessDenied")) {
            return "Access denied: insufficient permissions";
        }
        if (exceptionClassName.contains("FileSystem")) {
            return "Filesystem error: " + e.getMessage();
        }
        if (exceptionClassName.contains("NoSpace")) {
            return "No space left on device";
        }
        if (exceptionClassName.contains("IO")) {
            return "I/O error: " + e.getMessage();
        }
        
        return "Deletion failed: " + exceptionClassName + " - " + e.getMessage();
    }
}
