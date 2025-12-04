Typically, this dataset is used to transform an JSON file to another format, e.g., to RDF.

## Reading

In addition to plain JSON files, *JSON Lines* files can also be read.

For reading, the JSON dataset supports a number of special paths:
- `#id` Is a special syntax for generating an id for a selected element. It can be used in URI patterns for entities which do not provide an identifier. Examples: `http://example.org/{#id}` or `http://example.org/{/pathToEntity/#id}`.
- `#text` retrieves the text of the selected node.
- The backslash can be used to navigate to the parent JSON node, e.g., `\parent/key`. The name of the backslash key (here `parent`) is ignored.

## Writing

When writing JSON, all entities need to possess a unique URI. Writing multiple root entities with the same URI will result in multiple entries in the generated JSON. If multiple nested entities with the same URI are written, only the last entity with a given URI will be written.