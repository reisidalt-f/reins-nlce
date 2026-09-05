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

import br.com.dizeno.reins.run.config.settings.ContextSourceSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContextSourcePatternFilteringTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultSpecValues_useMdPatternAndWildcardPhase() {
        File file = tempDir.resolve("docs/arch.md").toFile();
        ContextSourceSpec spec = new ContextSourceSpec(file);

        assertEquals("**/*.md", spec.getPattern());
        assertEquals(List.of("*"), spec.getPhases());
    }

    @Test
    void customSpecValues_parseMultiplePhasesAndPattern() {
        File file = tempDir.resolve("docs/domain.md").toFile();
        ContextSourceSpec spec = new ContextSourceSpec(file, "**/domain/*.md", List.of());
        spec.setPhaseString("initial-context, verification");

        assertEquals("**/domain/*.md", spec.getPattern());
        assertEquals(List.of("initial-context", "verification"), spec.getPhases());
    }

    @Test
    void backgroundFileMatching_matchesTargetFilePattern() {
        Path absPath = tempDir.resolve("docs/domain-rules.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/domain-rules.md",
                "rules content",
                "**/domain/*.md",
                List.of("initial-context", "compile")
        );

        assertTrue(bgFile.matchesTargetFile("src/main/nl/domain/Customer.md"));
        assertFalse(bgFile.matchesTargetFile("src/main/nl/service/CustomerService.md"));
    }

    @Test
    void backgroundFileMatching_matchesMultiplePhases() {
        Path absPath = tempDir.resolve("docs/domain-rules.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/domain-rules.md",
                "rules content",
                "**/*.md",
                List.of("initial-context", "verification")
        );

        assertTrue(bgFile.matchesPhase("initial-context"));
        assertTrue(bgFile.matchesPhase("verification"));
        assertFalse(bgFile.matchesPhase("code-gen"));
        assertTrue(bgFile.matchesPhase(null));
    }

    @Test
    void backgroundFileMatching_wildcardPhaseMatchesAnyPhase() {
        Path absPath = tempDir.resolve("docs/arch.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/arch.md",
                "arch content",
                "**/*.md",
                List.of("*")
        );

        assertTrue(bgFile.matchesPhase("initial-context"));
        assertTrue(bgFile.matchesPhase("code-gen"));
        assertTrue(bgFile.matchesPhase("verification"));
    }

    @Test
    void backgroundFileMatching_extglobNegativePatternExcludesImplFiles() {
        Path absPath = tempDir.resolve("docs/arch.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/arch.md",
                "arch content",
                "**/!(*-impl).md",
                List.of("*")
        );

        assertTrue(bgFile.matchesTargetFile("src/main/nl/user-interface.md"));
        assertFalse(bgFile.matchesTargetFile("src/main/nl/user-interface-impl.md"));
        assertTrue(bgFile.matchesTargetFile("docs/arch.md"));
        assertFalse(bgFile.matchesTargetFile("docs/arch-impl.md"));
    }

    @Test
    void backgroundFileMatching_leadingExclamationNegatesPattern() {
        Path absPath = tempDir.resolve("docs/arch.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/arch.md",
                "arch content",
                "!**/*-impl.md",
                List.of("*")
        );

        assertTrue(bgFile.matchesTargetFile("src/main/nl/user-interface.md"));
        assertFalse(bgFile.matchesTargetFile("src/main/nl/user-interface-impl.md"));
    }

    @Test
    void backgroundFileMatching_commaSeparatedPositiveAndNegativePatterns() {
        Path absPath = tempDir.resolve("docs/arch.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/arch.md",
                "arch content",
                "**/*.md, !**/*-impl.md",
                List.of("*")
        );

        assertTrue(bgFile.matchesTargetFile("src/main/nl/user-interface.md"));
        assertFalse(bgFile.matchesTargetFile("src/main/nl/user-interface-impl.md"));
        assertFalse(bgFile.matchesTargetFile("src/main/nl/user-interface.txt"));
    }

    @Test
    void backgroundFileMatching_negatedPhasePatterns() {
        Path absPath = tempDir.resolve("docs/arch.md");
        CompilationBackgroundFile bgFile = new CompilationBackgroundFile(
                absPath,
                "docs/arch.md",
                "arch content",
                "**/*.md",
                List.of("!code-gen")
        );

        assertTrue(bgFile.matchesPhase("initial-context"));
        assertTrue(bgFile.matchesPhase("verification"));
        assertFalse(bgFile.matchesPhase("code-gen"));
    }
}
