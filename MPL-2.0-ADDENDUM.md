# Addendum to the Mozilla Public License 2.0

## Markdown Source Code and Generated Files Policy

This Addendum supplements the terms of the Mozilla Public License, Version 2.0 ("MPL 2.0"), for software projects in which Markdown files or other human-readable textual representations are used as the preferred form for making modifications to a software system, executable specification, programming language source, configuration source, or system definition.

---

## 1. Definition of Primary Source Input Files

For the purposes of this project, any file intentionally maintained as an authoritative source of software behavior, architecture, configuration, requirements, constraints, executable specifications, generation instructions, or other software-defining information, and used as an input to a compiler, interpreter, transpiler, code generator, artificial intelligence system, machine learning model, language model, autonomous software agent, or other equivalent transformation process, shall be considered a **Primary Source Input File**.

Primary Source Input Files may include, without limitation:

### Natural-language and human-readable text files

Files containing human-readable textual content, instructions, descriptions, specifications, requirements, rules, knowledge, documentation, or other semantic information intentionally maintained as an authoritative source for automated processing, including but not limited to:

* plain text files (`.txt`);
* Markdown documents (`.md`, `.markdown`);
* reStructuredText documents (`.rst`);
* AsciiDoc documents (`.adoc`, `.asciidoc`);
* Textile documents (`.textile`);
* Org-mode documents (`.org`);
* LaTeX documents (`.tex`, `.latex`);
* documentation source files;
* specification documents;
* requirement documents;
* design documents;
* architecture documents;
* technical notes;
* instruction files;
* prompt files;
* knowledge-base files;
* natural-language rule files;
* human-readable configuration descriptions.

### Structured text and data representation files

Files containing structured, semi-structured, or declarative textual information, including but not limited to:

* YAML files (`.yaml`, `.yml`);
* JSON files (`.json`);
* JSON with comments (`.jsonc`);
* XML files (`.xml`);
* TOML files (`.toml`);
* INI-style configuration files (`.ini`, `.cfg`, `.conf`);
* CSV files (`.csv`);
* TSV files (`.tsv`);
* serialized data definitions;
* schema definitions;
* metadata files.

### Source code and formal specification files

Files containing formal instructions, executable definitions, programming constructs, or machine-processable specifications, including but not limited to:

* programming language source files;
* domain-specific language files;
* configuration language files;
* interface definition files;
* API specification files;
* database schema files;
* infrastructure-as-code files;
* workflow definitions;
* build system definitions;
* templates;
* models;
* diagrams represented as text;
* declarative system descriptions.

### AI-oriented source material

Files used as authoritative source material, context, constraints, instructions, examples, or knowledge input for artificial intelligence-assisted software generation, including but not limited to:

* prompt files;
* system instruction files;
* agent instruction files;
* AI workflow definitions;
* task specification files;
* contextual knowledge files;
* retrieval documents;
* example-based generation files.

The examples above are illustrative and shall not be interpreted as an exhaustive list.

The classification of a file as a Primary Source Input File depends on its purpose and role in the software generation process, rather than its extension, syntax, programming language, readability, or file format.

Human-readable text, documentation-like content, descriptive material, or natural-language instructions shall not be excluded from classification as Primary Source Input Files when intentionally maintained as an authoritative source for producing, modifying, configuring, or defining software behavior.

---

## 1A. Preferred Form for Making Modifications

For the purposes of this project, the Primary Source Input Files constitute the preferred form of the Covered Software for making modifications within the meaning of Section 1.10 of the Mozilla Public License, Version 2.0.

Human-readable files generated from the Primary Source Input Files are distributed for execution, interoperability, integration, portability, convenience, debugging, or compatibility, and are not intended to replace the Primary Source Input Files as the preferred form for making modifications.

The preferred form for making modifications is determined by the documented development process and the functional role of the files, rather than by their syntax, programming language, readability, or file extension.

---

## 2. Definition of Generated Files

Any file produced, in whole or in part, by a compiler, interpreter, transpiler, generator, artificial intelligence system, language model, autonomous software agent, or equivalent transformation process from Primary Source Input Files shall be considered **Generated Code**.

This definition includes, without limitation:

* source files in programming languages;
* script files;
* configuration files;
* infrastructure definition files;
* data description files;
* intermediate compilation outputs;
* generated documentation;
* final software artifacts.

The programming language, data format, file extension, target platform, or human readability of the generated file shall not affect its classification as Generated Code under this Addendum.

---

## 3. Treatment of Generated Code

For the purposes of compliance with the Mozilla Public License, Version 2.0, Generated Code shall be treated in the same manner as Object Code whenever the Primary Source Input Files constitute the preferred form for making modifications.

The fact that Generated Code is expressed in a human-readable programming language shall not, by itself, cause such files to be regarded as the preferred form for making modifications.

Distribution of Generated Code does not remove, replace, or diminish the obligations applicable to the corresponding Primary Source Input Files under the MPL 2.0 and this Addendum.

When Generated Code is distributed separately from the corresponding Primary Source Input Files, the distribution shall include, where reasonably practicable:

(a) a clear reference to the Primary Source Input File or files from which the Generated Code was produced;

(b) an indication that the file was automatically generated;

(c) a reasonable method for obtaining the corresponding Primary Source Input Files, as required by the MPL 2.0.

Nothing in this Section shall be interpreted as preventing Generated Code from also constituting Source Code under the MPL 2.0 where applicable. This Section solely clarifies which files constitute the preferred form for making modifications for purposes of this project.

---

## 4. Relationship Between Primary Source Input Files and Generated Code

Compilation, transformation, inference, generation, or other automated processing shall not create an independent licensing separation between the Primary Source Input Files and the corresponding Generated Code.

The use of a compiler, transpiler, generator, artificial intelligence system, language model, autonomous software agent, or equivalent transformation process shall not, by itself, be interpreted as creating an independent work solely by reason of that transformation.

Generated Code that substantially expresses, implements, represents, or derives its functional behavior from the Primary Source Input Files remains functionally and legally connected to those files for purposes of compliance with the MPL 2.0.

Automatic generation of code shall not, solely by reason of the generation process itself, be interpreted as creating an independently authored work where the generated output substantially expresses or implements definitions contained in the Primary Source Input Files.

Nothing in this Addendum shall be interpreted as expanding or reducing the scope of copyright protection, authorship, or derivative-work determinations under applicable law. This Addendum solely clarifies how the project identifies the relationship between its preferred source representation and generated outputs.

---

## 5. Modifications

Changes made directly to Generated Code do not replace modifications made to the Primary Source Input Files.

Whenever the transformation process is executed again, the Primary Source Input Files remain the authoritative and preferred reference for maintenance, modification, regeneration, and long-term evolution of the software system.

Where practical, functional modifications should be made to the Primary Source Input Files rather than directly to Generated Code.

Functional modifications made directly to Generated Code remain subject to the applicable provisions of the MPL 2.0 governing modified files.

Project maintainers may regenerate Generated Code from the updated Primary Source Input Files without altering the licensing relationship established by this Addendum.

---

## 6. Compiler and Generation Tools

This Addendum does not determine the license of any compiler, interpreter, transpiler, code generator, artificial intelligence system, machine learning model, language model, autonomous software agent, or other transformation tool used to produce Generated Code, except where such tool is itself covered by the Mozilla Public License, Version 2.0.

The use of intermediate tools, transformation stages, or automated generation processes shall not alter the identification of the Primary Source Input Files as the preferred form for making modifications.

Nothing in this Addendum shall be interpreted as imposing licensing obligations upon software generation tools solely by reason of their use with Covered Software.

---

## 7. Preservation of Source Relationship

The existence of one or more intermediate representations, generated artifacts, transformation stages, caches, temporary outputs, or derived implementation files shall not affect the identification of the Primary Source Input Files as the preferred form for making modifications.

Generated files may themselves be human-readable, editable, or expressed in a programming language. Such characteristics shall not, by themselves, alter the status of the Primary Source Input Files as the authoritative source representation of the software system.

Where multiple generated representations exist, each shall be regarded as originating from the corresponding Primary Source Input Files unless independently developed outside the documented generation process.

The existence of generated intermediary representations shall not be interpreted as creating an independent licensing boundary between the Primary Source Input Files and the resulting Generated Code.

---

## 8. Compatibility with the Mozilla Public License 2.0

This Addendum shall be interpreted solely as a clarification of the application of the Mozilla Public License, Version 2.0, to projects whose preferred form for making modifications consists of Markdown documents or other Primary Source Input Files described herein.

Nothing in this Addendum is intended to modify, replace, restrict, or expand the rights and obligations established by the Mozilla Public License, Version 2.0.

In the event of any conflict between this Addendum and the Mozilla Public License, Version 2.0, the provisions of the Mozilla Public License, Version 2.0 shall prevail.

This Addendum shall be interpreted, to the maximum extent possible, consistently with the definitions and principles of the Mozilla Public License, Version 2.0, including its definition of Source Code as the preferred form for making modifications.

---

## 9. Purpose and Intent

The purpose of this Addendum is to clarify the application of the Mozilla Public License, Version 2.0, to software projects in which Markdown documents or other textual representations constitute the preferred form for making modifications.

This Addendum recognizes Markdown-based and other text-based source systems as legitimate source representations of software where they serve as the authoritative and preferred source from which software is generated, configured, interpreted, or otherwise produced.

Generated source files, executable artifacts, configuration files, documentation, infrastructure definitions, and other transformation outputs remain functionally connected to their originating Primary Source Input Files for purposes of compliance with the Mozilla Public License, Version 2.0.

The purpose of this Addendum is not to redefine Source Code or Object Code under the Mozilla Public License, Version 2.0, but to identify, for this project, which files constitute the preferred form for making modifications and how generated outputs relate to those files within the existing framework of the Mozilla Public License, Version 2.0.

Accordingly, software systems whose primary development process is based upon Markdown or other Primary Source Input Files shall receive the same source availability, modification rights, file-level copyleft protections, and distribution obligations that the Mozilla Public License, Version 2.0 provides to projects whose preferred source representation consists of traditional programming language source files.