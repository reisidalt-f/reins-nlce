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

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.llm.service.DefaultLlmService;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;

/**
 * DefaultReasoningServiceFactory is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing default reasoning service factory.
 */
public final class DefaultReasoningServiceFactory {

    private DefaultReasoningServiceFactory() {
    }

    /**
     * Creates a new resource default.
     *
     * @return the resolved or constructed object
     */
    public static DefaultReasoningService createDefault() {
        return new DefaultReasoningService(
                new InferenceService(new DefaultLlmService()),
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                new ToolingService(),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore(),
                new ToolInfoPhraseFormatter(),
                new AttachmentNormalizer(),
                new ReferenceTreeContextService(),
                new ReferencedAttachmentBuilder(),
                new ToolRequestParser());
    }

    /**
     * Creates a new resource with custom script evaluator.
     *
     * @param evaluator the script evaluator instance
     * @return the resolved or constructed object
     */
    public static DefaultReasoningService createWithCustomScriptEvaluator(ScriptEvaluator evaluator) {
        return createDefault().withScriptEvaluator(evaluator);
    }

}
