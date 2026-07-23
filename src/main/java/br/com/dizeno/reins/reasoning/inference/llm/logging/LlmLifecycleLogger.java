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

package br.com.dizeno.reins.reasoning.inference.llm.logging;

import br.com.dizeno.reins.reasoning.inference.llm.model.LlmError;
import br.com.dizeno.reins.util.LogSanitizer;
import org.apache.maven.plugin.logging.Log;

/**
 * LlmLifecycleLogger is part of the general application functions in the reins architecture.
 * Acts as a component managing llm lifecycle logger.
 */
public class LlmLifecycleLogger {
    private Log log;

    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * Log Start.
     *
     * @param requestId the request id
     * @param providerId the provider id
     */
    public void logStart(String requestId, String providerId) {
        if (log == null) {
            return;
        }
        log.debug("[llm] start requestId=" + sanitize(requestId) + " provider=" + sanitize(providerId));
    }

    /**
     * Log Selection.
     *
     * @param providerId the provider id
     */
    public void logSelection(String providerId) {
        logSelection(providerId, true);
    }

    /**
     * Log Selection with conditional check.
     *
     * @param providerId the provider id
     * @param enabled the enabled toggle flag
     */
    public void logSelection(String providerId, boolean enabled) {
        if (log == null || !enabled) {
            return;
        }
        log.info("Selected provider: " + sanitize(providerId));
    }

    /**
     * Log Success.
     *
     * @param requestId the request id
     * @param providerId the provider id
     */
    public void logSuccess(String requestId, String providerId) {
        if (log == null) {
            return;
        }
        log.debug("[llm] success requestId=" + sanitize(requestId) + " provider=" + sanitize(providerId));
    }

    /**
     * Log Failure.
     *
     * @param requestId the request id
     * @param providerId the provider id
     * @param error the error
     */
    public void logFailure(String requestId, String providerId, LlmError error) {
        if (log == null) {
            return;
        }
        String category = error == null || error.getCategory() == null ? "unknown" : error.getCategory().name().toLowerCase();
        String message = error == null ? "unknown" : sanitize(error.getMessage());
        log.warn("[llm] failure requestId=" + sanitize(requestId)
                + " provider=" + sanitize(providerId)
                + " category=" + category
                + " message=" + message);
    }

    private String sanitize(String value) {
        return LogSanitizer.sanitizeForLog(value);
    }
}
