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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GlobPatternMatcher provides pattern matching capabilities for target file paths
 * and reasoning phase names, supporting standard Ant globs, leading negative globs (!pattern),
 * and extended glob syntax (!(pattern)).
 */
public class GlobPatternMatcher {

    /**
     * Matches a relative path against a glob pattern string (which may contain multiple comma-separated patterns,
     * prefix negative globs, or extglobs like extglob negated pattern).
     *
     * @param globPattern the glob pattern or comma-separated list of patterns
     * @param relativePath the relative path to evaluate
     * @return true if matching, false otherwise
     */
    public static boolean matchPath(String globPattern, String relativePath) {
        if (globPattern == null || globPattern.isBlank() || relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String normalizedPath = relativePath.replace('\\', '/');
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        String[] patterns = globPattern.split(",");
        List<String> positivePatterns = new ArrayList<>();
        List<String> negativePatterns = new ArrayList<>();

        for (String p : patterns) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("!") && !trimmed.startsWith("!(")) {
                negativePatterns.add(trimmed.substring(1).trim());
            } else {
                positivePatterns.add(trimmed);
            }
        }

        for (String neg : negativePatterns) {
            if (matchSinglePattern(neg, normalizedPath)) {
                return false;
            }
        }

        if (positivePatterns.isEmpty() && !negativePatterns.isEmpty()) {
            return true;
        }

        for (String pos : positivePatterns) {
            if (matchSinglePattern(pos, normalizedPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Matches a single string (such as a reasoning phase name) against a pattern.
     * Supports prefix negation ("!phaseName") and extglobs ("!(phase1|phase2)").
     *
     * @param pattern the pattern string
     * @param target the target string to evaluate
     * @return true if matching, false otherwise
     */
    public static boolean match(String pattern, String target) {
        if (pattern == null || pattern.isBlank() || target == null) {
            return false;
        }
        String trimmed = pattern.trim();
        if (trimmed.startsWith("!") && !trimmed.startsWith("!(")) {
            String subPattern = trimmed.substring(1).trim();
            return !matchSinglePattern(subPattern, target);
        }
        return matchSinglePattern(trimmed, target);
    }

    private static boolean matchSinglePattern(String pattern, String targetPath) {
        try {
            Pattern regex = globToRegex(pattern);
            if (regex != null) {
                return regex.matcher(targetPath).matches();
            }
        } catch (Exception ignored) {
            // Fallback to SelectorUtils if regex creation encounters unexpected input
        }
        return SelectorUtils.matchPath(pattern, targetPath, "/", true);
    }

    /**
     * Converts a glob pattern string (including ** and !(extglob)) to a compiled regex Pattern.
     *
     * @param glob the glob pattern string
     * @return compiled Pattern
     */
    public static Pattern globToRegex(String glob) {
        if (glob == null || glob.isBlank()) {
            return null;
        }
        String regexSnippet = globToRegexSnippet(glob);
        return Pattern.compile("^" + regexSnippet + "$");
    }

    private static String globToRegexSnippet(String glob) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = glob.length();

        while (i < len) {
            char c = glob.charAt(i);

            if (c == '*' && i + 1 < len && glob.charAt(i + 1) == '*') {
                if (i + 2 < len && glob.charAt(i + 2) == '/') {
                    sb.append("(?:.*/)?");
                    i += 3;
                } else {
                    sb.append(".*");
                    i += 2;
                }
            } else if (c == '*') {
                sb.append("[^/]*");
                i++;
            } else if (c == '?') {
                sb.append("[^/]");
                i++;
            } else if (c == '!' && i + 1 < len && glob.charAt(i + 1) == '(') {
                int closeParen = glob.indexOf(')', i + 2);
                if (closeParen != -1) {
                    String inside = glob.substring(i + 2, closeParen);
                    String restOfGlob = glob.substring(closeParen + 1);
                    String insideRegex = convertInsideExtglob(inside);
                    String restRegex = globToRegexSnippet(restOfGlob) + "$";
                    sb.append("(?!(?:").append(insideRegex).append(")").append(restRegex).append(")[^/]*");
                    i = closeParen + 1;
                } else {
                    sb.append("\\!");
                    i++;
                }
            } else if ("./\\()[]{}+^$|".indexOf(c) != -1) {
                sb.append("\\").append(c);
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }

        return sb.toString();
    }

    private static String convertInsideExtglob(String inside) {
        String[] options = inside.split("\\|");
        List<String> optionRegexes = new ArrayList<>();
        for (String opt : options) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < opt.length(); k++) {
                char c = opt.charAt(k);
                if (c == '*') {
                    sb.append("[^/]*");
                } else if (c == '?') {
                    sb.append("[^/]");
                } else if ("./\\()[]{}+^$|".indexOf(c) != -1) {
                    sb.append("\\").append(c);
                } else {
                    sb.append(c);
                }
            }
            optionRegexes.add(sb.toString());
        }
        return String.join("|", optionRegexes);
    }
}
