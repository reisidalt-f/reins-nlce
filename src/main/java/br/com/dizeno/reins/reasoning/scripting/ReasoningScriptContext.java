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

import java.util.Collections;
import java.util.List;

 
/**
 * ReasoningScriptContext is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing inference script context.
 */
public final class ReasoningScriptContext {

    private final ReasoningScriptViews.SourceView source;
    private final ReasoningScriptViews.FileBasesView fileBases;
    private final ReasoningScriptViews.TrackingView tracking;
    private final String referenceTree;
    private final ReasoningScriptViews.InferenceStateView inference;
    private final List<ReasoningScriptViews.AttachmentView> attachments;
    private final ReasoningScriptViews.ConfigView config;
    private final ReasoningScriptViews.CycleView cycle;
    private final ReasoningScriptViews.PolicyView policy;
    private final ReasoningScriptViews.PipelineView pipeline;
    private final ReasoningScriptViews.ToolResultView currentToolResult;

    private ReasoningScriptContext(Builder b) {
        this.source = b.source;
        this.fileBases = b.fileBases;
        this.tracking = b.tracking;
        this.referenceTree = b.referenceTree != null ? b.referenceTree : "";
        this.inference = b.inference;
        this.attachments = b.attachments != null
                ? Collections.unmodifiableList(b.attachments)
                : Collections.emptyList();
        this.config = b.config;
        this.cycle = b.cycle;
        this.policy = b.policy;
        this.pipeline = b.pipeline;
        this.currentToolResult = b.currentToolResult;
    }

    public ReasoningScriptViews.SourceView getSource() { return source; }
    public ReasoningScriptViews.FileBasesView getFileBases() { return fileBases; }
     
    public ReasoningScriptViews.TrackingView getTracking() { return tracking; }
    /**
     * Gets the reference tree.
     *
     * @return the string result
     */
    public String getReferenceTree() { return referenceTree; }
    public ReasoningScriptViews.InferenceStateView getInference() { return inference; }
    public List<ReasoningScriptViews.AttachmentView> getAttachments() { return attachments; }
    public ReasoningScriptViews.ConfigView getConfig() { return config; }
    public ReasoningScriptViews.CycleView getCycle() { return cycle; }
    public ReasoningScriptViews.PolicyView getPolicy() { return policy; }
    public ReasoningScriptViews.PipelineView getPipeline() { return pipeline; }
     
    public ReasoningScriptViews.ToolResultView getCurrentToolResult() { return currentToolResult; }

    /**
     * Builds the configured target er.
     *
     * @return the resolved or constructed object
     */
    public static Builder builder() { return new Builder(); }

    
    
    

    /**
     * Builder is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
     * Acts as a helper utility for building its prefix objects.
     */
    public static final class Builder {
        private ReasoningScriptViews.SourceView source;
        private ReasoningScriptViews.FileBasesView fileBases;
        private ReasoningScriptViews.TrackingView tracking;
        private String referenceTree;
        private ReasoningScriptViews.InferenceStateView inference;
        private List<ReasoningScriptViews.AttachmentView> attachments;
        private ReasoningScriptViews.ConfigView config;
        private ReasoningScriptViews.CycleView cycle;
        private ReasoningScriptViews.PolicyView policy;
        private ReasoningScriptViews.PipelineView pipeline;
        private ReasoningScriptViews.ToolResultView currentToolResult;

        /**
         * Source.
         *
         * @param source the source
         * @return the resolved or constructed object
         */
        public Builder source(ReasoningScriptViews.SourceView source) {
            this.source = source; return this;
        }
        /**
         * File Bases.
         *
         * @param fileBases the file bases
         * @return the resolved or constructed object
         */
        public Builder fileBases(ReasoningScriptViews.FileBasesView fileBases) {
            this.fileBases = fileBases; return this;
        }
        /**
         * Tracking.
         *
         * @param tracking the tracking
         * @return the resolved or constructed object
         */
        public Builder tracking(ReasoningScriptViews.TrackingView tracking) {
            this.tracking = tracking; return this;
        }
        /**
         * Reference Tree.
         *
         * @param referenceTree the reference tree
         * @return the resolved or constructed object
         */
        public Builder referenceTree(String referenceTree) {
            this.referenceTree = referenceTree; return this;
        }
        /**
         * Inference.
         *
         * @param inference the inference
         * @return the resolved or constructed object
         */
        public Builder inference(ReasoningScriptViews.InferenceStateView inference) {
            this.inference = inference; return this;
        }
        /**
         * Attachments.
         *
         * @param attachments the list of attachments
         * @return the resolved or constructed object
         */
        public Builder attachments(List<ReasoningScriptViews.AttachmentView> attachments) {
            this.attachments = attachments; return this;
        }
        /**
         * Config.
         *
         * @param config the Reins configuration settings
         * @return the resolved or constructed object
         */
        public Builder config(ReasoningScriptViews.ConfigView config) {
            this.config = config; return this;
        }
        /**
         * Cycle.
         *
         * @param cycle the cycle
         * @return the resolved or constructed object
         */
        public Builder cycle(ReasoningScriptViews.CycleView cycle) {
            this.cycle = cycle; return this;
        }
        /**
         * Policy.
         *
         * @param policy the policy
         * @return the resolved or constructed object
         */
        public Builder policy(ReasoningScriptViews.PolicyView policy) {
            this.policy = policy; return this;
        }
        /**
         * Pipeline.
         *
         * @param pipeline the pipeline
         * @return the resolved or constructed object
         */
        public Builder pipeline(ReasoningScriptViews.PipelineView pipeline) {
            this.pipeline = pipeline; return this;
        }
        /**
         * Current Tool Result.
         *
         * @param currentToolResult the current tool result
         * @return the resolved or constructed object
         */
        public Builder currentToolResult(ReasoningScriptViews.ToolResultView currentToolResult) {
            this.currentToolResult = currentToolResult; return this;
        }

        /**
         * Builds the configured target.
         *
         * @return the resulting context
         */
        public ReasoningScriptContext build() {
            return new ReasoningScriptContext(this);
        }
    }
}
