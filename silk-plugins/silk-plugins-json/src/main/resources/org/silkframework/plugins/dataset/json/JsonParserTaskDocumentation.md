## Parse JSON

Parse JSON is a workflow operator that extracts structured data from a JSON string held in a field on incoming
entities. It sits inside a pipeline between an upstream source and a downstream operator — typically a transformation
— and turns the JSON content into entities ready for further processing.

The operator is useful whenever JSON arrives not as a file but as a string stored in a field: the result of an HTTP
request, a column in a database record, a payload embedded in another dataset. Parse JSON consumes that string in place
and produces entities for the rest of the pipeline.

## Input

Parse JSON accepts exactly one input. It iterates over every entity in that input, extracts the JSON string from a
field on each entity, parses it, and produces output entities from its contents. The output entities from all input
entities are concatenated into a single stream for the downstream operator.

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
*/{id}* applied to an input entity with URI *http://example.org/record/42* produces URIs by appending the value of the
*id* field — so an entity whose *id* is *7* receives URI *http://example.org/record/42/7*. When left empty, URIs are
generated automatically.

**Navigate into arrays** controls how JSON arrays are handled during path traversal. In JSON, an array is an anonymous
container with no name of its own — just a list of items. When a path expression crosses an array mid-way, it is
ambiguous whether the array itself or its contents is the intended target. This parameter resolves that ambiguity. When
enabled — the default — the operator descends into arrays automatically, so a path like */Persons/Person* reaches the
Person elements directly even if Persons is an array. When disabled, the array is treated as an explicit step in the
path: to reach the same Person elements, the path must be written as */Persons/#array/Person*.

Parse JSON supports the same path expressions as the JSON dataset, including wildcards for children and descendants,
backward paths, and special paths for hash IDs, key names, and array elements.

## Schema

Before producing output entities, Parse JSON needs to know which fields to extract. The set of fields — the output
schema — is requested by the downstream operator and reaches Parse JSON before any parsing happens. For each
requested field, Parse JSON evaluates its path expression against the parsed JSON, starting from the configured base
path, and writes the resulting values onto the output entity. When the downstream operator requests a multi-entity
schema, Parse JSON produces the root entities and the nested sub-entity tables in a single pass. In practice that
operator is a transformation.

Parse JSON cannot be connected directly to a dataset. A dataset declares no fields to read, so Parse JSON has nothing
to extract. Workflows that wire Parse JSON straight into a dataset fail at execution time with an error stating that
the operator cannot be connected directly to a dataset and must feed an operator that declares a target schema.

## Example

An upstream operator produces a single entity with the following JSON string in its first field. Because *Input path*
is not set, Parse JSON reads from that first field by default.

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

The result is two entities — one for Alice, one for Bob — each with id, name, and city as fields.

If the upstream operator produces several entities, Parse JSON parses the JSON string in each one in turn and
concatenates the resulting entities into a single output stream.
