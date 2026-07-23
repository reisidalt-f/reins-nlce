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

package br.com.dizeno.reins.run.config;

/**
 * ConfigValidationException is part of the general application functions in the reins architecture.
 * Acts as a exception representing errors in its prefix operations.
 */
public class ConfigValidationException extends RuntimeException {
    /**
     * Constructs a new instance of {@link ConfigValidationException}.
     *
     * @param message the message content
     */
    public ConfigValidationException(String message) {
        super(message);
    }
}
