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

package br.com.dizeno.reins.reasoning.tooling.file;

import br.com.dizeno.reins.run.config.*;
import br.com.dizeno.reins.run.config.settings.*;

import java.io.File;
import java.nio.file.Path;

/**
 * BasePathMappingSet is part of the general application functions in the reins architecture.
 * Acts as a component managing base path mapping set.
 */
public class BasePathMappingSet {
    private Path mainRoot;
    private Path testRoot;
    private Path targetRoot;
    private Path mainTargetRoot;
    private Path testTargetRoot;
    private Path scriptRoot;

    /**
     * From Config.
     *
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @return the collection of elements
     */
    public static BasePathMappingSet fromConfig(ReinsConfig config, Path projectRoot) {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(resolveBaseRoot(config.getMainNlRoot(), projectRoot.resolve("src/main/nl"), projectRoot));
        mappings.setTestRoot(resolveBaseRoot(config.getTestNlRoot(), projectRoot.resolve("src/test/nl"), projectRoot));
        mappings.setTargetRoot(config.getTarget() != null
            ? config.getTarget().resolveProjectTarget(projectRoot)
                : projectRoot.toAbsolutePath().normalize());
        mappings.setScriptRoot(resolveScriptRoot(config, projectRoot));
        return mappings;
    }

     
    /**
     * For Scope.
     *
     * @param config the Reins configuration settings
     * @param projectRoot the root path of the project
     * @param sourceScope the source scope
     * @return the collection of elements
     */
    public static BasePathMappingSet forScope(ReinsConfig config, Path projectRoot, String sourceScope) {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(resolveBaseRoot(config.getMainNlRoot(), projectRoot.resolve("src/main/nl"), projectRoot));
        mappings.setTestRoot(resolveBaseRoot(config.getTestNlRoot(), projectRoot.resolve("src/test/nl"), projectRoot));
        if (config.getTarget() == null) {
            Path project = projectRoot.toAbsolutePath().normalize();
            mappings.setMainTargetRoot(project);
            mappings.setTestTargetRoot(project);
            mappings.setTargetRoot(project);
        } else if ("main".equals(sourceScope)) {
            mappings.setMainTargetRoot(config.getTarget().resolveMainOutput(projectRoot));
            mappings.setTestTargetRoot(config.getTarget().resolveTestOutput(projectRoot));
            mappings.setTargetRoot(mappings.getMainTargetRoot());
        } else if ("test".equals(sourceScope)) {
            mappings.setMainTargetRoot(config.getTarget().resolveMainOutput(projectRoot));
            mappings.setTestTargetRoot(config.getTarget().resolveTestOutput(projectRoot));
            mappings.setTargetRoot(mappings.getTestTargetRoot());
        } else {
            Path projectTarget = config.getTarget().resolveProjectTarget(projectRoot);
            mappings.setMainTargetRoot(config.getTarget().resolveMainOutput(projectRoot));
            mappings.setTestTargetRoot(config.getTarget().resolveTestOutput(projectRoot));
            mappings.setTargetRoot(projectTarget);
        }
        mappings.setScriptRoot(resolveScriptRoot(config, projectRoot));
        return mappings;
    }

     
    /**
     * For Project Inference.
     *
     * @param projectRoot the root path of the project
     * @param targetRoot the target root
     * @return the collection of elements
     */
    public static BasePathMappingSet forProjectInference(Path projectRoot, Path targetRoot) {
        return forProjectInference(projectRoot, targetRoot, null);
    }

    /**
     * For Project Inference.
     *
     * @param projectRoot the root path of the project
     * @param targetRoot the target root
     * @param scriptRoot the script root
     * @return the collection of elements
     */
    public static BasePathMappingSet forProjectInference(Path projectRoot, Path targetRoot, Path scriptRoot) {
        BasePathMappingSet mappings = new BasePathMappingSet();
        mappings.setMainRoot(projectRoot.toAbsolutePath().normalize());
        mappings.setTargetRoot(targetRoot.toAbsolutePath().normalize());
        mappings.setTestRoot(null);
        mappings.setScriptRoot(scriptRoot == null ? null : scriptRoot.toAbsolutePath().normalize());
        return mappings;
    }

    private static Path resolveScriptRoot(ReinsConfig config, Path projectRoot) {
        if (config == null || config.getReasoning() == null) {
            return null;
        }
        String scriptPath = config.getReasoning().getScriptsPath();
        if (scriptPath == null || scriptPath.isBlank()) {
            return null;
        }
        Path configured = Path.of(scriptPath.trim());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return projectRoot.resolve(configured).toAbsolutePath().normalize();
    }

    private static Path resolveBaseRoot(File configuredRoot, Path defaultRoot, Path projectRoot) {
        if (configuredRoot == null) {
            return defaultRoot.toAbsolutePath().normalize();
        }
        Path configuredPath = configuredRoot.toPath();
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return projectRoot.resolve(configuredPath).toAbsolutePath().normalize();
    }

    /**
     * Gets the main root.
     *
     * @return the resolved or constructed object
     */
    public Path getMainRoot() {
        return mainRoot;
    }

    /**
     * Sets the main root.
     *
     * @param mainRoot the main root
     */
    public void setMainRoot(Path mainRoot) {
        this.mainRoot = mainRoot;
    }

    /**
     * Gets the test root.
     *
     * @return the resolved or constructed object
     */
    public Path getTestRoot() {
        return testRoot;
    }

    /**
     * Sets the test root.
     *
     * @param testRoot the test root
     */
    public void setTestRoot(Path testRoot) {
        this.testRoot = testRoot;
    }

    /**
     * Gets the target root.
     *
     * @return the resolved or constructed object
     */
    public Path getTargetRoot() {
        return targetRoot;
    }

    /**
     * Sets the target root.
     *
     * @param targetRoot the target root
     */
    public void setTargetRoot(Path targetRoot) {
        this.targetRoot = targetRoot;
        if (targetRoot == null) {
            return;
        }
        Path normalized = targetRoot.toAbsolutePath().normalize();
        if (mainTargetRoot == null && testTargetRoot == null) {
            String leaf = normalized.getFileName() == null ? "" : normalized.getFileName().toString();
            if ("main-target".equals(leaf)) {
                this.mainTargetRoot = normalized;
                this.testTargetRoot = normalized.resolveSibling("test-target").normalize();
            } else if ("test-target".equals(leaf)) {
                this.testTargetRoot = normalized;
                this.mainTargetRoot = normalized.resolveSibling("main-target").normalize();
            } else {
                this.mainTargetRoot = normalized;
                this.testTargetRoot = normalized;
            }
        }
    }

    /**
     * Gets the main target root.
     *
     * @return the resolved or constructed object
     */
    public Path getMainTargetRoot() {
        return mainTargetRoot;
    }

    /**
     * Sets the main target root.
     *
     * @param mainTargetRoot the main target root
     */
    public void setMainTargetRoot(Path mainTargetRoot) {
        this.mainTargetRoot = mainTargetRoot;
    }

    /**
     * Gets the test target root.
     *
     * @return the resolved or constructed object
     */
    public Path getTestTargetRoot() {
        return testTargetRoot;
    }

    /**
     * Sets the test target root.
     *
     * @param testTargetRoot the test target root
     */
    public void setTestTargetRoot(Path testTargetRoot) {
        this.testTargetRoot = testTargetRoot;
    }

    /**
     * Gets the script root.
     *
     * @return the resolved or constructed object
     */
    public Path getScriptRoot() {
        return scriptRoot;
    }

    /**
     * Sets the script root.
     *
     * @param scriptRoot the script root
     */
    public void setScriptRoot(Path scriptRoot) {
        this.scriptRoot = scriptRoot;
    }

     
    /**
     * Gets the composed source bases.
     *
     * @param sourceScope the source scope
     * @return the resolved or constructed object
     */
    public Path[] getComposedSourceBases(String sourceScope) {
        if ("test".equals(sourceScope) && testRoot != null) {
            return new Path[]{testRoot, mainRoot};
        }
        return new Path[]{mainRoot};
    }

     
    /**
     * Gets the composed target bases.
     *
     * @param sourceScope the source scope
     * @return the resolved or constructed object
     */
    public Path[] getComposedTargetBases(String sourceScope) {
        if ("test".equals(sourceScope)) {
            java.util.LinkedHashSet<Path> composed = new java.util.LinkedHashSet<>();
            if (testTargetRoot != null) {
                composed.add(testTargetRoot);
            }
            if (mainTargetRoot != null) {
                composed.add(mainTargetRoot);
            }
            if (composed.isEmpty() && targetRoot != null) {
                composed.add(targetRoot);
            }
            return composed.toArray(new Path[0]);
        }
        Path main = mainTargetRoot != null ? mainTargetRoot : targetRoot;
        return main == null ? new Path[0] : new Path[]{main};
    }

    /**
     * Gets the active target root.
     *
     * @param sourceScope the source scope
     * @return the resolved or constructed object
     */
    public Path getActiveTargetRoot(String sourceScope) {
        if ("test".equals(sourceScope)) {
            return testTargetRoot != null ? testTargetRoot : targetRoot;
        }
        if ("main".equals(sourceScope)) {
            return mainTargetRoot != null ? mainTargetRoot : targetRoot;
        }
        return targetRoot;
    }

     
    /**
     * Checks if the component is composition enabled.
     *
     * @param sourceScope the source scope
     * @return true if successful or matching, false otherwise
     */
    public boolean isCompositionEnabled(String sourceScope) {
        return "test".equals(sourceScope) && testRoot != null;
    }
}
