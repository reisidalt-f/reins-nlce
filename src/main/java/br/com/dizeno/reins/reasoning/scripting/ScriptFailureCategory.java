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

package br.com.dizeno.reins.reasoning.scripting;

 
/**
 * ScriptFailureCategory is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing script failure category.
 */
public enum ScriptFailureCategory {
    SYNTAX_FAILURE,
    EVALUATION_FAILURE,
    SECURITY_FAILURE,
    INCLUDE_FAILURE,
    NO_USABLE_OUTPUT
}
