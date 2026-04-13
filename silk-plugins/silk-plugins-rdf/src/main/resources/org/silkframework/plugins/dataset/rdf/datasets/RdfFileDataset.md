## 1. Purpose

The **RDF file** dataset plugin reads RDF data from a local file (or ZIP archive) into the project and, for supported formats, can also write RDF data back to a file.

Typical use cases:

- Loading small to medium RDF files (e.g. `.ttl`, `.nt`, `.rdf`, `.nq`) into a project.
- Providing a local RDF snapshot for exploration, mapping, or linking.
- Exporting RDF data from a workflow into a simple N-Triples file.

> **Important:** The entire dataset is loaded **in memory**. Very large files should be imported into an external RDF store and accessed via a SPARQL dataset instead.

## 2. Input and output

### Input

- **Source:**
    - A single RDF file, or
    - A ZIP archive containing multiple RDF files.

The file is read from the configured resource; no upstream operators are required.

### Output

- **Data:** RDF graph(s) exposed as a dataset that can be:
    - queried (e.g. via SPARQL-based components),
    - used as input for transformations,
    - or used in linking tasks.
- **Graphs:**
    - If a graph name is configured and supported by the chosen format, only that named graph is exposed.
    - Otherwise, the default graph is used.

## 3. Configuration notes

The detailed parameter list is generated in the UI. This section explains how to think about the most relevant settings.

### 3.1 File

- Points to the RDF file or ZIP archive to be used as the dataset.
- If the file is a ZIP archive:
    - all contained files are scanned,
    - only files matching the **ZIP file regex** (see below) are loaded.

### 3.2 Format

- Optional parameter that specifies the RDF serialization format (e.g. `RDF/XML`, `N-Triples`, `N-Quads`, `Turtle`).
- If left empty:
    - the format is **auto-detected** from the file name/extension,
    - if no format can be detected, an error is raised and the dataset will not load.
- Reading:
    - Supported formats include at least `RDF/XML`, `N-Triples`, `N-Quads`, and `Turtle`.
- Writing:
    - **Only `N-Triples` is supported for writing.**
    - For other formats, the dataset is effectively read-only.

### 3.3 Graph

- Optional graph name (IRI) that tells the dataset which graph to expose.
- Behaviour:
    - For **graph-aware formats** (e.g. `N-Quads`):
        - you must provide a graph name to select which graph is used.
    - For **graph-less formats** (e.g. `N-Triples`, `Turtle`):
        - the graph parameter is ignored and the data is treated as a single default graph.
- If left empty:
    - the default graph of the loaded dataset is used.

### 3.4 Entity list (advanced)

- Advanced parameter that allows restricting which entities (subjects) are retrieved.
- You can provide a list of entity IRIs, separated by whitespace or line breaks.
- Typical use:
    - limit retrieval to a known set of resources during exploration,
    - avoid scanning the entire file when only specific entities are needed.

### 3.5 ZIP file regex (advanced)

- Only relevant if the configured file is a ZIP archive.
- Regular expression that filters which files inside the ZIP are considered.
    - Default: `.*` (all files).
- Examples:
    - `.*\.ttl` → only Turtle files.
    - `data-.*\.nt` → only N-Triples files whose names start with `data-`.

## 4. Behaviour

When the dataset is used (e.g. queried or read by downstream components), it behaves as follows:

1. **File size check**
    - Before loading, the file size is checked against an internal maximum for in-memory processing.
    - If the file is too large, the dataset fails with a “resource too large” error rather than attempting to load it.

2. **Loading RDF data**
    - The RDF file (or all matching files inside a ZIP) is parsed into an internal RDF dataset held in memory.
    - The dataset may contain:
        - a default graph,
        - and, depending on the format, one or more named graphs.

3. **Graph selection**
    - If a graph name is configured and the selected format supports named graphs, that graph is used.
    - Otherwise, the default graph is used.
    - For graph-less formats (e.g. `N-Triples`, `Turtle`), the graph setting is ignored.

4. **Data access**
    - Once loaded, the data can be:
        - queried for types and paths,
        - used to retrieve entities by schema or by URI,
        - sampled for values and schema extraction.
    - Repeated reads re-use the in-memory dataset as long as the underlying file has not changed.

5. **File changes**
    - The dataset monitors changes to the underlying file:
        - if the file’s modification timestamp changes,
        - the RDF data is reloaded the next time it is accessed.

6. **Writing**
    - When used as a sink (writing RDF):
        - data is serialized as **N-Triples** to the configured file.
        - other output formats are not supported.

## 5. Limitations and recommendations

- **In-memory dataset**
    - All data is held in memory; this is suitable for:
        - small to medium RDF files,
        - test datasets,
        - local snapshots.
    - For large production datasets, prefer:
        - importing the data into an external RDF store,
        - and using a SPARQL dataset instead.

- **Format restrictions**
    - Reading: multiple standard RDF formats are supported.
    - Writing: only `N-Triples` is available as output format.

- **Graph handling**
    - Graph selection only works where the underlying format supports named graphs.
    - For formats without named graphs, the graph parameter is ignored.

## 6. Examples

### 6.1 Simple Turtle file as dataset

- **File:** `data/example.ttl`
- **Format:** (empty → auto-detects Turtle)
- **Graph:** (empty → default graph)

Use case:
- Small RDF dataset loaded from a Turtle file for exploration and transformations.

### 6.2 N-Quads file with explicit graph

- **File:** `data/dump.nq`
- **Format:** `N-Quads`
- **Graph:** `http://example.com/graph/main`

Use case:
- Large export containing multiple graphs, but only a specific named graph is used in the project.

### 6.3 ZIP archive with multiple RDF files

- **File:** `snapshots/rdf-data.zip`
- **Format:** (empty → formats are detected per file)
- **ZIP file regex:** `.*\.ttl`

Use case:
- ZIP archive containing various files; only Turtle files are loaded into the dataset.

## 7. When to use this dataset

Use the **RDF file** dataset when:

- You have a **local RDF file or ZIP archive** you want to work with directly.
- The data volume fits comfortably into memory.
- You need a simple, file-based way to:
    - explore RDF data,
    - run mappings or link specifications,
    - or export RDF as N-Triples.

For large or frequently updated datasets, prefer an external RDF store and a **SPARQL** dataset.
