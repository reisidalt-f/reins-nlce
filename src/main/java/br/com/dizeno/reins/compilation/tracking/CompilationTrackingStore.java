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
import br.com.dizeno.reins.source.domain.ProjectDirectoryPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

 
/**
 * CompilationTrackingStore is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a component managing compilation tracking store.
 */
public class CompilationTrackingStore {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new instance of {@link CompilationTrackingStore}.
     */
    public CompilationTrackingStore() {
        this.objectMapper = ManifestJsonMapper.getInstance();
    }

    
    
    

     
    /**
     * Tracking File Path.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @return the resolved or constructed object
     */
    public Path trackingFilePath(Path projectRoot, String sourcePath) {
        return TrackedPathResolver.trackingFilePath(projectRoot, sourcePath);
    }

     
    /**
     * Loads the resource or context.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @return an optional containing the value, or empty if none
     */
    public Optional<SourceTrackingRecord> load(Path projectRoot, String sourcePath) throws IOException {
        Path file = trackingFilePath(projectRoot, sourcePath);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        SourceTrackingRecord record = objectMapper.readValue(file.toFile(), SourceTrackingRecord.class);
        return Optional.of(record);
    }

     
    /**
     * Saves the resource or context.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     * @param record the tracking record
     */
    public void save(Path projectRoot, String sourcePath, SourceTrackingRecord record) throws IOException {
        Path dest = trackingFilePath(projectRoot, sourcePath);
        Files.createDirectories(dest.getParent());
        
        Path tmp = dest.resolveSibling(dest.getFileName() + ".tmp");
        try {
            objectMapper.writeValue(tmp.toFile(), record);
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                
            }
            throw ex;
        }
    }

     
    /**
     * Deletes the target.
     *
     * @param projectRoot the root path of the project
     * @param sourcePath the path of the source file
     */
    public void delete(Path projectRoot, String sourcePath) throws IOException {
        Path file = trackingFilePath(projectRoot, sourcePath);
        Files.deleteIfExists(file);
    }

     
    /**
     * List All Tracked Source Paths.
     *
     * @param projectRoot the root path of the project
     * @return the string result
     */
    public List<String> listAllTrackedSourcePaths(Path projectRoot) throws IOException {
        Path trackingDir = projectRoot.resolve(ProjectDirectoryPaths.COMPILATION_TRACKING_DIR).normalize();
        if (!Files.exists(trackingDir)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(trackingDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> result.add(TrackedPathResolver.sourcePathFromTrackingFile(trackingDir, p)));
        }
        result.sort(String::compareTo);
        return result;
    }

     
    /**
     * Find Owner Source.
     *
     * @param projectRoot the root path of the project
     * @param canonicalCompiledPath the canonical compiled path
     * @return the string result
     */
    public Optional<String> findOwnerSource(Path projectRoot, String canonicalCompiledPath) throws IOException {
        List<String> trackedSources = listAllTrackedSourcePaths(projectRoot);
        for (String sourcePath : trackedSources) {
            Optional<SourceTrackingRecord> record = load(projectRoot, sourcePath);
            if (record.isPresent()) {
                for (String compiledPath : record.get().getCompiledFiles().keySet()) {
                    if (canonicalizePath(compiledPath).equals(canonicalCompiledPath)) {
                        return Optional.of(canonicalizePath(sourcePath));
                    }
                }
            }
        }
        return Optional.empty();
    }

     
    /**
     * Canonicalize Path.
     *
     * @param sourcePath the path of the source file
     * @return the string result
     */
    public String canonicalizePath(String sourcePath) {
        return TrackedPathResolver.canonicalizeSourcePath(sourcePath);
    }
}
