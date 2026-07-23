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

package br.com.dizeno.reins.reasoning.tooling.file;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 
public class FileComposedViewFixture {

    private final Path rootDir;
    private final Map<String, Path> basePaths;

    public FileComposedViewFixture(Path tempDir) {
        this.rootDir = tempDir;
        this.basePaths = new HashMap<>();

        
        this.basePaths.put("main-source", rootDir.resolve("main-source"));
        this.basePaths.put("test-source", rootDir.resolve("test-source"));
        this.basePaths.put("main-target", rootDir.resolve("main-target"));
        this.basePaths.put("test-target", rootDir.resolve("test-target"));

        
        basePaths.values().forEach(path -> {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create base directory: " + path, e);
            }
        });
    }

     
    public void writeFile(String base, String relativePath, String content) {
        Path basePath = basePaths.get(base);
        if (basePath == null) {
            throw new IllegalArgumentException("Unknown base: " + base + ". Available: " + basePaths.keySet());
        }

        Path filePath = basePath.resolve(relativePath);
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file: " + filePath, e);
        }
    }

     
    public void writeSourceFile(String base, String relativePath, String content) {
        writeFile(base, relativePath, content);
    }

     
    public void writeTargetFile(String base, String relativePath, String content) {
        writeFile(base, relativePath, content);
    }

     
    public String readFile(String base, String relativePath) {
        Path basePath = basePaths.get(base);
        if (basePath == null) {
            throw new IllegalArgumentException("Unknown base: " + base);
        }

        Path filePath = basePath.resolve(relativePath);
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

     
    public List<String> listFiles(String base) {
        Path basePath = basePaths.get(base);
        if (basePath == null) {
            throw new IllegalArgumentException("Unknown base: " + base);
        }

        List<String> files = new ArrayList<>();
        try {
            Files.walk(basePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String relativePath = basePath.relativize(path).toString();
                    files.add(relativePath);
                });
        } catch (Exception e) {
            throw new RuntimeException("Failed to list files in: " + basePath, e);
        }
        return files;
    }

     
    public Path getBasePath(String base) {
        Path basePath = basePaths.get(base);
        if (basePath == null) {
            throw new IllegalArgumentException("Unknown base: " + base);
        }
        return basePath;
    }

     
    public Path getRootDir() {
        return rootDir;
    }

     
    public br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver createResolver() {
        
        
        throw new UnsupportedOperationException(
            "Concrete resolver creation depends on br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver API implementation. " +
            "Use getBasePath(base) to manually configure resolver mappings.");
    }

     
    public boolean fileExists(String base, String relativePath) {
        try {
            return Files.isRegularFile(getBasePath(base).resolve(relativePath));
        } catch (Exception e) {
            return false;
        }
    }

     
    public void clearBase(String base) {
        Path basePath = getBasePath(base);
        try {
            Files.walk(basePath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        
                    }
                });
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear base: " + base, e);
        }
    }
}
