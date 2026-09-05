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

package br.com.dizeno.reins.run.mojo;

import br.com.dizeno.reins.run.ReinsRunner;
import br.com.dizeno.reins.run.config.ReinsConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * ClearReasoningNotesMojo is part of the general application functions in the reins architecture.
 * Maven goal that clears reasoning notes across scanned or designated source files.
 */
@Mojo(name = "clearNotes", requiresProject = true)
public class ClearReasoningNotesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "source", required = false)
    private String source;

    /**
     * Executes the operation.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            ReinsConfig config = new ReinsConfig();
            new ReinsRunner().clearNotes(config, project.getBasedir(), source, getLog());
        } catch (Exception ex) {
            throw new MojoExecutionException(
                    "Failed to clear reasoning notes: " + ex.getMessage(), ex);
        }
    }
}
