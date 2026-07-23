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

package br.com.dizeno.reins.reasoning.inference.llm.providers.gemini;

import br.com.dizeno.reins.reasoning.inference.MarkdownInferenceRequest;

/**
 * GeminiPromptBuilder is part of the general application functions in the reins architecture.
 * Acts as a helper utility for building its prefix objects.
 */
public class GeminiPromptBuilder {
    /**
     * Builds the configured target prompt.
     *
     * @param request the request containing path and scope metadata
     * @return the string result
     */
    public String buildPrompt(MarkdownInferenceRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are compiling files(source code and resources) for a Java project.\n");
        sb.append("The markdown content can describe functionalities, logic, data, and software components.\n");
        sb.append("Infer Java files and any additional non-Java files needed to implement the description as comprehensively as possible,");
        sb.append(" but do not make independent assumptions beyond the provided information.\n");
        sb.append("Source scope: ").append(request.getSourceScope()).append("\n");
        sb.append("Return only fenced code blocks. Each block must include a path attribute in the opening fence.\n");
        sb.append("Use this exact format for every inferred file:\n");
        sb.append("```<language> path=\"relative/path/from-java-or-resources-root\"\n");
        sb.append("<full file content>\n");
        sb.append("```\n");
        sb.append("Use .java paths for Java classes and non-.java paths for resource files.\n");
        sb.append("Do not include explanations outside the code blocks.\n\n");
        sb.append("Markdown input:\n");
        sb.append(request.getMarkdownContent());
        return sb.toString();
    }
}
