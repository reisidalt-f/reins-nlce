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

package br.com.dizeno.reins.testutil;

import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class RecordingLog implements Log {
    private final List<String> debugMessages = new ArrayList<>();
    private final List<String> infoMessages = new ArrayList<>();
    private final List<String> warnMessages = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(CharSequence content) {
        debugMessages.add(String.valueOf(content));
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        debugMessages.add(String.valueOf(content));
    }

    @Override
    public void debug(Throwable error) {
        debugMessages.add(error == null ? null : error.getMessage());
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(CharSequence content) {
        infoMessages.add(String.valueOf(content));
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        infoMessages.add(String.valueOf(content));
    }

    @Override
    public void info(Throwable error) {
        infoMessages.add(error == null ? null : error.getMessage());
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(CharSequence content) {
        warnMessages.add(String.valueOf(content));
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        warnMessages.add(String.valueOf(content));
    }

    @Override
    public void warn(Throwable error) {
        warnMessages.add(error == null ? null : error.getMessage());
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(CharSequence content) {
        errorMessages.add(String.valueOf(content));
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        errorMessages.add(String.valueOf(content));
    }

    @Override
    public void error(Throwable error) {
        errorMessages.add(error == null ? null : error.getMessage());
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<String> getDebugMessages() {
        return debugMessages;
    }

    public List<String> getWarnMessages() {
        return warnMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean hasInfoContaining(String needle) {
        return infoMessages.stream().anyMatch(msg -> msg != null && msg.contains(needle));
    }

    public boolean hasInfoStartingWith(String prefix) {
        return infoMessages.stream().anyMatch(msg -> msg != null && msg.startsWith(prefix));
    }

    public boolean hasDebugContaining(String needle) {
        return debugMessages.stream().anyMatch(msg -> msg != null && msg.contains(needle));
    }

    public boolean hasWarnContaining(String needle) {
        return warnMessages.stream().anyMatch(msg -> msg != null && msg.contains(needle));
    }
}
