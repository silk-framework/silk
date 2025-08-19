This transformer takes _three_ inputs: a single _output value_, a sequence of _regular expressions_ and a sequence of _values to check_ against the regular expressions. It returns a sequence of copies of the output value exclusively at those positions where one of the regular expressions matches the input value.

In other words: It _selects_ the matches within the sequence of values against the regular expressions and 'marks' them with the provided output value.

As a further detail of its operation: If the parameter `oneOnly` of the transformer is set to `true`, then only the position of the _first_ matching regular expression will be marked with the output value. There won't be any further marked matches.