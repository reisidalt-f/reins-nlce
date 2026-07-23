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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

 
/**
 * CleanupOutcomeSummary is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing cleanup outcome summary.
 */
public class CleanupOutcomeSummary {

    /**
     * OverallStatus is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a component managing overall status.
     */
    public enum OverallStatus {
        SUCCESS,
        PARTIAL_FAILURE
    }

    private final String session;
    private final Instant startedAt;
    private final Instant completedAt;
    private final Path projectRoot;
    private final List<CleanupOutcome> outcomes;
    private final OverallStatus overallStatus;
    private final CleanupStatistics statistics;

    private CleanupOutcomeSummary(String session,
                                  Instant startedAt,
                                  Instant completedAt,
                                  Path projectRoot,
                                  List<CleanupOutcome> outcomes,
                                  OverallStatus overallStatus,
                                  CleanupStatistics statistics) {
        this.session = session;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.projectRoot = projectRoot;
        this.outcomes = Collections.unmodifiableList(outcomes);
        this.overallStatus = overallStatus;
        this.statistics = statistics;
    }

    /**
     * Gets the session.
     *
     * @return the string result
     */
    public String getSession() {
        return session;
    }

    /**
     * Gets the started at.
     *
     * @return the resolved or constructed object
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Gets the completed at.
     *
     * @return the resolved or constructed object
     */
    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Gets the project root.
     *
     * @return the resolved or constructed object
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Gets the outcomes.
     *
     * @return the collection of elements
     */
    public List<CleanupOutcome> getOutcomes() {
        return outcomes;
    }

    /**
     * Gets the overall status.
     *
     * @return the resulting status
     */
    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    /**
     * Gets the statistics.
     *
     * @return the resolved or constructed object
     */
    public CleanupStatistics getStatistics() {
        return statistics;
    }

    /**
     * Gets the duration ms.
     *
     * @return the numeric value
     */
    public long getDurationMs() {
        return completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    /**
     * Checks if the component is successful.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isSuccessful() {
        return overallStatus == OverallStatus.SUCCESS;
    }

    /**
     * Checks if the component has failures.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean hasFailures() {
        return overallStatus == OverallStatus.PARTIAL_FAILURE;
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return "CleanupOutcomeSummary{" +
                "session='" + session + '\'' +
                ", duration=" + getDurationMs() + "ms" +
                ", projectRoot=" + projectRoot +
                ", outcomes=" + outcomes.size() +
                ", status=" + overallStatus +
                ", statistics=" + statistics +
                '}';
    }

    /**
     * Builder is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
     * Acts as a helper utility for building its prefix objects.
     */
    public static class Builder {
        private final Instant startedAt = Instant.now();
        private final Path projectRoot;
        private Instant completedAt;
        private List<CleanupOutcome> outcomes = Collections.emptyList();
        private OverallStatus overallStatus = OverallStatus.SUCCESS;
        private CleanupStatistics statistics;

        /**
         * Constructs a new instance of {@link Builder}.
         *
         * @param projectRoot the root path of the project
         */
        public Builder(Path projectRoot) {
            this.projectRoot = projectRoot;
        }

        /**
         * Completed At.
         *
         * @param completedAt the completed at
         * @return the resolved or constructed object
         */
        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        /**
         * Outcomes.
         *
         * @param outcomes the outcomes
         * @return the resolved or constructed object
         */
        public Builder outcomes(List<CleanupOutcome> outcomes) {
            this.outcomes = outcomes == null ? Collections.emptyList() : outcomes;
            return this;
        }

        /**
         * Overall Status.
         *
         * @param overallStatus the overall status
         * @return the resolved or constructed object
         */
        public Builder overallStatus(OverallStatus overallStatus) {
            this.overallStatus = overallStatus == null ? OverallStatus.SUCCESS : overallStatus;
            return this;
        }

        /**
         * Statistics.
         *
         * @param statistics the statistics
         * @return the resolved or constructed object
         */
        public Builder statistics(CleanupStatistics statistics) {
            this.statistics = statistics;
            return this;
        }

        /**
         * Builds the configured target.
         *
         * @return the resulting summary
         */
        public CleanupOutcomeSummary build() {
            Instant end = completedAt == null ? Instant.now() : completedAt;
            String sessionId = "cleanup-" + Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID().toString().substring(0, 8);
            return new CleanupOutcomeSummary(
                    sessionId,
                    startedAt,
                    end,
                    projectRoot,
                    outcomes,
                    overallStatus,
                    statistics
            );
        }
    }
}
