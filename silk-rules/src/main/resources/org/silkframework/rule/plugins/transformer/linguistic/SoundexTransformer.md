This transformer plugin implements the **Soundex** phonetic algorithm for indexing names by their English sounds.

## Description

This plugin is a _linguistic_ transformer plugin. Specifically, it provides an implementation of the
[**Soundex algorithm**](https://en.wikipedia.org/wiki/Soundex), also known as
[**Soundex Indexing System**](https://www.archives.gov/research/census/soundex).

Soundex is the simplest —and oldest— of the _phonetic algorithms_. This plugin includes both the original Soundex and a
variation thereof known as "Refined Soundex".

### Soundex

The (plain) **Soundex algorithm** encodes or _indexes_ an English word such as a (sur)name into the pattern
`initial letter + three digits`, where the three digits represent given _consonant groups_. This _mapping_ is the
following:

* `b, f, p, v → 1`
* `c, g, j, k, q, s, x, z → 2`
* `d, t → 3`
* `l → 4`
* `m, n → 5`
* `r → 6`

Notice that, in order to use the classical Soundex algorithm, the plugin parameter `refined` needs to be set to `false`.

### Refined Soundex

The **Refined Soundex** algorithm is an improvement of the Soundex algorithm, without the limitations of dropping the
vocals and restricting the output to a four-digit encoding of the input. This variation of the Soundex algorithm can be
used by setting the plugin parameter `refined` to `true` (default). Its mapping is the following:

* `a, e, h, i, o, u, w, y → 0`
* `b, p → 1`
* `f, v → 2`
* `c, k, s → 3`
* `g, j → 4`
* `q, x, z → 5`
* `d, t → 6`
* `l → 7`
* `m, n → 8`
* `r → 9`

## Examples

### Soundex

We can get an idea of the output of the Soundex algorithm using an online Soundex Converter such as
https://www.mainegenealogy.net/soundex_converter.asp.

* `robert` and `rupert` lead to the same Soundex index: `r163`.
* `euler` leads to `e460`, `gauss` is `g200` and `hilbert` corresponds to `h416`.

### Refined Soundex

* `braz` and `broz` lead to the same Refined Soundex index: `b1905`.
* `caren`, `carren`, `coram`, `corran`, `curreen` and `curwen` are all encoded with `c30908`.
* `hairs`, `hark`, `hars`, `hayers`, `heers` and `hiers` are all mapped to `h093`.
* All sorts of variations of `lambard`, such as `lambart`, `lambert`, `lambird` or `lampaert`, lead to `l7081096`. 

## Related plugins

Other phonetic algorithms usually associated with Soundex are the variations or improvements implemented by the
[`NYSIIS`](https://en.wikipedia.org/wiki/New_York_State_Identification_and_Intelligence_System)
and [`Metaphone`](https://en.wikipedia.org/wiki/Metaphone) algorithms. The corresponding linguistic transformer plugins
are named accordingly.
