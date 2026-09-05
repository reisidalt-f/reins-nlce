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

package br.com.dizeno.reins.source.graph;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarkdownReferenceExtractor is part of the general application functions in the reins architecture.
 * Parses markdown source files to extract hyperlinks and file references.
 */
public class MarkdownReferenceExtractor {
    private static final Pattern BRACKET_REF = Pattern.compile("\\[([^\\]\\s]+\\.md)\\](?![\\(\\[])");
    private static final Pattern PAREN_REF = Pattern.compile("\\(([^)\\s]+\\.md)\\)");

    /**
     * Extract.
     *
     * @param content the content
     * @return the string result
     */
    public List<String> extract(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String cleaned = stripCodeBlocksAndSpans(content);

        Set<String> references = new LinkedHashSet<>();
        collect(cleaned, BRACKET_REF, references);
        collect(cleaned, PAREN_REF, references);
        return List.copyOf(references);
    }

    private String stripCodeBlocksAndSpans(String content) {
        
        String noBlocks = content.replaceAll("(?s)```+.*?```+", "");
        
        return noBlocks.replaceAll("`[^`]*`", "");
    }

    private void collect(String content, Pattern pattern, Set<String> references) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (isValidReference(candidate)) {
                references.add(candidate.replace('\\', '/'));
            }
        }
    }

    private boolean isValidReference(String candidate) {
        if (candidate == null || candidate.isBlank() || !candidate.endsWith(".md")) {
            return false;
        }
        if (candidate.contains("://")) {
            return false;
        }
        return !Path.of(candidate).isAbsolute();
    }

}