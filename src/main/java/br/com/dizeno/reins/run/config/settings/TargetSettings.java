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

/**
 * TargetSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class TargetSettings {
    private File project;

    @Deprecated
    private File root;
    private String main;
    private String test;

    /**
     * Gets the project.
     *
     * @return the resolved or constructed object
     */
    public File getProject() {
        return project;
    }

    /**
     * Sets the project.
     *
     * @param project the project
     */
    public void setProject(File project) {
        this.project = project;
    }

    /**
     * Gets the root.
     *
     * @return the resolved or constructed object
     */
    @Deprecated
    public File getRoot() {
        return root;
    }

    /**
     * Sets the root.
     *
     * @param root the root path of the project
     */
    @Deprecated
    public void setRoot(File root) {
        this.root = root;
    }

    /**
     * Gets the legacy root alias.
     *
     * @return the resolved or constructed object
     */
    public File getLegacyRootAlias() {
        return root;
    }

    /**
     * Sets the legacy root alias.
     *
     * @param root the root path of the project
     */
    public void setLegacyRootAlias(File root) {
        this.root = root;
    }

    /**
     * Gets the main.
     *
     * @return the string result
     */
    public String getMain() {
        return main;
    }

    /**
     * Sets the main.
     *
     * @param main the main
     */
    public void setMain(String main) {
        this.main = main;
    }

    /**
     * Gets the test.
     *
     * @return the string result
     */
    public String getTest() {
        return test;
    }

    /**
     * Sets the test.
     *
     * @param test the test
     */
    public void setTest(String test) {
        this.test = test;
    }

    /**
     * Resolves the configured value or path project target.
     *
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    public Path resolveProjectTarget(Path projectRoot) {
        File configured = project != null ? project : root;
        if (configured == null) {
            return projectRoot.toAbsolutePath().normalize();
        }
        Path configuredPath = configured.toPath();
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return projectRoot.resolve(configuredPath).toAbsolutePath().normalize();
    }

    /**
     * Resolves the configured value or path root.
     *
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    @Deprecated
    public Path resolveRoot(Path projectRoot) {
        return resolveProjectTarget(projectRoot);
    }

    /**
     * Resolves the configured value or path main output.
     *
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    public Path resolveMainOutput(Path projectRoot) {
        if (main == null || main.isBlank()) {
            return projectRoot.resolve("src/main/java").toAbsolutePath().normalize();
        }
        Path mainPath = Path.of(main);
        if (mainPath.isAbsolute()) {
            return mainPath.normalize();
        }
        return projectRoot.resolve(mainPath).toAbsolutePath().normalize();
    }

    /**
     * Resolves the configured value or path test output.
     *
     * @param projectRoot the root path of the project
     * @return the resolved or constructed object
     */
    public Path resolveTestOutput(Path projectRoot) {
        if (test == null || test.isBlank()) {
            return projectRoot.resolve("src/test/java").toAbsolutePath().normalize();
        }
        Path testPath = Path.of(test);
        if (testPath.isAbsolute()) {
            return testPath.normalize();
        }
        return projectRoot.resolve(testPath).toAbsolutePath().normalize();
    }
}
