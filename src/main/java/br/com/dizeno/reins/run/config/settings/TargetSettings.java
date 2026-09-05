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

package br.com.dizeno.reins.run.config.settings;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TargetSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for target settings.
 */
public class TargetSettings {
    private String main;
    private String test;
    private Map<String, String> targetBases = new LinkedHashMap<>();

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
        if (main != null) {
            setTargetBase("main", main);
        }
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
        if (test != null) {
            setTargetBase("test", test);
        }
    }

    public Map<String, String> getTargetBases() {
        return targetBases;
    }

    public void setTargetBases(Map<String, String> targetBases) {
        this.targetBases = targetBases != null ? new LinkedHashMap<>(targetBases) : new LinkedHashMap<>();
    }

    public String getTargetBase(String name) {
        return name == null ? null : targetBases.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public void setTargetBase(String name, String value) {
        if (name != null && value != null) {
            String normalizedKey = name.toLowerCase(java.util.Locale.ROOT);
            this.targetBases.put(normalizedKey, value);
            if ("main".equals(normalizedKey)) {
                this.main = value;
            } else if ("test".equals(normalizedKey)) {
                this.test = value;
            }
        }
    }

    public boolean hasTargetBase(String name) {
        return name != null && targetBases.containsKey(name.toLowerCase(java.util.Locale.ROOT));
    }

    public Path resolveTargetOutput(String baseName, Path projectRoot) {
        if (baseName == null || baseName.isBlank()) {
            throw new IllegalStateException("Target output directory is not configured for scope: " + baseName);
        }
        String key = baseName.toLowerCase(java.util.Locale.ROOT);
        String configuredPath = targetBases.get(key);
        if (configuredPath == null || configuredPath.isBlank()) {
            if ("main".equals(key) && main != null && !main.isBlank()) {
                configuredPath = main;
            } else if ("test".equals(key) && test != null && !test.isBlank()) {
                configuredPath = test;
            } else if (targetBases.containsKey("main")) {
                configuredPath = targetBases.get("main");
            } else if (main != null && !main.isBlank()) {
                configuredPath = main;
            }
        }
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("Target output directory is not configured for scope: " + baseName);
        }
        Path path = Path.of(configuredPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return projectRoot.resolve(path).toAbsolutePath().normalize();
    }

    public Path resolveMainOutput(Path projectRoot) {
        return resolveTargetOutput("main", projectRoot);
    }

    public Path resolveTestOutput(Path projectRoot) {
        return resolveTargetOutput("test", projectRoot);
    }
}
