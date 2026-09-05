package br.com.dizeno.reins.reasoning.tooling.handler;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathMappingSet;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.ToolingSettings;
import br.com.dizeno.reins.security.PathValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ReasoningNotesGuardTest {

    @TempDir
    File tempDir;

    @Test
    void validatePermissionsBlocksNotesWhenAddReasoningNotesIsFalse() {
        ScopeValidationGuard guard = new ScopeValidationGuard();
        ToolingSettings settings = new ToolingSettings();
        settings.setAddReasoningNotes(false);
        FilePolicy policy = new FilePolicy(settings);

        ToolExecutionRequest addReq = new ToolExecutionRequest();
        addReq.setOperation(ToolExecutionRequest.Operation.ADD_REASONING_NOTE);
        addReq.setSource("domain/User.md");
        addReq.setNote("Fix missing field");

        String addResult = guard.validatePermissions(addReq, policy);
        assertNotNull(addResult);
        assertTrue(addResult.contains("disabled"));

        ToolExecutionRequest clearReq = new ToolExecutionRequest();
        clearReq.setOperation(ToolExecutionRequest.Operation.CLEAR_REASONING_NOTES);
        clearReq.setSource("domain/User.md");

        String clearResult = guard.validatePermissions(clearReq, policy);
        assertNotNull(clearResult);
        assertTrue(clearResult.contains("disabled"));
    }

    @Test
    void validatePermissionsAllowsNotesWhenAddReasoningNotesIsTrue() {
        ScopeValidationGuard guard = new ScopeValidationGuard();
        ToolingSettings settings = new ToolingSettings();
        settings.setAddReasoningNotes(true);
        FilePolicy policy = new FilePolicy(settings);

        ToolExecutionRequest addReq = new ToolExecutionRequest();
        addReq.setOperation(ToolExecutionRequest.Operation.ADD_REASONING_NOTE);
        addReq.setSource("domain/User.md");
        addReq.setNote("Fix missing field");

        String addResult = guard.validatePermissions(addReq, policy);
        assertNull(addResult);
    }

    @Test
    void reasoningNotesHandlerRejectsExecutionWhenDisabledInScriptRunnerConfig() {
        CompilationTrackingStore store = new CompilationTrackingStore();
        SourceTrackingManager manager = new SourceTrackingManager();
        ReasoningNotesHandler handler = new ReasoningNotesHandler(store, manager);

        ReinsConfig config = new ReinsConfig();
        config.getTooling().setAddReasoningNotes(false);
        ScriptRunnerConfig scriptConfig = ScriptRunnerConfig.fromSettings(config, tempDir.toPath());

        ToolExecutionRequest addReq = new ToolExecutionRequest();
        addReq.setOperation(ToolExecutionRequest.Operation.ADD_REASONING_NOTE);
        addReq.setSource("domain/User.md");
        addReq.setNote("Fix missing field");

        BasePathMappingSet mappings = new BasePathMappingSet();
        BasePathResolver resolver = new BasePathResolver(
                mappings,
                new PathValidator(tempDir.toPath())
        );

        ToolExecutionResult result = handler.execute(addReq, resolver, "main", scriptConfig);
        assertEquals(ToolExecutionResult.Status.ERROR, result.getStatus());
        assertTrue(result.getFailureReason().contains("disabled"));
    }

    @Test
    void reasoningNotesHandlerExecutesWhenEnabledInScriptRunnerConfig() {
        CompilationTrackingStore store = new CompilationTrackingStore();
        SourceTrackingManager manager = new SourceTrackingManager();
        ReasoningNotesHandler handler = new ReasoningNotesHandler(store, manager);

        ReinsConfig config = new ReinsConfig();
        config.getTooling().setAddReasoningNotes(true);
        ScriptRunnerConfig scriptConfig = ScriptRunnerConfig.fromSettings(config, tempDir.toPath());

        ToolExecutionRequest addReq = new ToolExecutionRequest();
        addReq.setOperation(ToolExecutionRequest.Operation.ADD_REASONING_NOTE);
        addReq.setSource("domain/User.md");
        addReq.setNote("Fix missing field");

        BasePathMappingSet mappings = new BasePathMappingSet();
        BasePathResolver resolver = new BasePathResolver(
                mappings,
                new PathValidator(tempDir.toPath())
        );

        ToolExecutionResult result = handler.execute(addReq, resolver, "main", scriptConfig);
        assertEquals(ToolExecutionResult.Status.SUCCESS, result.getStatus());
    }
}
