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

package br.com.dizeno.reins.run.config.settings;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * ContextSourceSpec represents a background context source file configuration
 * along with target file pattern matching and reasoning phase matching specifications.
 */
public class ContextSourceSpec {
    private File file;
    private String pattern = "**/*.md";
    private List<String> phases = List.of("*");

    public ContextSourceSpec() {}

    public ContextSourceSpec(File file) {
        this(file, "**/*.md", List.of("*"));
    }

    public ContextSourceSpec(File file, String pattern, List<String> phases) {
        this.file = file;
        setPattern(pattern);
        setPhases(phases);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null && !pattern.isBlank() ? pattern.trim() : "**/*.md";
    }

    public List<String> getPhases() {
        return phases;
    }

    public void setPhases(List<String> phases) {
        if (phases == null || phases.isEmpty()) {
            this.phases = List.of("*");
        } else {
            this.phases = phases.stream()
                    .filter(p -> p != null && !p.isBlank())
                    .map(String::trim)
                    .toList();
        }
    }

    public void setPhaseString(String phaseCsv) {
        if (phaseCsv == null || phaseCsv.isBlank()) {
            this.phases = List.of("*");
        } else {
            this.phases = Arrays.stream(phaseCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}
