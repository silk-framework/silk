## Overview

The Alignment plugin provides support for **writing alignment files**, following the format specified by the Alignment API ([format specification](https://moex.gitlabpages.inria.fr/alignapi/format.html)) and described in [Semantic Web Journal, SWJ60](https://www.semantic-web-journal.net/sites/default/files/swj60_1.pdf). It is designed to integrate seamlessly with Silk and CMEM BUILD's **modular, task-specific dataset architecture**, allowing users to produce alignment links in a structured and standardized way.

This plugin **focuses exclusively on writing links** between entities. Unlike other datasets, it does **not provide reading of entities**, transformations, or additional processing. Its purpose is simple and clear: to **export alignment links in a format compatible with alignment-aware tools or downstream processing**.
## General Principles

The Alignment plugin reflects two guiding principles:

1. **Separation of concerns:** The plugin isolates the task of writing alignment links from other workflow activities, such as data analysis, transformations, or validation. This ensures that each dataset remains highly focused and predictable in its behavior.

2. **Modular interoperability:** Alignment files produced can be consumed by external tools, linked back into the Silk or CMEM ecosystem, or used in subsequent workflows. The format follows a well-established specification, enabling smooth exchange and integration.

3. **Structured serialization:** Each link is represented as a `<Cell>` element with explicit fields for source, target, relation type, and confidence measure. The structure ensures clarity and interoperability, allowing these links to be consumed reliably in workflows or by external tools.

## Alignment File Format

The plugin produces files in the **Alignment format**, which is an XML-based standard for representing correspondences (links) between entities. Each link consists of:

- A **source entity URI**
- A **target entity URI**
- An optional **relation type** (e.g., equality `=`)
- An optional **confidence measure** (0.0–1.0)

The plugin handles all formatting automatically, ensuring the correct XML structure, headers, footers, and encoding (UTF-8).

### Minimal Example

To illustrate, here is a small Alignment file with two example links:

```xml
<Alignment>
  <xml>...</xml> <!-- Header metadata -->
  <map>
    <Cell>
      <entity1 rdf:resource="http://example.org/source/Person1"/>
      <entity2 rdf:resource="http://example.org/target/PersonA"/>
      <relation>=</relation>
      <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">0.95</measure>
    </Cell>
    <Cell>
      <entity1 rdf:resource="http://example.org/source/Person2"/>
      <entity2 rdf:resource="http://example.org/target/PersonB"/>
      <relation>=</relation>
      <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">0.87</measure>
    </Cell>
  </map>
  <xml>...</xml> <!-- Footer metadata -->
</Alignment>
```

Each `<Cell>` corresponds to one link between a source and a target entity.  
The `<relation>` field specifies the type of correspondence (here, equality), while `<measure>` captures an optional confidence score.

The Alignment plugin writes these `<Cell>` entries to a file, ensuring proper formatting according to the Align API specification. Users provide the file resource, and the plugin manages serialization, including the `<header>` and `<footer>` required for a valid alignment file.

### Reference

For complete details on the Alignment file format and semantics, see:

- [AlignAPI Format Documentation](https://moex.gitlabpages.inria.fr/alignapi/format.html)
- [Semantic Web Journal: Alignment API](https://www.semantic-web-journal.net/sites/default/files/swj60_1.pdf)
