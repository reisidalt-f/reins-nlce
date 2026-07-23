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

package br.com.dizeno.reins.compilation.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceTrackingRecordTest {

    @Test
    void deserializeWithoutNotesFieldDefaultsToEmptyList() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  \"sourcePath\": \"src/main/nl/domain/entity.md\",
                  \"sourceCategory\": \"main\",
                  \"sourceHash\": \"abc123\",
                  \"lastStatus\": \"compiled\"
                }
                """;

        SourceTrackingRecord record = mapper.readValue(json, SourceTrackingRecord.class);

        assertNotNull(record.getNotes(), "notes should default to a non-null list");
        assertTrue(record.getNotes().isEmpty(), "notes should default to empty when missing in legacy JSON");
    }
}
