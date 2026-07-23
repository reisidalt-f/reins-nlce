# Usage Modes & Quick Start

Reins is designed for maximum versatility and can be run in three different ways:
1. **As a Build Tool Plugin:** Integrate directly into your build lifecycle (offering first-class support for **Apache Maven**).
2. **As a Command Line Tool (CLI):** Run compilation directly from your terminal.
3. **As a Reusable Java Library:** Embed AI-driven compilation capabilities into any Java 17+ application.

---

## Prerequisites & API Credentials

Export your Gemini API credentials to start:

```bash
export GEMINI_API_KEY="your-actual-api-key"
```

If using a remote Ollama server, export your Ollama credentials instead:

```bash
export OLLAMA_API_KEY="your-actual-api-key"
```

---

## 1. Build Tools Integration: Maven Plugin

Bind Reins directly to your Maven lifecycle to compile natural-language source texts during standard builds.

### Step A: Configure the Plugin in your `pom.xml`

```xml
<plugin>
  <groupId>br.com.dizeno</groupId>
  <artifactId>reins-nlce-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>compile</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <provider>gemini</provider>
    <gemini>
      <apiKey>${env.GEMINI_API_KEY}</apiKey>
      <model>gemini-2.0-flash</model>
    </gemini>
    <tooling>
      <main>list,list_compiled,read</main>
      <test>list,list_compiled,read</test>
    </tooling>
  </configuration>
</plugin>
```

### Step B: Run Compilation

```bash
mvn reins:compile
```

---

## 2. Command Line Tool (CLI)

Reins packages a self-contained CLI entry point, allowing you to run compilation cycles directly from a terminal without Maven wrapper projects.

### How Configuration is Loaded by CLI

When invoking a CLI command, Reins automatically looks in the working directory for a default configuration file in one of these formats:
1. `reins.yaml` / `reins.yml`
2. `reins.json`
3. `reins.xml`
4. `reins.properties`

Alternatively, specify any configuration file path using the `--config` option.

### CLI Command Execution Structure

```bash
java -cp reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli <command> [options]
```

### Supported Commands

* `compile`: Compiles scanned Markdown source files.
* `clean`: Deletes generated outputs and tracking manifests.
* `add-note` / `addnote`: Appends a corrective note to a source file's metadata tracker.

### Command Options

* `--config <file>`: Path to your JSON, YAML, XML, or Properties configuration file.
* `--provider <name>`: LLM provider override (`gemini`, `ollama`).
* `--verbose`: Activates verbose logging diagnostics.
* `--dryRun`: Runs analysis, file scans, and queries without writing output files.
* `--failOnError`: Fails immediately if any compilation phase throws exceptions.
* `--source <path>`: Specifies explicit compilation source directories or files (also used to designate the target for `add-note`).
* `--note <text>`: The note content string (required for `add-note`).

### CLI Usage Examples

* **Basic Compilation (using auto-discovered `reins.yaml` in current folder):**
  
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli compile
  ```
  
* **Compilation with Custom Config and Local Ollama Provider:**
  
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli compile \
    --config custom-config.json \
    --provider ollama \
    --verbose
  ```
  
* **Explicit Source Mode:**
  Compiles only a single source file under the scan roots:
  
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli compile \
    --source domain/Customer.md
  ```
  
* **Validating with a Dry Run:**
  Performs directory scanning and resolves imports to check for dependency cycles without querying the LLM or writing any files to disk:
  
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli compile \
    --dryRun
  ```
  
* **Injecting a Corrective Instruction Note:**
  Appends an inline instruction to the tracker database for `Customer.md`, forcing it to re-trigger compilation on the next compile execution:
  
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli add-note \
    --source src/main/nl/domain/Customer.md \
    --note "Rename the customer ID field to customerUuid and verify references."
  ```
  
* **Cleaning Generated Files:**
  Deletes all generated Java files and assets tracked in the manifest, logging each deletion:
  ```bash
  java -cp target/reins-nlce-plugin-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli clean \
    --verbose
  ```

### Example Configuration File (`reins.yaml`)

Save this file as `reins.yaml` in your project root:

```yaml
provider: gemini
gemini:
  apiKey: "${env.GEMINI_API_KEY}"
  model: gemini-2.0-flash
  timeoutSeconds: 45
tooling:
  main: list,list_compiled,read
  test: list,list_compiled,read
reasoning:
  maxTurns: 8
```

---

## 3. Reusable Java Library

Reins can be consumed programmatically as a standard library dependency in any Java 17+ codebase. The API is exposed through the static entry class `br.com.dizeno.reins.run.library.ReinsLibrary`.

### Maven Dependency

```xml
<dependency>
  <groupId>br.com.dizeno</groupId>
  <artifactId>reins-nlce-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Programmatic Overloads

`ReinsLibrary` provides unified static helpers for `compile`, `clean`, and `addNote` accepting:
* `Map<String, Object>` configurations
* `java.util.Properties`
* Configuration `File` references
* Structured `ReinsConfig` configuration objects

### Example Usage

```java
import br.com.dizeno.reins.run.library.ReinsLibrary;
import br.com.dizeno.reins.compilation.CompilationSummary;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) throws Exception {
        File baseDir = new File("/path/to/my/project");

        // Define options programmatically
        Map<String, Object> config = new HashMap<>();
        config.put("provider", "gemini");
        config.put("gemini.apiKey", System.getenv("GEMINI_API_KEY"));
        config.put("gemini.model", "gemini-2.0-flash");
        config.put("tooling.main", "list,list_compiled,read");

        // Trigger Reins compilation cycle
        CompilationSummary summary = ReinsLibrary.compile(baseDir, config);

        System.out.println("Reins Compiled files: " + summary.getCompiledCount());
        System.out.println("Reins Skipped files: " + summary.getSkippedCount());
    }
}
```
