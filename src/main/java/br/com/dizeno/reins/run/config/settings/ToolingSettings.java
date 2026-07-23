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

/**
 * ToolingSettings is part of the general application functions in the reins architecture.
 * Acts as a configuration data holder for its prefix settings.
 */
public class ToolingSettings {
    private String main;
    private String test;
    private String target;
    private String scriptPath;
    private boolean addReasoningNotes;
    private String scriptDir;

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
     * Gets the target.
     *
     * @return the string result
     */
    public String getTarget() {
        return target;
    }

    /**
     * Sets the target.
     *
     * @param target the target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Gets the script path.
     *
     * @return the string result
     */
    public String getScriptPath() {
        return scriptPath;
    }

    /**
     * Sets the script path.
     *
     * @param scriptPath the script path
     */
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * Checks if the component is add reasoning notes.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isAddReasoningNotes() {
        return addReasoningNotes;
    }

    /**
     * Sets the add reasoning notes.
     *
     * @param addReasoningNotes the add reasoning notes
     */
    public void setAddReasoningNotes(boolean addReasoningNotes) {
        this.addReasoningNotes = addReasoningNotes;
    }

    /**
     * Gets the script dir.
     *
     * @return the string result
     */
    public String getScriptDir() {
        return scriptDir;
    }

    /**
     * Sets the script dir.
     *
     * @param scriptDir the script dir
     */
    public void setScriptDir(String scriptDir) {
        this.scriptDir = scriptDir;
    }
}
