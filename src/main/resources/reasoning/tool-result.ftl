<#ftl strip_whitespace=true>
<#-- tool-result.ftl: Formats a tool operation result block. TOOL_RESPONSE phase.
     Replicates ToolResultFormatter.format(). -->
<#assign r = project.currentToolResult>
MCP_RESULT
status: ${r.status}
<#if r.operation?has_content>
operation: ${r.operation}
</#if>
<#if r.qualifiedPath?has_content>
path: ${r.qualifiedPath}
</#if>
<#if r.resolvedBase?has_content>
resolved_base: ${r.resolvedBase}
</#if>
<#if r.failureReason?has_content>
failure: ${r.failureReason}
</#if>
<#if r.policyCode?has_content>
policy_code: ${r.policyCode}
</#if>
<#if r.resolvedPath?has_content>
resolved_path: ${r.resolvedPath}
</#if>
<#if r.exitCode?has_content>
exit_code: ${r.exitCode}
</#if>
started: ${r.started?c}
<#if r.truncated>
truncated: true
</#if>
<#if r.stdout?has_content>
stdout:
${r.stdout}
</#if>
<#if r.stderr?has_content>
stderr:
${r.stderr}
</#if>
<#if r.operation?has_content && r.operation == "ADD_INFERENCE_NOTE" && r.status == "ERROR" && r.failureReason?has_content && r.failureReason?contains("No tracked source found owning compiled file")>
HINT:
- The `compiled` value should be canonical qualified path with base prefix, for example `target:com/example/Foo.java`.
- Re-send add_reasoning_note using canonical compiled path.
</#if>
<#if !r.readFileSuccess && r.content?has_content>
content:
${r.content}
</#if>
<#if r.readFileSuccess>
content: [attached]
</#if>
<#if r.listedPaths?has_content>
listed:
<#list r.listedPaths as p>
- ${p}
</#list>
</#if>
<#if r.compiledFileStatuses?has_content>
compiled_files:
<#list r.compiledFileStatuses as gf>
- path: ${gf.qualifiedPath}
  attach_status: ${gf.attachStatus}
<#if gf.reason?has_content>
  reason: ${gf.reason}
</#if>
</#list>
</#if>
<#if r.readFileStatuses?has_content>
read_files:
<#list r.readFileStatuses as rf>
- path: ${rf.qualifiedPath}
  attach_status: ${rf.attachStatus}
<#if rf.reason?has_content>
  reason: ${rf.reason}
</#if>
</#list>
</#if>
<#if r.excludedPaths?has_content>
excluded_paths:
<#list r.excludedPaths as ep>
- ${ep}
</#list>
</#if>
<#if r.exclusionReason?has_content>
exclusion_reason: ${r.exclusionReason}
</#if>
