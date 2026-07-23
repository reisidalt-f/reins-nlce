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

/**
 * SkipReasonCode is part of the general application functions in the reins architecture.
 * Acts as a component managing skip reason code.
 */
public enum SkipReasonCode {
    SKIP_ENABLED,
    SKIP_DISABLED,
    SKIP_INVALID_VALUE
}