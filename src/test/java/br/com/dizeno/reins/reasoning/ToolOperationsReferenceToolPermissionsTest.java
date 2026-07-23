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

import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

 
class ToolOperationsReferenceToolPermissionsTest {

    
    
    

    @Test
    void sectionAppearsBeforeOperationDetails() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false);

        int permIdx = ref.indexOf("### Tool Permissions");
        int detailIdx = ref.indexOf("### Operation Details");

        assertTrue(permIdx >= 0, "### Tool Permissions section must be present");
        assertTrue(detailIdx >= 0, "### Operation Details section must be present");
        assertTrue(permIdx < detailIdx,
                 "### Tool Permissions must appear before ### Operation Details");
    }

    @Test
    void bulletFormatIsToolNameColonBases() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false);

        assertTrue(ref.contains("- list_files: "), "list_files bullet must follow '- list_files: ' format");
        assertTrue(ref.contains("- read_file: "),  "read_file bullet must follow '- read_file: ' format");
        assertTrue(ref.contains("- write_file: "), "write_file bullet must follow '- write_file: ' format");
    }

    
    
    

    @Test
    void addInferenceNoteAndClearAlwaysPresent_allPermissive() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false);

        assertTrue(ref.contains("- add_reasoning_note: always available"),
                 "add_reasoning_note must always appear");
        assertTrue(ref.contains("- clear_inference_notes: always available"),
                 "clear_inference_notes must always appear");
        assertFalse(ref.contains("- **fallback when add_reasoning_note is unavailable**"));
    }

    @Test
    void addInferenceNoteAndClearAlwaysPresent_emptyPolicy() {
        
        FilePolicy emptyPolicy = new FilePolicy(
                 java.util.Map.of(
                         FilePolicy.Base.MAIN,   java.util.Set.<FilePolicy.OperationToken>of(),
                         FilePolicy.Base.TEST,   java.util.Set.<FilePolicy.OperationToken>of(),
                         FilePolicy.Base.TARGET, java.util.Set.<FilePolicy.OperationToken>of()
                 )
        );
        
        String ref = ToolOperationsReference.build(emptyPolicy, false, false, false, false);

        assertTrue(ref.contains("- add_reasoning_note: always available"),
                 "add_reasoning_note must appear even with empty policy");
        assertTrue(ref.contains("- clear_inference_notes: always available"),
                 "clear_inference_notes must appear even with empty policy");
        assertFalse(ref.contains("- **fallback when add_reasoning_note is unavailable**"));
    }

    @Test
    void hidesInferenceNotesAndShowsFallbackWhenDisabled() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false, false);

        assertFalse(ref.contains("add_reasoning_note: always available"),
                 "add_reasoning_note must not appear in permissions when disabled");
        assertFalse(ref.contains("clear_inference_notes: always available"),
                 "clear_inference_notes must not appear in permissions when disabled");
        assertFalse(ref.contains("- **add_reasoning_note**:"),
                 "add_reasoning_note details must not appear when disabled");
        assertFalse(ref.contains("- **clear_inference_notes**:"),
                 "clear_inference_notes details must not appear when disabled");
        assertTrue(ref.contains("- **fallback when add_reasoning_note is unavailable**:"),
                 "fallback instructions must appear when disabled");
    }

    @Test
    void showsInferenceNotesAndHidesFallbackWhenEnabledExplicilty() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false, true);

        assertTrue(ref.contains("- add_reasoning_note: always available"),
                 "add_reasoning_note must appear in permissions when enabled");
        assertTrue(ref.contains("- clear_inference_notes: always available"),
                 "clear_inference_notes must appear in permissions when enabled");
        assertTrue(ref.contains("- **add_reasoning_note**:"),
                 "add_reasoning_note details must appear when enabled");
        assertTrue(ref.contains("- **clear_inference_notes**:"),
                 "clear_inference_notes details must appear when enabled");
        assertFalse(ref.contains("- **fallback when add_reasoning_note is unavailable**:"),
                 "fallback instructions must not appear when enabled");
    }

    
    
    

    @Test
    void runScriptAbsentWhenFlagsAreFalse() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, false, true, true, false);

        assertFalse(ref.contains("- run_script: always available"),
                 "run_script must not appear when scriptRunnerEnabled and scriptRunEnabled are false");
    }

    @Test
    void runScriptPresentWhenBothFlagsTrue() {
        FilePolicy policy = FilePolicy.allPermissive();
        String ref = ToolOperationsReference.build(policy, true, true, true, true);

        assertTrue(ref.contains("- run_script: always available"),
                 "run_script must appear when both scriptRunnerEnabled and scriptRunEnabled are true");
    }

    
    
    

    @Test
    void nullPolicyThrows() {
        assertThrows(NullPointerException.class,
                () -> ToolOperationsReference.build(null, false, true, true, false),
                "build() with null policy must throw NullPointerException");
    }
}
