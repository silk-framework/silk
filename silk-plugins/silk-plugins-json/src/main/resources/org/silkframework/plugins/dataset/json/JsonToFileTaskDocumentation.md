## JSON to File

The JSON to File operator takes a JSON string held in a field on each incoming entity and writes it to a file. The
resulting file is surfaced downstream as a file entity, so any operator that accepts file entities — a file-backed
dataset, another file-processing operator — can pick it up.

The operator does not parse the JSON into structured entities. It validates that the value is well-formed JSON and then
writes the JSON value to the file. The content type of the produced file is set via the *MIME type* parameter and
defaults to *application/json*.

## Input

JSON to File accepts exactly one input. It iterates over every entity in that input, reads the JSON string from a
field on each entity, and validates it. What it then produces depends on the *Output mode*: in *file* mode (the
default) one file per entity; in *zip* mode a single ZIP archive with one entry per entity; in *jsonArray* mode a
single file holding all the JSON values merged into one JSON array. The output is surfaced as a stream of file
entities for the downstream operator.

Which field holds the JSON string is controlled by the *Input path* parameter. When set, the operator reads the value
at the given path expression. When left empty, it reads the value of the first available field. If no value is found
at the expected location, or if the field is empty, the operator raises an error and stops. If the value is present
but is not valid JSON, the operator raises an error naming the parse failure.

## Output

The output of JSON to File is a stream of file entities. In *file* mode each file entity wraps a file holding the
JSON value from one input entity. In *zip* mode the stream contains a single file entity whose backing file is a ZIP
archive with one entry per input entity. In *jsonArray* mode the stream contains a single file entity backed by one
file holding a JSON array of all the input values. In *file* and *jsonArray* mode the MIME type is the value of the
*MIME type* parameter; in *zip* mode a default *application/json* is overridden to *application/zip* (see *MIME type*
below). Downstream operators or datasets that accept file entities consume the stream directly.

When the output is wired to a file-backed dataset, the dataset writes the file's bytes into its own resource. The end
result is a file on disk — a JSON file per entity in *file* mode, a ZIP archive in *zip* mode, or a single JSON array
file in *jsonArray* mode.

## Parameters

**Input path** controls which field of the input entity holds the JSON string. When set to a Silk path expression
such as */jsonContent*, the operator reads the value at that path. When left empty, the operator reads the value of
the first available field.

**Output file name** controls how the produced files are named. When left empty, each file is allocated with an
auto-generated temporary name. When set, the parameter is used as the literal filename for single-entity input; when
the input contains more than one entity, an index suffix is appended before the extension to keep filenames unique.
For example, *out.json* applied to three input entities produces *out-0.json*, *out-1.json*, *out-2.json*. When the
filename has no extension, the index is appended to the end: *out* produces *out-0*, *out-1*, *out-2*. This
index-suffix behaviour applies in *file* mode. In *zip* mode the name is used for the ZIP container and as the base
for entry names; in *jsonArray* mode there is always exactly one output file, so the name is used as-is with no index
suffix.

**MIME type** sets the content type of every produced file. Defaults to *application/json*. In *zip* mode, when this
parameter is left at its default value, the executor overrides it to *application/zip* automatically; an explicit
value is used as-is even in *zip* mode. In *file* and *jsonArray* mode the default *application/json* is correct and
is not overridden.

**Output property** wraps the JSON value in a JSON object under the given property key before writing. When set
to *payload*, an input value of `{"name":"Alice"}` is written as `{"payload":{"name":"Alice"}}`. When left empty
(default), the value is written as-is. The wrapping applies in all three output modes; in *jsonArray* mode each
element of the array is the wrapped form.

**Output mode** selects what the operator produces. *file* (the default) writes one file per input entity. *zip* packs
all input entities into a single ZIP file — one ZIP entry per entity, producing a single file entity whose backing
file is a ZIP archive. The entry naming inside the ZIP follows the same convention as the output file name in *file*
mode: a single entity with no output file name set uses the literal entry name *entry.json*; multiple entities with no
output file name set use *entry-0.json*, *entry-1.json*, and so on; when an output file name is configured, it is used
as the ZIP container name and as the base for entry names. *jsonArray* merges all input entities into a single file
holding one JSON array whose elements are the JSON values from each entity, in input order; there is always exactly
one output file.

## Output mode examples

In *zip* mode: an upstream operator produces two entities, each with a JSON string in the *jsonContent* field. With
*Input path* set to */jsonContent* and *Output mode* set to *zip*, JSON to File produces a single file entity backed
by a ZIP archive containing two entries: *entry-0.json* and *entry-1.json*. The archive is written with a content type
of *application/zip*. Wiring the output into a file-backed dataset writes the ZIP file to that dataset's resource.

In *jsonArray* mode: with two entities holding `{"id":1}` and `{"id":2}` and *Output mode* set to *jsonArray*, JSON to
File produces a single file containing the JSON array `[{"id":1},{"id":2}]`, with a content type of *application/json*.

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

JSON to File validates the string and writes it to a file with a content type of *application/json*. The produced file
entity can be wired into a downstream JSON dataset to persist the value as a file on disk, or fed into any other
operator that accepts file entities.

With the `outputProperty` parameter set to `payload`, the same input is instead written as:

```json
{
  "payload": {
    "response": {
      "persons": [
        { "id": "1", "name": "Alice" },
        { "id": "2", "name": "Bob" }
      ]
    }
  }
}
```
