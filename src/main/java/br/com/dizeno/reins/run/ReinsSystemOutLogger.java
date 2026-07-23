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

package br.com.dizeno.reins.run;

import org.apache.maven.plugin.logging.Log;

/**
 * ReinsSystemOutLogger is part of the entry points and integrations for running reins via Mojo, CLI, or library programmatic access in the reins architecture.
 * Acts as a component managing reins system out logger.
 */
public class ReinsSystemOutLogger implements Log {
    private final boolean debug;
    private final boolean verbose;

    /**
     * Constructs a new instance of {@link ReinsSystemOutLogger}.
     *
     * @param debug the debug
     * @param verbose the verbose
     */
    public ReinsSystemOutLogger(boolean debug, boolean verbose) {
        this.debug = debug;
        this.verbose = verbose;
    }

    /**
     * Checks if the component is debug enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean isDebugEnabled() {
        return debug;
    }

    /**
     * Debug.
     *
     * @param content the content
     */
    @Override
    public void debug(CharSequence content) {
        if (debug) {
            System.out.println("[DEBUG] " + content);
        }
    }

    /**
     * Debug.
     *
     * @param content the content
     * @param error the error
     */
    @Override
    public void debug(CharSequence content, Throwable error) {
        if (debug) {
            System.out.println("[DEBUG] " + content);
            if (error != null) {
                error.printStackTrace(System.out);
            }
        }
    }

    /**
     * Debug.
     *
     * @param error the error
     */
    @Override
    public void debug(Throwable error) {
        if (debug && error != null) {
            error.printStackTrace(System.out);
        }
    }

    /**
     * Checks if the component is info enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    /**
     * Info.
     *
     * @param content the content
     */
    @Override
    public void info(CharSequence content) {
        System.out.println("[INFO] " + content);
    }

    /**
     * Info.
     *
     * @param content the content
     * @param error the error
     */
    @Override
    public void info(CharSequence content, Throwable error) {
        System.out.println("[INFO] " + content);
        if (error != null) {
            error.printStackTrace(System.out);
        }
    }

    /**
     * Info.
     *
     * @param error the error
     */
    @Override
    public void info(Throwable error) {
        if (error != null) {
            error.printStackTrace(System.out);
        }
    }

    /**
     * Checks if the component is warn enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    /**
     * Warn.
     *
     * @param content the content
     */
    @Override
    public void warn(CharSequence content) {
        System.err.println("[WARN] " + content);
    }

    /**
     * Warn.
     *
     * @param content the content
     * @param error the error
     */
    @Override
    public void warn(CharSequence content, Throwable error) {
        System.err.println("[WARN] " + content);
        if (error != null) {
            error.printStackTrace(System.err);
        }
    }

    /**
     * Warn.
     *
     * @param error the error
     */
    @Override
    public void warn(Throwable error) {
        if (error != null) {
            error.printStackTrace(System.err);
        }
    }

    /**
     * Checks if the component is error enabled.
     *
     * @return true if successful or matching, false otherwise
     */
    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    /**
     * Error.
     *
     * @param content the content
     */
    @Override
    public void error(CharSequence content) {
        System.err.println("[ERROR] " + content);
    }

    /**
     * Error.
     *
     * @param content the content
     * @param error the error
     */
    @Override
    public void error(CharSequence content, Throwable error) {
        System.err.println("[ERROR] " + content);
        if (error != null) {
            error.printStackTrace(System.err);
        }
    }

    /**
     * Error.
     *
     * @param error the error
     */
    @Override
    public void error(Throwable error) {
        if (error != null) {
            error.printStackTrace(System.err);
        }
    }
}
