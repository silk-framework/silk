## JSON to File

The JSON to File operator takes a JSON string held in a field on each incoming entity and writes it verbatim to a
file. The resulting file is surfaced downstream as a file entity, so any consumer that accepts file entities — a
file-backed dataset, another file-processing operator — can pick it up.

The operator does not parse the JSON into structured entities. It validates that the value is well-formed JSON and then
writes the bytes as they are. The produced file is tagged with *application/json* as its content type.

## Input

JSON to File accepts exactly one input. It iterates over every entity in that input, reads the JSON string from a
field on each entity, validates it, and produces one file per entity. The output entities from all input entities are
concatenated into a single stream for the downstream consumer.

Which field holds the JSON string is controlled by the *Input path* parameter. When set, the operator reads the value
at the given path expression. When left empty, it reads the value of the first available field. If no value is found
at the expected location, or if the field is empty, the operator raises an error and stops. If the value is present
but is not valid JSON, the operator raises an error naming the parse failure.

## Output

The output of JSON to File is a stream of file entities. Each file entity wraps a file holding the JSON bytes from one
input entity, tagged with *application/json* as its MIME type. Downstream operators or datasets that accept file
entities consume the stream directly.

When the output is wired to a file-backed dataset, the dataset writes the file's bytes into its own resource. The end
result is a JSON file on disk.

## Parameters

**Input path** controls which field of the input entity holds the JSON string. When set to a Silk path expression
such as */jsonContent*, the operator reads the value at that path. When left empty, the operator reads the value of
the first available field.

**Output file name** controls how the produced files are named. When left empty, each file is allocated with an
auto-generated temporary name. When set, the parameter is used as the literal filename for single-entity input; when
the input carries more than one entity, an index suffix is appended before the extension to keep filenames unique.
For example, *out.json* applied to three input entities produces *out-0.json*, *out-1.json*, *out-2.json*.

## Example

An upstream operator produces a single entity with the following JSON string in its *jsonContent* field. With
*Input path* set to */jsonContent*, JSON to File reads from that field.

```json
{
  "response": {
    "persons": [
      { "id": "1", "name": "Alice" },
      { "id": "2", "name": "Bob" }
    ]
  }
}
```

JSON to File validates the string and writes it verbatim to a file tagged with *application/json*. The produced file
entity can be wired into a downstream JSON dataset to persist the value as a file on disk, or fed into any other
operator that accepts file entities.
