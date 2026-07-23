<#-- reference-tree.ftl: Returns the pre-rendered reference tree text. PROMPT_ASSEMBLY phase.
     Replicates ReferenceTreeContextService.toPromptSection(). -->
<#if project.referenceTree?has_content>
${project.referenceTree}<#t>
<#else>
<empty>
</#if>
