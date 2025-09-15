This transformer uses a regular expression as a matching condition, in order to distinguish which input to take.

For calling this transformer, we need to give it _two_ or _three_ values as its input.
The regular condition within the transformer is used as a matching condition on the first input.
If it matches, then the _second_ input is returned.
If it doesn't match, then the _third_ input is returned, provided it exists.
If the third input doesn't exist, an empty sequence is returned.

In other words: This transformation is a conditional expression based on the matching of a regular expression. As inputs, we provide the text to be matched as well as the two possible outcomes of the comparison.