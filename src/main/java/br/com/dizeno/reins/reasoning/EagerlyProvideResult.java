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

import java.util.Collections;
import java.util.List;
import br.com.dizeno.reins.reasoning.scripting.AttachedFilePayload;

/**
 * EagerlyProvideResult is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
public class EagerlyProvideResult {
    private final List<AttachedFilePayload> compiledAttachments;
    private final List<AttachedFilePayload> inspectedAttachments;
    private final List<CompiledSourceGroup> compiledSourceGroups;

    /**
     * Constructs a new instance of {@link EagerlyProvideResult}.
     *
     * @param compiledAttachments the compiled attachments
     * @param inspectedAttachments the inspected attachments
     */
    public EagerlyProvideResult(List<AttachedFilePayload> compiledAttachments,
                                List<AttachedFilePayload> inspectedAttachments) {
        this(compiledAttachments, inspectedAttachments, Collections.emptyList());
    }

    /**
     * Constructs a new instance of {@link EagerlyProvideResult}.
     *
     * @param compiledAttachments the compiled attachments
     * @param inspectedAttachments the inspected attachments
     * @param compiledSourceGroups the compiled source groups
     */
    public EagerlyProvideResult(List<AttachedFilePayload> compiledAttachments,
                                List<AttachedFilePayload> inspectedAttachments,
                                List<CompiledSourceGroup> compiledSourceGroups) {
        this.compiledAttachments = compiledAttachments != null ? compiledAttachments : Collections.emptyList();
        this.inspectedAttachments = inspectedAttachments != null ? inspectedAttachments : Collections.emptyList();
        this.compiledSourceGroups = compiledSourceGroups != null ? compiledSourceGroups : Collections.emptyList();
    }

    /**
     * Empty.
     *
     * @return the resulting result
     */
    public static EagerlyProvideResult empty() {
        return new EagerlyProvideResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Gets the compiled attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getCompiledAttachments() {
        return compiledAttachments;
    }

    /**
     * Gets the inspected attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getInspectedAttachments() {
        return inspectedAttachments;
    }

    /**
     * Gets the compiled source groups.
     *
     * @return the collection of elements
     */
    public List<CompiledSourceGroup> getCompiledSourceGroups() {
        return compiledSourceGroups;
    }
}
