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

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

 
class RecompilationDeciderComprehensiveTest {

    @TempDir
    Path projectDir;

    private final RecompilationDecider decider = new RecompilationDecider();

    
    
    

     
    private SourceTrackingRecord baseRecord(String sourcePath) {
        SourceTrackingRecord r = new SourceTrackingRecord();
        r.setSourcePath(sourcePath);
        r.setSourceHash("hash1");
        r.setBlockFingerprints(List.of("fp1"));
        r.setSourceModificationTime(1000L);
        r.setCompiledFiles(new LinkedHashMap<>());
        r.setMarkdownReferences(new LinkedHashMap<>());
        return r;
    }

    private ReprocessingDecision eval(SourceTrackingRecord record,
                                      String hash,
                                      List<String> fingerprints,
                                      List<String> expectedOutputs,
                                      long sourceMtime,
                                      Set<String> changedChildren,
                                      String resolvedTargetRoot,
                                      RecompilationDecider.EvaluationInputs inputs,
                                      RecompileOnSettings settings) {
        return decider.evaluate(
                record == null ? "src/main/nl/Foo.md" : record.getSourcePath(),
                record,
                hash,
                fingerprints,
                expectedOutputs,
                projectDir,
                sourceMtime,
                changedChildren,
                resolvedTargetRoot,
                inputs,
                settings
        );
    }

     
    private ReprocessingDecision evalSimple(SourceTrackingRecord record,
                                            String hash,
                                            List<String> fingerprints,
                                            List<String> expectedOutputs) {
        return eval(record, hash, fingerprints, expectedOutputs,
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);
    }

    private RecompileOnSettings settings(boolean md,
                                                                boolean gen,
                                                                boolean insp) {
        RecompileOnSettings s = new RecompileOnSettings();
        s.setMarkdownReferences(md);
        s.setCompiledFiles(gen);
        s.setInspectedFiles(insp);
        return s;
    }

    private RecompilationDecider.EvaluationInputs inputsWithMarkdownRefs(Map<String, Long> mdMtimes) {
        return new RecompilationDecider.EvaluationInputs(
                mdMtimes, Map.of(), Map.of(), Set.of(), Map.of(), Set.of(), Map.of(), Set.of());
    }

    private RecompilationDecider.EvaluationInputs inputsWithCompiledFiles(Map<String, Long> genMtimes) {
        return new RecompilationDecider.EvaluationInputs(
                Map.of(), genMtimes, Map.of(), Set.of(), Map.of(), Set.of(), Map.of(), Set.of());
    }

    private RecompilationDecider.EvaluationInputs inputsWithInspectedFiles(Map<String, Long> inspMtimes) {
        return new RecompilationDecider.EvaluationInputs(
                Map.of(), Map.of(), inspMtimes, Set.of(), Map.of(), Set.of(), Map.of(), Set.of());
    }

     
    private RecompilationDecider.EvaluationInputs inputsWithMissingPath(String path, long recordedMtime) {
        Map<String, Long> mdMtimes = new HashMap<>();
        mdMtimes.put(path, recordedMtime);
        return new RecompilationDecider.EvaluationInputs(
                mdMtimes, Map.of(), Map.of(), Set.of(), Map.of(), Set.of(), Map.of(), Set.of(path));
    }

    private FileTrackingDetails details(long mtime) {
        return new FileTrackingDetails(null, null, mtime);
    }

    private Path writeFile(String relativePath, String content) throws IOException {
        Path file = projectDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    
    
    

    @Test
    void evaluate_nullRecord_returnsNoPriorRecord() {
        ReprocessingDecision d = evalSimple(null, "hash1", List.of("fp1"), List.of());

        assertEquals(ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD, d.reason());
        assertTrue(d.shouldReprocess());
    }

    @Test
    void shouldRecompile_nullRecord_returnsTrue() {
        assertTrue(decider.shouldRecompile(null, "hash1", List.of("fp1"), List.of()));
    }

    
    
    

    @Test
    void evaluate_sourceHashDiffers_returnsSourceHashChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md"); 

        ReprocessingDecision d = evalSimple(r, "hash2", List.of("fp1"), List.of());

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED, d.reason());
        assertTrue(d.shouldReprocess());
    }

    @Test
    void evaluate_blockFingerprintsDiffer_returnsSourceHashChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md"); 

        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp2"), List.of());

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED, d.reason());
    }

    @Test
    void evaluate_extraBlockFingerprint_returnsSourceHashChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");

        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1", "fp2"), List.of());

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_HASH_CHANGED, d.reason());
    }

    @Test
    void shouldRecompile_hashMatches_returnsFalse() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");

        assertFalse(decider.shouldRecompile(r, "hash1", List.of("fp1"), List.of()));
    }

    @Test
    void shouldRecompile_hashMismatch_returnsTrue() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");

        assertTrue(decider.shouldRecompile(r, "different", List.of("fp1"), List.of()));
    }

    
    
    

    @Test
    void evaluate_expectedOutputNotInRecord_returnsOutputPathsChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md"); 

        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Foo.java"));

        assertEquals(ReprocessingDecision.ReprocessingReason.OUTPUT_PATHS_CHANGED, d.reason());
    }

    @Test
    void evaluate_recordHasExtraOutput_returnsOutputPathsChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/Foo.java", details(500L));
        files.put("src/main/java/Bar.java", details(500L));
        r.setCompiledFiles(files);

        
        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Foo.java"));

        assertEquals(ReprocessingDecision.ReprocessingReason.OUTPUT_PATHS_CHANGED, d.reason());
    }

    @Test
    void evaluate_outputPathOrderDiffers_returnsOutputPathsChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/A.java", details(500L));
        files.put("src/main/java/B.java", details(500L));
        r.setCompiledFiles(files);

        
        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"),
                List.of("src/main/java/B.java", "src/main/java/A.java"));

        assertEquals(ReprocessingDecision.ReprocessingReason.OUTPUT_PATHS_CHANGED, d.reason());
    }

    
    
    

    @Test
    void evaluate_targetRootDiffers_returnsTargetRootChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setResolvedTargetRoot("src/main/java");

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), "src/test/java",
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, d.reason());
    }

    @Test
    void evaluate_storedTargetRootNull_doesNotTriggerTargetRootChanged() {
        
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setResolvedTargetRoot(null);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), "src/main/java",
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, d.reason());
    }

    @Test
    void evaluate_currentTargetRootNull_doesNotTriggerTargetRootChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setResolvedTargetRoot("src/main/java");

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.TARGET_ROOT_CHANGED, d.reason());
    }

    
    
    

    @Test
    void evaluate_outputFileAbsent_returnsOutputMissing() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/Missing.java", details(500L));
        r.setCompiledFiles(files);

        
        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Missing.java"));

        assertEquals(ReprocessingDecision.ReprocessingReason.OUTPUT_MISSING, d.reason());
    }

    @Test
    void evaluate_outputFilePresent_doesNotReturnOutputMissing() throws IOException {
        Path output = writeFile("src/main/java/Present.java", "class Present {}");
        long outputMtime = Files.getLastModifiedTime(output).toMillis();

        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(outputMtime - 1000);
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/Present.java", details(outputMtime - 1000));
        r.setCompiledFiles(files);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Present.java"),
                outputMtime - 1000, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.OUTPUT_MISSING, d.reason());
    }

    
    
    

    @Test
    void evaluate_sourceMtimeNewerThanOutput_returnsSourceNewerThanOutputs() throws IOException {
        Path output = writeFile("src/main/java/Stale.java", "class Stale {}");
        long outputMtime = Files.getLastModifiedTime(output).toMillis();
        long sourceMtime = outputMtime + 5000;

        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(outputMtime - 1000); 
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/Stale.java", details(outputMtime));
        r.setCompiledFiles(files);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Stale.java"),
                sourceMtime, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_NEWER_THAN_OUTPUTS, d.reason());
    }

    @Test
    void evaluate_sourceMtimeUnavailable_doesNotReturnSourceNewerThanOutputs() throws IOException {
        Path output = writeFile("src/main/java/Fresh.java", "class Fresh {}");

        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        Map<String, FileTrackingDetails> files = new LinkedHashMap<>();
        files.put("src/main/java/Fresh.java", details(0L));
        r.setCompiledFiles(files);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"),
                List.of("src/main/java/Fresh.java"),
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_NEWER_THAN_OUTPUTS, d.reason());
    }

    
    
    

    @Test
    void evaluate_changedChildInMarkdownRefs_returnsChildOutputChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Parent.md");
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/main/nl/Child.md", new FileTrackingDetails("src/main/nl/Child.md", null, null));
        r.setMarkdownReferences(mdRefs);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE,
                Set.of("src/main/nl/Child.md"), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertEquals(ReprocessingDecision.ReprocessingReason.CHILD_OUTPUT_CHANGED, d.reason());
    }

    @Test
    void evaluate_changedChildNotInMarkdownRefs_doesNotReturnChildOutputChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Parent.md");

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE,
                Set.of("src/main/nl/Unrelated.md"), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
    }

    
    
    

    @Test
    void evaluate_allConditionsMatch_returnsSkipUpToDate() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");

        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"), List.of());

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
        assertFalse(d.shouldReprocess());
    }

    @Test
    void evaluate_emptyExtendedInputs_skipsTimestampAndDependencyChecks() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1L);

        
        
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                9999L, Set.of(), null,
                RecompilationDecider.EvaluationInputs.empty(), null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
    }

    
    
    

    @Test
    void evaluate_sourceMtimeAdvancedBeyondRecord_returnsSourceRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("any.md", 500L));

        
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                2000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_recordedSourceMtimeNull_returnsSourceRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(null);
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("any.md", 500L));

        
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_mtimeUnavailable_doesNotTriggerSourceRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 500L));

        
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(), null, inputs, null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_sourceMtimeEqualToRecord_doesNotReturnSourceRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 500L));

        
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    
    
    

    @Test
    void evaluate_markdownRefSetGrew_returnsSourceDependencySetChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(
                Map.of("dep.md", 500L, "new.md", 600L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_markdownRefSetShrunk_returnsSourceDependencySetChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("a.md", details(500L));
        mdRefs.put("b.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("a.md", 500L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_markdownRefDisabled_ignoresSetChange() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("other.md", 500L));

        RecompileOnSettings s = settings(false, false, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, s);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_compiledFileSetChanged_withSettingEnabled_returnsSetChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> genFiles = new LinkedHashMap<>();
        genFiles.put("out/Old.java", details(500L));
        r.setCompiledFiles(genFiles);

        
        RecompilationDecider.EvaluationInputs inputs = inputsWithCompiledFiles(
                Map.of("out/Old.java", 500L, "out/Extra.java", 600L));

        RecompileOnSettings s = settings(false, true, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of("out/Old.java"),
                1000L, Set.of(), null, inputs, s);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_compiledFileSetChanged_withSettingDisabled_ignoresChange() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> genFiles = new LinkedHashMap<>();
        genFiles.put("out/Old.java", details(500L));
        r.setCompiledFiles(genFiles);

        RecompilationDecider.EvaluationInputs inputs = inputsWithCompiledFiles(
                Map.of("out/Old.java", 500L, "out/Extra.java", 600L));

        RecompileOnSettings s = settings(false, false, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of("out/Old.java"),
                1000L, Set.of(), null, inputs, s);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_inspectedFileSetChanged_withSettingEnabled_returnsSetChanged() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> inspFiles = new LinkedHashMap<>();
        inspFiles.put("src/main/java/A.java", details(500L));
        r.setInspectedFiles(inspFiles);

        RecompilationDecider.EvaluationInputs inputs = inputsWithInspectedFiles(
                Map.of("src/main/java/A.java", 500L, "src/main/java/B.java", 600L));

        RecompileOnSettings s = settings(false, false, true);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, s);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_inspectedFileSetChanged_withSettingDisabled_ignoresChange() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> inspFiles = new LinkedHashMap<>();
        inspFiles.put("src/main/java/A.java", details(500L));
        r.setInspectedFiles(inspFiles);

        RecompilationDecider.EvaluationInputs inputs = inputsWithInspectedFiles(
                Map.of("src/main/java/A.java", 500L, "src/main/java/B.java", 600L));

        RecompileOnSettings s = settings(false, false, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, s);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_SET_CHANGED, d.reason());
    }

    
    
    

    @Test
    void evaluate_markdownRefMtimeAdvanced_returnsSourceDependencyTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 999L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_markdownRefMtimeEqual_doesNotReturnTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 500L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
    }

    @Test
    void evaluate_compiledFileMtimeAdvanced_withSettingEnabled_returnsTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> genFiles = new LinkedHashMap<>();
        genFiles.put("out/Foo.java", details(500L));
        r.setCompiledFiles(genFiles);
        RecompilationDecider.EvaluationInputs inputs = inputsWithCompiledFiles(Map.of("out/Foo.java", 999L));

        RecompileOnSettings s = settings(false, true, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of("out/Foo.java"),
                1000L, Set.of(), null, inputs, s);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_compiledFileMtimeAdvanced_withSettingDisabled_ignoresMtimeChange() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> genFiles = new LinkedHashMap<>();
        genFiles.put("out/Foo.java", details(500L));
        r.setCompiledFiles(genFiles);
        RecompilationDecider.EvaluationInputs inputs = inputsWithCompiledFiles(Map.of("out/Foo.java", 999L));

        RecompileOnSettings s = settings(false, false, false);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of("out/Foo.java"),
                1000L, Set.of(), null, inputs, s);

        assertNotEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_inspectedFileMtimeAdvanced_withSettingEnabled_returnsTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> inspFiles = new LinkedHashMap<>();
        inspFiles.put("src/Resource.java", details(500L));
        r.setInspectedFiles(inspFiles);
        RecompilationDecider.EvaluationInputs inputs = inputsWithInspectedFiles(
                Map.of("src/Resource.java", 999L));

        RecompileOnSettings s = settings(false, false, true);
        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, s);

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED, d.reason());
    }

    
    
    

    @Test
    void evaluate_markdownRefRecordedMtimeNull_returnsTrackedFileMtimeMissing() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        
        mdRefs.put("dep.md", new FileTrackingDetails("dep.md", null, null));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 500L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MTIME_MISSING, d.reason());
    }

    
    
    

    @Test
    void evaluate_markdownRefInMissingOrUnreadablePaths_returnsTrackedFileMissingOrUnreadable() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMissingPath("dep.md", 500L);

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MISSING_OR_UNREADABLE, d.reason());
    }

    @Test
    void evaluate_markdownRefCurrentMtimeNullValue_returnsTrackedFileMissingOrUnreadable() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);

        
        Map<String, Long> nullValueMap = new HashMap<>();
        nullValueMap.put("dep.md", null);
        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                nullValueMap, Map.of(), Map.of(), Set.of(), Map.of(), Set.of(), Map.of(), Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.TRACKED_FILE_MISSING_OR_UNREADABLE, d.reason());
    }

    
    
    

    @Test
    void evaluate_projectRecord_markdownSourceSetGrew_returnsProjectTrackedSetChanged() {
        SourceTrackingRecord r = baseRecord("sources/all.md");
        r.setSourceCategory("project");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/A.md", details(500L));
        r.setMarkdownReferences(mdRefs);

        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("src/A.md", 500L), Map.of(), Map.of(),
                Set.of("src/A.md", "src/B.md"), 
                Map.of("src/A.md", 500L, "src/B.md", 600L),
                Set.of(), Map.of(), Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.PROJECT_TRACKED_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_projectRecord_inspectedFileSetGrew_returnsProjectTrackedSetChanged() {
        SourceTrackingRecord r = baseRecord("sources/all.md");
        r.setSourceCategory("project");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/A.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        Map<String, FileTrackingDetails> inspFiles = new LinkedHashMap<>();
        inspFiles.put("pkg/Existing.java", details(500L));
        r.setInspectedFiles(inspFiles);

        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("src/A.md", 500L), Map.of(), Map.of(),
                Set.of("src/A.md"),
                Map.of("src/A.md", 500L),
                Set.of("pkg/Existing.java", "pkg/New.java"), 
                Map.of("pkg/Existing.java", 500L, "pkg/New.java", 600L),
                Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.PROJECT_TRACKED_SET_CHANGED, d.reason());
    }

    @Test
    void evaluate_nonProjectRecord_differentProjectSet_doesNotTriggerProjectChecks() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceCategory("regular"); 
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);

        
        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("dep.md", 500L), Map.of(), Map.of(),
                Set.of("dep.md", "extra.md"),
                Map.of("dep.md", 500L),
                Set.of(), Map.of(), Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
    }

    
    
    

    @Test
    void evaluate_projectRecord_sourceMtimeAdvanced_returnsProjectRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("sources/all.md");
        r.setSourceCategory("project");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/A.md", details(500L));
        r.setMarkdownReferences(mdRefs);

        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("src/A.md", 500L), Map.of(), Map.of(),
                Set.of("src/A.md"),
                Map.of("src/A.md", 999L), 
                Set.of(), Map.of(), Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.PROJECT_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_projectRecord_inspectedFileMtimeAdvanced_returnsProjectRecordTimestampAdvanced() {
        SourceTrackingRecord r = baseRecord("sources/all.md");
        r.setSourceCategory("project");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/A.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        Map<String, FileTrackingDetails> inspFiles = new LinkedHashMap<>();
        inspFiles.put("pkg/Res.java", details(500L));
        r.setInspectedFiles(inspFiles);

        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("src/A.md", 500L), Map.of(), Map.of(),
                Set.of("src/A.md"),
                Map.of("src/A.md", 500L), 
                Set.of("pkg/Res.java"),
                Map.of("pkg/Res.java", 999L), 
                Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.PROJECT_RECORD_TIMESTAMP_ADVANCED, d.reason());
    }

    @Test
    void evaluate_projectRecord_allMtimesUnchanged_returnsSkipUpToDate() {
        SourceTrackingRecord r = baseRecord("sources/all.md");
        r.setSourceCategory("project");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("src/A.md", details(500L));
        r.setMarkdownReferences(mdRefs);

        RecompilationDecider.EvaluationInputs inputs = new RecompilationDecider.EvaluationInputs(
                Map.of("src/A.md", 500L), Map.of(), Map.of(),
                Set.of("src/A.md"),
                Map.of("src/A.md", 500L), 
                Set.of(), Map.of(), Set.of());

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null);

        assertEquals(ReprocessingDecision.ReprocessingReason.SKIP_UP_TO_DATE, d.reason());
    }

    
    
    

    @Test
    void evaluate_nullRecompileOn_usesDefaultsAndDetectsMarkdownRefChange() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Foo.md");
        r.setSourceModificationTime(1000L);
        Map<String, FileTrackingDetails> mdRefs = new LinkedHashMap<>();
        mdRefs.put("dep.md", details(500L));
        r.setMarkdownReferences(mdRefs);
        
        RecompilationDecider.EvaluationInputs inputs = inputsWithMarkdownRefs(Map.of("dep.md", 999L));

        ReprocessingDecision d = eval(r, "hash1", List.of("fp1"), List.of(),
                1000L, Set.of(), null, inputs, null  );

        assertEquals(ReprocessingDecision.ReprocessingReason.SOURCE_DEPENDENCY_TIMESTAMP_ADVANCED, d.reason());
    }

    
    
    

    @Test
    void evaluate_sourcePathPropagatedToDecision() {
        SourceTrackingRecord r = baseRecord("src/main/nl/Service.md");

        ReprocessingDecision d = evalSimple(r, "hash1", List.of("fp1"), List.of());

        assertEquals("src/main/nl/Service.md", d.sourcePath());
    }

    @Test
    void evaluate_nullRecord_sourcePathFromFirstArg() {
        ReprocessingDecision d = decider.evaluate(
                "explicit/path.md", null,
                "hash1", List.of("fp1"), List.of(),
                projectDir,
                RecompilationDecider.MTIME_UNAVAILABLE, Set.of(),
                null,
                RecompilationDecider.EvaluationInputs.empty(),
                null);

        assertEquals("explicit/path.md", d.sourcePath());
        assertEquals(ReprocessingDecision.ReprocessingReason.NO_PRIOR_RECORD, d.reason());
    }
}
