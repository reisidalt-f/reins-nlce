<#-- file-list.ftl: Renders workspace-relative paths of compiled files for provenance tracking. FILE_LIST phase.
     Each non-blank line is a workspace-relative path of a compiled file. -->
<#list project.inference.compiledFiles as filePath>
${filePath}
</#list>
