<#-- attachment-list.ftl: Renders workspace-relative paths of files to attach. ATTACHMENT_LIST phase.
     Each non-blank line is a workspace-relative file path to attach as inline content. -->
<#list project.attachments as att>
${att.path}
</#list>
