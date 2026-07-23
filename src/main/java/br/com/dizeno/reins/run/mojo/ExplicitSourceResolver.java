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

import br.com.dizeno.reins.source.scanner.CoderMdScanner;
import br.com.dizeno.reins.source.scanner.SourceDiscoveryMode;
import br.com.dizeno.reins.security.PathValidator;
import br.com.dizeno.reins.source.validation.ExplicitSourceValidator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * ExplicitSourceResolver is part of the general application functions in the reins architecture.
 * Acts as a helper utility for resolving its prefix elements.
 */
public class ExplicitSourceResolver {

    private final ExplicitSourceValidator validator;

    /**
     * Constructs a new instance of {@link ExplicitSourceResolver}.
     */
    public ExplicitSourceResolver() {
        this(new ExplicitSourceValidator());
    }

    ExplicitSourceResolver(ExplicitSourceValidator validator) {
        this.validator = validator;
    }

    /**
     * Resolves the configured value or path.
     *
     * @param source the source
     * @param projectRoot the root path of the project
     * @param mainNlRoot the main nl root
     * @param testNlRoot the test nl root
     * @param includePattern the include pattern
     * @param pathValidator the path validator
     * @return the resulting result
     */
    public ResolutionResult resolve(String source,
                                    Path projectRoot,
                                    File mainNlRoot,
                                    File testNlRoot,
                                    String includePattern,
                                    PathValidator pathValidator) {
        String validated = validator.validateSourceInput(source);
        Path mainRoot = pathValidator.validateInProject(mainNlRoot.toPath());
        Path testRoot = pathValidator.validateInProject(testNlRoot.toPath());

        Path mainDirectory = pathValidator.validateInProject(mainRoot.resolve(validated));
        Path testDirectory = pathValidator.validateInProject(testRoot.resolve(validated));

        if (Files.isDirectory(mainDirectory)) {
            List<File> files = new CoderMdScanner().scan(List.of(mainDirectory.toFile()), includePattern, pathValidator);
            return new ResolutionResult(SourceDiscoveryMode.EXPLICIT_SOURCE, mainDirectory, true, files);
        }
        if (Files.isDirectory(testDirectory)) {
            List<File> files = new CoderMdScanner().scan(List.of(testDirectory.toFile()), includePattern, pathValidator);
            return new ResolutionResult(SourceDiscoveryMode.EXPLICIT_SOURCE, testDirectory, true, files);
        }

        List<Path> fileMatches = new ArrayList<>();
        Path mainFile = pathValidator.validateInProject(mainRoot.resolve(validated));
        Path testFile = pathValidator.validateInProject(testRoot.resolve(validated));
        
        
        if (Files.isRegularFile(mainFile)) {
            fileMatches.add(mainFile);
        }
        if (Files.isRegularFile(testFile)) {
            fileMatches.add(testFile);
        }
        
        
        if (fileMatches.isEmpty()) {
            String fileName = new File(validated).getName();
            
            
            List<Path> mainMatches = findFilesByName(mainRoot, fileName);
            fileMatches.addAll(mainMatches);
            
            
            if (fileMatches.isEmpty()) {
                List<Path> testMatches = findFilesByName(testRoot, fileName);
                fileMatches.addAll(testMatches);
            }
        }
        
        validator.validateSingleMatch(fileMatches, validated);

        if (fileMatches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Explicit source '" + validated + "' does not resolve to an eligible directory or file under main/test source bases.");
        }

        return new ResolutionResult(
                SourceDiscoveryMode.EXPLICIT_SOURCE,
                fileMatches.get(0),
                false,
                List.of(fileMatches.get(0).toFile()));
    }

     
    private List<Path> findFilesByName(Path root, String fileName) {
        List<Path> matches = new ArrayList<>();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return matches;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .forEach(matches::add);
        } catch (IOException e) {
            
        }
        return matches;
    }

    /**
     * ResolutionResult is part of the general application functions in the reins architecture.
     * Acts as a data carrier representation of its prefix information.
     */
    public record ResolutionResult(
            SourceDiscoveryMode mode,
            Path resolvedPath,
            boolean directory,
            List<File> files
    ) {
    }
}
