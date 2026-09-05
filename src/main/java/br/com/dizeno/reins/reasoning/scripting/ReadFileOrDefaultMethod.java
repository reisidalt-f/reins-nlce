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
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.util.Collections;
import java.util.List;

/**
 * ReadFileOrDefaultMethod is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Exposes a FreeMarker function (`readFileOrDefault`) to read file content with a default string fallback.
 */
public class ReadFileOrDefaultMethod implements TemplateMethodModelEx {
    private final ReadFileMethod readFileMethod;

    /**
     * Constructs a new instance of {@link ReadFileOrDefaultMethod}.
     *
     * @param basePathResolver the base path resolver instance
     * @param scriptContext the script context
     */
    public ReadFileOrDefaultMethod(BasePathResolver basePathResolver, ReasoningScriptContext scriptContext) {
        this.readFileMethod = new ReadFileMethod(basePathResolver, scriptContext);
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        if (arguments == null || arguments.isEmpty()) {
            return "";
        }
        String defaultValue = arguments.size() > 1 && arguments.get(1) != null ? arguments.get(1).toString() : "";
        Object content = readFileMethod.exec(Collections.singletonList(arguments.get(0)));
        if (content == null || content.toString().isEmpty()) {
            return defaultValue;
        }
        return content.toString();
    }
}
