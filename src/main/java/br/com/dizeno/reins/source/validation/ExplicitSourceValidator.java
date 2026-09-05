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

package br.com.dizeno.reins.source.validation;

import java.nio.file.Path;
import java.util.List;

/**
 * ExplicitSourceValidator is part of the general application functions in the reins architecture.
 * Acts as a helper utility for validating its prefix constraints.
 */
public class ExplicitSourceValidator {

    /**
     * Validates the inputs or files source input.
     *
     * @param source the source
     * @return the string result
     */
    /**
     * Validates and splits multiple comma-separated source inputs.
     *
     * @param source the comma-separated source string
     * @return the list of validated source entries
     */
    public List<String> validateSourceInputs(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("System property 'source' must not be empty.");
        }
        List<String> list = new java.util.ArrayList<>();
        for (String raw : source.split(",")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                validateSourceInput(trimmed);
                list.add(trimmed);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("System property 'source' must not be empty.");
        }
        return list;
    }

    /**
     * Validates the inputs or files source input.
     *
     * @param source the source
     * @return the string result
     */
    public String validateSourceInput(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("System property 'source' must not be empty.");
        }
        String trimmed = source.trim();
        String pathPart = trimmed;
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx > 0 && !trimmed.contains("://")) {
            pathPart = trimmed.substring(colonIdx + 1);
        }
        if (colonIdx == 1 && Character.isLetter(trimmed.charAt(0))) {
            throw new IllegalArgumentException("System property 'source' must be a relative path under configured source bases.");
        }
        for (String segment : pathPart.replace('\\', '/').split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("System property 'source' must not contain '..' segments.");
            }
        }
        return trimmed;
    }

    /**
     * Validates the inputs or files single match.
     *
     * @param matches the matches
     * @param source the source
     */
    public void validateSingleMatch(List<Path> matches, String source) {
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Ambiguous explicit source '" + source + "': more than one match found across main/test bases.");
        }
    }
}
