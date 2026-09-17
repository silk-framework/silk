The **Combined input hash** operator produces exactly one hash value covering all input values combined, across all connected input ports, with an optional separator inserted between them.

## How combining works

All values from all input ports are fed sequentially into a single hash function, in port order; within each port, values are processed in the order they arrive. An optional separator, set with the glue parameter, can be inserted between every adjacent pair of values in that order — see the Glue parameter section below. The hash covers the concatenated byte content of all values in that order, together with any inserted separator.

Swapping two values, or changing a single character within one value, produces a different hash. Connecting one port with values `["apple", "banana"]` produces the same hash as connecting two ports with `["apple"]` and `["banana"]` respectively, because the bytes are fed in the same sequence either way.

## Output

The output is a single lowercase hexadecimal string whose length is fixed by the chosen algorithm: 64 characters for SHA-256, 32 for MD5, 40 for SHA-1, 96 for SHA-384, 128 for SHA-512. Connecting no values at all still produces a hash — the hash of the empty message, not an error.

Values are encoded as UTF-8 before hashing, so the same text produces the same bytes regardless of platform or locale.

## Algorithm parameter

The algorithm parameter selects the hash function. The following algorithms from the [SPARQL 1.1 specification](https://www.w3.org/TR/sparql11-query/#func-hash) are supported:

| SPARQL name | Java name | Notes |
|-------------|-----------|-------|
| MD5 | MD5 | Has known collision vulnerabilities. |
| SHA1 | SHA-1 | Deprecated for most security purposes. |
| SHA256 | SHA-256 | Default. |
| SHA384 | SHA-384 | |
| SHA512 | SHA-512 | |

Additional algorithms available on the JVM (such as SHA-512/256 and SHA-3 variants) are also accepted. The full list is JVM-dependent and visible in the algorithm parameter dropdown.

The Java names use hyphens (SHA-256, SHA-1) where SPARQL uses none (SHA256, SHA1); both forms are accepted by this operator.

## Glue parameter

The glue parameter sets a separator string that is inserted between every adjacent pair of values in the traversal order described above, defaulting to an empty string. Its text can contain the escaped sequences `\n`, `\t`, and `\\`, which are converted to a newline, a tab, and a backslash respectively before hashing.

Connecting one port with values `["apple", "banana"]` and a glue of `-` produces the same hash as connecting two ports with `["apple"]` and `["banana"]` respectively, using the same glue, because the glue is inserted at the same point in the byte sequence either way.