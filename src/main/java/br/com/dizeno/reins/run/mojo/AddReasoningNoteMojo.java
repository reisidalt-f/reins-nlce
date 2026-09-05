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
import br.com.dizeno.reins.run.config.*;

import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

 
/**
 * AddReasoningNoteMojo is part of the general application functions in the reins architecture.
 * Maven goal that appends diagnostic notes or instructions to the compilation cycle.
 */
@Mojo(name = "addNote", requiresProject = true)
public class AddReasoningNoteMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

     
    @Parameter(property = "source", required = true)
    private String source;

     
    @Parameter(property = "note", required = true)
    private String note;

    /**
     * Executes the operation.
     *
     */
    @Override
    public void execute() throws MojoExecutionException {
        if (source == null || source.isBlank()) {
            throw new MojoExecutionException("'source' parameter is required and must not be blank.");
        }
        if (note == null || note.isBlank()) {
            throw new MojoExecutionException("'note' parameter is required and must not be blank.");
        }

        try {
            br.com.dizeno.reins.run.config.ReinsConfig config = new br.com.dizeno.reins.run.config.ReinsConfig();
            new br.com.dizeno.reins.run.ReinsRunner().addNote(config, project.getBasedir(), source, note, ReasoningNote.Origin.MAVEN_GOAL, getLog());
        } catch (IllegalArgumentException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new MojoExecutionException(
                    "Failed to append reasoning note for source '" + source + "': " + ex.getMessage(), ex);
        }
    }
}
