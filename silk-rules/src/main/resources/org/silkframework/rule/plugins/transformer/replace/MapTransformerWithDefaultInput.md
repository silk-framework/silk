This transformer requires a _map of values_, when created. This can be a map such as `"A:1,B:2,C:3"`, representing the mapping between the first three letters and the corresponding numbers (i.e. `A` to `1`, `B` to `2` and `C` to `3`).

The transformer requires _two_ input value sequences, when called: the first sequence of values are the _values to map_, and the second is a sequence of _default values_.

With these parameterization and applied value sequences, the transformer then works in the following way:
* The _map of values_ (specified when the transformer is _created_) is used for _obtaining_ values from the transformer.
* The _values to map_ (specified when the transformer is _called_) is used for _mapping_ values by the transformer.
* The _default values_ (specified when the transformer is _called_, as a mandatory second argument) is used as a backup sequence of values, in case the (first) value to map is not found within the map of values. It is simply a default.

Normally, the sequence of _default values_ is expected to have the same size as the _values to map_ (i.e. the two sequences provided when _calling_ the transformer are supposed to be compatible). Additionally, in order to provide a certain amount of flexibility: Should that _not_ be the case, if there are _less_ default values than values to map, the _last_ default value is replicated to match the count. This fallback shouldn't be relied upon, since it may result in a somewhat confusing or unexpected behavior.