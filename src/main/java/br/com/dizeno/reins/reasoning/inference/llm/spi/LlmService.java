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

package br.com.dizeno.reins.reasoning.inference.llm.spi;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmRequest;
import br.com.dizeno.reins.reasoning.inference.llm.model.LlmResponse;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * LlmService is part of the general application functions in the reins architecture.
 * Acts as a service component responsible for managing and executing operations related to its prefix.
 */
public interface LlmService {
    /**
     * Sets the log.
     *
     * @param log the logger instance
     */
    void setLog(Log log);

    /**
     * Validates the inputs or files compatibility.
     *
     * @param config the Reins configuration settings
     */
    void validateCompatibility(ReinsConfig config);

    /**
     * Invoke.
     *
     * @param request the request containing path and scope metadata
     * @param config the Reins configuration settings
     * @return the resolved or constructed object
     */
    LlmResponse invoke(LlmRequest request, ReinsConfig config) throws Exception;

    /**
     * Creates a new resource cached content.
     *
     * @param config the Reins configuration settings
     * @param systemMessages the system messages
     * @return the string result
     */
    String createCachedContent(ReinsConfig config, List<br.com.dizeno.reins.reasoning.scripting.ConversationMessage> systemMessages) throws Exception;

    /**
     * Deletes the target cached content.
     *
     * @param config the Reins configuration settings
     * @param cachedContentId the cached content id
     */
    void deleteCachedContent(ReinsConfig config, String cachedContentId) throws Exception;
}
