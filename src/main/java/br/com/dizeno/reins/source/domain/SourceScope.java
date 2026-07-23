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

package br.com.dizeno.reins.source.domain;

 
/**
 * SourceScope is part of the general application functions in the reins architecture.
 * Acts as a component managing source scope.
 */
public enum SourceScope {

     
    MAIN("main"),

     
    TEST("test"),

     
    PROJECT("project"),

     
    UNCLASSIFIED("unclassified");

    private final String value;

    SourceScope(String value) {
        this.value = value;
    }

     
    /**
     * Value.
     *
     * @return the string result
     */
    public String value() {
        return value;
    }

    /**
     * To String.
     *
     * @return the string result
     */
    @Override
    public String toString() {
        return value;
    }

     
    /**
     * From Path.
     *
     * @param projectRelativePath the project relative path
     * @return the resolved or constructed object
     */
    public static SourceScope fromPath(String projectRelativePath) {
        if (projectRelativePath == null) {
            return UNCLASSIFIED;
        }
        String normalized = projectRelativePath.replace('\\', '/');
        if (normalized.startsWith(ProjectDirectoryPaths.MAIN_NL_ROOT + "/")) {
            return MAIN;
        }
        if (normalized.startsWith(ProjectDirectoryPaths.TEST_NL_ROOT + "/")) {
            return TEST;
        }
        return UNCLASSIFIED;
    }

     
    /**
     * From Value.
     *
     * @param value the value
     * @return the resolved or constructed object
     */
    public static SourceScope fromValue(String value) {
        if (value == null) {
            return UNCLASSIFIED;
        }
        for (SourceScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        return UNCLASSIFIED;
    }
}
