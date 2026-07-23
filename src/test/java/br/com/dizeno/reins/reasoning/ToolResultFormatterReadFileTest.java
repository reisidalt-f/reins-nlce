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

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultFormatterReadFileTest {

    private final ToolResultFormatter formatter = new ToolResultFormatter();

    @Test
    void successfulReadFileUsesAttachmentSummaryInsteadOfInliningContent() {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.success(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                "main:/domain/entities.md",
                "read"
        );
        result.setResolvedBase("main");
        result.setContent("secret-content-line\n");
        result.setReadFileStatuses(java.util.List.of(
                new br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.ReadFileStatus("main:/domain/entities.md", "attached", null)
        ));

        String output = formatter.format(result);

        assertTrue(output.contains("content: [attached]"));
        assertTrue(output.contains("read_files:"));
        assertFalse(output.contains("secret-content-line"));
    }

    @Test
    void failedReadFileRetainsFailureDetails() {
        br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult result = br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.error(
                br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest.Operation.READ_FILE,
                "main:/missing.md",
                "File does not exist."
        );
        result.setReadFileStatuses(java.util.List.of(
                new br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult.ReadFileStatus("main:/missing.md", "read_failed", "File does not exist.")
        ));

        String output = formatter.format(result);

        assertTrue(output.contains("failure: File does not exist."));
        assertTrue(output.contains("attach_status: read_failed"));
    }
}
