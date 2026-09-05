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

package br.com.dizeno.reins.reasoning.scripting;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.security.PathValidator;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReadFileMethod is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Exposes a FreeMarker function (`readFile`) to read file content safely from any Reins base.
 */
public class ReadFileMethod implements TemplateMethodModelEx {
    private static final Logger LOGGER = Logger.getLogger(ReadFileMethod.class.getName());
    private final BasePathResolver basePathResolver;
    private final ReasoningScriptContext scriptContext;

    /**
     * Constructs a new instance of {@link ReadFileMethod}.
     *
     * @param basePathResolver the base path resolver instance
     */
    public ReadFileMethod(BasePathResolver basePathResolver) {
        this(basePathResolver, null);
    }

    /**
     * Constructs a new instance of {@link ReadFileMethod}.
     *
     * @param basePathResolver the base path resolver instance
     * @param scriptContext the script context
     */
    public ReadFileMethod(BasePathResolver basePathResolver, ReasoningScriptContext scriptContext) {
        this.basePathResolver = basePathResolver;
        this.scriptContext = scriptContext;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        if (arguments == null || arguments.isEmpty() || arguments.get(0) == null) {
            return "";
        }

        String rawInput = arguments.get(0).toString().trim();
        if (rawInput.isEmpty()) {
            return "";
        }

        String base = "main";
        String relativePath = rawInput;

        int colon = rawInput.indexOf(':');
        if (colon > 0) {
            base = rawInput.substring(0, colon).trim();
            relativePath = rawInput.substring(colon + 1).trim();
        }

        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        if (relativePath.isEmpty()) {
            return "";
        }

        try {
            Path file = resolveFile(base, relativePath);
            if (file != null && Files.isRegularFile(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (SecurityException ex) {
            LOGGER.log(Level.WARNING, "[reins-script] readFile security violation for '" + rawInput + "': " + ex.getMessage());
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "[reins-script] readFile failed to read '" + rawInput + "': " + ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "[reins-script] readFile error resolving '" + rawInput + "': " + ex.getMessage());
        }

        return "";
    }

    private Path resolveFile(String base, String relativePath) {
        if (basePathResolver != null) {
            return basePathResolver.resolve(base, relativePath);
        }

        if (scriptContext == null || scriptContext.getFileBases() == null) {
            return null;
        }

        ReasoningScriptViews.FileBasesView fileBases = scriptContext.getFileBases();
        String projectRootStr = fileBases.getProjectRoot();
        if (projectRootStr == null || projectRootStr.isBlank()) {
            return null;
        }

        Path projectRoot = Path.of(projectRootStr).toAbsolutePath().normalize();
        PathValidator validator = new PathValidator(projectRoot);

        String normalizedBase = base.toLowerCase(java.util.Locale.ROOT);
        String baseDir = switch (normalizedBase) {
            case "main", "main-source", "source" -> fileBases.getMain();
            case "test", "test-source" -> fileBases.getTest();
            case "target", "main-target", "test-target" -> fileBases.getTarget();
            default -> null;
        };

        Path baseRoot = (baseDir != null && !baseDir.isBlank())
                ? Path.of(baseDir).toAbsolutePath().normalize()
                : projectRoot;

        Path targetPath = baseRoot.resolve(relativePath).normalize();
        return validator.validateInProject(targetPath);
    }
}
