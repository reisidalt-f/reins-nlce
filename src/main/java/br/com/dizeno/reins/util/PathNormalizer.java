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

package br.com.dizeno.reins.util;

import java.nio.file.Path;

 
/**
 * PathNormalizer is part of the shared utility components like path normalizers and log sanitizers in the reins architecture.
 * Acts as a component managing path normalizer.
 */
public final class PathNormalizer {

    private PathNormalizer() {
        
    }

     
    /**
     * To Forward Slashes.
     *
     * @param path the file or directory path
     * @return the string result
     */
    public static String toForwardSlashes(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

     
    /**
     * To Forward Slashes.
     *
     * @param path the file or directory path
     * @return the string result
     */
    public static String toForwardSlashes(Path path) {
        return toForwardSlashes(path.toString());
    }
}
