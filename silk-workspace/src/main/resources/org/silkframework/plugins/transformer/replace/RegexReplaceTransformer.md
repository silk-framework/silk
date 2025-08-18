The most common use-cases of regular expressions are e.g. `"\\s*"` for representing whitespace characters,
`[^0-9]*`for numbers, or `[a-z]*` for the usual English characters between `a` and `z`.

An uppercase version of the predefined character classes means _negation_, such as `"\\S*"` for _non_-whitespace
characters, or `"\\D*"` for _non_-digits.
Similarly, the hat sign `^` can be used for negating (arbitrary) character classes, such as `[^xyz]` for any character
except `x`, `y` or `z`.

**Note for advanced users**:
A compilation of the available constructs for building regular expressions is available in the
[API of the Java `Pattern`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#sum).
