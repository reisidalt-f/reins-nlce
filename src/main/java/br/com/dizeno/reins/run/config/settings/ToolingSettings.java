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
    private java.util.Map<String, String> bases = new java.util.LinkedHashMap<>();
    private String scriptPath;
    private boolean addReasoningNotes;
    private String scriptDir;
    private boolean grantFileOwnership = true;

    /**
     * Gets all configured tooling bases.
     *
     * @return map of base names to permission tokens
     */
    public java.util.Map<String, String> getBases() {
        return bases;
    }

    /**
     * Sets all configured tooling bases.
     *
     * @param bases map of base names to permission tokens
     */
    public void setBases(java.util.Map<String, String> bases) {
        this.bases = bases != null ? new java.util.LinkedHashMap<>(bases) : new java.util.LinkedHashMap<>();
        if (this.bases.containsKey("main")) this.main = this.bases.get("main");
        if (this.bases.containsKey("test")) this.test = this.bases.get("test");
        if (this.bases.containsKey("target")) this.target = this.bases.get("target");
    }

    /**
     * Gets permissions for a specific base name.
     *
     * @param name base name
     * @return permission token string
     */
    public String getToolingBase(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (bases.containsKey(normalized)) {
            return bases.get(normalized);
        }
        return switch (normalized) {
            case "main" -> main;
            case "test" -> test;
            case "target" -> target;
            default -> null;
        };
    }

    /**
     * Sets permissions for a specific base name.
     *
     * @param name base name
     * @param value permission token string
     */
    public void setToolingBase(String name, String value) {
        if (name == null || name.isBlank()) {
            return;
        }
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        bases.put(normalized, value);
        if ("main".equals(normalized)) {
            this.main = value;
        } else if ("test".equals(normalized)) {
            this.test = value;
        } else if ("target".equals(normalized)) {
            this.target = value;
        }
    }

    /**
     * Gets the main.
     *
     * @return the string result
     */
    public String getMain() {
        return getToolingBase("main");
    }

    /**
     * Sets the main.
     *
     * @param main the main
     */
    public void setMain(String main) {
        setToolingBase("main", main);
    }

    /**
     * Gets the test.
     *
     * @return the string result
     */
    public String getTest() {
        return getToolingBase("test");
    }

    /**
     * Sets the test.
     *
     * @param test the test
     */
    public void setTest(String test) {
        setToolingBase("test", test);
    }

    /**
     * Gets the target.
     *
     * @return the string result
     */
    public String getTarget() {
        return getToolingBase("target");
    }

    /**
     * Sets the target.
     *
     * @param target the target
     */
    public void setTarget(String target) {
        setToolingBase("target", target);
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

    /**
     * Checks if file ownership granting is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isGrantFileOwnership() {
        return grantFileOwnership;
    }

    /**
     * Sets whether file ownership granting is enabled.
     *
     * @param grantFileOwnership whether file ownership granting is enabled
     */
    public void setGrantFileOwnership(boolean grantFileOwnership) {
        this.grantFileOwnership = grantFileOwnership;
    }
}
