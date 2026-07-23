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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompilationTrackingStoreTest {

    @TempDir
    Path projectRoot;

    private final CompilationTrackingStore store = new CompilationTrackingStore();

    
    
    

    @Test
    void trackingFilePath_derivesExpectedLocation() {
        Path result = store.trackingFilePath(projectRoot, "src/main/nl/Foo.md");
        Path expected = projectRoot.resolve(".reins/compilation-tracking/main/Foo.md.json").normalize();
        assertEquals(expected, result);
    }

    @Test
    void trackingFilePath_normalizesBackslashes() {
        Path result = store.trackingFilePath(projectRoot, "src\\main\\nl\\Foo.md");
        Path expected = projectRoot.resolve(".reins/compilation-tracking/main/Foo.md.json").normalize();
        assertEquals(expected, result);
    }

    @Test
    void trackingFilePath_stripsLeadingDotSlash() {
        Path result = store.trackingFilePath(projectRoot, "./src/main/nl/Foo.md");
        Path expected = projectRoot.resolve(".reins/compilation-tracking/main/Foo.md.json").normalize();
        assertEquals(expected, result);
    }

    @Test
    void trackingFilePath_rejectsDotDotSegment() {
        assertThrows(IllegalArgumentException.class,
                () -> store.trackingFilePath(projectRoot, "src/../etc/passwd"));
    }

    @Test
    void trackingFilePath_rejectsDotDotLeading() {
        assertThrows(IllegalArgumentException.class,
                () -> store.trackingFilePath(projectRoot, "../etc/passwd"));
    }

    
    
    

    @Test
    void load_returnsEmptyWhenFileAbsent() throws IOException {
        Optional<SourceTrackingRecord> result = store.load(projectRoot, "src/main/nl/Missing.md");
        assertTrue(result.isEmpty());
    }

    
    
    

    @Test
    void saveAndLoad_roundtrips() throws IOException {
        SourceTrackingRecord record = buildRecord("main:Foo.md", "abc123", "success");

        store.save(projectRoot, "src/main/nl/Foo.md", record);

        Optional<SourceTrackingRecord> loaded = store.load(projectRoot, "src/main/nl/Foo.md");
        assertTrue(loaded.isPresent());
        assertEquals("main:Foo.md", loaded.get().getSourcePath());
        assertEquals("abc123", loaded.get().getSourceHash());
        assertEquals("success", loaded.get().getLastStatus());
    }

    @Test
    void save_createsParentDirectories() throws IOException {
        String sourcePath = "deeply/nested/dir/Source.md";
        store.save(projectRoot, sourcePath, buildRecord(sourcePath, "hash1", "success"));

        assertTrue(Files.exists(store.trackingFilePath(projectRoot, sourcePath)));
    }

    @Test
    void save_overwritesPreviousRecord() throws IOException {
        store.save(projectRoot, "src/main/nl/Foo.md", buildRecord("main:Foo.md", "v1", "success"));
        store.save(projectRoot, "src/main/nl/Foo.md", buildRecord("main:Foo.md", "v2", "failed"));

        Optional<SourceTrackingRecord> loaded = store.load(projectRoot, "src/main/nl/Foo.md");
        assertTrue(loaded.isPresent());
        assertEquals("v2", loaded.get().getSourceHash());
        assertEquals("failed", loaded.get().getLastStatus());
    }

    
    
    

    @Test
    void delete_removesFile() throws IOException {
        store.save(projectRoot, "src/main/nl/Foo.md", buildRecord("main:Foo.md", "h", "success"));
        assertTrue(Files.exists(store.trackingFilePath(projectRoot, "src/main/nl/Foo.md")));

        store.delete(projectRoot, "src/main/nl/Foo.md");
        assertFalse(Files.exists(store.trackingFilePath(projectRoot, "src/main/nl/Foo.md")));
    }

    @Test
    void delete_isNoOpWhenFileAbsent() {
        assertDoesNotThrow(() -> store.delete(projectRoot, "src/main/nl/NonExistent.md"));
    }

    
    
    

    @Test
    void listAllTrackedSourcePaths_returnsEmptyWhenNone() throws IOException {
        List<String> paths = store.listAllTrackedSourcePaths(projectRoot);
        assertTrue(paths.isEmpty());
    }

    @Test
    void listAllTrackedSourcePaths_returnsAllStoredPaths() throws IOException {
        store.save(projectRoot, "src/main/nl/A.md", buildRecord("main:A.md", "h1", "success"));
        store.save(projectRoot, "src/main/nl/B.md", buildRecord("main:B.md", "h2", "success"));
        store.save(projectRoot, "src/test/nl/C.md", buildRecord("test:C.md", "h3", "success"));

        List<String> paths = store.listAllTrackedSourcePaths(projectRoot);
        assertEquals(3, paths.size());
        assertTrue(paths.contains("main:A.md"));
        assertTrue(paths.contains("main:B.md"));
        assertTrue(paths.contains("test:C.md"));
    }

    @Test
    void listAllTrackedSourcePaths_excludesDeletedPaths() throws IOException {
        store.save(projectRoot, "src/main/nl/A.md", buildRecord("main:A.md", "h1", "success"));
        store.save(projectRoot, "src/main/nl/B.md", buildRecord("main:B.md", "h2", "success"));
        store.delete(projectRoot, "src/main/nl/A.md");

        List<String> paths = store.listAllTrackedSourcePaths(projectRoot);
        assertEquals(1, paths.size());
        assertEquals("main:B.md", paths.get(0));
    }

    
    
    

    @Test
    void canonicalizePath_normalizesLeadingDotSlash() {
        assertEquals("main:Foo.md", store.canonicalizePath("./src/main/nl/Foo.md"));
    }

    @Test
    void canonicalizePath_normalizesBackslashes() {
        assertEquals("main:Foo.md", store.canonicalizePath("src\\main\\nl\\Foo.md"));
    }

    @Test
    void canonicalizePath_rejectsDotDot() {
        assertThrows(IllegalArgumentException.class, () -> store.canonicalizePath("src/../evil"));
    }

    @Test
    void canonicalizePath_producesEquivalentCanonicalIdentityAcrossPathVariants() {
        String canonical = store.canonicalizePath("main:Foo.md");

        assertEquals(canonical, store.canonicalizePath("./src/main/nl/Foo.md"));
        assertEquals(canonical, store.canonicalizePath("src\\main\\nl\\Foo.md"));
    }

    
    
    

    private SourceTrackingRecord buildRecord(String sourcePath, String hash, String status) {
        SourceTrackingRecord r = new SourceTrackingRecord();
        r.setSourcePath(sourcePath);
        r.setSourceHash(hash);
        r.setLastStatus(status);
        r.setCompiledFiles(new LinkedHashMap<>());
        return r;
    }
}
