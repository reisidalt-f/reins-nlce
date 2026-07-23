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
 * ProjectDirectoryPaths is part of the general application functions in the reins architecture.
 * Acts as a component managing project directory paths.
 */
public final class ProjectDirectoryPaths {

     
    public static final String MAIN_NL_ROOT = "src/main/nl";

     
    public static final String TEST_NL_ROOT = "src/test/nl";

     
    public static final String MAIN_JAVA_ROOT = "src/main/java";

     
    public static final String TEST_JAVA_ROOT = "src/test/java";

     
    public static final String MAIN_RESOURCES_ROOT = "src/main/resources";

     
    public static final String TEST_RESOURCES_ROOT = "src/test/resources";

     
    public static final String DEFAULT_INCLUDE_PATTERN = "**/*.md";


     
    public static final String COMPILATION_TRACKING_DIR = ".reins/compilation-tracking";

    private ProjectDirectoryPaths() {
        
    }
}
