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

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolRequestParserTest {

    private final ToolRequestParser parser = new ToolRequestParser();

    @Test
    void parsesSingleReadFileRequest() {
        String raw = "--reins-boundary\n"
                + "READ_FILE main domain/entities.md\n"
                + "--reins-boundary--\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals(ToolExecutionRequest.Operation.READ_FILE, requests.get(0).getOperation());
        assertEquals("main", requests.get(0).getBase());
        assertEquals("domain/entities.md", requests.get(0).getPath());
    }

    @Test
    void parsesWriteFileWithTextPayload() {
        String raw = "--reins-boundary\n"
                + "WRITE_FILE target src/Foo.java\n"
                + "\n"
                + "public class Foo {\n"
                + "    public static void main(String[] args) {}\n"
                + "}\n"
                + "--reins-boundary--\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals(ToolExecutionRequest.Operation.WRITE_FILE, requests.get(0).getOperation());
        assertEquals("target", requests.get(0).getBase());
        assertEquals("src/Foo.java", requests.get(0).getPath());
        assertFalse(requests.get(0).isBase64());
        assertTrue(requests.get(0).getContent().contains("public class Foo"));
    }

    @Test
    void parsesWriteFileWithBase64Payload() {
        String raw = "--reins-boundary\n"
                + "WRITE_FILE target assets/logo.png base64\n"
                + "\n"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==\n"
                + "--reins-boundary--\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals(ToolExecutionRequest.Operation.WRITE_FILE, requests.get(0).getOperation());
        assertEquals("target", requests.get(0).getBase());
        assertEquals("assets/logo.png", requests.get(0).getPath());
        assertTrue(requests.get(0).isBase64());
        assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==", requests.get(0).getContent().trim());
    }

    @Test
    void parsesBatchToolRequests() {
        String raw = "--reins-boundary\n"
                + "READ_FILE main domain/entities.md\n"
                + "--reins-boundary\n"
                + "LIST_FILES main domain true\n"
                + "--reins-boundary\n"
                + "WRITE_FILE target src/Bar.java\n"
                + "\n"
                + "public class Bar {}\n"
                + "--reins-boundary--\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(3, requests.size());
        assertEquals(ToolExecutionRequest.Operation.READ_FILE, requests.get(0).getOperation());
        assertEquals(ToolExecutionRequest.Operation.LIST_FILES, requests.get(1).getOperation());
        assertTrue(requests.get(1).isRecursive());
        assertEquals(ToolExecutionRequest.Operation.WRITE_FILE, requests.get(2).getOperation());
    }

    @Test
    void parsesFencedBlockWithBoundary() {
        String raw = "Here is the request:\n"
                + "```tool_request\n"
                + "--reins-boundary\n"
                + "READ_FILE main domain/entities.md\n"
                + "--reins-boundary--\n"
                + "```\n";
        List<ToolExecutionRequest> requests = parser.parse(raw);
        assertEquals(1, requests.size());
        assertEquals(ToolExecutionRequest.Operation.READ_FILE, requests.get(0).getOperation());
    }
}
