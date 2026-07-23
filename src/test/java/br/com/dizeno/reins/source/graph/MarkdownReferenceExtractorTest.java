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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownReferenceExtractorTest {
    @Test
    void extractsStrictBracketAndParenthesisReferencesInEncounterOrder() {
        String markdown = "Uses [shared/base.md], then (domain/service.md), then [shared/base.md] again.";

        List<String> references = new MarkdownReferenceExtractor().extract(markdown);

        assertEquals(List.of("shared/base.md", "domain/service.md"), references);
    }

    @Test
    void ignoresUrlsAndAbsolutePaths() {
        String markdown = String.join("\n",
                "Accept [Label](target.md)",
                "Ignore (https://example.com/spec.md)",
                "Ignore (/absolute/path.md)",
                "Accept [relative/path.md]"
        );

        List<String> references = new MarkdownReferenceExtractor().extract(markdown);

        assertEquals(List.of("relative/path.md", "target.md"), references);
    }

    @Test
    void extractsMarkdownLinkTargetAsReference() {
        String markdown = "See [Model Design](editor/model.md) and [Cursor Logic](editor/cursor.md) for details.";

        List<String> references = new MarkdownReferenceExtractor().extract(markdown);

        assertEquals(List.of("editor/model.md", "editor/cursor.md"), references);
    }

    @Test
    void ignoresBracketContentIfFollowedByParenthesesOrBrackets() {
        String markdown = String.join("\n",
                "extends [parent.md](relative-path)",
                "ref [another.md][some-id]",
                "empty-ref [third.md][]",
                "standalone [valid.md]"
        );

        List<String> references = new MarkdownReferenceExtractor().extract(markdown);

        assertEquals(List.of("valid.md"), references);
    }

    @Test
    void ignoresReferencesInsideCodeBlocksAndSpans() {
        String markdown = String.join("\n",
                "Outside block [valid.md]",
                "Inline code: `Ignore [ignored-1.md]` and `(ignored-2.md)`",
                "Fenced block:",
                "```markdown",
                "Ignore [ignored-3.md] and (ignored-4.md)",
                "```",
                "Another outside block [another-valid.md]"
        );

        List<String> references = new MarkdownReferenceExtractor().extract(markdown);

        assertEquals(List.of("valid.md", "another-valid.md"), references);
    }
}