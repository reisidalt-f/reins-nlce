<#-- max-turn-grace-prompt.ftl: Grace-turn prompt when max turns reached. PROMPT_ASSEMBLY phase.
     Replicates ReasoningPromptBuilder.buildMaxTurnGracePrompt(). -->
The reasoning cycle reached the configured max turns without successful fulfillment. Respond now with a final finish-error message to the user.

Requirements:
1) Use headers INTENT: finish-error and CONTENT_TYPE: message-to-user.
2) Summarize the reasoning and MCP work completed so far.
3) Explicitly state you could not conclude because max reasoning turns were reached before fulfillment.
4) Do not include MCP requests or additional tool operations in this response.
<#if (project.cycle.maxTurns > 0)>
5) Mention configured max turns: ${project.cycle.maxTurns}.
</#if>
