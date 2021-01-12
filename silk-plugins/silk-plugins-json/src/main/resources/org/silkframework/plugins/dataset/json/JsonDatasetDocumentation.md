Typically, this dataset is used to transform an JSON file to another format, e.g., to RDF.

It supports a number of special paths:
- `#id` Is a special syntax for generating an id for a selected element. It can be used in URI patterns for entities which do not provide an identifier. Examples: `http://example.org/{#id}` or `http://example.org/{/pathToEntity/#id}`.
- `#text` retrieves the text of the selected node.
- The backslash can be used to navigate to the parent JSON node, e.g., `\parent/key`. The name of the backslash key (here `parent`) is ignored.


When storing entities in Json format all entities will be stored in an array at the top-level of the Json document. The option makeFirstEntityJsonObject (false by default) can change this.
If activated a top level object will be used. To preserve valid Json, only the first entity will be stored in this case.