## Description

The `validateRegex` plugin validates whether all values match a given regular expression.

This plugin is a _validation_ transformer plugin. This means that if the regular expression does _not_ match the input
value, it will _fail_ with a validation exception.

### Notes on regular expressions

The most commonly used examples of regular expressions are `"\\s*"` for representing whitespace characters, `[^0-9]*`
for numbers, and `[a-z]*` for the usual English characters between `a` and `z`.

An uppercase version of the predefined character classes means _negation_, such as `"\\S*"` for _non_-whitespace
characters, or `"\\D*"` for _non_-digits.
Similarly, the hat sign `^` can be used for negating (arbitrary) character classes, such as `[^xyz]` for any character
except `x`, `y` or `z`.

**Attention**: Slashes in regular expressions have to be _escaped_, e.g. instead of `\s` we need to escape it as `\\s`.

### Note for advanced users

A compilation of the available constructs for building regular expressions is available in the
[API of the Java `Pattern`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#sum).

## Relation to other plugins

Additionally to the `validateRegex` plugin, there are related plugins such as `ifMatchesRegex`, `regexReplace` and
`regexExtract`.

The distinctive feature of each of these plugins lies in what happens whenever the regular expression
matches the input value(s): the `validateRegex` plugin is used for _validating_ the input, `ifMatchesRegex` is useful
for _conditionally distinguishing_ which input to take, `regexReplace` _replaces_ all occurrences, and `regexExtract`
_extracts_ them.
