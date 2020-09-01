Typically, this dataset is used to transform an JSON file to another format, e.g., to RDF.

It supports a number of special paths:
- `#id` Is a special syntax for generating an id for a selected element. It can be used in URI patterns for entities which do not provide an identifier. Examples: `http://example.org/{#id}` or `http://example.org/{/pathToEntity/#id}`.
- `#text` retrieves the text of the selected node.
- The backslash can be used to navigate to the parent JSON node, e.g., `\parent/key`. The name of the backslash key (here `parent`) is ignored.
