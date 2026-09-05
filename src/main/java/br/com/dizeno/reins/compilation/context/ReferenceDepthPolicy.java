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

package br.com.dizeno.reins.compilation.context;

/**
 * ReferenceDepthPolicy is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
 * Acts as a component managing reference depth policy.
 */
public final class ReferenceDepthPolicy {
    /**
     * Mode is part of the loading and representation of project-wide compile contexts, scan roots, and reference depth policies in the reins architecture.
     * Acts as a component managing mode.
     */
    public enum Mode {
        DISABLED,
        BOUNDED,
        UNLIMITED
    }

    private static final String VALID_VALUES_MESSAGE =
            "context.referencesTree.depth must be 0, a non-negative integer, or *.";

    private final Mode mode;
    private final Integer maxDepth;
    private final String rawValue;
    private final boolean defaulted;

    private ReferenceDepthPolicy(Mode mode, Integer maxDepth, String rawValue, boolean defaulted) {
        this.mode = mode;
        this.maxDepth = maxDepth;
        this.rawValue = rawValue;
        this.defaulted = defaulted;
    }

    /**
     * Default Policy.
     *
     * @return the resolved or constructed object
     */
    public static ReferenceDepthPolicy defaultPolicy() {
        return new ReferenceDepthPolicy(Mode.BOUNDED, 3, "3", true);
    }

    /**
     * Parse.
     *
     * @param rawValue the raw value
     * @return the resolved or constructed object
     */
    public static ReferenceDepthPolicy parse(String rawValue) {
        if (rawValue == null) {
            return defaultPolicy();
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            throw invalidValue();
        }
        if ("*".equals(trimmed)) {
            return new ReferenceDepthPolicy(Mode.UNLIMITED, null, trimmed, false);
        }

        int numericValue;
        try {
            numericValue = Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw invalidValue();
        }

        if (numericValue < 0) {
            throw invalidValue();
        }
        if (numericValue == 0) {
            return new ReferenceDepthPolicy(Mode.DISABLED, 0, trimmed, false);
        }
        return new ReferenceDepthPolicy(Mode.BOUNDED, numericValue, trimmed, false);
    }

    private static IllegalArgumentException invalidValue() {
        return new IllegalArgumentException(VALID_VALUES_MESSAGE);
    }

    /**
     * Gets the mode.
     *
     * @return the resolved or constructed object
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Gets the max depth.
     *
     * @return the numeric value
     */
    public Integer getMaxDepth() {
        return maxDepth;
    }

    /**
     * Gets the raw value.
     *
     * @return the string result
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * Checks if the component is defaulted.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDefaulted() {
        return defaulted;
    }

    /**
     * Checks if the component is disabled.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDisabled() {
        return mode == Mode.DISABLED;
    }

    /**
     * Checks if the component is bounded.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isBounded() {
        return mode == Mode.BOUNDED;
    }

    /**
     * Checks if the component is unlimited.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isUnlimited() {
        return mode == Mode.UNLIMITED;
    }

    /**
     * Includes Depth.
     *
     * @param depthFromRoot the depth from root
     * @return true if successful or matching, false otherwise
     */
    public boolean includesDepth(int depthFromRoot) {
        if (depthFromRoot < 0) {
            return false;
        }
        if (isUnlimited()) {
            return true;
        }
        if (isDisabled()) {
            return false;
        }
        return depthFromRoot <= maxDepth;
    }

    /**
     * Can Traverse Children.
     *
     * @param currentDepthFromRoot the current depth from root
     * @return true if successful or matching, false otherwise
     */
    public boolean canTraverseChildren(int currentDepthFromRoot) {
        if (isUnlimited()) {
            return true;
        }
        if (isDisabled()) {
            return false;
        }
        return currentDepthFromRoot < maxDepth;
    }

    /**
     * To Render Max Depth.
     *
     * @return the numeric value
     */
    public int toRenderMaxDepth() {
        if (isUnlimited()) {
            return Integer.MAX_VALUE;
        }
        if (isDisabled()) {
            return 0;
        }
        return maxDepth;
    }

    /**
     * Describe For Log.
     *
     * @return the string result
     */
    public String describeForLog() {
        if (isUnlimited()) {
            return "unlimited (*)";
        }
        if (isDisabled()) {
            return "disabled (0)";
        }
        if (defaulted) {
            return "default (3)";
        }
        return "bounded (" + maxDepth + ")";
    }

    /**
     * Valid Values Message.
     *
     * @return the string result
     */
    public static String validValuesMessage() {
        return VALID_VALUES_MESSAGE;
    }
}