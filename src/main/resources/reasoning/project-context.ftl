<#-- project-context.ftl: Renders project compilation background files section. PROMPT_ASSEMBLY phase.
     Replicates the compilation background section in ReasoningPromptBuilder.buildFirstTurnPrompt(). -->
<#if attachments?has_content>

Compilation background files (attached):
<#list attachments as att>
${att.path}
</#list>

These files provide additional context for the project and should be considered as a guide when analyzing changes required to implement the design found in the main source document. No file should be created, deleted, or modified motivated solely by the compilation background files.

</#if>
