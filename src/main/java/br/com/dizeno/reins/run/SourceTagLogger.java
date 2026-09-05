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
 * SourceTagLogger is part of the logging components in the reins architecture.
 * Wraps a {@link Log} instance to prefix all log messages emitted during a source file's
 * reasoning cycle with [simple-file-name].
 */
public class SourceTagLogger implements Log {

    private final Log delegate;
    private final String prefix;

    /**
     * Constructs a new instance of {@link SourceTagLogger}.
     *
     * @param delegate the underlying logger instance
     * @param sourceName the simple file name to tag log messages with
     */
    public SourceTagLogger(Log delegate, String sourceName) {
        this.delegate = delegate;
        this.prefix = "[" + sourceName + "] ";
    }

    /**
     * Gets the underlying delegate logger.
     *
     * @return the delegate logger
     */
    public Log getDelegate() {
        return delegate;
    }

    /**
     * Gets the prefix used by this logger.
     *
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate != null && delegate.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence content) {
        if (delegate != null) {
            delegate.debug(prefix + content);
        }
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        if (delegate != null) {
            delegate.debug(prefix + content, error);
        }
    }

    @Override
    public void debug(Throwable error) {
        if (delegate != null) {
            delegate.debug(error);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate == null || delegate.isInfoEnabled();
    }

    @Override
    public void info(CharSequence content) {
        if (delegate != null) {
            delegate.info(prefix + content);
        }
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        if (delegate != null) {
            delegate.info(prefix + content, error);
        }
    }

    @Override
    public void info(Throwable error) {
        if (delegate != null) {
            delegate.info(error);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate == null || delegate.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence content) {
        if (delegate != null) {
            delegate.warn(prefix + content);
        }
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        if (delegate != null) {
            delegate.warn(prefix + content, error);
        }
    }

    @Override
    public void warn(Throwable error) {
        if (delegate != null) {
            delegate.warn(error);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate == null || delegate.isErrorEnabled();
    }

    @Override
    public void error(CharSequence content) {
        if (delegate != null) {
            delegate.error(prefix + content);
        }
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        if (delegate != null) {
            delegate.error(prefix + content, error);
        }
    }

    @Override
    public void error(Throwable error) {
        if (delegate != null) {
            delegate.error(error);
        }
    }
}
