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

import br.com.dizeno.reins.compilation.tracking.CleanupOutcomeSummary;
import br.com.dizeno.reins.compilation.tracking.CleanupReporter;
import br.com.dizeno.reins.compilation.tracking.CleanupService;
import br.com.dizeno.reins.security.PathValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;

 
/**
 * CleanMojo is part of the general application functions in the reins architecture.
 * Maven goal that cleans up generated target files and tracking states.
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class CleanMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "false")
    private boolean verbose;

    @Parameter(defaultValue = "false")
    private boolean dryRun;

    @Parameter(defaultValue = "true")
    private boolean failOnError;

    @Parameter(property = "skipReins")
    private String skipReins;

    @Parameter(property = "source")
    private String source;

    /**
     * Executes the operation.
     *
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        SkipDecision skipDecision = new SkipReinsResolver().resolve(skipReins);
        if (skipDecision.shouldFail()) {
            throw new MojoExecutionException(SkipReinsMessages.invalidValueMessage("reins:clean", skipDecision));
        }
        if (skipDecision.shouldSkip()) {
            getLog().info(SkipReinsMessages.skipMessage("reins:clean", skipDecision));
            return;
        }

        try {
            br.com.dizeno.reins.run.config.ReinsConfig config = new br.com.dizeno.reins.run.config.ReinsConfig();
            config.setVerbose(verbose);
            config.setDryRun(dryRun);
            config.setFailOnError(failOnError);
            if (source != null && !source.isBlank()) {
                config.setSource(source);
                config.setExplicitSourceMode(true);
            }

            new br.com.dizeno.reins.run.ReinsRunner().clean(config, project.getBasedir(), getLog());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cleanup failed with")) {
                throw new MojoFailureException(e.getMessage(), e);
            }
            throw new MojoExecutionException("Cleanup execution failed", e);
        } catch (Exception e) {
            throw new MojoExecutionException("Cleanup execution failed", e);
        }
    }

    /**
     * Gets the project.
     *
     * @return the resolved or constructed object
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * Checks if the component is verbose.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Checks if the component is dry run.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Checks if the component is fail on error.
     *
     * @return true if successful or matching, false otherwise
     */
    public boolean isFailOnError() {
        return failOnError;
    }
}
