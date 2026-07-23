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

package br.com.dizeno.reins.run.mojo;

import java.util.Locale;

/**
 * SkipReinsResolver is part of the general application functions in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class SkipReinsResolver {
    /**
     * Resolves the configured value or path.
     *
     * @param rawValue the raw value
     * @return the resolved or constructed object
     */
    public SkipDecision resolve(String rawValue) {
        if (rawValue == null) {
            return SkipDecision.execute(null, "false");
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return SkipDecision.skip(rawValue, "true");
        }

        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return SkipDecision.skip(rawValue, normalized);
        }
        if ("false".equals(normalized)) {
            return SkipDecision.execute(rawValue, normalized);
        }
        return SkipDecision.fail(rawValue);
    }
}