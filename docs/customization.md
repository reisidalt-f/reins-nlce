# Advanced Customization Recipes

Reins offers deep customization capabilities allowing developers to tailor how prompts are structured, how reasoning loops behave, and how security tool boundaries are enforced.

---

## 1. Custom FreeMarker Inference Scripts

You can override or augment Reins's built-in inference execution phases by specifying a custom scripts directory in your configuration:

```xml
<configuration>
  <reasoning>
    <scriptsPath>scripts/reins</scriptsPath>
  </reasoning>
</configuration>
```

### Script Resolution Priority Hierarchy

When resolving Apache FreeMarker (`.ftl`) template scripts for compilation phases, Reins queries resources in the following order:

1. **Custom Filesystem Path:** Checked under `reasoning.scriptsPath` relative to the project root.
2. **Internal Classpath Overrides.**
3. **Default Packaged Templates:** Located inside the plugin JAR at `inference/*.ftl`.

### Customizing Prompt Templates

To customize prompt contexts or reasoning pipelines, create files inside your custom scripts directory matching the standard phase template names:

* `system-context.ftl`: Configures the base system prompt and system persona for the LLM.
* `user-prompt.ftl`: Controls how natural-language source texts, markdown references, and attached files are packaged into the user turn.
* `phase-list.ftl`: Outlines execution phase step instructions.
* `max-turn-grace-prompt.ftl`: Prompt appended when approaching maximum multi-turn conversation limits (`maxTurns`).
* `reasoning-pipeline.ftl`: Defines multi-turn iteration loops and step sequencing.

---

## 2. Fine-Tuned Tooling Permission Profiles

Reins restricts file mutations through a sandboxed operations layer. You can tailor tool privileges for `main`, `test`, and `target` source locations depending on your project security and flexibility needs.

### Profile A: Standard Least-Privilege Baseline (Recommended)

Blocks the model from modifying anything outside standard compilation target roots, allowing read-only access to existing source texts.

```xml
<tooling>
  <main>list,list_compiled,read</main>
  <test>list,list_compiled,read</test>
  <!-- Write permission on target output is granted implicitly by the plugin -->
</tooling>
```

### Profile B: Broad Compatibility Mode

Allows the model to clean up old files, write test code, or patch existing classes interactively during complex multi-turn reasoning cycles.

```xml
<tooling>
  <main>list,list_compiled,read,write,patch,delete</main>
  <test>list,list_compiled,read,write,patch,delete</test>
  <target>list,list_compiled,read,write,patch,delete</target>
</tooling>
```
