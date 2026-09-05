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

package br.com.dizeno.reins.compilation.context;

import org.codehaus.plexus.util.SelectorUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * CompilationBackgroundFile is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
 * Acts as a component managing compilation background file.
 */
public class CompilationBackgroundFile {
    private final Path absolutePath;
    private final String displayPath;
    private final String content;
    private final String filePattern;
    private final List<String> phasePatterns;

    /**
     * Constructs a new instance of {@link CompilationBackgroundFile}.
     *
     * @param absolutePath the absolute path
     * @param displayPath the display path
     * @param content the content
     */
    public CompilationBackgroundFile(Path absolutePath, String displayPath, String content) {
        this(absolutePath, displayPath, content, "**/*.md", List.of("*"));
    }

    /**
     * Constructs a new instance of {@link CompilationBackgroundFile}.
     *
     * @param absolutePath the absolute path
     * @param displayPath the display path
     * @param content the content
     * @param filePattern the file pattern for target processed files
     * @param phasePatterns the phase patterns for reasoning phases
     */
    public CompilationBackgroundFile(Path absolutePath,
                                   String displayPath,
                                   String content,
                                   String filePattern,
                                   List<String> phasePatterns) {
        this.absolutePath = absolutePath;
        this.displayPath = displayPath;
        this.content = content;
        this.filePattern = filePattern != null && !filePattern.isBlank() ? filePattern.trim() : "**/*.md";
        this.phasePatterns = phasePatterns != null && !phasePatterns.isEmpty()
                ? phasePatterns.stream().filter(p -> p != null && !p.isBlank()).map(String::trim).toList()
                : List.of("*");
    }

    /**
     * Gets the absolute path.
     *
     * @return the resolved or constructed object
     */
    public Path getAbsolutePath() {
        return absolutePath;
    }

    /**
     * Gets the display path.
     *
     * @return the string result
     */
    public String getDisplayPath() {
        return displayPath;
    }

    /**
     * Gets the content.
     *
     * @return the string result
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the file pattern.
     *
     * @return the string result
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Gets the phase patterns.
     *
     * @return the collection of elements
     */
    public List<String> getPhasePatterns() {
        return phasePatterns;
    }

    /**
     * Checks whether this background file matches the given processed target file relative path.
     *
     * @param relativeSourcePath the relative path of the processed source file
     * @return true if matching, false otherwise
     */
    public boolean matchesTargetFile(String relativeSourcePath) {
        return GlobPatternMatcher.matchPath(filePattern, relativeSourcePath);
    }

    /**
     * Checks whether this background file matches the current reasoning phase.
     *
     * @param currentPhaseName the active reasoning phase name
     * @return true if matching, false otherwise
     */
    public boolean matchesPhase(String currentPhaseName) {
        if (currentPhaseName == null || currentPhaseName.isBlank()) {
            return true;
        }
        for (String pattern : phasePatterns) {
            if ("*".equals(pattern) || GlobPatternMatcher.match(pattern, currentPhaseName)) {
                return true;
            }
        }
        return false;
    }
}
