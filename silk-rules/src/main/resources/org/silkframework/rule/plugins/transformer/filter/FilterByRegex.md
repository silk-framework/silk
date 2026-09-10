## Description

The Filter by regex plugin keeps or drops values based on a regular expression. By default, a value is kept only if it matches the regex in full; setting `contains` relaxes that to keep any value the regex matches anywhere within. Setting `negate` inverts whichever of those two decisions is in effect.

## Notes on regular expressions

Attention: regex metacharacters in the pattern have to be escaped to be matched literally, e.g. a literal dot needs `\\.`, not `.`, which otherwise means "any character."

A pattern that can match an empty string, such as `a*`, matches every value under `contains`, since an empty match always "occurs somewhere" in any string.

A regex that fails to compile throws a `PatternSyntaxException` rather than silently doing nothing.

### Note for advanced users

A compilation of the available constructs for building regular expressions is available in the
[API of the Java `Pattern`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#sum).

## Missing or empty input

If no input is connected to this transformer, it throws a `ValidationException`. If an input is connected but carries no values, the transformer returns nothing for that entity — this is not an error.
