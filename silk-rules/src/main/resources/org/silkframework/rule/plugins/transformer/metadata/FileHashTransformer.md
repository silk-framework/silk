Computes a file hash using the selected algorithm and returns it as a hexadecimal string. If no file
is selected, the operator uses the file of the input dataset. It ignores individual input values.

## Hash algorithms

The default algorithm is `SHA256`. The operator supports algorithms from the Secure Hash Algorithms
family as well as `MD2` and `MD5` from the Message-Digest Algorithm family. Some supported algorithms
are vulnerable to collision attacks and are unsuitable for security-sensitive use.
[Secure Hash Algorithms](https://en.wikipedia.org/wiki/Secure_Hash_Algorithms) · [Message-Digest Algorithm](https://en.wikipedia.org/wiki/MD5)

## Caching

The operator caches the hash and recalculates it when the file changes. For large files, calculating
the hash for the first time or after an update can take a while.
