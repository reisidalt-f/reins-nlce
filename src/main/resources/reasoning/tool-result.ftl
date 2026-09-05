<#ftl strip_whitespace=true>
<#-- tool-result.ftl: Formats a tool operation result block. TOOL_RESPONSE phase.
     Replicates ToolResultFormatter.format(). -->
<#assign r = project.currentToolResult>
--reins-boundary
${r.status}<#if r.operation?has_content> ${r.operation}</#if><#if r.qualifiedPath?has_content> ${r.qualifiedPath}</#if><#if r.exitCode?has_content> EXIT_CODE: ${r.exitCode}</#if><#if r.readFileSuccess> [attached]</#if>
<#if r.failureReason?has_content>
Reason: ${r.failureReason}
</#if>
<#if r.policyCode?has_content>
PolicyCode: ${r.policyCode}
</#if>
<#if !r.readFileSuccess && r.content?has_content>

${r.content}
</#if>
<#if r.listedPaths?has_content>

<#list r.listedPaths as p>
${p}
</#list>
</#if>
<#if r.stdout?has_content>

STDOUT:
${r.stdout}
</#if>
<#if r.stderr?has_content>

STDERR:
${r.stderr}
</#if>
--reins-boundary--
