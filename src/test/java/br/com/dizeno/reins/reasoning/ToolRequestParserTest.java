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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.scripting.*;

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolRequestParserTest {

    private final ToolRequestParser parser = new ToolRequestParser();

    @Test
    void parsesFencedBlockWithNestedFences() {
        String raw = "Here is the request:\n"
                + "```yaml\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: src/Foo.java\n"
                + "content: |\n"
                + "  ```java\n"
                + "  public class Foo {\n"
                + "  }\n"
                + "  ```\n"
                + "```\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals("src/Foo.java", requests.get(0).getPath());
        assertEquals("```java\npublic class Foo {\n}\n```\n", requests.get(0).getContent());
    }

    @Test
    void preprocessesUnindentedLinesInBlockScalars() {
        String raw = "operation: write_file\n"
                + "base: target\n"
                + "path: src/Foo.java\n"
                + "content: |\n"
                + "  public class Foo {\n"
                + "  \n"
                + "public static void main(String[] args) {\n"
                + "    System.out.println(\"hello\");\n"
                + "}\n"
                + "  }\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals("public class Foo {\n\npublic static void main(String[] args) {\n    System.out.println(\"hello\");\n}\n}\n", requests.get(0).getContent());
    }

    @Test
    void parsesMultipleDocumentsInFencedBlock() {
        String raw = "```yaml\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: foo\n"
                + "content: first\n"
                + "---\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: bar\n"
                + "content: second\n"
                + "```\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(2, requests.size());
        assertEquals("foo", requests.get(0).getPath());
        assertEquals("bar", requests.get(1).getPath());
    }

    @Test
    void parsesRawYamlDocumentsWithNestedSeparators() {
        String raw = "operation: write_file\n"
                + "base: target\n"
                + "path: first.yml\n"
                + "content: |\n"
                + "  ---\n"
                + "  key: val\n"
                + "---\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: second.yml\n"
                + "content: second\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(2, requests.size());
        assertEquals("first.yml", requests.get(0).getPath());
        assertEquals("---\nkey: val\n", requests.get(0).getContent());
        assertEquals("second.yml", requests.get(1).getPath());
    }

    @Test
    void parsesFencedBlockWithTripleQuotes() {
        String raw = "Here is the request:\n"
                + "\"\"\"yaml\n"
                + "operation: write_file\n"
                + "base: target\n"
                + "path: src/Foo.java\n"
                + "content: |\n"
                + "  public class Foo {\n"
                + "  }\n"
                + "\"\"\"\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals("src/Foo.java", requests.get(0).getPath());
        assertEquals("public class Foo {\n}\n", requests.get(0).getContent());
    }

    @Test
    void terminatesBlockScalarAtTripleQuotes() {
        String raw = "operation: write_file\n"
                + "base: target\n"
                + "path: src/Foo.java\n"
                + "content: |\n"
                + "  public class Foo {\n"
                + "  }\n"
                + "\"\"\"\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals("src/Foo.java", requests.get(0).getPath());
        assertEquals("public class Foo {\n}\n", requests.get(0).getContent());
    }
}
