This transformer plugin implements the **Metaphone** phonetic algorithm for indexing words according to English.

## Description

[Metaphone](https://en.wikipedia.org/wiki/Metaphone) is an algorithm which encodes words according to their English
pronunciation. This is also what other _phonetic algorithms_ such as Soundex do. Compared to Soundex, Metaphone's
algorithm contains a richer description of English spelling, leading to a better phonetic encoding of the input.

A description of the procedure can be found in the corresponding
[Wikipedia page](https://en.wikipedia.org/wiki/Metaphone#Procedure).

## Examples

We can get an idea of the output of the Metaphone algorithm using an online version of it such as the
[Metaphone Generator](https://en.toolpage.org/tool/metaphone).

Illustrative examples:

* `knuth` leads to the encoding `n0`.
* `school` is encoded as `skhl`.
* `encyclopedia` is encoded as `ensklpt`.
* `accuracy` is encoded as `akkrs`.
* `eccenca` is encoded as `eksnk`

## Related plugins

Related phonetic algorithms are the different variations or improvements of the Soundex algorithm, implemented by this
(Metaphone) and the [`Metaphone`](https://en.wikipedia.org/wiki/Metaphone) algorithms.
The corresponding linguistic transformer plugins are named accordingly.
