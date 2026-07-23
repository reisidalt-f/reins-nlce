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

package br.com.dizeno.reins.reasoning.phase;

import br.com.dizeno.reins.reasoning.DefaultReasoningExecutionCoordinator;
import br.com.dizeno.reins.reasoning.ReasoningContext;
import br.com.dizeno.reins.reasoning.ReasoningPhase;
import br.com.dizeno.reins.reasoning.scripting.ConversationMessage;
import br.com.dizeno.reins.reasoning.scripting.MessageTypePlan;
import br.com.dizeno.reins.reasoning.ReasoningLogEntry;
import java.util.List;

/**
 * ContentCachingPhase is part of the execution phases representing distinct steps during reasoning turns in the reins architecture.
 * Acts as a processing phase executed as part of the compilation pipeline.
 */
public class ContentCachingPhase implements ReasoningPhase {
    private final DefaultReasoningExecutionCoordinator coordinator;

    /**
     * Constructs a new instance of {@link ContentCachingPhase}.
     *
     * @param coordinator the coordinator
     */
    public ContentCachingPhase(DefaultReasoningExecutionCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Executes the operation.
     *
     * @param context the context
     * @return the resolved or constructed object
     */
    @Override
    public ReasoningPhase execute(ReasoningContext context) throws Exception {
        boolean cachedContentEnabled = context.isCachedContentEnabled();
        String cycleCachedContentId = null;

        if (cachedContentEnabled) {
            try {
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-create-attempt");
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-create-payload: systemAttachmentCount="
                        + context.getFirstTurnContextAttachments().size()
                        + " firstUserPayloadMoved=false");
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-create-payload-first-user-preview: "
                        + coordinator.messageFormattingService.previewForLog(context.getNextMessage()));
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-create-payload-attachments: "
                        + coordinator.messageFormattingService.joinQualifiedPathsForLog(context.getFirstTurnContextAttachments()));

                cycleCachedContentId = coordinator.inferenceService.createCachedContent(
                        context.getConfig(),
                        context.getPrependMessages()
                );
                context.setCycleCachedContentId(cycleCachedContentId);

                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-created: " + cycleCachedContentId);
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-create-payload-context: "
                        + (context.isLogSystemContextEnabled() ? context.getPrependMessagesSummary() : context.getPrependMessagesSafeSummary()));

                if (context.getCycleLog() != null && context.isLogSystemContextEnabled()) {
                    List<ConversationMessage> prependMessages = context.getPrependMessages();
                    List<MessageTypePlan> contextPlan = context.getContextPlan();
                    for (int i = 0; i < prependMessages.size() && i < contextPlan.size(); i++) {
                        ConversationMessage cm = prependMessages.get(i);
                        coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.OUTBOUND,
                                "context-" + contextPlan.get(i).getStepName(), 0,
                                coordinator.messageFormattingService.buildOutboundLogBody(cm.getText(), cm.getAttachments()));
                    }
                }
            } catch (Exception ex) {
                coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "cache-fallback-direct-system: " + ex.getMessage());
                cachedContentEnabled = false;
                cycleCachedContentId = null;
                context.setCachedContentEnabled(false);
                context.setCycleCachedContentId(null);
            }
        }

        if (!cachedContentEnabled || cycleCachedContentId == null) {
            context.getConversationHistory().addAll(context.getPrependMessages());
            coordinator.cycleStateManager.emitLifecycleMessage(context.getCycleLog(), "uncached-prepend-context: "
                    + (context.isLogSystemContextEnabled() ? context.getPrependMessagesSummary() : context.getPrependMessagesSafeSummary()));

            if (context.getCycleLog() != null && context.isLogSystemContextEnabled()) {
                List<ConversationMessage> prependMessages = context.getPrependMessages();
                List<MessageTypePlan> contextPlan = context.getContextPlan();
                for (int i = 0; i < prependMessages.size() && i < contextPlan.size(); i++) {
                    ConversationMessage cm = prependMessages.get(i);
                    coordinator.cycleStateManager.writeLog(context.getCycleLog(), ReasoningLogEntry.Direction.OUTBOUND,
                            "context-" + contextPlan.get(i).getStepName(), 0,
                            coordinator.messageFormattingService.buildOutboundLogBody(cm.getText(), cm.getAttachments()));
                }
            }
        }

        return new TurnOutboundPrepPhase(coordinator);
    }
}
