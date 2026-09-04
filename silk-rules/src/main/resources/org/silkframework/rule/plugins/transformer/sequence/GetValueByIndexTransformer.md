## Description

This transformer extracts a single value from a sequence at a fixed position — useful whenever a data source
returns its values in a known, meaningful order and only one of them is needed. A positive index counts from the
start of the sequence, `0`-based. A negative index counts from the end — `-1` is the last value, `-2` the
second-to-last, and so on. An index equal to the negative length of the sequence wraps around to the first value,
rather than falling out of range.

If no value exists at the given index, the behavior depends on `failIfNotFound`: by default the input is simply
dropped from the result, but if `failIfNotFound` is enabled, the transformation fails instead.

Each input value is a sequence in its own right, and the index is resolved independently for each one. If this
transformer receives several input values, every one of them is indexed on its own, not treated as a single
combined sequence.

Indexing only makes sense where the data source preserves some kind of order, such as XML or JSON. RDF has no
inherent ordering, so using this transformer on values sourced from an RDF model is not recommended — "index 0"
against such a sequence would be arbitrary, and could change between runs.

If `emptyStringToEmptyResult` is enabled, a result that is an empty string is treated the same as no result at
all, rather than being returned as an empty string.

## Example usage

Given the input sequence `a, b, c`:

- Index `-1` returns `c` — the last value.
- Index `-3` — equal to the negative length of the sequence — wraps around and returns `a`, the first value.

Given the single-element input sequence `a`:

- Index `-2` is past the start of the sequence, so by default the result is empty.
- Index `-2` with `failIfNotFound` enabled throws an error instead of returning an empty result.
