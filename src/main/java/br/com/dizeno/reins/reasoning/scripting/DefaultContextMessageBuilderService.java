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

package br.com.dizeno.reins.reasoning.scripting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultContextMessageBuilderService is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public class DefaultContextMessageBuilderService implements ContextMessageBuilderService {

    private final ContextStepPlanner stepPlanner;
    private final ScriptStepPayloadDecoder payloadDecoder;
    private final ContextMessagePolicyGuard messagePolicyGuard;
    private final ContextAttachmentPolicyGuard attachmentPolicyGuard;
    private final ContextCacheFingerprintService fingerprintService;

    /**
     * Constructs a new instance of {@link DefaultContextMessageBuilderService}.
     */
    public DefaultContextMessageBuilderService() {
        this(new ContextStepPlanner(),
                new ScriptStepPayloadDecoder(),
                new ContextMessagePolicyGuard(),
                new ContextAttachmentPolicyGuard(),
                new ContextCacheFingerprintService());
    }

    /**
     * Constructs a new instance of {@link DefaultContextMessageBuilderService}.
     *
     * @param stepPlanner the step planner
     * @param payloadDecoder the payload decoder
     * @param messagePolicyGuard the message policy guard
     * @param attachmentPolicyGuard the attachment policy guard
     * @param fingerprintService the service used to calculate file fingerprints
     */
    public DefaultContextMessageBuilderService(ContextStepPlanner stepPlanner,
                                               ScriptStepPayloadDecoder payloadDecoder,
                                               ContextMessagePolicyGuard messagePolicyGuard,
                                               ContextAttachmentPolicyGuard attachmentPolicyGuard,
                                               ContextCacheFingerprintService fingerprintService) {
        this.stepPlanner = stepPlanner;
        this.payloadDecoder = payloadDecoder;
        this.messagePolicyGuard = messagePolicyGuard;
        this.attachmentPolicyGuard = attachmentPolicyGuard;
        this.fingerprintService = fingerprintService;
    }

    /**
     * Builds the configured target.
     *
     * @param request the request containing path and scope metadata
     * @param stepScriptExecutor the step script executor
     * @return the resolved or constructed object
     */
    @Override
    public ContextMessageBundle build(ContextMessageBuildRequest request,
                                      StepScriptExecutor stepScriptExecutor) throws Exception {
        String stepListOutput = stepScriptExecutor.execute("list-message-type");
        List<MessageTypePlan> plan = stepPlanner.parseMessageTypePlan(stepListOutput);

        List<ConversationMessage> messages = new ArrayList<>();
        Map<String, String> stepScriptFingerprints = new LinkedHashMap<>();
        for (MessageTypePlan entry : plan) {
            String stepPayload = stepScriptExecutor.execute(entry.getStepName());
            stepScriptFingerprints.put(entry.getStepName(), Integer.toHexString(stepPayload == null ? 0 : stepPayload.hashCode()));
            ScriptStepResult decoded = payloadDecoder.decode(entry.getStepName(), stepPayload);
            ScriptStepResult guarded = messagePolicyGuard.apply(decoded);
            List<AttachedFilePayload> guardedAttachments = attachmentPolicyGuard.apply(guarded.getAttachments());
            messages.add(new ConversationMessage(guarded.getRole(), guarded.getText(), guardedAttachments));
        }

        String buildFingerprint = fingerprintService.buildFingerprint(
                request.getSourceHash(),
                request.getConfigFingerprint(),
                request.getPolicyFingerprint(),
                plan,
                stepScriptFingerprints);
        return new ContextMessageBundle(messages, plan, buildFingerprint);
    }
}
