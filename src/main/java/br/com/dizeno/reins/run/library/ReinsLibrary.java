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

package br.com.dizeno.reins.run.library;

import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.tracking.CleanupOutcomeSummary;
import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.run.ReinsRunner;
import br.com.dizeno.reins.run.ReinsSystemOutLogger;
import br.com.dizeno.reins.run.config.*;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * ReinsLibrary is part of the general application functions in the reins architecture.
 * Library entrypoint providing Reins features to external programmatic clients.
 */
public class ReinsLibrary {

    
    
    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, Map<String, Object> config) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return compile(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, Map<String, Object> config, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return compile(baseDir, pluginConfig, log);
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, Properties properties) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return compile(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, Properties properties, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return compile(baseDir, pluginConfig, log);
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, File configFile) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return compile(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, File configFile, Log log) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return compile(baseDir, pluginConfig, log);
    }

    /**
     * Compile.
     *
     * @param baseDir the base dir
     * @param pluginConfig the plugin config
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CompilationSummary compile(File baseDir, ReinsConfig pluginConfig, Log log) throws Exception {
        return new ReinsRunner().compile(pluginConfig, baseDir, log);
    }

    

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, Map<String, Object> config) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return clean(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, Map<String, Object> config, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return clean(baseDir, pluginConfig, log);
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, Properties properties) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return clean(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, Properties properties, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return clean(baseDir, pluginConfig, log);
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, File configFile) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return clean(baseDir, pluginConfig, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, File configFile, Log log) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return clean(baseDir, pluginConfig, log);
    }

    /**
     * Clean.
     *
     * @param baseDir the base dir
     * @param pluginConfig the plugin config
     * @param log the logger instance
     * @return the resulting summary
     */
    public static CleanupOutcomeSummary clean(File baseDir, ReinsConfig pluginConfig, Log log) throws Exception {
        return new ReinsRunner().clean(pluginConfig, baseDir, log);
    }

    

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @return the numeric value
     */
    public static int addNote(File baseDir, Map<String, Object> config, String source, String note, ReasoningNote.Origin origin) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param config the Reins configuration settings
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @param log the logger instance
     * @return the numeric value
     */
    public static int addNote(File baseDir, Map<String, Object> config, String source, String note, ReasoningNote.Origin origin, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, config, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, log);
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @return the numeric value
     */
    public static int addNote(File baseDir, Properties properties, String source, String note, ReasoningNote.Origin origin) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param properties the properties
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @param log the logger instance
     * @return the numeric value
     */
    public static int addNote(File baseDir, Properties properties, String source, String note, ReasoningNote.Origin origin, Log log) throws Exception {
        ReinsConfig pluginConfig = ReinsConfigLoader.load(null, null, properties, null, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, log);
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @return the numeric value
     */
    public static int addNote(File baseDir, File configFile, String source, String note, ReasoningNote.Origin origin) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, new ReinsSystemOutLogger(pluginConfig.isVerbose(), pluginConfig.isVerbose()));
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param configFile the config file
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @param log the logger instance
     * @return the numeric value
     */
    public static int addNote(File baseDir, File configFile, String source, String note, ReasoningNote.Origin origin, Log log) throws Exception {
        String[] cliArgs = configFile != null ? new String[]{"--config", configFile.getAbsolutePath()} : null;
        ReinsConfig pluginConfig = ReinsConfigLoader.load(cliArgs, null, null, baseDir);
        return addNote(baseDir, pluginConfig, source, note, origin, log);
    }

    /**
     * Add Note.
     *
     * @param baseDir the base dir
     * @param pluginConfig the plugin config
     * @param source the source
     * @param note the note
     * @param origin the origin
     * @param log the logger instance
     * @return the numeric value
     */
    public static int addNote(File baseDir, ReinsConfig pluginConfig, String source, String note, ReasoningNote.Origin origin, Log log) throws Exception {
        return new ReinsRunner().addNote(pluginConfig, baseDir, source, note, origin, log);
    }
}
