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

package br.com.dizeno.reins.reasoning;

import br.com.dizeno.reins.reasoning.tooling.file.BasePathResolver;
import br.com.dizeno.reins.reasoning.tooling.ToolExecutionRequest;
import br.com.dizeno.reins.util.LogSanitizer;

/**
 * ToolInfoPhraseFormatter is part of the orchestration of conversational reasoning loops, prompt construction, and tool instruction mapping in the reins architecture.
 * Acts as a component managing tool info phrase formatter.
 */
public class ToolInfoPhraseFormatter {
    private static final String UNSPECIFIED_PATH = "<unspecified-path>";
    private static final String UNSPECIFIED_INTENTION = "unspecified intention";

    private final ToolInfoPhraseTemplateRegistry templateRegistry;

    /**
     * Constructs a new instance of {@link ToolInfoPhraseFormatter}.
     */
    public ToolInfoPhraseFormatter() {
        this(new ToolInfoPhraseTemplateRegistry());
    }

    /**
     * Constructs a new instance of {@link ToolInfoPhraseFormatter}.
     *
     * @param templateRegistry the template registry
     */
    public ToolInfoPhraseFormatter(ToolInfoPhraseTemplateRegistry templateRegistry) {
        this.templateRegistry = templateRegistry;
    }

    /**
     * Format.
     *
     * @param request the request containing path and scope metadata
     * @param resolver the resolver
     * @return the string result
     */
    public String format(ToolExecutionRequest request, BasePathResolver resolver) {
        String template = templateRegistry.templateFor(request == null ? null : request.getOperation());
        String path = resolvePath(request, resolver);
        String intention = resolveIntention(request);
        return template
                .replace("<path>", path)
                .replace("<intention>", intention);
    }

    private String resolvePath(ToolExecutionRequest request, BasePathResolver resolver) {
        if (request == null) {
            return UNSPECIFIED_PATH;
        }

        if (request.getOperation() == ToolExecutionRequest.Operation.RUN_SCRIPT) {
            String script = sanitizeInline(request.getScript());
            return (script == null || script.isBlank()) ? UNSPECIFIED_PATH : script;
        }

        String base = sanitizeInline(request.getBase());
        String path = sanitizeInline(request.getPath());
        if (path == null || path.isBlank()) {
            if (base == null || base.isBlank()) {
                return UNSPECIFIED_PATH;
            }
            return base + ":";
        }

        if (resolver == null || base == null || base.isBlank()) {
            return path;
        }

        try {
            return sanitizeInline(resolver.qualify(base, path));
        } catch (Exception ex) {
            return path;
        }
    }

    private String resolveIntention(ToolExecutionRequest request) {
        if (request == null) {
            return UNSPECIFIED_INTENTION;
        }
        String intention = sanitizeInline(redactSensitivePairs(request.getIntent()));
        if (intention == null || intention.isBlank()) {
            return UNSPECIFIED_INTENTION;
        }
        return intention;
    }

    private String sanitizeInline(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = LogSanitizer.sanitizeForLog(value).replace('\t', ' ');
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 240) {
            return cleaned.substring(0, 237) + "...";
        }
        return cleaned;
    }

    private String redactSensitivePairs(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.replaceAll("(?i)(api[_-]?key|token|secret|password)\\s*[:=]\\s*\\S+", "$1=<redacted>");
    }
}