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

import java.nio.file.Path;
import java.time.Instant;

 
/**
 * ReasoningLogService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface ReasoningLogService {
     
    /**
     * Initializes the component cycle log.
     *
     * @param cycleId the cycle id
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    ReasoningCycleLog initializeCycleLog(String cycleId, Path projectRoot) throws Exception;

    /**
     * Initializes the component cycle log with source path.
     *
     * @param cycleId the cycle id
     * @param projectRoot the root path of the project
     * @param sourcePath the current compilation source path
     * @return the resolved or constructed object
     */
    default ReasoningCycleLog initializeCycleLog(String cycleId, Path projectRoot, String sourcePath) throws Exception {
        return initializeCycleLog(cycleId, projectRoot);
    }

     
    /**
     * Write Entry.
     *
     * @param cycleLog the reasoning cycle log instance
     * @param entry the entry
     * @return the resulting result
     */
    ReasoningLogWriteResult writeEntry(ReasoningCycleLog cycleLog, ReasoningLogEntry entry);

    default ReasoningLogWriteResult writePipelineEvent(ReasoningCycleLog cycleLog,
                                                       String phase,
                                                       String outcome,
                                                       int sequence,
                                                       String body) {
        ReasoningLogEntry entry = new ReasoningLogEntry(Instant.now(), ReasoningLogEntry.Direction.OUTBOUND,
                "pipeline", sequence, body);
        entry.setPipelinePhase(phase);
        entry.setPipelineOutcome(outcome);
        /**
         * Write Entry.
         *
         * @param cycleLog the reasoning cycle log instance
         * @param entry the entry
         * @return the resolved or constructed object
         */
        return writeEntry(cycleLog, entry);
    }

     
    /**
     * Close Cycle Log.
     *
     * @param cycleLog the reasoning cycle log instance
     */
    void closeCycleLog(ReasoningCycleLog cycleLog);
}
