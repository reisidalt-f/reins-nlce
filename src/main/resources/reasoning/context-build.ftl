<#ftl strip_whitespace=true>
<#-- context-build.ftl: Evaluates a single context-building step and returns the
     rendered content for that step.  The step name is passed as the template
     variable `step`.

     Recognized step values (returned by list-message-type):
       list-message-type          - returns the ordered list of step names
       system-context             - full system-context payload (includes system-context.ftl)
       previously-compiled-files - tracking: files compiled from this source
       previously-inspected-files - tracking: files inspected during this cycle
       references-tree            - reference tree for the current source
       background-files           - background attachments supplied to this cycle
-->
<#assign s = (step!"")?trim>

<#if s == "list-message-type">
system-context
previously-compiled-files
previously-inspected-files
references-tree
background-files
<#elseif s == "system-context">
SYSTEM

<#include "system-context.ftl">
<#elseif s == "source-notes">
SYSTEM

Important notes for this source:
${tracking.notesOrNone}
<#elseif s == "previously-compiled-files">
SYSTEM

Previously compiled files:
<#if (inference.compiledSourceGroups)?has_content>
<#list inference.compiledSourceGroups as group>
Owner: ${group.sourceCanonicalPath} (${group.sourceSimpleName})
${group.compiledPathsOrNone}
<#if group_has_next>

</#if>
</#list>
<#else>
${tracking.compiledPathsOrNone}
</#if>
<#elseif s == "previously-inspected-files">
SYSTEM

Previously inspected files in this cycle:
${inference.inspectedFilesOrNone}
<#elseif s == "references-tree">
SYSTEM

Reference tree:
<#if (project.referenceTree!"")?has_content>
${project.referenceTree}
<#else>
none
</#if>
<#elseif s == "background-files">
SYSTEM

Background attachments passed to this cycle:
<#if attachments?has_content>
<#list attachments as a>
- ${a.path}
</#list>
<#else>
- none
</#if>

These files provide additional context for the project and should be considered as a guide when analyzing changes required to implement the design found in the main source document. No file should be created, deleted, or modified motivated solely by the background files.
<#else>
SYSTEM

Unknown context step '${s}'.
</#if>
