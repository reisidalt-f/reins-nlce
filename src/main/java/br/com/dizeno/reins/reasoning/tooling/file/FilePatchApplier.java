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

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.util.Arrays;
import java.util.List;

/**
 * FilePatchApplier handles applying Unified Diff patches to file contents.
 */
public class FilePatchApplier {

    /**
     * Applies a unified diff patch to the original content.
     *
     * @param original the original file content
     * @param unifiedDiff the unified diff patch string
     * @return the patched file content
     */
    public String apply(String original, String unifiedDiff) {
        if (original == null) {
            original = "";
        }
        if (unifiedDiff == null || unifiedDiff.isBlank()) {
            throw new IllegalArgumentException("patch content (unified diff) must not be empty.");
        }

        List<String> originalLines = splitLines(original);
        List<String> rawDiffLines = splitLines(unifiedDiff);
        List<String> diffLines = normalizeIndentation(rawDiffLines);

        List<String> preparedDiff = new java.util.ArrayList<>();
        boolean hasFileHeader = diffLines.stream().anyMatch(l -> l.startsWith("--- "));
        if (!hasFileHeader) {
            preparedDiff.add("--- a/file");
            preparedDiff.add("+++ b/file");
        }
        preparedDiff.addAll(diffLines);

        // 1. Try standard diff parsing first
        try {
            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(preparedDiff);
            List<String> patchedLines = DiffUtils.patch(originalLines, patch);
            boolean preserveTrailing = original.endsWith("\n") || original.endsWith("\r\n");
            return joinLines(patchedLines, preserveTrailing);
        } catch (Exception primaryEx) {
            // 2. Try after normalizing hunk headers (fixing incorrect line counts in @@ headers)
            try {
                List<String> headerNormalizedDiff = normalizeHunkHeaders(preparedDiff);
                Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(headerNormalizedDiff);
                List<String> patchedLines = DiffUtils.patch(originalLines, patch);
                boolean preserveTrailing = original.endsWith("\n") || original.endsWith("\r\n");
                return joinLines(patchedLines, preserveTrailing);
            } catch (Exception secondaryEx) {
                // 3. Fallback to fuzzy/whitespace-tolerant patch engine
                try {
                    List<String> fuzzyPatched = applyFuzzyPatch(originalLines, preparedDiff);
                    boolean preserveTrailing = original.endsWith("\n") || original.endsWith("\r\n");
                    return joinLines(fuzzyPatched, preserveTrailing);
                } catch (Exception fuzzyEx) {
                    throw new IllegalArgumentException("Failed to apply unified diff patch. Primary: " + primaryEx.getMessage() + " | Secondary: " + secondaryEx.getMessage() + " | Fuzzy: " + fuzzyEx.getMessage(), primaryEx);
                }
            }
        }
    }

    /**
     * Recalculates line counts in @@ -start,count +start,count @@ headers to match
     * the actual body lines present in each hunk.
     */
    private List<String> normalizeHunkHeaders(List<String> diffLines) {
        List<String> result = new java.util.ArrayList<>(diffLines.size());
        java.util.regex.Pattern headerPattern = java.util.regex.Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@(.*)$");

        int i = 0;
        while (i < diffLines.size()) {
            String line = diffLines.get(i);
            java.util.regex.Matcher matcher = headerPattern.matcher(line);
            if (matcher.matches()) {
                int oldStart = Integer.parseInt(matcher.group(1));
                int newStart = Integer.parseInt(matcher.group(3));
                String suffix = matcher.group(5);

                int contextCount = 0;
                int minusCount = 0;
                int plusCount = 0;

                int j = i + 1;
                while (j < diffLines.size()) {
                    String bodyLine = diffLines.get(j);
                    if (bodyLine.startsWith("@@") || bodyLine.startsWith("--- ") || bodyLine.startsWith("+++ ")) {
                        break;
                    }
                    if (bodyLine.startsWith("-")) {
                        minusCount++;
                    } else if (bodyLine.startsWith("+")) {
                        plusCount++;
                    } else if (bodyLine.startsWith(" ")) {
                        contextCount++;
                    } else if (bodyLine.isEmpty() && j == diffLines.size() - 1) {
                        // Trailing empty line at end of diff input
                        break;
                    } else {
                        contextCount++;
                    }
                    j++;
                }

                int actualOldCount = contextCount + minusCount;
                int actualNewCount = contextCount + plusCount;

                String normalizedHeader = String.format("@@ -%d,%d +%d,%d @@%s", oldStart, actualOldCount, newStart, actualNewCount, suffix);
                result.add(normalizedHeader);
                i++;
            } else {
                result.add(line);
                i++;
            }
        }
        return result;
    }

    /**
     * Tolerant patch engine that matches hunks using trimmed/whitespace-insensitive matching.
     */
    private List<String> applyFuzzyPatch(List<String> originalLines, List<String> diffLines) {
        List<Hunk> hunks = parseHunks(diffLines);
        if (hunks.isEmpty()) {
            return originalLines;
        }

        List<String> workingLines = new java.util.ArrayList<>(originalLines);

        for (Hunk hunk : hunks) {
            int matchIndex = findFuzzyMatch(workingLines, hunk);
            if (matchIndex < 0) {
                throw new IllegalArgumentException("Hunk context could not be matched");
            }

            // Replace original target lines with new hunk lines
            for (int r = 0; r < hunk.expectedOldLines.size(); r++) {
                if (matchIndex < workingLines.size()) {
                    workingLines.remove(matchIndex);
                }
            }
            workingLines.addAll(matchIndex, hunk.newLines);
        }

        return workingLines;
    }

    private static class Hunk {
        int oldStartLine;
        List<String> expectedOldLines = new java.util.ArrayList<>();
        List<String> newLines = new java.util.ArrayList<>();
    }

    private List<Hunk> parseHunks(List<String> diffLines) {
        List<Hunk> hunks = new java.util.ArrayList<>();
        java.util.regex.Pattern headerPattern = java.util.regex.Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$");

        Hunk currentHunk = null;
        for (int i = 0; i < diffLines.size(); i++) {
            String line = diffLines.get(i);
            java.util.regex.Matcher matcher = headerPattern.matcher(line);
            if (matcher.matches()) {
                currentHunk = new Hunk();
                currentHunk.oldStartLine = Math.max(1, Integer.parseInt(matcher.group(1)));
                hunks.add(currentHunk);
                continue;
            }

            if (currentHunk != null) {
                if (line.startsWith("-")) {
                    currentHunk.expectedOldLines.add(line.substring(1));
                } else if (line.startsWith("+")) {
                    currentHunk.newLines.add(line.substring(1));
                } else if (line.startsWith(" ")) {
                    String content = line.substring(1);
                    currentHunk.expectedOldLines.add(content);
                    currentHunk.newLines.add(content);
                } else if (line.isEmpty() && i == diffLines.size() - 1) {
                    // Skip trailing empty line at end of diff input
                    continue;
                } else {
                    currentHunk.expectedOldLines.add(line);
                    currentHunk.newLines.add(line);
                }
            }
        }
        return hunks;
    }

    private int findFuzzyMatch(List<String> originalLines, Hunk hunk) {
        if (hunk.expectedOldLines.isEmpty()) {
            return Math.min(hunk.oldStartLine - 1, originalLines.size());
        }

        int targetIndex = Math.min(Math.max(0, hunk.oldStartLine - 1), originalLines.size());

        // First try exact position with trimmed comparison
        if (matchesAt(originalLines, hunk.expectedOldLines, targetIndex)) {
            return targetIndex;
        }

        // Search outwards around targetIndex
        int maxOffset = Math.max(originalLines.size(), 100);
        for (int offset = 1; offset <= maxOffset; offset++) {
            int tryBefore = targetIndex - offset;
            if (tryBefore >= 0 && matchesAt(originalLines, hunk.expectedOldLines, tryBefore)) {
                return tryBefore;
            }
            int tryAfter = targetIndex + offset;
            if (tryAfter <= originalLines.size() - hunk.expectedOldLines.size() && matchesAt(originalLines, hunk.expectedOldLines, tryAfter)) {
                return tryAfter;
            }
        }

        return -1;
    }

    private boolean matchesAt(List<String> originalLines, List<String> expectedLines, int startIndex) {
        if (startIndex < 0 || startIndex + expectedLines.size() > originalLines.size()) {
            return false;
        }
        for (int i = 0; i < expectedLines.size(); i++) {
            String originalTrimmed = originalLines.get(startIndex + i).trim();
            String expectedTrimmed = expectedLines.get(i).trim();
            if (!originalTrimmed.equals(expectedTrimmed)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Strips common leading whitespace injected by YAML block scalars.
     * Detects the indent level from {@code @@} hunk header lines, which must
     * start at column 0 in a valid unified diff. All lines have that prefix
     * removed. Patches already at column 0 are returned unchanged.
     */
    private List<String> normalizeIndentation(List<String> lines) {
        String commonIndent = null;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            int atAt = line.indexOf("@@");
            if (atAt > 0) {
                String candidate = line.substring(0, atAt);
                if (candidate.isBlank()) {
                    commonIndent = candidate;
                    break;
                }
            } else if (atAt == 0) {
                // already at column 0 — no stripping needed
                return lines;
            }
        }
        if (commonIndent == null || commonIndent.isEmpty()) {
            return lines;
        }
        final String prefix = commonIndent;
        List<String> normalized = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            normalized.add(line.startsWith(prefix) ? line.substring(prefix.length()) : line);
        }
        return normalized;
    }

    private List<String> splitLines(String text) {
        if (text.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(text.split("\\r?\\n", -1));
    }

    private String joinLines(List<String> lines, boolean preserveTrailingNewline) {
        String result = String.join("\n", lines);
        if (preserveTrailingNewline && !result.endsWith("\n") && !result.isEmpty()) {
            result += "\n";
        }
        return result;
    }
}
