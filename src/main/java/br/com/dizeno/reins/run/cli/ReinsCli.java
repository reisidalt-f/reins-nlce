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

package br.com.dizeno.reins.run.cli;

import br.com.dizeno.reins.compilation.tracking.ReasoningNote;
import br.com.dizeno.reins.run.ReinsRunner;
import br.com.dizeno.reins.run.ReinsSystemOutLogger;
import br.com.dizeno.reins.run.config.*;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.Arrays;

/**
 * ReinsCli is part of the general application functions in the reins architecture.
 * Command-line interface entrypoint for running Reins compilation outside of Maven.
 */
public class ReinsCli {

    /**
     * Main.
     *
     * @param args the args
     */
    public static void main(String[] args) {
        int exitCode = runCli(args);
        System.exit(exitCode);
    }

    /**
     * Runs the execution cycle cli.
     *
     * @param args the args
     * @return the numeric value
     */
    public static int runCli(String[] args) {
        if (args == null || args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return (args != null && args.length > 0) ? 0 : 1;
        }

        String command = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        File baseDir = new File(".");
        ReinsConfig config;
        try {
            config = ReinsConfigLoader.load(subArgs, System.getProperties(), null, baseDir);
        } catch (Exception ex) {
            System.err.println("[ERROR] Configuration loading failed: " + ex.getMessage());
            return 1;
        }

        Log log = new ReinsSystemOutLogger(config.isVerbose(), config.isVerbose());
        ReinsRunner runner = new ReinsRunner();

        try {
            if ("compile".equals(command)) {
                runner.compile(config, baseDir, log);
                return 0;
            } else if ("clean".equals(command)) {
                runner.clean(config, baseDir, log);
                return 0;
            } else if ("add-note".equals(command)) {
                java.util.List<String> sourcesList = new java.util.ArrayList<>();
                String note = null;
                for (int i = 0; i < subArgs.length - 1; i++) {
                    if ("--source".equals(subArgs[i])) {
                        sourcesList.add(subArgs[i + 1]);
                        i++;
                    } else if ("--note".equals(subArgs[i])) {
                        note = subArgs[i + 1];
                        i++;
                    }
                }
                String source = String.join(",", sourcesList);
                runner.addNote(config, baseDir, source, note, ReasoningNote.Origin.CLI, log);
                return 0;
            } else if ("list-notes".equals(command)) {
                java.util.List<String> sourcesList = new java.util.ArrayList<>();
                for (int i = 0; i < subArgs.length; i++) {
                    if ("--source".equals(subArgs[i]) && i + 1 < subArgs.length) {
                        sourcesList.add(subArgs[i + 1]);
                        i++;
                    }
                }
                String source = sourcesList.isEmpty() ? null : String.join(",", sourcesList);
                runner.listNotes(config, baseDir, source, log);
                return 0;
            } else if ("clear-notes".equals(command)) {
                java.util.List<String> sourcesList = new java.util.ArrayList<>();
                for (int i = 0; i < subArgs.length; i++) {
                    if ("--source".equals(subArgs[i]) && i + 1 < subArgs.length) {
                        sourcesList.add(subArgs[i + 1]);
                        i++;
                    }
                }
                String source = sourcesList.isEmpty() ? null : String.join(",", sourcesList);
                runner.clearNotes(config, baseDir, source, log);
                return 0;
            } else {
                System.err.println("[ERROR] Unknown command: " + command);
                printUsage();
                return 1;
            }
        } catch (ConfigValidationException | IllegalArgumentException ex) {
            System.err.println("[ERROR] Validation error: " + ex.getMessage());
            return 1;
        } catch (Exception ex) {
            System.err.println("[ERROR] Execution failed: " + ex.getMessage());
            return 2;
        }
    }

    private static void printUsage() {
        System.out.println("Reins NLCE CLI - Natural Language Compilation Engine");
        System.out.println("Usage:");
        System.out.println("  java -cp reins.jar br.com.dizeno.reins.run.cli.ReinsCli <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  compile      Compile instruction files");
        System.out.println("  clean        Clean up generated files and trackings");
        System.out.println("  add-note     Add a corrective note for a source file");
        System.out.println("  list-notes   List reasoning notes for source files");
        System.out.println("  clear-notes  Clear reasoning notes for source files");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --config <file>             Specify external configuration file (YAML, JSON, XML, Properties)");
        System.out.println("  --provider <name>           LLM provider (gemini, openai, ollama, stub)");
        System.out.println("  --compilationThreads <N>    Number of simultaneous compilation threads (default: 1)");
        System.out.println("  --verbose                   Enable verbose logging");
        System.out.println("  --dryRun                    Perform dry run");
        System.out.println("  --failOnError               Fails immediately on any errors");
        System.out.println("  --source <path>             Specify source path(s), comma-separated or repeated (required for add-note; optional for compile, clean, list-notes, clear-notes)");
        System.out.println("  --note <text>               Specify note text (required for add-note)");
    }
}
