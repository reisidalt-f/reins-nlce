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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * ResponseDirective is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing response directive.
 */
public class ResponseDirective {
    /**
     * Intent is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing intent.
     */
    public enum Intent {
        FINISH_SUCCESS,
        FINISH_ERROR,
        WAITING_FOR_NEXT_MESSAGE,
        GOTO_PHASE
    }

    /**
     * ContentType is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing content type.
     */
    public enum ContentType {
        MESSAGE_TO_USER,
        TOOL_REQUEST,
        CONVERSATION_SUMMARY
    }

    /**
     * DecisionReference is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a model representation of a source file reference.
     */
    public static class DecisionReference {
        private String relativeMarkdownPath;
        private String sectionHeading;
        private String quotedSubjectPhrase;

        /**
         * Gets the relative markdown path.
         *
         * @return the string result
         */
        public String getRelativeMarkdownPath() { return relativeMarkdownPath; }
        /**
         * Sets the relative markdown path.
         *
         * @param v the v
         */
        public void setRelativeMarkdownPath(String v) { this.relativeMarkdownPath = v; }
        /**
         * Gets the section heading.
         *
         * @return the string result
         */
        public String getSectionHeading() { return sectionHeading; }
        /**
         * Sets the section heading.
         *
         * @param v the v
         */
        public void setSectionHeading(String v) { this.sectionHeading = v; }
        /**
         * Gets the quoted subject phrase.
         *
         * @return the string result
         */
        public String getQuotedSubjectPhrase() { return quotedSubjectPhrase; }
        /**
         * Sets the quoted subject phrase.
         *
         * @param v the v
         */
        public void setQuotedSubjectPhrase(String v) { this.quotedSubjectPhrase = v; }
    }

    /**
     * DecisionState is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing decision state.
     */
    public static class DecisionState {
        private boolean hasNewDecision;
        private List<String> decisions = new ArrayList<>();
        private List<String> questions = new ArrayList<>();
        private String noNewDecisionMarker;
        private List<DecisionReference> references = new ArrayList<>();

        /**
         * Checks if the component is has new decision.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isHasNewDecision() { return hasNewDecision; }
        /**
         * Sets the has new decision.
         *
         * @param v the v
         */
        public void setHasNewDecision(boolean v) { this.hasNewDecision = v; }
        /**
         * Gets the decisions.
         *
         * @return the string result
         */
        public List<String> getDecisions() { return decisions; }
        /**
         * Sets the decisions.
         *
         * @param v the v
         */
        public void setDecisions(List<String> v) { this.decisions = v; }
        /**
         * Gets the questions.
         *
         * @return the string result
         */
        public List<String> getQuestions() { return questions; }
        /**
         * Sets the questions.
         *
         * @param v the v
         */
        public void setQuestions(List<String> v) { this.questions = v; }
        /**
         * Gets the no new decision marker.
         *
         * @return the string result
         */
        public String getNoNewDecisionMarker() { return noNewDecisionMarker; }
        /**
         * Sets the no new decision marker.
         *
         * @param v the v
         */
        public void setNoNewDecisionMarker(String v) { this.noNewDecisionMarker = v; }
        /**
         * Gets the references.
         *
         * @return the collection of elements
         */
        public List<DecisionReference> getReferences() { return references; }
        /**
         * Sets the references.
         *
         * @param v the v
         */
        public void setReferences(List<DecisionReference> v) { this.references = v; }
    }

    /**
     * PlanningContent is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing planning content.
     */
    public static class PlanningContent {
        private String goalSummary;
        private String strategySummary;
        private String progressSummary;

        /**
         * Gets the goal summary.
         *
         * @return the string result
         */
        public String getGoalSummary() { return goalSummary; }
        /**
         * Sets the goal summary.
         *
         * @param v the v
         */
        public void setGoalSummary(String v) { this.goalSummary = v; }
        /**
         * Gets the strategy summary.
         *
         * @return the string result
         */
        public String getStrategySummary() { return strategySummary; }
        /**
         * Sets the strategy summary.
         *
         * @param v the v
         */
        public void setStrategySummary(String v) { this.strategySummary = v; }
        /**
         * Gets the progress summary.
         *
         * @return the string result
         */
        public String getProgressSummary() { return progressSummary; }
        /**
         * Sets the progress summary.
         *
         * @param v the v
         */
        public void setProgressSummary(String v) { this.progressSummary = v; }
    }

    /**
     * GoalStatus is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing goal status.
     */
    public enum GoalStatus { PLANNED, IN_PROGRESS, REFINED, COMPLETED }
    /**
     * TaskType is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing task type.
     */
    public enum TaskType { TOOL_REQUEST, ANALYSIS, VALIDATION }

    /**
     * GoalTask is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing goal task.
     */
    public static class GoalTask {
        private String taskId;
        private String description;
        private TaskType taskType;
        private String referencedTool;

        /**
         * Gets the task id.
         *
         * @return the string result
         */
        public String getTaskId() { return taskId; }
        /**
         * Sets the task id.
         *
         * @param v the v
         */
        public void setTaskId(String v) { this.taskId = v; }
        /**
         * Gets the description.
         *
         * @return the string result
         */
        public String getDescription() { return description; }
        /**
         * Sets the description.
         *
         * @param v the v
         */
        public void setDescription(String v) { this.description = v; }
        /**
         * Gets the task type.
         *
         * @return the resolved or constructed object
         */
        public TaskType getTaskType() { return taskType; }
        /**
         * Sets the task type.
         *
         * @param v the v
         */
        public void setTaskType(TaskType v) { this.taskType = v; }
        /**
         * Gets the referenced tool.
         *
         * @return the string result
         */
        public String getReferencedTool() { return referencedTool; }
        /**
         * Sets the referenced tool.
         *
         * @param v the v
         */
        public void setReferencedTool(String v) { this.referencedTool = v; }
    }

    /**
     * IntermediateGoal is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
     * Acts as a component managing intermediate goal.
     */
    public static class IntermediateGoal {
        private String goalId;
        private String title;
        private GoalStatus status;
        private List<GoalTask> tasks = new ArrayList<>();

        /**
         * Gets the goal id.
         *
         * @return the string result
         */
        public String getGoalId() { return goalId; }
        /**
         * Sets the goal id.
         *
         * @param v the v
         */
        public void setGoalId(String v) { this.goalId = v; }
        /**
         * Gets the title.
         *
         * @return the string result
         */
        public String getTitle() { return title; }
        /**
         * Sets the title.
         *
         * @param v the v
         */
        public void setTitle(String v) { this.title = v; }
        /**
         * Gets the status.
         *
         * @return the resulting status
         */
        public GoalStatus getStatus() { return status; }
        /**
         * Sets the status.
         *
         * @param v the v
         */
        public void setStatus(GoalStatus v) { this.status = v; }
        /**
         * Gets the tasks.
         *
         * @return the collection of elements
         */
        public List<GoalTask> getTasks() { return tasks; }
        /**
         * Sets the tasks.
         *
         * @param v the v
         */
        public void setTasks(List<GoalTask> v) { this.tasks = v; }
    }

    private Intent intent;
    private ContentType contentType;
    private Map<String, String> rawHeaders = new LinkedHashMap<>();
    private boolean valid;
    private String failureReason;
    private PlanningContent planningContent;
    private DecisionState decisionState;
    private String targetPhase;
    private List<IntermediateGoal> intermediateGoals = new ArrayList<>();

    /**
     * Gets the target phase.
     *
     * @return the target phase name
     */
    public String getTargetPhase() {
        return targetPhase;
    }

    /**
     * Sets the target phase.
     *
     * @param targetPhase the target phase name
     */
    public void setTargetPhase(String targetPhase) {
        this.targetPhase = targetPhase;
    }

    /**
     * Gets the intent.
     *
     * @return the resolved or constructed object
     */
    public Intent getIntent() {
        return intent;
    }

    /**
     * Sets the intent.
     *
     * @param intent the reasoning intent
     */
    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    /**
     * Gets the content type.
     *
     * @return the resolved or constructed object
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the content type
     */
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the raw headers.
     *
     * @return the string result
     */
    public Map<String, String> getRawHeaders() {
        return rawHeaders;
    }

    /**
     * Sets the raw headers.
     *
     * @param rawHeaders the raw headers
     */
    public void setRawHeaders(Map<String, String> rawHeaders) {
        this.rawHeaders = rawHeaders;
    }

    /**
     * Checks if the component is valid.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Sets the valid.
     *
     * @param valid the valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the failure reason.
     *
     * @return the string result
     */
    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Sets the failure reason.
     *
     * @param failureReason the failure reason
     */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /**
     * Checks if the component is finish intent.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFinishIntent() {
        return intent == Intent.FINISH_SUCCESS || intent == Intent.FINISH_ERROR;
    }

    /**
     * Checks if the component is waiting intent.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isWaitingIntent() {
        return intent == Intent.WAITING_FOR_NEXT_MESSAGE;
    }

        /**
         * Checks if the component is non finish user message.
         *
         * @return true if successful or matching, false otherwise
         */
        public boolean isNonFinishUserMessage() {
            return intent == Intent.WAITING_FOR_NEXT_MESSAGE
                    && contentType == ContentType.MESSAGE_TO_USER;
        }

        /**
         * Gets the planning content.
         *
         * @return the resolved or constructed object
         */
        public PlanningContent getPlanningContent() { return planningContent; }
        /**
         * Sets the planning content.
         *
         * @param v the v
         */
        public void setPlanningContent(PlanningContent v) { this.planningContent = v; }
        /**
         * Gets the decision state.
         *
         * @return the resulting state
         */
        public DecisionState getDecisionState() { return decisionState; }
        /**
         * Sets the decision state.
         *
         * @param v the v
         */
        public void setDecisionState(DecisionState v) { this.decisionState = v; }
        /**
         * Gets the intermediate goals.
         *
         * @return the collection of elements
         */
        public List<IntermediateGoal> getIntermediateGoals() { return intermediateGoals; }
        /**
         * Sets the intermediate goals.
         *
         * @param v the v
         */
        public void setIntermediateGoals(List<IntermediateGoal> v) { this.intermediateGoals = v; }
}
