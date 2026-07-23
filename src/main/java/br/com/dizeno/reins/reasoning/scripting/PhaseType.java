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

import java.util.List;

 
/**
 * PhaseType is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing phase type.
 */
public enum PhaseType {

     
    PROMPT_ASSEMBLY,

     
    TOOL_RESPONSE,

     
    PHASE_LIST,

     
    ATTACHMENT_LIST,

     
    FILE_LIST

        ;

        public static final String SCRIPT_PHASE_LIST = "phase-list.ftl";
        public static final String SCRIPT_REASONING_PIPELINE = "reasoning-pipeline.ftl";
        public static final String SCRIPT_SOURCE_BASE_PHASE_LIST = "source-base-phase-list.ftl";
        public static final String SCRIPT_SYSTEM_CONTEXT = "system-context.ftl";
        public static final String SCRIPT_REFERENCE_TREE = "reference-tree.ftl";
        public static final String SCRIPT_PROJECT_CONTEXT = "project-context.ftl";
        public static final String SCRIPT_ATTACHMENT_LIST = "attachment-list.ftl";
        public static final String SCRIPT_FILE_LIST = "file-list.ftl";
        public static final String SCRIPT_TOOL_RESULT = "tool-result.ftl";
        public static final String SCRIPT_MAX_TURN_GRACE_PROMPT = "max-turn-grace-prompt.ftl";
        public static final String SCRIPT_RETRY_MESSAGE = "retry-message.ftl";

        public static final List<String> DEFAULT_PER_SOURCE_PHASE_NAMES = List.of(
            "system-context",
            "reference-tree",
            "project-context",
            "attachment-list",
            "file-list",
            "tool-result",
            "max-turn-grace-prompt",
            "retry-message");

        public static final List<String> DEFAULT_SOURCE_BASE_PHASE_NAMES = List.of(
            "system-context",
            "tool-result",
            "max-turn-grace-prompt");
}
