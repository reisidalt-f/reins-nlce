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

import java.util.ArrayList;
import java.util.List;

/**
 * ContextAttachmentPolicyGuard is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing context attachment policy guard.
 */
public class ContextAttachmentPolicyGuard {
    /**
     * Apply.
     *
     * @param attachments the list of attachments
     * @return the collection of elements
     */
    public List<AttachedFilePayload> apply(List<AttachedFilePayload> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(attachments);
    }
}
