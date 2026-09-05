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

If using OpenAI:

```bash
export OPENAI_API_KEY="your-actual-api-key"
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

### Installing the `reins` Launcher Script

Running `mvn install` on the `reins-nlce` project automatically installs the `reins` shell script into `~/.local/bin/reins`. Make sure `~/.local/bin` is in your `PATH`.

Once installed, you can simply run:

```bash
reins <command> [options]
```

### CLI Command Execution Structure

You can run the CLI using the `reins` helper command:

```bash
reins <command> [options]
```

Or directly via `java` with classpath:

```bash
java -cp ~/.m2/repository/br/com/dizeno/reins-nlce/0.0.1-SNAPSHOT/reins-nlce-0.0.1-SNAPSHOT.jar br.com.dizeno.reins.run.cli.ReinsCli <command> [options]
```

### Supported Commands

* `compile`: Compiles scanned Markdown source files.
* `clean`: Deletes generated outputs and tracking manifests.
* `add-note`: Appends a reasoning note to a source file's metadata tracker.
* `list-notes`: Lists reasoning notes for specific or all scanned sources.
* `clear-notes`: Clears reasoning notes for specific or all scanned sources.

### Command Options

* `--config <file>`: Path to your JSON, YAML, XML, or Properties configuration file.
* `--provider <name>`: LLM provider override (`gemini`, `openai`, `ollama`).
* `--compilationThreads <N>`: Number of simultaneous compilation threads (default: 1).
* `--verbose`: Activates verbose logging diagnostics.
* `--dryRun`: Runs analysis, file scans, and queries without writing output files.
* `--failOnError`: Fails immediately if any compilation phase throws exceptions.
* `--source <path>`: Specifies explicit source directories, files, or glob patterns (e.g. `domain/*.md`, `main:services/**/*.md`) for `compile`, `clean`, `add-note`, `list-notes`, or `clear-notes` (accepts comma-separated values or repeated `--source` flags). When omitted for `list-notes` or `clear-notes`, operates on every scanned source file.
* `--note <text>`: The note content string (required for `add-note`).

### CLI Usage Examples

* **Basic Compilation (using auto-discovered `reins.yaml` in current folder):**
  
  ```bash
  reins compile
  ```
  
* **Compilation with Custom Config and Local Ollama Provider:**
  
  ```bash
  reins compile \
    --config custom-config.json \
    --provider ollama \
    --verbose
  ```
  
* **Compilation with OpenAI Provider:**
  
  ```bash
  reins compile \
    --provider openai \
    --verbose
  ```
  
* **Explicit Source Mode (Single or Multiple Sources):**
  Compiles specified source file(s) or directory entries under the scan roots (comma-separated or repeated `--source` flags):
  
  ```bash
  reins compile \
    --source domain/Customer.md,domain/Order.md
  ```
  
  or using repeated flags:
  
  ```bash
  reins compile \
    --source domain/Customer.md \
    --source domain/Order.md
  ```
  
* **Validating with a Dry Run:**
  Performs directory scanning and resolves imports to check for dependency cycles without querying the LLM or writing any files to disk:
  
  ```bash
  reins compile \
    --dryRun
  ```
  
* **Injecting a Corrective Instruction Note:**
  Appends an inline instruction to the tracker database for `Customer.md`, forcing it to re-trigger compilation on the next compile execution:
  
  ```bash
  reins add-note \
    --source src/main/nl/domain/Customer.md \
    --note "Rename the customer ID field to customerUuid and verify references."
  ```
  
* **Cleaning All Generated Files:**
  Deletes all generated Java files and assets tracked in the manifest, logging each deletion:
  ```bash
  reins clean \
    --verbose
  ```

* **Cleaning Generated Files for a Specific Source:**
  Deletes only the generated files and tracking manifest associated with a specific source file or directory:
  ```bash
  reins clean \
    --source domain/Customer.md
  ```

### Example Configuration File (`reins.yaml`)

Save this file as `reins.yaml` in your project root:

```yaml
provider: gemini
gemini:
  apiKey: "${env.GEMINI_API_KEY}"
  model: gemini-2.0-flash
  timeoutSeconds: 45
compilationThreads: 4
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
