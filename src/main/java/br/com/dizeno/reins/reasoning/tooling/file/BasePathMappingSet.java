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
    private final java.util.Map<String, Path> sourceRoots = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Path> targetRoots = new java.util.LinkedHashMap<>();

    public static BasePathMappingSet fromConfig(ReinsConfig config, Path projectRoot) {
        BasePathMappingSet mappings = new BasePathMappingSet();
        if (config != null && config.getSourceBases() != null) {
            for (java.util.Map.Entry<String, File> entry : config.getSourceBases().entrySet()) {
                if (entry.getValue() != null) {
                    Path rootPath = resolveBaseRoot(entry.getValue(), projectRoot);
                    mappings.setSourceRoot(entry.getKey(), rootPath);
                    if ("main".equals(entry.getKey())) mappings.setMainRoot(rootPath);
                    else if ("test".equals(entry.getKey())) mappings.setTestRoot(rootPath);
                }
            }
        }
        if (config != null && config.getTarget() != null) {
            mappings.setTargetRoot(projectRoot.toAbsolutePath().normalize());
            if (config.getTarget().getTargetBases() != null) {
                for (java.util.Map.Entry<String, String> entry : config.getTarget().getTargetBases().entrySet()) {
                    if (entry.getValue() != null) {
                        try {
                            mappings.setTargetRoot(entry.getKey(), config.getTarget().resolveTargetOutput(entry.getKey(), projectRoot));
                        } catch (Exception ex) {
                            // ignore fallback
                        }
                    }
                }
            }
        } else {
            mappings.setTargetRoot(projectRoot.toAbsolutePath().normalize());
        }
        mappings.setScriptRoot(resolveScriptRoot(config, projectRoot));
        return mappings;
    }

    public static BasePathMappingSet forScope(ReinsConfig config, Path projectRoot, String sourceScope) {
        BasePathMappingSet mappings = fromConfig(config, projectRoot);

        if (config.getTarget() == null) {
            throw new IllegalStateException("Target configuration is missing; target output directory must be defined.");
        }

        Path activeTarget = config.getTarget().resolveTargetOutput(sourceScope, projectRoot);
        mappings.setTargetRoot(activeTarget);
        try {
            mappings.setMainTargetRoot(config.getTarget().resolveTargetOutput("main", projectRoot));
        } catch (Exception ex) {
            mappings.setMainTargetRoot(activeTarget);
        }
        try {
            mappings.setTestTargetRoot(config.getTarget().resolveTargetOutput("test", projectRoot));
        } catch (Exception ex) {
            mappings.setTestTargetRoot(activeTarget);
        }

        mappings.setScriptRoot(resolveScriptRoot(config, projectRoot));
        return mappings;
    }

    public java.util.Map<String, Path> getSourceRoots() {
        return java.util.Collections.unmodifiableMap(sourceRoots);
    }

    public java.util.Map<String, Path> getTargetRoots() {
        return java.util.Collections.unmodifiableMap(targetRoots);
    }

    public Path getSourceRoot(String name) {
        if (name == null) return null;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (sourceRoots.containsKey(normalized)) {
            return sourceRoots.get(normalized);
        }
        return switch (normalized) {
            case "main" -> mainRoot;
            case "test" -> testRoot;
            default -> null;
        };
    }

    public void setSourceRoot(String name, Path path) {
        if (name == null || name.isBlank()) return;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        sourceRoots.put(normalized, path);
        if ("main".equals(normalized)) this.mainRoot = path;
        else if ("test".equals(normalized)) this.testRoot = path;
    }

    public Path getTargetRoot(String name) {
        if (name == null) return targetRoot;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        if (targetRoots.containsKey(normalized)) {
            return targetRoots.get(normalized);
        }
        return switch (normalized) {
            case "main" -> mainTargetRoot != null ? mainTargetRoot : targetRoot;
            case "test" -> testTargetRoot != null ? testTargetRoot : targetRoot;
            case "target" -> targetRoot;
            default -> targetRoot;
        };
    }

    public void setTargetRoot(String name, Path path) {
        if (name == null || name.isBlank()) return;
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        targetRoots.put(normalized, path);
        if ("target".equals(normalized)) this.targetRoot = path;
        else if ("main".equals(normalized)) this.mainTargetRoot = path;
        else if ("test".equals(normalized)) this.testTargetRoot = path;
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

    private static Path resolveBaseRoot(File configuredRoot, Path projectRoot) {
        if (configuredRoot == null) {
            return null;
        }
        Path configuredPath = configuredRoot.toPath();
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return projectRoot.resolve(configuredPath).toAbsolutePath().normalize();
    }

    public Path getMainRoot() {
        return mainRoot;
    }

    public void setMainRoot(Path mainRoot) {
        this.mainRoot = mainRoot;
    }

    public Path getTestRoot() {
        return testRoot;
    }

    public void setTestRoot(Path testRoot) {
        this.testRoot = testRoot;
    }

    public Path getTargetRoot() {
        return targetRoot;
    }

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

    public Path getMainTargetRoot() {
        return mainTargetRoot;
    }

    public void setMainTargetRoot(Path mainTargetRoot) {
        this.mainTargetRoot = mainTargetRoot;
    }

    public Path getTestTargetRoot() {
        return testTargetRoot;
    }

    public void setTestTargetRoot(Path testTargetRoot) {
        this.testTargetRoot = testTargetRoot;
    }

    public Path getScriptRoot() {
        return scriptRoot;
    }

    public void setScriptRoot(Path scriptRoot) {
        this.scriptRoot = scriptRoot;
    }

    public Path[] getComposedSourceBases(String sourceScope) {
        if ("test".equals(sourceScope) && testRoot != null) {
            return new Path[]{testRoot, mainRoot};
        }
        return new Path[]{mainRoot};
    }

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

    public Path getActiveTargetRoot(String sourceScope) {
        if ("test".equals(sourceScope)) {
            return testTargetRoot != null ? testTargetRoot : targetRoot;
        }
        if ("main".equals(sourceScope)) {
            return mainTargetRoot != null ? mainTargetRoot : targetRoot;
        }
        return targetRoot;
    }

    public boolean isCompositionEnabled(String sourceScope) {
        return "test".equals(sourceScope) && testRoot != null;
    }
}
