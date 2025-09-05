# Description

This transformer takes _three_ inputs: a single _output value_, a sequence of _regular expressions_ and a sequence of
_values to check_ against the regular expressions. It returns a sequence of copies of the output value exclusively at
those positions where one of the regular expressions matches the input value.

In other words: It _selects_ the matches within the sequence of values against the regular expressions and 'marks' them
with the provided output value.

As a further detail of its operation: If the parameter `oneOnly` of the transformer is set to `true`, then only the
position of the _first_ matching regular expression will be marked with the output value. There won't be any further
marked matches.

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

Additionally to the `regexSelect` plugin, there are related plugins such as `ifMatchesRegex`, `validateRegex`,
`regexReplace` and `regexExtract`.

The distinctive feature of each of these plugins lies in what happens whenever the regular expression
matches the input value(s): the `ifMatchesRegex` plugin is useful for _conditionally distinguishing_ which input to
take, `validateRegex` is used for _validating_ the input, `regexReplace` _replaces_ all occurrences, and
`regexExtract` _extracts_ them.
