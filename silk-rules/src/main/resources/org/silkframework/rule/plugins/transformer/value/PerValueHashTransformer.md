The **Per-value hash** operator hashes each input value independently and returns one hash per value. The output count always equals the input count — cardinality is preserved.

## SPARQL alignment

This operator produces the same output as the SPARQL 1.1 hash functions applied per value. For a single input value, `SHA256(?x)` in SPARQL returns the same result as this operator with the default SHA256 algorithm.

## Single-input constraint

The operator accepts exactly one input port. Connecting more than one port throws an `IllegalArgumentException`. This constraint exists because per-value hashing is defined relative to a single value sequence — combining values across ports would require choosing a port-merging strategy, which is the behaviour of the **Combined input hash** operator instead.

## Output

Each input value produces one lowercase hexadecimal hash string. The output order matches the input order. If the input is empty, the output is empty — no hash is produced.

Values are encoded as UTF-8 before hashing.

## Algorithm parameter

The algorithm parameter selects the hash function. The default is SHA-256. The five algorithms from the [SPARQL 1.1 specification](https://www.w3.org/TR/sparql11-query/#func-hash) are supported:

| SPARQL name | Java name | Notes |
|-------------|-----------|-------|
| MD5 | MD5 | Weak — vulnerable to collision attacks. Avoid for security-sensitive use. |
| SHA1 | SHA-1 | Weak — deprecated for most security purposes. |
| SHA256 | SHA-256 | Recommended default. |
| SHA384 | SHA-384 | Stronger than SHA-256. |
| SHA512 | SHA-512 | Strongest in the SPARQL set. |

Additional algorithms available on the JVM are also accepted. The full list is JVM-dependent and visible in the algorithm parameter dropdown.

Note that the Java names use hyphens (SHA-256, SHA-1) where SPARQL uses none (SHA256, SHA1). Both forms are accepted by this operator.

## Contrast with Combined input hash

The **Combined input hash** operator feeds all values from all ports into a single hash function and returns one hash regardless of input size. Use it when you need a single fingerprint for a set of values taken together.

Use **Per-value hash** when each value needs its own hash — for example, to hash a column of URIs independently, or to replicate `SHA256(?x)` in SPARQL.
