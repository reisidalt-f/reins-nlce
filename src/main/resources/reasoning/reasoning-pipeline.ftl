<#ftl strip_whitespace=true>
<#assign p = (phase!"")?trim>
<#assign lastDirective = (project.pipeline?? && project.pipeline.lastDirectiveIntent?has_content)?then(project.pipeline.lastDirectiveIntent, "")>
<#assign lastContentType = (project.pipeline?? && project.pipeline.lastDirectiveContentType?has_content)?then(project.pipeline.lastDirectiveContentType, "")>
<#assign lastFailureClass = (project.pipeline?? && project.pipeline.lastFailureClass?has_content)?then(project.pipeline.lastFailureClass, "")>
<#assign mcpEnabled = project.policy?? && (
	project.policy.listFilesBases?has_content
	|| project.policy.readFilesBases?has_content
	|| project.policy.writeFilesBases?has_content
	|| project.policy.patchFilesBases?has_content
	|| project.policy.deleteFilesBases?has_content
	|| project.policy.listCompiledFilesBases?has_content
	|| project.policy.scriptRunnerEnabled
)>

<#if p == "list-phases">
default-cycle
<#elseif p == "default-cycle">
INTENT: waiting-for-next-message
CONTENT_TYPE: gemini-message

<#assign sourceScope = (project.source?? && project.source.scope?has_content)?then(project.source.scope, "main")>
<#assign sourcePathRaw = (project.source?? && project.source.path?has_content)?then(project.source.path?replace("\\", "/"), "")>
<#assign sourcePathNormalized = sourcePathRaw>
<#assign sourceQualifiedPath = "">
<#assign processingStatus = (project.pipeline?? && project.pipeline.processingStatus?has_content)?then(project.pipeline.processingStatus, "COMPILE")>
<#if sourcePathNormalized?starts_with("src/main/nl/")>
	<#assign sourcePathNormalized = sourcePathNormalized?substring("src/main/nl/"?length)>
<#elseif sourcePathNormalized?starts_with("src/test/nl/")>
	<#assign sourcePathNormalized = sourcePathNormalized?substring("src/test/nl/"?length)>
</#if>
<#if sourcePathNormalized?contains(":")>
	<#assign sourceQualifiedPath = sourcePathNormalized>
<#else>
	<#assign sourceQualifiedPath = sourceScope + ":" + sourcePathNormalized>
</#if>
<#assign isInitialDefaultCycleMessage = !(project.pipeline?? && project.pipeline.lastDirectiveIntent?has_content) && !(project.pipeline?? && project.pipeline.currentToolResultAvailable)>

<#if lastFailureClass == "parse-error">
<#include "retry-message.ftl">
<#elseif project.pipeline?? && project.pipeline.currentToolResultAvailable>
<#include "tool-result.ftl">
<#elseif lastDirective == "waiting_for_next_message" && lastContentType == "message_to_user">
Understood.
<#elseif isInitialDefaultCycleMessage>
<#if processingStatus == "VALIDATE">
Revalidate the files previously compiled from the main source markdown ${sourceQualifiedPath} and the files it references. Apply only the targeted fixes needed to keep compiled outputs consistent with referenced-source changes. Do not perform a full recompilation when patching is sufficient.
<#else>
Compile the main source file:
${sourceQualifiedPath}
</#if>
<#else>
<#assign msg = project.inference.context?has_content?then(project.inference.context?trim, "")>
<#if msg?has_content>${msg}

</#if>
<#assign addReasoningNotesEnabled = mcpEnabled && project.config?? && project.config.addReasoningNotes>

<#if addReasoningNotesEnabled>

Note-handoff guidance when fixing compilation across sources:
- Use add_reasoning_note to capture corrective context for files not compiled by the current main source. This includes:
  - Compilation errors that need correction in files owned by other sources
  - Changes needed to improve, increment, or alter the implementation of target files
  - Design changes or refactoring needed in files compiled by other sources
- Put concrete corrective detail in `note` (what must change, why it must change, and the intended outcome) so the next source cycle can apply it.
- Use `compiled` when you only know the compiled file path; use canonical qualified format with base prefix (for example `target:com/example/Foo.java`). Use `source` when you know the owning source path.
- Add notes only for other sources (or files compiled by other sources); if the issue is in the current source or files compiled by it, fix it in this current pipeline.
- You may continue with normal mutation requests in the same cycle when that is the best corrective path.
- You MUST verify the errors for fixes in files not compiled by the current pipeline and inference notes to those files (add_reasoning_note)
- You MUST not finish the pipeline without add inference notes when you find errors in files not compiled by the current pipeline

<#else>

Note-handoff fallback when add_reasoning_note is unavailable or disabled:
- Finish the pipeline with intent `finish-error` and content type `message-to-user` when pending notes exist.
- Do not emit a dedicated `add_reasoning_note` request.
- Group any pending notes by canonical recipient key format `base:path`, where `base` and `path` are the parameters from the intended `add_reasoning_note` operation.
- Preserve deterministic ordering of grouped notes and keep duplicate note text only once per target group when describing the fallback.
- Surface all pending notes in the final user-facing message so none are lost.

</#if>
</#if>
<#elseif p == "phase-entry">
INTENT: waiting-for-next-message
CONTENT_TYPE: user-progress

Starting pipeline phase `${project.pipeline.currentPhase}`.
<#elseif p == "phase-transition">
INTENT: waiting-for-next-message
CONTENT_TYPE: user-progress

Advanced to pipeline phase `${project.pipeline.currentPhase}` after `${lastDirective}`.
<#else>
INTENT: finish-error
CONTENT_TYPE: message-to-user

Unknown inference pipeline phase `${p}`.
</#if>