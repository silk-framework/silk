This transformer takes 3 inputs.
- The first input should have exactly one value that should be passed out again untouched.
- The second input has at least two Regex values - two in order to make sense.
- The third input should have exactly one value which is checked against the regexes.

The result of the transformer is a sequence with the same length of number of regexes.
For the output value (of the first input) is set to each position in this sequence where
the related regex also matched.

If `oneOnly` is true only the position of the **first** matching regex will be set to the output value.