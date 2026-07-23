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

package br.com.dizeno.reins.util;

/**
 * LogSanitizer is part of the shared utility components like path normalizers
 * and log sanitizers in the reins architecture.
 * Acts as a component managing log sanitizer.
 */
public final class LogSanitizer {
    private LogSanitizer() {
    }

    /**
     * Sanitize Secret.
     *
     * @param value the value
     * @return the string result
     */
    public static String sanitizeSecret(String value) {
        if (value == null || value.isEmpty()) {
            return "<empty>";
        }
        int visible = Math.min(4, value.length());
        String suffix = value.substring(value.length() - visible);
        return "***" + suffix;
    }

    /**
     * Sanitize For Log.
     *
     * @param value the value
     * @return the string result
     */
    public static String sanitizeForLog(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        normalized = normalized.replaceAll("(?i)(api[_-]?key\\s*[:=]\\s*)([^\\s,;]+)", "$1<redacted>");
        normalized = normalized.replaceAll("(?i)(bearer\\s+)([a-z0-9._-]+)", "$1<redacted>");
        return normalized;
    }
}
