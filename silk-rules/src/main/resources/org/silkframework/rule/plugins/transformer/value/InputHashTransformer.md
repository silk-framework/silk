The **Combined input hash** operator produces exactly one hash value covering all input values combined, across all connected input ports. However many values arrive and however many ports are connected, the output is always a single string.

## How combining works

All values from all input ports are fed sequentially into a single hash function — port 1 first, then port 2, and so on. Within each port, values are processed in the order they arrive. No separator is inserted between values or between ports. The hash covers the concatenated byte content of all values in that traversal order.

This means the result depends on both the content and the order of values. The same set of values in a different order produces a different hash. Connecting one port with values `["apple", "banana"]` produces the same hash as connecting two ports with `["apple"]` and `["banana"]` respectively, because the bytes are fed in the same sequence either way.

## Output

The output is a single lowercase hexadecimal string. The length depends on the algorithm: 64 characters for SHA-256, 32 for MD5, 40 for SHA-1, 96 for SHA-384, 128 for SHA-512. If the input is empty, the output is the hash of an empty message.

Values are encoded as UTF-8 before hashing.

## Algorithm parameter

The algorithm parameter selects the hash function. The default is SHA-256. The following algorithms from the [SPARQL 1.1 specification](https://www.w3.org/TR/sparql11-query/#func-hash) are supported:

| SPARQL name | Java name | Notes |
|-------------|-----------|-------|
| MD5 | MD5 | Weak — vulnerable to collision attacks. Avoid for security-sensitive use. |
| SHA1 | SHA-1 | Weak — deprecated for most security purposes. |
| SHA256 | SHA-256 | Recommended default. |
| SHA384 | SHA-384 | Stronger than SHA-256. |
| SHA512 | SHA-512 | Strongest in the SPARQL set. |

Additional algorithms available on the JVM (such as SHA-512/256 and SHA-3 variants) are also accepted. The full list is JVM-dependent and visible in the algorithm parameter dropdown.

Note that the Java names use hyphens (SHA-256, SHA-1) where SPARQL uses none (SHA256, SHA1). Both forms are accepted by this operator.