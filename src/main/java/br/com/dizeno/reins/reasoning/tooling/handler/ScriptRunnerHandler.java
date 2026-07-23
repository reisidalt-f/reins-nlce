/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.reasoning.tooling.handler;

import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionResult;
import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.scripting.ScriptRunnerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ScriptRunnerHandler is part of the general application functions in the reins architecture.
 * Acts as a component managing script runner handler.
 */
public class ScriptRunnerHandler implements ToolOperationHandler {
    /**
     * Supports.
     *
     * @param operation the operation
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean supports(ToolExecutionRequest.Operation operation) {
        return operation == ToolExecutionRequest.Operation.RUN_SCRIPT;
    }

    /**
     * Executes the operation.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @param sourceScope the source scope
     * @param scriptRunnerConfig the script runner config
     * @return the resulting result
     */
    @Override
    public ToolExecutionResult execute(ToolExecutionRequest request,
                                       BasePathResolver resolver,
                                       String sourceScope,
                                       ScriptRunnerConfig scriptRunnerConfig) {
        String qualifiedPath = qualifyScriptRequestPath(request);
        if (scriptRunnerConfig == null || !scriptRunnerConfig.isEnabled()) {
            return ToolExecutionResult.error(request.getOperation(), qualifiedPath, "Operation run_script is not available.");
        }

        final Path scriptPath;
        try {
            scriptPath = scriptRunnerConfig.resolveScriptPath(request.getScript());
        } catch (Exception ex) {
            ToolExecutionResult result = ToolExecutionResult.error(request.getOperation(), qualifiedPath, ex.getMessage());
            result.setStarted(false);
            result.setStdout("");
            result.setStderr("");
            return result;
        }

        ToolExecutionResult baseResult;
        if (!Files.exists(scriptPath)) {
            baseResult = ToolExecutionResult.error(request.getOperation(), qualifiedPath, "Script not found: " + request.getScript());
            baseResult.setStarted(false);
            baseResult.setResolvedPath(scriptRunnerConfig.toProjectRelativeDisplayPath(scriptPath));
            baseResult.setStdout("");
            baseResult.setStderr("");
            return baseResult;
        }
        if (!Files.isRegularFile(scriptPath)) {
            baseResult = ToolExecutionResult.error(request.getOperation(), qualifiedPath, "Script path is not a regular file: " + request.getScript());
            baseResult.setStarted(false);
            baseResult.setResolvedPath(scriptRunnerConfig.toProjectRelativeDisplayPath(scriptPath));
            baseResult.setStdout("");
            baseResult.setStderr("");
            return baseResult;
        }
        if (!Files.isExecutable(scriptPath)) {
            baseResult = ToolExecutionResult.error(request.getOperation(), qualifiedPath, "Script is not executable: " + request.getScript());
            baseResult.setStarted(false);
            baseResult.setResolvedPath(scriptRunnerConfig.toProjectRelativeDisplayPath(scriptPath));
            baseResult.setStdout("");
            baseResult.setStderr("");
            return baseResult;
        }

        try {
            List<String> command = new ArrayList<>();
            command.add(scriptPath.toAbsolutePath().normalize().toString());
            if (request.getArgs() != null && !request.getArgs().isEmpty()) {
                command.addAll(request.getArgs());
            }
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(resolver.getProjectRoot().toFile());
            Process process = builder.start();

            CompletableFuture<byte[]> stdoutFuture = readAllAsync(process.getInputStream());
            CompletableFuture<byte[]> stderrFuture = readAllAsync(process.getErrorStream());
            int exitCode = process.waitFor();
            String stdout = decode(stdoutFuture.join());
            String stderr = decode(stderrFuture.join());

            TruncationResult truncationResult = truncateOutput(stdout, stderr, scriptRunnerConfig.getOutputTruncationLimit());

            ToolExecutionResult result = exitCode == 0
                    ? ToolExecutionResult.success(request.getOperation(), qualifiedPath, "script-executed")
                    : ToolExecutionResult.error(request.getOperation(), qualifiedPath, "Script exited with code " + exitCode + ".");
            result.setStarted(true);
            result.setResolvedPath(scriptRunnerConfig.toProjectRelativeDisplayPath(scriptPath));
            result.setExitCode(exitCode);
            result.setStdout(truncationResult.stdout());
            result.setStderr(truncationResult.stderr());
            result.setTruncated(truncationResult.truncated());
            return result;
        } catch (Exception ex) {
            ToolExecutionResult result = ToolExecutionResult.error(request.getOperation(), qualifiedPath, ex.getMessage());
            result.setStarted(false);
            result.setResolvedPath(scriptRunnerConfig.toProjectRelativeDisplayPath(scriptPath));
            result.setStdout("");
            result.setStderr("");
            return result;
        }
    }

    private String qualifyScriptRequestPath(ToolExecutionRequest request) {
        String script = request.getScript() == null ? "" : request.getScript();
        if (script.regionMatches(true, 0, "script:", 0, "script:".length())) {
            return script;
        }
        return "script:" + script;
    }

    private CompletableFuture<byte[]> readAllAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream in = inputStream) {
                return in.readAllBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    private String decode(byte[] content) {
        return new String(content, StandardCharsets.UTF_8);
    }

    private TruncationResult truncateOutput(String stdout, String stderr, int limit) {
        String safeStdout = stdout == null ? "" : stdout;
        String safeStderr = stderr == null ? "" : stderr;
        if (safeStdout.length() + safeStderr.length() <= limit) {
            return new TruncationResult(safeStdout, safeStderr, false);
        }

        int stdoutAllowance = Math.min(safeStdout.length(), Math.max(0, limit / 2));
        int stderrAllowance = Math.min(safeStderr.length(), Math.max(0, limit - stdoutAllowance));
        if (stdoutAllowance + stderrAllowance < limit && safeStdout.length() > stdoutAllowance) {
            stdoutAllowance = Math.min(safeStdout.length(), limit - stderrAllowance);
        }
        String truncationMarker = "\n...[truncated]";
        return new TruncationResult(
                safeStdout.substring(0, stdoutAllowance) + truncationMarker,
                safeStderr.substring(0, stderrAllowance) + truncationMarker,
                true);
    }

    /**
     * TruncationResult is part of the general application functions in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    private record TruncationResult(String stdout, String stderr, boolean truncated) {}
}
