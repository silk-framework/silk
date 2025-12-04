## Description

The `regexReplace` plugin replaces all occurrences of a regular expression.

This plugin is a _replace_ transformer plugin. This means that if the regular expression does _not_ match the input
value, it will be replaced with an empty string, i.e. deleted.

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

Additionally to the `regexReplace` plugin, there are related plugins such as `validateRegex`, `ifMatchesRegex` and
`regexExtract`.

The distinctive feature of each of these plugins lies in what happens whenever the regular expression
matches the input value(s): the `regexReplace` plugin is used for _replacing_ the input, `validateRegex` is useful for
_validating_ the input, `ifMatchesRegex` _conditionally distinguishes_ which input to take, and `regexExtract`
_extracts_ all occurrences of the matching.
