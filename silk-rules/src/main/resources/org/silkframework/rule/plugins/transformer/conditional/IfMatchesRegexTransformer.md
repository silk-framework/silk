## Description

The `ifMatchesRegex` plugin uses a regular expression as a matching condition in order to distinguish which input to
take.

This plugin is a _conditional transformer_ plugin. This means that a _regular expression_ is used as a matching
condition, i.e. it is used in order to answer the question "does the regular expression match the provided text?".
If it does match, then the next input is taken as output. Otherwise, the last input is taken as output, provided it
exists. If it doesn't, the output is empty.

### Creating and calling the conditional transformer

For _creating_ an instance of this transformer, we need to give it a _regular expression_ with the parameter `regex`
and, optionally, tell with the parameter `negate` whether we will _take_ or _exclude_ the second input value upon
calling it (by default, we take it). In other words: the `negate` parameter tells us which 'way' we want to go after the
comparison.

For _calling_ this transformer, we need to give it _two_ or _three_ values as its input. The _first_ input value is
what the regular expression should match (or not, depending on `negate`), and the following values are the branches of
the condition, i.e. the output if the regex matches the first input value, and the alternative if it doesn't.

The following list summarizes the creation and the behavior of the conditional transformer:

* The regular condition within the transformer is used as a matching condition on the first input.
* If it matches, then the _second_ input is returned.
* If it doesn't match, then the _third_ input is returned, provided it exists.
* If the third input doesn't exist, an empty sequence is returned.

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

Additionally to the `ifMatchesRegex` plugin, there are related plugins such as `validateRegex`, `regexReplace` and
`regexExtract`.

The distinctive feature of each of these plugins lies in what happens whenever the regular expression
matches the input value(s): the `ifMatchesRegex` plugin is useful for _conditionally distinguishing_ which input to
take, `validateRegex` is used for _validating_ the input, `regexReplace` _replaces_ all occurrences, and
`regexExtract` _extracts_ them.
