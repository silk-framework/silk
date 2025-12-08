## Description

The `regexExtract` plugin extracts one or all matches of a regular expression within the input.

This plugin is an _extraction_ transformer plugin. It is configured with the parameters `regex` and `extractAll`. The
regular expression `regex` is simply the pattern used in the matching. With `extractAll`, we tell the `regexExtract`
plugin whether to extract _all_ values (with `extractAll = true`) or only the _first_ occurrence of the matching
(with `extractAll = false`, which is the default).

Additionally to normal regular expressions, we can also use _capturing groups_ such as in `(A)(B)(C)` instead of just
`ABC`. If capturing groups are used in a regular expression, only the _first_ capturing group will be considered. This
does _not_ mean the first matching group, but the first capturing group in the regex.

### Notes on regular expressions

The most commonly used examples of regular expressions are `"\\s*"` for representing whitespace characters, `[^0-9]*`
for numbers, and `[a-z]*` for the usual English characters between `a` and `z`. The star (`*`) represents an arbitrary
number of occurrences (zero included), whereas the plus sign (`+`) indicates a strictly positive number of occurrences
(zero excluded).

An uppercase version of the predefined character classes means _negation_, such as `"\\S*"` for _non_-whitespace
characters, or `"\\D*"` for _non_-digits.
Similarly, the hat sign `^` can be used for negating (arbitrary) character classes, such as `[^xyz]` for any character
except `x`, `y` or `z`.

**Attention**: Slashes in regular expressions have to be _escaped_, e.g. instead of `\s` we need to escape it as `\\s`.

### Note for advanced users

A compilation of the available constructs for building regular expressions is available in the
[API of the Java `Pattern`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#sum).

## Relation to other plugins

Additionally to the `regexExtract` plugin, there are related plugins such as `validateRegex`, `ifMatchesRegex` and
`regexReplace`.

The distinctive feature of each of these plugins lies in what happens whenever the regular expression
matches the input value(s): the `regexExtract` plugin is used for _extracting_ matches from the input, `validateRegex`
is useful for _validating_ the input, `ifMatchesRegex` _conditionally distinguishes_ which input to take, and
`regexReplace` _replaces_ all occurrences of the matching.
