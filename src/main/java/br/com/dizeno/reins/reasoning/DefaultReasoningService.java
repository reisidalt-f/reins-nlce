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
import br.com.dizeno.reins.run.config.*;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.reasoning.inference.InferenceService;
import br.com.dizeno.reins.reasoning.inference.llm.service.DefaultLlmService;
import br.com.dizeno.reins.reasoning.tooling.ToolingService;
import br.com.dizeno.reins.reasoning.scripting.ScriptEvaluator;

/**
 * DefaultReasoningService is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Executes the main LLM-based reasoning loops, managing conversational state, script evaluations, and tool calls.
 */
public class DefaultReasoningService implements ReasoningService {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     */
    public DefaultReasoningService() {
        this(new InferenceService(new DefaultLlmService()),
                new ResponseDirectiveParser(),
                new ReasoningPromptBuilder(),
                new ToolingService(),
                new ToolResultFormatter(),
                new FileReasoningLogService(),
                new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     */
    public DefaultReasoningService(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter) {
        this(inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                new FileReasoningLogService(),
                new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     */
    public DefaultReasoningService(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService) {
        this(inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                inferenceLogService,
                new CompilationTrackingStore());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     */
    public DefaultReasoningService(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore) {
        this(inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                inferenceLogService,
                trackingStore,
                new ToolInfoPhraseFormatter(),
                new AttachmentNormalizer(),
                new ReferenceTreeContextService(),
                new ReferencedAttachmentBuilder(),
                new ToolRequestParser());
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     * @param toolInfoPhraseFormatter the tool info phrase formatter
     * @param attachmentNormalizer the attachment normalizer
     * @param referenceTreeContextService the reference tree context service
     * @param toolRequestParser the tool request parser
     */
    public DefaultReasoningService(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore,
                                   ToolInfoPhraseFormatter toolInfoPhraseFormatter,
                                   AttachmentNormalizer attachmentNormalizer,
                                   ReferenceTreeContextService referenceTreeContextService,
                                   ToolRequestParser toolRequestParser) {
        this(inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                inferenceLogService,
                trackingStore,
                toolInfoPhraseFormatter,
                attachmentNormalizer,
                referenceTreeContextService,
                new ReferencedAttachmentBuilder(),
                toolRequestParser);
    }

    /**
     * Constructs a new instance of {@link DefaultReasoningService}.
     *
     * @param inferenceService the service invoking LLM endpoints
     * @param directiveParser the directive parser
     * @param promptBuilder the prompt builder
     * @param toolingService the tooling service
     * @param toolResultFormatter the tool result formatter
     * @param inferenceLogService the inference log service
     * @param trackingStore the persistence store for file tracking records
     * @param toolInfoPhraseFormatter the tool info phrase formatter
     * @param attachmentNormalizer the attachment normalizer
     * @param referenceTreeContextService the reference tree context service
     * @param referencedAttachmentBuilder the referenced attachment builder
     * @param toolRequestParser the tool request parser
     */
    public DefaultReasoningService(InferenceService inferenceService,
                                   ResponseDirectiveParser directiveParser,
                                   ReasoningPromptBuilder promptBuilder,
                                   ToolingService toolingService,
                                   ToolResultFormatter toolResultFormatter,
                                   ReasoningLogService inferenceLogService,
                                   CompilationTrackingStore trackingStore,
                                   ToolInfoPhraseFormatter toolInfoPhraseFormatter,
                                   AttachmentNormalizer attachmentNormalizer,
                                   ReferenceTreeContextService referenceTreeContextService,
                                   ReferencedAttachmentBuilder referencedAttachmentBuilder,
                                   ToolRequestParser toolRequestParser) {
        this.coordinator = new DefaultReasoningExecutionCoordinator(
                inferenceService,
                directiveParser,
                promptBuilder,
                toolingService,
                toolResultFormatter,
                inferenceLogService,
                trackingStore,
                toolInfoPhraseFormatter,
                attachmentNormalizer,
                referenceTreeContextService,
                referencedAttachmentBuilder,
                toolRequestParser);
    }

    private DefaultReasoningService(DefaultReasoningExecutionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * With Script Evaluator.
     *
     * @param evaluator the script evaluator instance
     * @return the resolved or constructed object
     */
    public DefaultReasoningService withScriptEvaluator(ScriptEvaluator evaluator) {
        return new DefaultReasoningService(coordinator.withScriptEvaluator(evaluator));
    }

     
    /**
     * Gets the tooling service.
     *
     * @return the resolved or constructed object
     */
    public ToolingService getToolingService() {
        return coordinator.getToolingService();
    }

    /**
     * Runs the execution cycle cycle.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @return the resulting result
     */
    @Override
    public ReasoningResult runCycle(ReasoningRequest request, br.com.dizeno.reins.run.config.ReinsConfig config) throws Exception {
        return coordinator.runCycle(request, config);
    }
}
