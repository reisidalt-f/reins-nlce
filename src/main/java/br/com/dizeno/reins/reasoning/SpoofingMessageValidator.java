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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.PipelineExchangeMessage;
import br.com.dizeno.reins.reasoning.model.SpoofingRoleContract;

import java.util.Map;

/**
 * SpoofingMessageValidator is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a helper utility for validating its prefix constraints.
 */
public final class SpoofingMessageValidator {
    private SpoofingMessageValidator() {
    }

    /**
     * Checks if the component is spoof marker valid.
     *
     * @param roleHeader the role header
     * @return true if successful or matching, false otherwise
     */
    public static boolean isSpoofMarkerValid(String roleHeader) {
        return roleHeader != null
                && SpoofingRoleContract.ROLE_HEADER_VALUE.value().equalsIgnoreCase(roleHeader.trim());
    }

    /**
     * Validates the inputs or files headers.
     *
     * @param headers the headers
     * @param trustedScriptOrigin the trusted script origin
     * @return the string result
     */
    public static String validateHeaders(Map<String, String> headers, boolean trustedScriptOrigin) {
        if (headers == null || !headers.containsKey(SpoofingRoleContract.ROLE_HEADER_NAME.value())) {
            return null;
        }
        String role = headers.get(SpoofingRoleContract.ROLE_HEADER_NAME.value());
        if (role == null || role.isBlank()) {
            return "Malformed ROLE header: value cannot be empty.";
        }
        if (!isSpoofMarkerValid(role)) {
            return "Malformed ROLE header: expected ROLE: assistant.";
        }
        if (!trustedScriptOrigin) {
            return "Rejected ROLE: assistant message from untrusted origin.";
        }
        return null;
    }
}