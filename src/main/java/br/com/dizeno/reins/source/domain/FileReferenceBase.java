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

package br.com.dizeno.reins.source.domain;

import java.util.Locale;

/**
 * FileReferenceBase is part of the general application functions in the reins architecture.
 * Acts as a component managing file reference base.
 */
public enum FileReferenceBase {
    MAIN("main"),
    TEST("test"),
    TARGET("target"),
    SCRIPT("script");

    private final String value;

    FileReferenceBase(String value) {
        this.value = value;
    }

    /**
     * Value.
     *
     * @return the string result
     */
    public String value() {
        return value;
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * From.
     *
     * @param rawBase the raw base
     * @return the resolved or constructed object
     */
    public static FileReferenceBase from(String rawBase) {
        if (rawBase == null || rawBase.isBlank()) {
            throw new IllegalArgumentException("Base is required.");
        }
        String normalized = rawBase.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith(":/")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        } else if (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return switch (normalized) {
            case "main" -> MAIN;
            case "test" -> TEST;
            case "target" -> TARGET;
            case "main-target" -> TARGET;
            case "test-target" -> TARGET;
            case "script" -> SCRIPT;
            default -> throw new IllegalArgumentException("Unsupported base prefix: " + rawBase);
        };
    }
}