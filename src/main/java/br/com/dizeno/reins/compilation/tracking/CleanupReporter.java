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

import org.apache.maven.plugin.logging.Log;
import br.com.dizeno.reins.util.PathLogFormatter;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

 
/**
 * CleanupReporter is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup reporter.
 */
public class CleanupReporter {
    
    private final Log log;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
     
    /**
     * Constructs a new instance of {@link CleanupReporter}.
     *
     * @param log the logger instance
     */
    public CleanupReporter(Log log) {
        this.log = log;
    }
    
     
    /**
     * Report Outcome.
     *
     * @param summary the summary
     * @param verbose the verbose
     */
    public void reportOutcome(CleanupOutcomeSummary summary, boolean verbose) {
        if (summary.hasFailures()) {
            reportFailures(summary, verbose);
        } else {
            reportSuccess(summary, verbose);
        }
    }
    
     
    private void reportSuccess(CleanupOutcomeSummary summary, boolean verbose) {
        log.info("========================================");
        log.info("Cleanup Outcome Summary");
        log.info("========================================");
        log.info("Session ID: " + summary.getSession());
        log.info("Project Root: " + summary.getProjectRoot());
        log.info("Started: " + formatTimestamp(summary.getStartedAt()));
        log.info("Completed: " + formatTimestamp(summary.getCompletedAt()) + 
                 " (" + summary.getDurationMs() + "ms)");
        log.info("");
        
        reportStatistics(summary);
        
        if (verbose && !summary.getOutcomes().isEmpty()) {
            reportVerboseOutcomes(summary.getOutcomes(), summary.getProjectRoot());
        }
        
        log.info("Overall Status: SUCCESS");
        log.info("========================================");
    }
    
     
    private void reportFailures(CleanupOutcomeSummary summary, boolean verbose) {
        log.warn("========================================");
        log.warn("Cleanup Outcome Summary (WITH FAILURES)");
        log.warn("========================================");
        log.warn("Session ID: " + summary.getSession());
        log.warn("Project Root: " + summary.getProjectRoot());
        log.warn("Started: " + formatTimestamp(summary.getStartedAt()));
        log.warn("Completed: " + formatTimestamp(summary.getCompletedAt()) + 
                 " (" + summary.getDurationMs() + "ms)");
        log.warn("");
        
        reportStatistics(summary);
        
        
        List<CleanupOutcome> failures = summary.getOutcomes().stream()
            .filter(CleanupOutcome::isFailure)
            .toList();
        
        if (!failures.isEmpty()) {
            log.warn("");
            log.warn("FAILURES");
            log.warn("--------");
            for (int i = 0; i < failures.size(); i++) {
                reportFailure(failures.get(i), i + 1, summary.getProjectRoot());
            }
        }
        
        log.warn("");
        log.warn("Overall Status: PARTIAL_FAILURE");
        log.warn("========================================");
    }
    
     
    private void reportStatistics(CleanupOutcomeSummary summary) {
        CleanupStatistics stats = summary.getStatistics();
        
        log.info("SUMMARY");
        log.info("-------");
        log.info("Targeted for deletion: " + stats.getTargetedForDeletion() + " artifacts");
        log.info("Successfully deleted: " + stats.getSuccessfullyDeleted());
        log.info("Already missing: " + stats.getAlreadyMissing());
        log.info("Rejected (boundary violation): " + stats.getRejected());
        log.info("Failed deletion: " + stats.getFailedDeletion());
        log.info("");
        log.info("Tracking records updated: " + stats.getTrackingRecordsUpdated());
    }
    
     
    private void reportFailure(CleanupOutcome outcome, int index, Path projectRoot) {
        CleanupTarget target = outcome.getTarget();
        
        log.warn("[" + index + "] " + PathLogFormatter.formatPath(target.getPath(), projectRoot));
        log.warn("    Status: " + outcome.getStatus());
        log.warn("    Reason: " + outcome.getReason());
        
        if (outcome.getErrorCause() != null) {
            log.warn("    Error: " + outcome.getErrorCause().getClass().getSimpleName());
        }
    }
    
     
    private void reportVerboseOutcomes(List<CleanupOutcome> outcomes, Path projectRoot) {
        log.debug("");
        log.debug("DETAILED OUTCOMES");
        log.debug("-----------------");
        
        for (CleanupOutcome outcome : outcomes) {
            CleanupTarget target = outcome.getTarget();
            log.debug(String.format("[%s] %s - %s", 
                outcome.getStatus(), 
                PathLogFormatter.formatPath(target.getPath(), projectRoot), 
                outcome.getReason()
            ));
        }
    }
    
     
    private String formatTimestamp(java.time.Instant instant) {
        return TIMESTAMP_FORMAT.format(
            java.time.LocalDateTime.ofInstant(
                instant,
                java.time.ZoneId.systemDefault()
            )
        );
    }
}
