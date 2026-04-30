## Parse JSON

Parse JSON is a workflow operator that extracts structured data from a JSON string carried as a field value by an
incoming entity. It sits inside a pipeline between an upstream source and a downstream consumer — typically a
transformation — and turns the JSON content into entities ready for further processing.

The operator is useful whenever JSON arrives not as a file but as a string stored in a field: the result of an HTTP
request, a column in a database record, a payload embedded in another dataset. Rather than requiring that content to be
written to a temporary file and read back through a dataset, Parse JSON handles it directly in the pipeline.

## Input

Parse JSON accepts exactly one input operator. From that input it reads only the first entity — all subsequent entities
in the input stream are ignored. The operator extracts the JSON string from a field on that entity, parses it, and
produces output entities from its contents.

Which field is used as the JSON source is controlled by the *Input path* parameter. When set, Parse JSON looks for the
JSON string at the given path expression. When left empty, it reads the value of the first available field. If no value
is found at the expected location, or if the field is empty, the operator raises an error and stops.

## Output

The output of Parse JSON is a set of entities extracted from the parsed JSON structure. These entities are shaped by
three parameters: *Base path*, *URI suffix pattern*, and *Navigate into arrays*.

**Base path** determines the starting point within the JSON document. When set to a path such as */Persons/Person*, only
elements found at that location are read as entities; everything else in the document is ignored. When left empty, all
direct children of the root element are read.

**URI suffix pattern** controls how the URIs of the output entities are constructed. The pattern is evaluated relative
to the URI of the input entity: whatever suffix is specified gets appended to that URI. For example, a pattern of
*/{id}* applied to an input entity with URI *http://example.org/record/42* would produce output entity URIs like
*http://example.org/record/42/value-of-id*. When left empty, URIs are generated automatically.

**Navigate into arrays** controls how JSON arrays are handled during path traversal. In JSON, an array is an anonymous
container with no name of its own — just a list of items. When a path expression crosses an array mid-way, it is
ambiguous whether the array itself or its contents is the intended target. This parameter resolves that ambiguity. When
enabled — the default — the operator descends into arrays automatically, so a path like */Persons/Person* reaches the
Person elements directly even if Persons is an array. When disabled, the array is treated as an explicit step in the
path: to reach the same Person elements, the path must be written as */Persons/#array/Person*.

The full path model of the JSON dataset applies in Parse JSON without restriction. This covers forward paths into nested
objects, the wildcard for selecting all direct children, the double wildcard for all descendants at any depth, the
backward path for navigating to the parent element, and special paths for generating hash-based identifiers, reading the
string representation of a node, retrieving the current key name, and accessing array elements explicitly.

## Schema

Before producing output entities, Parse JSON needs to know which fields to extract. This schema comes from one of two
places.

In the normal case, a downstream operator — typically a transformation — specifies the fields it expects. Parse JSON
receives this schema as part of the execution pipeline and uses it directly to extract the right values from the parsed
JSON.

When no schema is specified by a downstream operator, Parse JSON infers one. It does this by inspecting the JSON content
of the first input entity and collecting all paths present in the document. The inferred schema covers every field
reachable from the configured base path. This inference happens automatically and requires no configuration.

## Example

Consider an upstream operator that produces a single entity carrying the following JSON string in its first field.
Because *Input path* is not set, Parse JSON reads from that first field by default.

```json
{
  "response": {
    "persons": [
      { "id": "1", "name": "Alice", "city": "Berlin" },
      { "id": "2", "name": "Bob", "city": "London" }
    ]
  }
}
```

With *Base path* set to */response/persons*, Parse JSON navigates past the response wrapper and reads each element of
the persons array as a separate entity. The array is crossed automatically because *Navigate into arrays* is enabled.
With *URI suffix pattern* set to */{id}*, the two output entities receive URIs constructed by appending the value of
their id field to the URI of the input entity.

The result is two entities — one for Alice, one for Bob — each carrying id, name, and city as fields.
