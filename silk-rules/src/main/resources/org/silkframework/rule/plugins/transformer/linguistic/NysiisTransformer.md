This transformer plugin implements the **NYSIIS** phonetic algorithm for encoding names.

The acronym NYSIIS stands for _New York State Identification and Intelligence System_. This so-called _phonetic code_
is an improvement upon the Soundex algorithm.

## Description

The NYSIIS Phonetic Code is more involved than the comparatively simple mapping of the Soundex algorithm.
The full procedure of the algorithm is described in the corresponding
[Wikipedia page](https://en.wikipedia.org/wiki/New_York_State_Identification_and_Intelligence_System).

### Plain NYSIIS

The (plain) NYSIIS algorithm was originally meant for encoding **names**. Originally, the maximum number of characters
in the output was limited to six, but modern implementations of this algorithm —and improvements thereupon— don't
necessarily contain this limitation. This plugin doesn't.

### Modified NYSIIS

The **Modified NYSIIS** is an improvement of the NYSIIS algorithm. Its working is illustrated, step by step, in
http://www.dropby.com/NYSIIS.html.

## Examples

We can get an idea of the output of the NYSIIS algorithm using an online version of it such as the already mentioned
http://www.dropby.com/NYSIIS.html. It contains both the (plain) NYSIIS and the _modified_ NYSIIS algorithms.

As a comparison of the two versions of NYSIIS, we give a few examples:

* `macintosh` is encoded as `mcant` by the NSIIS, and as `mcantas` by the refined or modified NYSIIS.
* `phillipson` leads to `ffalapsan` in NSIIS and `falapsan` in its refined version.
* `phone` leads to `ffan` in NSIIS and `fan` in its refined version.
* `eccenca` is converted to `ecanc` in both versions of NSIIS.

## Related plugins

Other phonetic algorithms usually associated are the different variations or improvements of the Soundex algorithm,
implemented by this (NYSIIS) and the [`Metaphone`](https://en.wikipedia.org/wiki/Metaphone) algorithms.
The corresponding linguistic transformer plugins are named accordingly.
