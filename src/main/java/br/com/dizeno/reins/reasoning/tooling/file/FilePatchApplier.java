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

import java.util.ArrayList;
import java.util.List;

/**
 * FilePatchApplier is part of the general application functions in the reins architecture.
 * Acts as a component managing file patch applier.
 */
public class FilePatchApplier {
     
    /**
     * Apply.
     *
     * @param original the original
     * @param atLine the at line
     * @param replacing the replacing
     * @param content the content
     * @return the string result
     */
    public String apply(String original, int atLine, int replacing, String content) {
        if (original == null) {
            original = "";
        }
        if (atLine < 1) {
            throw new IllegalArgumentException("atLine must be >= 1 (1-based). Got: " + atLine);
        }
        if (replacing < 0) {
            throw new IllegalArgumentException("replacing must be >= 0. Got: " + replacing);
        }

        
        List<String> lines = splitLines(original);

        
        int insertIdx = atLine - 1;
        if (insertIdx > lines.size()) {
            throw new IllegalArgumentException(
                "atLine " + atLine + " is out of range; file has " + lines.size() + " line(s).");
        }
        int removeCount = Math.min(replacing, lines.size() - insertIdx);

        
        List<String> newLines = splitLines(content);

        List<String> result = new ArrayList<>(lines.size() - removeCount + newLines.size());
        result.addAll(lines.subList(0, insertIdx));
        result.addAll(newLines);
        result.addAll(lines.subList(insertIdx + removeCount, lines.size()));

        return joinLines(result);
    }

     
    private List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            return lines;
        }
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i + 1)); 
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(text.substring(start)); 
        }
        return lines;
    }

    private String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
        }
        return sb.toString();
    }
}
