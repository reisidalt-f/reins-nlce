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

import br.com.dizeno.reins.compilation.tracking.FileTrackingDetails;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.testutil.RecordingLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EagerlyProvideServiceLogToggleTest {

    @TempDir
    Path projectRoot;

    @Test
    void doesNotLogAttachMessageWhenEagerlyProvidedToggleIsDisabled() throws Exception {
        EagerlyProvideService service = new EagerlyProvideService();
        SourceTrackingRecord record = createRecordWithCompiledFile();
        EagerlyProvideSettings settings = new EagerlyProvideSettings();
        settings.setPreviouslyCompiledFiles(true);
        settings.setPreviouslyInspectedFiles(false);
        RecordingLog log = new RecordingLog();

        EagerlyProvideResult result = service.build(record, settings, false, projectRoot, new PathValidator(projectRoot), log);

        assertEquals(1, result.getCompiledAttachments().size());
        assertEquals(1, result.getCompiledSourceGroups().size());
        assertEquals("main:fixture.txt", result.getCompiledSourceGroups().get(0).getSourceCanonicalPath());
        assertFalse(log.hasInfoContaining("Eagerly provide: attaching [main] src/main/nl/fixture.txt"));
    }

    @Test
    void logsAttachMessageWhenEagerlyProvidedToggleIsEnabled() throws Exception {
        EagerlyProvideService service = new EagerlyProvideService();
        SourceTrackingRecord record = createRecordWithCompiledFile();
        EagerlyProvideSettings settings = new EagerlyProvideSettings();
        settings.setPreviouslyCompiledFiles(true);
        settings.setPreviouslyInspectedFiles(false);
        RecordingLog log = new RecordingLog();

        EagerlyProvideResult result = service.build(record, settings, true, projectRoot, new PathValidator(projectRoot), log);

        assertEquals(1, result.getCompiledAttachments().size());
        assertEquals(1, result.getCompiledSourceGroups().size());
        assertTrue(log.hasInfoContaining("Eagerly provide: attaching [main] src/main/nl/fixture.txt"));
    }

    private SourceTrackingRecord createRecordWithCompiledFile() throws Exception {
        Path file = projectRoot.resolve("src/main/nl/fixture.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fixture-content");

        SourceTrackingRecord record = new SourceTrackingRecord();
        record.setSourcePath("main:fixture.txt");
        LinkedHashMap<String, FileTrackingDetails> compiled = new LinkedHashMap<>();
        compiled.put("main:fixture.txt", new FileTrackingDetails("main:fixture.txt", "main", Files.getLastModifiedTime(file).toMillis()));
        record.setCompiledFiles(compiled);
        return record;
    }
}
