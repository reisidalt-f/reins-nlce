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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiPromptBuilderTest {
    @Test
    void buildsPromptThatExplainsMarkdownIntentAndResponseGrammar() {
        MarkdownInferenceRequest request = new MarkdownInferenceRequest();
        request.setSourcePath("src/main/nl/CurrentFeatures.md");
        request.setSourceScope("main");
        request.setMarkdownContent("Describe a controller, service, entity, and gui component.");

        String prompt = new GeminiPromptBuilder().buildPrompt(request);

        assertTrue(prompt.contains("functionalities, logic, data, and software components"));
        assertTrue(prompt.contains("Infer Java files and any additional non-Java files needed"));
        assertTrue(prompt.contains("path=\"relative/path/from-java-or-resources-root\""));
        assertTrue(prompt.contains("Do not include explanations outside the code blocks"));
    }
}