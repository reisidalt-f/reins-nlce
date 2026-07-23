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
 * CompiledSourceGroup is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing compiled source group.
 */
public class CompiledSourceGroup {
    private final String sourceCanonicalPath;
    private final String sourceSimpleName;
    private final List<AttachedFilePayload> compiledAttachments;

    /**
     * Constructs a new instance of {@link CompiledSourceGroup}.
     *
     * @param sourceCanonicalPath the source canonical path
     * @param sourceSimpleName the source simple name
     * @param compiledAttachments the compiled attachments
     */
    public CompiledSourceGroup(String sourceCanonicalPath,
                                String sourceSimpleName,
                                List<AttachedFilePayload> compiledAttachments) {
        this.sourceCanonicalPath = sourceCanonicalPath;
        this.sourceSimpleName = sourceSimpleName;
        this.compiledAttachments = compiledAttachments == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(compiledAttachments);
    }

    /**
     * Gets the source canonical path.
     *
     * @return the string result
     */
    public String getSourceCanonicalPath() {
        return sourceCanonicalPath;
    }

    /**
     * Gets the source simple name.
     *
     * @return the string result
     */
    public String getSourceSimpleName() {
        return sourceSimpleName;
    }

    /**
     * Gets the compiled attachments.
     *
     * @return the collection of elements
     */
    public List<AttachedFilePayload> getCompiledAttachments() {
        return compiledAttachments;
    }
}
